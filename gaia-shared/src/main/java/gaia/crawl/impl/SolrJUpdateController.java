package gaia.crawl.impl;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.common.params.FieldMappingParams;
import gaia.crawl.UpdateController;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.FieldMappingUtil;
import gaia.update.FieldMappingRequest;
import gaia.utils.HttpClientSSLUtil;
import gaia.utils.RealmUtil;

public class SolrJUpdateController extends UpdateController {
	private static final transient Logger LOG = LoggerFactory.getLogger(SolrJUpdateController.class);
	private String dsId;
	private boolean hasFieldMapping;
	static int soTimeout = 600000;
	SolrServer server;
	int bufSize = 1;
	int threads = 2;
	String solrUrl;
	ArrayList<SolrInputDocument> buffer;
	Thread flushThread;
	private volatile long lastFlush;
	private Exception finishedFrom = null;
	private Exception startedFrom = null;

	public void init(DataSource ds) throws Exception {
		super.init(ds);
		if (ds != null) {
			dsId = ds.getDataSourceId().toString();
			String newSolrServer = ds.getString("output_args");
			if (newSolrServer == null) {
				if (solrUrl == null)
					throw new Exception("Configuration error: output_args is null, should be set to Solr collection URL.");
			} else {
				String[] args = newSolrServer.split(",");
				for (String s : args) {
					if (s.startsWith("http"))
						solrUrl = s;
					else if (s.startsWith("buffer="))
						try {
							bufSize = Integer.parseInt(s.substring(7));
							if (bufSize < 1)
								throw new NumberFormatException("should be 1 or higher");
						} catch (NumberFormatException nfe) {
							LOG.warn("Invalid buffer size in argument '" + s + "', assuming 1. (" + nfe.toString() + ")");
						}
					else if (s.startsWith("threads="))
						try {
							threads = Integer.parseInt(s.substring(8));
							if (threads < 1)
								throw new NumberFormatException("should be 1 or higher");
						} catch (NumberFormatException nfe) {
							LOG.warn("Invalid number of threads in argument '" + s + "', assuming 10. (" + nfe.toString() + ")");
						}
					else {
						LOG.warn("Unrecognized parameter '" + s + "' in " + "output_args" + ", skipping...");
					}
				}
				if (solrUrl == null) {
					throw new Exception(
							"Configuration error: output_args should include the Solr collection URL starting with 'http://' or 'https://'.");
				}
			}
		}
		hasFieldMapping = ((ds != null) && (ds.getFieldMapping() != null));
	}

	public void start() throws Exception {
		if (isStarted()) {
			LOG.error("Duplicate start() - application error", new Exception("dupe start"));
			LOG.error("But already started from", startedFrom);
		}
		startedFrom = new Exception("first start");
		finishedFrom = null;
		HttpSolrServer httpServer = new HttpSolrServer(solrUrl);

		httpServer.setSoTimeout(soTimeout);

		RealmUtil.prepareClient((AbstractHttpClient) httpServer.getHttpClient());
		HttpClientSSLUtil.prepareClient((AbstractHttpClient) httpServer.getHttpClient());
		if (threads > 1)
			server = new ConcurrentUpdateSolrServer(solrUrl, httpServer.getHttpClient(), 10 * threads, threads);
		else {
			server = httpServer;
		}
		buffer = new ArrayList<SolrInputDocument>(bufSize);
		if (bufSize > 1) {
			if ((flushThread != null) && (flushThread.isAlive())) {
				flushThread.interrupt();
			}
			flushThread = new FlushThread();
			flushThread.start();
		}
		if (hasFieldMapping) {
			FieldMapping map = new FieldMapping();
			map.setFrom(ds.getFieldMapping(), true);
			if (map.isAddGaiaSearchFields()) {
				FieldMappingUtil.addGaiaSearchFields(map, ds);
			}
			FieldMappingRequest req = new FieldMappingRequest("/fmap");
			req.setAction(FieldMappingParams.Action.DEFINE);

			req.setParam("fm.chain", "gaia-update-chain");
			req.defineMapping(map, dsId);
			Exception lastException = null;
			boolean success = false;
			int loop = 0;
			while ((!success) && (loop++ < 3)) {
				try {
					SolrResponse rsp = req.process(server);
					String res = String.valueOf(rsp.getResponse().get(FieldMappingParams.Action.DEFINE.toString()));
					if ((res == null) || (!res.startsWith("ok:"))) {
						LOG.warn("Failed to update mapping for ds=" + dsId + " (" + rsp.getResponse() + "), will re-try, attempt "
								+ loop);

						success = false;
					} else {
						success = true;
					}
				} catch (Exception e) {
					lastException = e;
					String msg = e.getMessage().replaceAll("[\\n\\r\\t]+", " - ");
					LOG.warn("Failed to update mapping for ds=" + dsId + ": " + msg + ", will re-try, attempt " + loop);
					success = false;
				}
				if (!success)
					try {
						Thread.sleep(10000L);
					} catch (InterruptedException e) {
					}
			}
			if (!success) {
				String msg = "Failed to update mapping for ds=" + dsId + ", giving up after attempt " + loop;
				if (lastException != null) {
					msg = msg + ": " + lastException.getMessage().replaceAll("[\\n\\r\\t]+", " - ");
				}
				LOG.warn(msg);
				try {
					server.shutdown();
				} catch (Exception e) {
				}
				if ((flushThread != null) && (flushThread.isAlive())) {
					flushThread.interrupt();
				}
				server = null;
				throw new Exception(msg);
			}
			LOG.info("Sent field mapping for ds=" + dsId + " to " + solrUrl);
		}

		super.start();
		LOG.trace("start, server=" + server);
	}

	private void ensureStarted() throws IOException {
		if (!isStarted()) {
			throw new IOException(this + " not started - application error, dsId=" + dsId);
		}
		finishedFrom = null;
	}

	public synchronized void finish(boolean commit) throws IOException {
		if (finishedFrom != null) {
			LOG.error("Already finished from: ", finishedFrom);
			LOG.error("Now finishing from: ", new Exception("dupe " + dsId + " finish(" + commit + ")"));
		}
		ensureStarted();

		if ((flushThread != null) && (flushThread.isAlive())) {
			flushThread.interrupt();
		}
		flush();
		LOG.trace("finish(" + commit + ")");
		super.finish(commit);
		finishedFrom = new Exception(dsId + " finished(" + commit + ")");
		if ((server instanceof ConcurrentUpdateSolrServer)) {
			((ConcurrentUpdateSolrServer) server).blockUntilFinished();
		}

		if (hasFieldMapping) {
			try {
				FieldMappingRequest req = new FieldMappingRequest("/fmap");

				req.setParam("fm.chain", "gaia-update-chain");
				req.deleteMapping(dsId);
				req.process(server);
			} catch (Exception t) {
				LOG.warn("Failed to delete field mappings for ds=" + dsId + ": " + t.getMessage());
			}
		}
		LOG.debug("Closing client to " + solrUrl + ", dsId=" + dsId);
		server.shutdown();
		server = null;
	}

	public void add(SolrInputDocument doc) throws IOException {
		ensureStarted();
		synchronized (buffer) {
			if (buffer.size() >= bufSize) {
				flush();
			}
			buffer.add(doc);
		}
	}

	private void flush() throws IOException {
		lastFlush = System.currentTimeMillis();
		if (buffer.isEmpty())
			return;
		try {
			UpdateRequest req = new UpdateRequest();
			req.add(buffer);
			req.setParam("update.chain", "gaia-update-chain");
			if (hasFieldMapping) {
				req.setParam("fm.ds", dsId);
			}
			if (isUseCommitWithin()) {
				req.setCommitWithin(getCommitWithin());
			}
			req.process(server);
			LOG.trace("{} Added " + buffer.size() + " documents: {}", server, buffer);
			numAdded.addAndGet(buffer.size());
			needCommit = true;
			buffer.clear();
		} catch (SolrServerException e) {
			processThrowable("Add failed", e);
		}
	}

	public void deleteByQuery(String query) throws IOException {
		ensureStarted();
		try {
			server.deleteByQuery(query);
			numDeleted.incrementAndGet();
			needCommit = true;
		} catch (SolrServerException e) {
			processThrowable("Delete failed", e);
		}
	}

	public void delete(String id) throws IOException {
		ensureStarted();
		try {
			server.deleteById(id);
			numDeleted.incrementAndGet();
			needCommit = true;
		} catch (SolrServerException e) {
			processThrowable("Delete failed", e);
		}
	}

	public void commit() throws IOException {
		ensureStarted();
		flush();
		LOG.trace("Commit for " + numAdded + " documents added and " + numDeleted + " documents deleted");

		if (needCommit) {
			LOG.trace("Committing...");
			try {
				server.commit();
				needCommit = false;
			} catch (Throwable e) {
				processThrowable("Commit failed", e);
			}
		}
	}

	private void processThrowable(String op, Throwable t) throws IOException {
		LOG.warn(op, t);
		if (server != null) {
			LOG.warn("Closing client to " + solrUrl + " after exception, dsId=" + dsId);
			if ((server instanceof ConcurrentUpdateSolrServer)) {
				((ConcurrentUpdateSolrServer) server).blockUntilFinished();
			}
			server.shutdown();
			server = null;
		}
		throw new IOException(op + ": " + t.getMessage());
	}

	private class FlushThread extends Thread {
		public FlushThread() {
			setDaemon(true);
		}

		public void run() {
			while (!isInterrupted()) {
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException ie) {
					break;
				}
				if (System.currentTimeMillis() - lastFlush > 5000L)
					try {
						flush();
					} catch (Exception e) {
						LOG.warn("Exception flushing the buffer: " + e);
					}
			}
		}
	}
}
