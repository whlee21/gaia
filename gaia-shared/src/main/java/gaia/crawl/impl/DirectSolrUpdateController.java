package gaia.crawl.impl;

import java.io.IOException;
import java.util.Collections;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerUtils;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.Defaults;
import gaia.common.params.FieldMappingParams;
import gaia.crawl.UpdateController;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.FieldMappingUtil;
import gaia.handler.FieldMappingRequestHandler;

public class DirectSolrUpdateController extends UpdateController {
	private static transient Logger LOG = LoggerFactory.getLogger(DirectSolrUpdateController.class);
	private String collection;
	private CoreContainer cores;
	private ModifiableSolrParams params;

	public DirectSolrUpdateController() {
		cores = ((CoreContainer) Defaults.injector.getInstance(CoreContainer.class));
		params = new ModifiableSolrParams();
		params.set("commit", new String[] { "true" });
		params.set("waitSearcher", new String[] { "true" });
	}

	public void init(DataSource ds) throws Exception {
		super.init(ds);
		if (ds != null) {
			collection = ds.getCollection();
			params.set("fm.ds", new String[] { ds.getDataSourceId().toString() });
		}
	}

	private UpdateRequestProcessor getUpdateProcessor(SolrCore core) {
		LocalSolrQueryRequest request = null;
		try {
			UpdateRequestProcessorChain chain = core.getUpdateProcessingChain("gaia-update-chain");

			request = new LocalSolrQueryRequest(core, params);
			SolrQueryResponse response = new SolrQueryResponse();

			UpdateRequestProcessor processor = chain.createProcessor(request, response);

			return processor;
		} finally {
			if (request != null)
				request.close();
		}
	}

	private SolrRequestHandler getFieldMappingHandler(SolrCore core) {
		if (core == null) {
			return null;
		}
		for (SolrRequestHandler h : core.getRequestHandlers().values()) {
			if ((h instanceof FieldMappingRequestHandler)) {
				return h;
			}
		}
		return null;
	}

	public void start() throws Exception {
		super.start();

		SolrCore core = cores.getCore(collection);
		boolean updated = false;
		try {
			FieldMapping map = new FieldMapping();
			FieldMapping dsMap = ds.getFieldMapping();
			map.setFrom(dsMap, true);
			if (map.isVerifySchema()) {
				FieldMappingUtil.verifySchema(map, core);
			}
			if (map.isAddGaiaSearchFields()) {
				FieldMappingUtil.addGaiaSearchFields(map, ds);
			}
			String dsId = ds.getDataSourceId().toString();
			String json = FieldMappingUtil.toJSON(map);
			ModifiableSolrParams params = new ModifiableSolrParams();
			params.set("fm.action", new String[] { FieldMappingParams.Action.DEFINE.toString() });
			params.set("fm.chain", new String[] { "gaia-update-chain" });
			params.set("fm.ds", new String[] { dsId });
			LocalSolrQueryRequest req = new LocalSolrQueryRequest(core, params);
			ContentStreamBase.StringStream ss = new ContentStreamBase.StringStream(json);
			ss.setName(dsId);
			req.setContentStreams(Collections.singletonList((ContentStream) ss));
			SolrRequestHandler h = getFieldMappingHandler(core);
			if (h == null) {
				throw new Exception("Can't find " + FieldMappingRequestHandler.class.getName()
						+ " in Solr handlers - configuration error!");
			}
			SolrQueryResponse rsp = new SolrQueryResponse();
			h.handleRequest(req, rsp);
			String res = String.valueOf(rsp.getValues().get(FieldMappingParams.Action.DEFINE.toString()));
			if ((res == null) || (!res.startsWith("ok:"))) {
				LOG.warn("Failed to update mapping for ds=" + dsId + " (" + rsp.getValues() + ")");
			} else {
				LOG.info("Successfully updated mapping for ds=" + dsId);
				updated = true;
			}
		} finally {
			if (core != null) {
				core.close();
			}
		}
		if (!updated)
			LOG.warn("Failed to update mappings - field mapping processor not in chain?");
	}

	protected AddUpdateCommand newAddUpdateCommand(SolrQueryRequest req, SolrInputDocument doc) {
		return newAddUpdateCommand(req, doc, true);
	}

	protected AddUpdateCommand newAddUpdateCommand(SolrQueryRequest req, SolrInputDocument doc, boolean overwrite) {
		AddUpdateCommand cmd = new AddUpdateCommand(req);
		cmd.clear();
		cmd.solrDoc = doc;
		cmd.overwrite = overwrite;
		if (isUseCommitWithin()) {
			cmd.commitWithin = getCommitWithin();
		}
		return cmd;
	}

	public void add(SolrInputDocument doc, String id) throws IOException {
		doAdd(doc);

		LOG.debug("Added document: " + id);
	}

	public void add(SolrInputDocument doc) throws IOException {
		if (!isStarted()) {
			throw new IOException("not started");
		}
		doAdd(doc);

		LOG.trace("Added document: " + doc.toString());
	}

	private void doAdd(SolrInputDocument doc) throws IOException {
		SolrQueryRequest req = null;
		UpdateRequestProcessor processor = null;
		SolrCore core = cores.getCore(collection);
		try {
			if (core == null) {
				LOG.error("SolrCore not found:" + collection + " cores:" + cores.getCoreNames());
				throw new IllegalStateException("SolrCore not found:" + collection);
			}
			req = new LocalSolrQueryRequest(core, params);
			AddUpdateCommand cmd = newAddUpdateCommand(req, doc);
			processor = getUpdateProcessor(core);
			processor.processAdd(cmd);
		} finally {
			if (processor != null) {
				processor.finish();
			}
			if (req != null) {
				req.close();
			}
			if (core != null) {
				core.close();
			}
		}

		numAdded.incrementAndGet();
		needCommit = true;
	}

	public void deleteByQuery(String query) throws IOException {
		SolrQueryRequest req = null;
		SolrCore core = cores.getCore(collection);
		try {
			if (core == null) {
				LOG.error("SolrCore not found:" + collection + " cores:" + cores.getCoreNames());
				throw new IllegalStateException("SolrCore not found:" + collection);
			}
			req = new LocalSolrQueryRequest(core, params);
			DeleteUpdateCommand deleteCmd = new DeleteUpdateCommand(req);
			deleteCmd.query = query;
			try {
				getUpdateProcessor(core).processDelete(deleteCmd);
			} catch (IOException e) {
				LOG.error("Exception", e);
			}
		} finally {
			if (req != null) {
				req.close();
			}
			if (core != null) {
				core.close();
			}
		}
		numDeleted.incrementAndGet();
		needCommit = true;
	}

	public void delete(String id) throws IOException {
		SolrQueryRequest req = null;
		SolrCore core = cores.getCore(collection);
		try {
			if (core == null) {
				LOG.error("SolrCore not found:" + collection + " cores:" + cores.getCoreNames());
				throw new IllegalStateException("SolrCore not found:" + collection);
			}
			req = new LocalSolrQueryRequest(core, params);
			DeleteUpdateCommand deleteCmd = new DeleteUpdateCommand(req);

			IndexSchema schema = core.getLatestSchema();
			SchemaField crawlURI = schema.getField("crawl_uri");
			if (crawlURI != null) {
				deleteCmd.query = (crawlURI.getName() + ":" + ClientUtils.escapeQueryChars(id));
			} else {
				deleteCmd.id = id;
			}
			try {
				getUpdateProcessor(core).processDelete(deleteCmd);
			} catch (IOException e) {
				LOG.error("Exception", e);
			}
		} finally {
			if (req != null) {
				req.close();
			}
			if (core != null) {
				core.close();
			}
		}
		numDeleted.incrementAndGet();
		needCommit = true;
	}

	public void commit() throws IOException {
		if (needCommit) {
			LOG.info("Commit for " + numAdded + " documents added and " + numDeleted + " documents deleted");
			SolrQueryRequest req = null;
			SolrCore core = cores.getCore(collection);
			try {
				if (core == null) {
					LOG.error("SolrCore not found:" + collection + " cores:" + cores.getCoreNames());
				}
				req = new LocalSolrQueryRequest(core, params);
				RequestHandlerUtils.handleCommit(req, getUpdateProcessor(core), params, false);
			} finally {
				if (req != null) {
					req.close();
				}
				if (core != null) {
					core.close();
				}
			}

			LOG.info("Commit completed");
			needCommit = false;
		} else {
			LOG.info("Commit not needed");
		}
	}
}
