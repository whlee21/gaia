package gaia.crawl.twitter.stream;

import gaia.crawl.CrawlStatus;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.io.Content;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.solr.common.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.FilterQuery;
import twitter4j.Place;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterStreamCrawler implements Runnable {
	private static transient Logger LOG = LoggerFactory.getLogger(TwitterStreamCrawler.class);
	private boolean stopped = false;
	private TwitterStreamCrawlState state;
	private DataSource dataSource;
	protected TwitterStreamFactory streamFactory;
	private long maxDocs = -1L;
	private int sleep = 10000;
	private FilterQuery filter;

	public TwitterStreamCrawler(TwitterStreamCrawlState state) {
		this.state = state;
		dataSource = state.getDataSource();

		sleep = dataSource.getInt("sleep", sleep);
		maxDocs = dataSource.getLong("max_docs", -1L);

		String consumerKey = dataSource.getString("consumer_key");
		String consumerSecret = dataSource.getString("consumer_secret");
		String accessToken = dataSource.getString("access_token");
		String tokenSecret = dataSource.getString("token_secret");

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey(consumerKey.trim()).setOAuthConsumerSecret(consumerSecret.trim())
				.setOAuthAccessToken(accessToken.trim()).setOAuthAccessTokenSecret(tokenSecret.trim());

		streamFactory = new TwitterStreamFactory(cb.build());

		List follow = (List) dataSource.getProperty("filter_follow");
		if ((follow == null) || (follow.isEmpty())) {
			follow = null;
		}
		List track = (List) dataSource.getProperty("filter_track");
		if ((track == null) || (track.isEmpty())) {
			track = null;
		}
		double[][] locations = TwitterAccessSpec.parseLocations((List) dataSource.getProperty("filter_locations"));

		if ((locations == null) || (locations.length == 0)) {
			locations = (double[][]) null;
		}
		if ((follow != null) || (track != null) || (locations != null)) {
			filter = new FilterQuery();
			if ((follow != null) && (!follow.isEmpty())) {
				long[] followIds = new long[follow.size()];
				for (int i = 0; i < follow.size(); i++) {
					Object obj = follow.get(i);
					followIds[i] = new Long(obj.toString()).longValue();
				}
				filter.follow(followIds);
			}
			if (track != null)
				filter.track((String[]) track.toArray(new String[0]));
			if (locations != null)
				filter.locations(locations);
		}
	}

	public void run() {
		state.getStatus().starting();
		try {
			state.getProcessor().start();
			long numDocs = listen();
		} catch (Throwable t) {
			LOG.warn("Exception in Twitter stream", t);
			state.getStatus().failed(t);
		} finally {
			boolean commit = dataSource.getBoolean("commit_on_finish", true);
			try {
				state.getProcessor().finish();

				state.getProcessor().getUpdateController().finish(commit);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (stopped)
				state.getStatus().end(CrawlStatus.JobState.STOPPED);
			else
				state.getStatus().end(CrawlStatus.JobState.FINISHED);
		}
	}

	private long listen() {
		TwitterStream tStream = streamFactory.getInstance();
		IndexingStatusListener listener = new IndexingStatusListener();
		tStream.addListener(listener);
		state.getStatus().running();
		if (filter != null)
			tStream.filter(filter);
		else {
			tStream.sample();
		}
		while ((!listener.completed) && ((maxDocs == -1L) || (listener.numDocs.longValue() < maxDocs)) && (!stopped))
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
			}
		LOG.info("Listener status: completed={}; numDocs={}", Boolean.valueOf(listener.completed),
				Long.valueOf(listener.numDocs.longValue()));
		long result = listener.numDocs.longValue();
		tStream.shutdown();
		tStream.cleanUp();
		return result;
	}

	public synchronized void stop() {
		stopped = true;
	}

	class IndexingStatusListener implements StatusListener {
		private AtomicLong numDocs = new AtomicLong(0L);
		private boolean completed = false;

		IndexingStatusListener() {
		}

		public void onStatus(Status status) {
			if ((maxDocs == -1L) || (numDocs.intValue() < maxDocs)) {
				Content content = new Content();
				content.setKey(String.valueOf(status.getId()));

				User user = status.getUser();

				content.addMetadata("userName", user.getName());
				content.addMetadata("userId", String.valueOf(user.getId()));
				content.addMetadata("userScreenName", user.getScreenName());
				content.addMetadata("userLang", user.getLang());
				content.addMetadata("userLocation", user.getLocation());

				content.addMetadata("source", status.getSource());
				if (status.getGeoLocation() != null) {
					content.addMetadata("location", status.getGeoLocation().getLatitude() + ","
							+ status.getGeoLocation().getLongitude());
				}
				content.addMetadata("createdAt", DateUtil.getThreadLocalDateFormat().format(status.getCreatedAt()));
				content.addMetadata("retweetCount", String.valueOf(status.getRetweetCount()));

				Place place = status.getPlace();
				if (place != null) {
					content.addMetadata("placeFullName", place.getFullName());
					content.addMetadata("placeCountry", place.getCountry());
					content.addMetadata("placeAddress", place.getStreetAddress());
					content.addMetadata("placeType", place.getPlaceType());
					content.addMetadata("placeCountry", place.getCountryCode());
					content.addMetadata("placeURL", place.getURL());
					content.addMetadata("placeId", place.getId());
					content.addMetadata("placeName", place.getName());
				}

				content.addMetadata("text", status.getText());

				content.setData(status.getText().getBytes(Charset.forName("UTF-8")));
				content.addMetadata("Content-type", "text/plain");
				try {
					state.getProcessor().process(content);
				} catch (Exception e) {
					state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
				}
				numDocs.incrementAndGet();
			}
		}

		public void onScrubGeo(long userId, long upToStatusId) {
			LOG.info("Scrub Geo: userId:" + userId + " upToStatusId: " + upToStatusId);
		}

		public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
			LOG.info("Deleting: " + statusDeletionNotice);
			try {
				state.getProcessor().delete(String.valueOf(statusDeletionNotice.getStatusId()));

				state.getStatus().incrementCounter(CrawlStatus.Counter.Deleted);
			} catch (Exception e) {
				LOG.error("Exception", e);
				state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
			}
		}

		public void onTrackLimitationNotice(int i) {
			LOG.info("Track Limitation: " + i);
		}

		public void onException(Exception e) {
			state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
		}

		@Override
		public void onStallWarning(StallWarning arg0) {
			// TODO Auto-generated method stub

		}
	}
}
