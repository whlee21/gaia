package gaia.crawl.fs;

import gaia.Defaults;
import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceAPI;
import gaia.crawl.datasource.DataSourceUtils;
import gaia.crawl.fs.ds.AuthException;
import gaia.crawl.fs.ds.FSSpec;
import gaia.crawl.io.Content;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.apache.tika.io.TikaInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FSCrawler {
	private static final Logger LOG = LoggerFactory.getLogger(FSCrawler.class);
	DataSource fsds;
	FSSpec spec;
	FS fs = null;
	CrawlProcessor processor;
	boolean addDirsAsDocs = false;
	int maxDepth;
	long maxSize;
	DataSourceAPI.Bounds bounds;
	FSCrawlState state;
	Boundaries boundaries = null;
	boolean stopRequested = false;
	boolean abortRequested = false;
	boolean running = false;
	int crawlItemTimeout;
	ThreadPoolExecutor pool;
	ExecutorCompletionService<String> executor;
	BlockingQueue<String> queue;
	ScheduledExecutorService terminator;
	private AtomicInteger documentsCounter;

	public FSCrawler(DataSource ds, CrawlProcessor processor, FSCrawlState state) {
		this.fsds = ds;
		this.processor = processor;
		this.state = state;
		spec = ((FSFactory) state.getCrawlerController().getDataSourceFactory()).getSpec(ds);
		maxDepth = DataSourceUtils.getCrawlDepth(ds);
		if (maxDepth < 0) {
			maxDepth = Integer.MAX_VALUE;
		}
		maxSize = DataSourceUtils.getMaxBytes(ds);
		if (maxSize < 0L) {
			maxSize = 104857600L;
			LOG.info("Maximum content size unlimited (max_bytes==-1), limiting to " + maxSize);
			DataSourceUtils.setMaxBytes(ds, maxSize);
		}

		addDirsAsDocs = DataSourceUtils.getIndexDirectories(ds);

		bounds = DataSourceUtils.getCrawlBounds(ds);

		Integer terminatorTimeOut = Integer.valueOf(fsds.getInt(FSSpec.CRAWL_ITEM_TIMEOUT));

		if (terminatorTimeOut == null) {
			crawlItemTimeout = Defaults.INSTANCE.getInt(Defaults.Group.crawlers, "crawl.item.timeout",
					Integer.valueOf(600000));
		} else {
			crawlItemTimeout = terminatorTimeOut.intValue();
		}

		documentsCounter = new AtomicInteger(0);
	}

	public void stop() {
		stopRequested = true;
		state.getStatus().setState(CrawlStatus.JobState.STOPPING);
	}

	public void abort() {
		abortRequested = true;
		state.getStatus().setState(CrawlStatus.JobState.ABORTING);
	}

	public boolean isRunning() {
		return running;
	}

	private void initializeExecutors() {
		int numThreads = fsds.getInt("max_threads");
		if (numThreads < 1)
			numThreads = 1;
		int queueSize = 50 * numThreads;
		pool = ((ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads));
		queue = new ArrayBlockingQueue<String>(queueSize);
		executor = new ExecutorCompletionService(pool, queue);
		terminator = Executors.newScheduledThreadPool(numThreads);
	}

	private Future<String> submitTask(FSTask task) {
		Future<String> f = executor.submit(task);
		return f;
	}

	private void fail(Throwable t) {
		state.getStatus().failed(t);
		running = false;
		if (fs != null)
			fs.close();
	}

	public void crawl() {
		running = true;
		state.getStatus().running();
		try {
			fs = ((FSFactory) state.getCrawlerController().getDataSourceFactory()).createFS(fsds);
			fs.init(state);
		} catch (Throwable t) {
			LOG.error(CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(),
					(String) state.getDataSource().getProperty("url"), t.getMessage()), t);

			fail(t);
			return;
		}
		try {
			processor.start();
		} catch (Throwable e1) {
			LOG.error(CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(),
					(String) state.getDataSource().getProperty("url"), "initializing crawl processor, cause:" + e1), e1);

			fail(e1);
			return;
		}
		FSObject fso = null;
		try {
			fso = fs.get(DataSourceUtils.getSource(fsds));
		} catch (Throwable t) {
			LOG.error(CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(),
					(String) state.getDataSource().getProperty("url"), t.getMessage()));

			LOG.debug("Error: ", t);
			fail(t);
			return;
		}
		if (fso != null) {
			boundaries = new Boundaries(fso.getUri(), DataSourceUtils.getIncludePattern(fsds),
					DataSourceUtils.getExcludePattern(fsds), DataSourceUtils.getCrawlBounds(fsds));
			try {
				initializeExecutors();
				FSTask it = new FSTask(fso, 0);
				Future f = submitTask(it);
				it.setFuture(f);
				while (true) {
					try {
						if ((executor.poll(1000L, TimeUnit.MILLISECONDS) == null) && (pool.getActiveCount() == 0))
							break;
					} catch (InterruptedException e) {
						LOG.debug("Error on while", e);
					}

				}

				LOG.debug("number of documents -> " + documentsCounter.get());
			} finally {
				pool.shutdownNow();
			}
		}
		fs.close();
		running = false;
		try {
			CrawlStatus.JobState endState = null;
			if (abortRequested) {
				endState = CrawlStatus.JobState.ABORTED;
			} else if (stopRequested) {
				endState = CrawlStatus.JobState.STOPPED;
			} else {
				endState = CrawlStatus.JobState.FINISHED;
				state.getStatus().setState(CrawlStatus.JobState.FINISHING);
			}

			if (fsds.getBoolean("remove_old_docs", true)) {
				String batchId = processor.getBatchId();
				if (batchId != null) {
					String query = fsds.getFieldMapping().getDatasourceField() + ":" + fsds.getDataSourceId()
							+ " AND NOT batch_id:" + batchId;
					processor.getUpdateController().deleteByQuery(query);
				}
			}

			boolean commit = fsds.getBoolean("commit_on_finish", true);
			if (endState != CrawlStatus.JobState.ABORTED)
				try {
					processor.getUpdateController().commit();
					state.getStatus().end(endState);
				} catch (IOException ioe) {
					LOG.warn("Exception when finishing crawl", ioe);
					state.getStatus().failed(ioe);
				}
			else {
				state.getStatus().end(endState);
			}
			processor.finish();
		} catch (Throwable e) {
			LOG.warn("Exception when finishing crawl", e);
			state.getStatus().failed(e);
		}
	}

	class FSTask implements Callable<String> {
		FSObject fso;
		int depth;
		Future<String> f;
		CountDownLatch cdl;

		public FSTask(FSObject fso, int depth) {
			LOG.debug(new StringBuilder().append("create new FSTask -> ").append(fso.getUri())
					.append(" | with depth --> ").append(depth).toString());

			documentsCounter.addAndGet(1);

			this.fso = fso;
			this.depth = depth;
			cdl = new CountDownLatch(1);
		}

		public void setFuture(Future<String> f) {
			this.f = f;

			cdl.countDown();
		}

		public void schedule() {
			if (f != null)
				try {
					terminator.schedule(new Runnable() {
						public void run() {
							LOG.debug("Cancel FSTask -> " + getFSObject().getUri());

							f.cancel(true);
						}
					}, crawlItemTimeout, TimeUnit.MILLISECONDS);
				} catch (Throwable e) {
					LOG.error("Error: ", e);
				}
			else
				LOG.debug("FSTask -> future null");
		}

		public FSObject getFSObject() {
			return fso;
		}

		public String call() throws Exception {
			LOG.debug(new StringBuilder().append("thread ").append(Thread.currentThread().toString())
					.append(", task ").append(fso.getUri()).toString());
			try {
				cdl.await();
			} catch (InterruptedException e) {
				LOG.warn(e.getMessage());
			}

			schedule();

			if (depth > maxDepth) {
				return "";
			}
			if ((stopRequested) || (abortRequested)) {
				return "";
			}
			if (!boundaries.inDomain(fso.getUri())) {
				LOG.debug(new StringBuilder().append("Skip: ").append(fso.getUri()).toString());
				state.getStatus().incrementCounter(CrawlStatus.Counter.Filter_Denied);
				return "";
			}
			try {
				if (fso.isDirectory()) {
					if (addDirsAsDocs) {
						addDoc(fso);
					}

					long initTimeKids = System.currentTimeMillis();

					Iterable<FSObject> kids = fso.getChildren();
					if (kids != null) {
						for (FSObject f : kids) {
							if ((!f.getUri().equals(fso.getUri())) || (f.getSize() != 0L)) {
								if ((stopRequested) || (abortRequested)) {
									return "";
								}
								try {
									FSTask it = new FSTask(f, depth + 1);
									Future<String> future = submitTask(it);
									it.setFuture(future);
								} catch (Throwable ioe) {
									LOG.debug("Error: ", ioe);
									LOG.error(CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(),
											state.getDataSource(), f.getUri(), ioe.getMessage()));
								}
							}

						}

					}

					long endTimeKids = System.currentTimeMillis();
					LOG.debug(new StringBuilder().append(fso.getUri()).append(" = time children -> ")
							.append(endTimeKids - initTimeKids).toString());
				} else {
					addDoc(fso);
				}
			} catch (Throwable e) {
				LOG.debug("Error: ", e);
			} finally {
				fso.dispose();
			}
			return "";
		}

		private void addDoc(FSObject fso) {
			if ((fso.getDocuments() != null) && (fso.getDocuments().size() > 0)) {
				for (SolrInputDocument doc : fso.getDocuments()) {
					try {
						processor.processParsedDocument(doc);
					} catch (Exception e) {
						state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
						LOG.error(CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(),
								fso.getUri(), e.getMessage()));

						LOG.debug("Error: ", e);
					}
				}
				return;
			}
			Content c = new Content();
			boolean failed = false;
			StringBuilder sb = new StringBuilder();
			Date date = new Date(fso.getLastModified());
			try {
				DateUtil.formatDate(date, null, sb);
			} catch (IOException ioe) {
				LOG.warn("Cannot format date", ioe);
				sb.setLength(0);
				sb.append(date.toString());
			}
			c.setKey(fso.getUri());
			c.addMetadata("Last-Modified", sb.toString());
			sb.setLength(0);
			date = new Date();
			try {
				DateUtil.formatDate(date, null, sb);
			} catch (IOException ioe) {
				LOG.warn("Cannot format date", ioe);
				sb.setLength(0);
				sb.append(date.toString());
			}
			c.addMetadata("fetch_time", sb.toString());

			for (String acl : fso.getAcls()) {
				c.addMetadata("acl", acl);
			}

			c.addMetadata("owner", fso.getOwner());
			c.addMetadata("group", fso.getGroup());
			c.addMetadata("Content-Length", String.valueOf(fso.getSize()));
			c.addMetadata("directory", String.valueOf(fso.isDirectory()));
			if (fso.isDirectory()) {
				SolrInputDocument doc = new SolrInputDocument();
				Content.fill(doc, fsds.getFieldMapping(), c);
				doc.addField("fetching", "ok");
				doc.addField("crawl_uri", c.getKey());
				doc.addField("batch_id", processor.getBatchId());
				try {
					processor.processParsedDocument(doc);
					LOG.info(new StringBuilder().append("New dir: ").append(fso.getUri()).toString());
				} catch (Throwable e) {
					state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
					LOG.warn("Failed to add directory as doc", e);
				}
				return;
			}

			long initTimeAddDoc = System.currentTimeMillis();

			StringBuilder msg = new StringBuilder(new StringBuilder().append("New doc: ").append(fso.getUri()).toString());

			boolean truncated = false;
			if (fso.getSize() > maxSize) {
				c.addMetadata("fetching",
						new StringBuilder().append("skipped: size ").append(fso.getSize()).append(" > max_bytes ").append(maxSize)
								.toString());
				msg.append(new StringBuilder().append(", truncated ").append(fso.getSize()).append(" > ").append(maxSize)
						.toString());
				c.setData(new byte[0]);
				truncated = true;
			}

			if ((fso.getSize() > 0L) && (!truncated)) {
				InputStream is = null;
				try {
					is = fso.open();

					LOG.debug(new StringBuilder().append("size file -> ").append(fso.getUri()).append(" = ")
							.append(fso.getSize()).toString());

					int size = (int) Math.min(maxSize, fso.getSize());
					byte[] data = new byte[size];
					int cnt = 0;
					int total = 0;
					String fetchMsg = "ok";
					try {
						while ((cnt = is.read(data, total, size - total)) > 0) {
							total += cnt;
							if (total > size)
								fetchMsg = "partial content - size limit";
						}
					} catch (Throwable ie) {
						fetchMsg = new StringBuilder().append("partial content - exception: ").append(ie.getMessage()).toString();

						LOG.debug("Error: ", ie);

						throw ie;
					}
					if (total < size) {
						byte[] newdata = new byte[total];
						System.arraycopy(data, 0, newdata, 0, total);
						data = newdata;
					}

					c.setData(data);

					SolrInputDocument doc = FsParserController.parse(TikaInputStream.get(data), c, processor.getBatchId(),
							fsds.getFieldMapping());

					if (doc != null) {
						LOG.debug("enter to pre tika");
						try {
							processor.processParsedDocument(doc);
						} catch (Exception e) {
							LOG.warn("Exception adding document", e);
						}

						return;
					}

					c.addMetadata("fetching", fetchMsg);

					data = null;
				} catch (Throwable e) {
					LOG.error(CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(),
							fso.getUri(), e.getMessage()));

					LOG.debug("Error: ", e);

					if ((e instanceof AuthException))
						state.getStatus().incrementCounter(CrawlStatus.Counter.Access_Denied);
					else {
						state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
					}

					c.addMetadata("fetching", new StringBuilder().append("failed: ").append(e.getMessage()).toString());
					c.setData(null);
					msg.append(new StringBuilder().append(", fetch failed: ").append(e.getMessage()).toString());
					failed = true;
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (Exception e1) {
							LOG.debug("Error closing input", e1);
							msg.append(new StringBuilder().append(", error closing input: ").append(e1.getMessage()).toString());
						}
					}
				}
			}
			LOG.info(msg.toString());
			finishDoc(c, failed);
			c.setData(null);
			c = null;

			long endTimeAddDoc = System.currentTimeMillis();

			LOG.debug(new StringBuilder().append(fso.getUri()).append(" = time add docs -> ")
					.append(endTimeAddDoc - initTimeAddDoc).toString());
		}

		private void finishDoc(Content c, boolean failed) {
			try {
				processor.process(c);
			} catch (Throwable t) {
				LOG.error(
						CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(), c.getKey(),
								t.toString()), t);

				state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
			}
		}
	}

	static class Boundaries {
		List<Pattern> includes;
		List<Pattern> excludes;
		String root;

		public Boundaries(String rootUri, List<String> inc, List<String> exc, DataSourceAPI.Bounds bounds) {
			root = rootUri;
			int pos = root.indexOf("://");
			String scheme = "";
			if (pos != -1) {
				scheme = root.substring(0, pos + 3);
			}
			if ((inc == null) || (inc.isEmpty()))
				includes = new ArrayList();
			else {
				includes = createPatterns(inc, scheme);
			}
			if ((exc == null) || (exc.isEmpty()))
				excludes = new ArrayList();
			else {
				excludes = createPatterns(exc, scheme);
			}
			if (bounds == DataSourceAPI.Bounds.tree)
				excludes.add(Pattern.compile("(?!\\Q" + rootUri + "\\E).*"));
		}

		private List<Pattern> createPatterns(List<String> list, String scheme) {
			List res = new ArrayList();
			for (String s : list) {
				try {
					if (!s.startsWith(scheme)) {
						s = scheme + ".*" + s;
					}
					res.add(Pattern.compile(s));
				} catch (Exception e) {
					LOG.warn("Can't build an include/exclude pattern " + s, e);
				}
			}
			return res;
		}

		boolean inDomain(String uri) {
			if (root.equals(uri)) {
				return true;
			}
			boolean inDomain = includes.isEmpty();
			for (Pattern p : includes) {
				Matcher m = p.matcher(uri);
				if (m.matches()) {
					inDomain = true;
					break;
				}
			}
			if (!inDomain) {
				return false;
			}
			for (Pattern p : excludes) {
				Matcher m = p.matcher(uri);
				if (m.matches()) {
					return false;
				}
			}
			return true;
		}
	}
}
