package gaia.api;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSolrConfig;
import gaia.crawl.DataSourceManager;
import gaia.crawl.datasource.DataSource;
import gaia.handler.FieldMappingRequestHandler;
import gaia.update.FieldMappingUpdateProcessorFactory;

public class APIUtils {
	private static Logger LOG = LoggerFactory.getLogger(APIUtils.class);

	public static final Pattern ALPHANUM = Pattern.compile("[A-Za-z0-9_-]+");
	public static final Pattern ALPHANUM_SPACE = Pattern.compile("[ A-Za-z0-9_-]+");

	public static void reloadCore(String collection, CoreContainer cores) throws ParserConfigurationException,
			IOException, SAXException {
		if (!cores.isZooKeeperAware()) {
			LOG.info("Reloading non-ZK core " + collection);
			cores.reload(collection);
			try {
				Thread.sleep(5000L);
			} catch (InterruptedException e) {
			}
		} else {
			SolrCore core = cores.getCore(collection);
			try {
				String collectionName = core.getCoreDescriptor().getCloudDescriptor().getCollectionName();

				LOG.info("Reloading ZK collection " + collectionName);
				ModifiableSolrParams params = new ModifiableSolrParams();
				params.set("action", new String[] { CollectionParams.CollectionAction.RELOAD.toString() });
				params.set("name", new String[] { collectionName });

				LocalSolrQueryRequest request = new LocalSolrQueryRequest(core, params);
				try {
					cores.getCollectionsHandler().handleRequestBody(request, new SolrQueryResponse());
					LOG.info("Request sent, waiting for 5 seconds");
					try {
						Thread.sleep(5000L);
					} catch (InterruptedException e) {
					}
				} catch (Exception e) {
					throw new IOException("could not reload zk collection", e);
				}
			} finally {
				if (core != null)
					core.close();
			}
		}
	}

	public static void ensureSolrFieldMappingConfig(String collection, CollectionManager cm, CoreContainer cores)
			throws Exception {
		SolrCore core = cores.getCore(collection);
		try {
			EditableSolrConfig esc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());

			boolean hasFieldMappingProcessor = false;
			UpdateRequestProcessorChain chain = core.getUpdateProcessingChain(cm.getUpdateChain());
			for (UpdateRequestProcessorFactory f : chain.getFactories()) {
				if ((f instanceof FieldMappingUpdateProcessorFactory)) {
					hasFieldMappingProcessor = true;
				}
			}
			boolean hasFieldMappingRequestHandler = false;
			if ((core.getRequestHandler("/fmap") != null)
					&& ((core.getRequestHandler("/fmap") instanceof FieldMappingRequestHandler))) {
				hasFieldMappingRequestHandler = true;
			}
			if ((!hasFieldMappingProcessor) || (!hasFieldMappingRequestHandler)) {
				if (!hasFieldMappingProcessor) {
					esc.setFieldMappingProcessor();
				}
				if (!hasFieldMappingRequestHandler) {
					esc.setFieldMappingRequestHandler();
				}
				esc.save();

				reloadCore(collection, cores);
			}
		} finally {
			if (core != null)
				core.close();
		}
	}

	public static void toggleConnectorsSecuritySearchComponent(String collection, CollectionManager cm,
			CoreContainer cores, DataSourceManager dm) throws Exception {
		boolean enableSecureSearch = false;
		for (DataSource ds : dm.getDataSources()) {
			if ((collection.equals(ds.getCollection())) && (ds.isSecurityTrimmingEnabled())) {
				enableSecureSearch = true;
			}

		}

		SolrCore core = cores.getCore(collection);
		try {
			boolean updated = false;
			EditableSolrConfig esc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());

			if (enableSecureSearch) {
				updated = esc.enableConnectorsSecurity();
			} else {
				updated = esc.disableConnectorsSecurity();
			}

			if (updated) {
				esc.save();
				reloadCore(collection, cores);
			}
		} finally {
			if (core != null)
				core.close();
		}
	}

	public static boolean coreExists(CoreContainer cores, String collection) {
		SolrCore core = cores.getCore(collection);
		try {
			if (core != null)
				return true;
		} finally {
			if (core != null) {
				core.close();
			}
		}
		return false;
	}
}
