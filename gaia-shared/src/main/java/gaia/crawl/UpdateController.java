package gaia.crawl;

import gaia.Defaults;
import gaia.crawl.batch.BatchManager;
import gaia.crawl.batch.BatchStatus;
import gaia.crawl.batch.BatchTeeUpdateController;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.impl.DirectSolrUpdateController;
import gaia.crawl.impl.SolrJUpdateController;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public abstract class UpdateController {
	private static transient Logger LOG = LoggerFactory.getLogger(UpdateController.class);

	protected volatile boolean needCommit = false;
	protected AtomicInteger numAdded = new AtomicInteger();
	protected AtomicInteger numDeleted = new AtomicInteger();
	protected AtomicInteger numFailed = new AtomicInteger();
	private int commitWithin;
	private boolean useCommitWithin;
	private boolean started = false;
	protected DataSource ds;
	public static final String UPDATE_CHAIN = "gaia-update-chain";
	public static final UpdateController NULL_UPDATE_CONTROLLER = new NullUpdateController();
	public static final String NULL = "NULL";
	protected String collection;

	@Inject
	@Named("solr-address")
	private static String solrAddress;

	public boolean isUseCommitWithin() {
		return useCommitWithin;
	}

	public int getCommitWithin() {
		return commitWithin;
	}

	public void init(DataSource ds) throws Exception {
		this.ds = ds;
		int commitWithin = -1;
		if ((ds != null) && (ds.getProperty("commit_within") != null)) {
			commitWithin = ds.getInt("commit_within");
		}
		this.commitWithin = commitWithin;
		if (commitWithin < 0)
			useCommitWithin = false;
		else
			useCommitWithin = true;
	}

	public static UpdateController create(CrawlerController cc, DataSource ds) throws Exception {
		boolean indexing = ds.getBoolean("indexing", Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, "indexing"));

		boolean caching = ds.getBoolean("caching", Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, "caching"));

		String outputType = ds.getString("output_type",
				Defaults.INSTANCE.getString(Defaults.Group.datasource, "output_type"));

		if (outputType == null) {
			outputType = "solr";
		}
		String outputArgs = ds.getString("output_args",
				Defaults.INSTANCE.getString(Defaults.Group.datasource, "output_args"));

		boolean useDirectSolrUpdate = Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, "use_direct_solr");
		String ucClass = null;
		if ("solr".equals(outputType)) {
			if (useDirectSolrUpdate) {
				ucClass = DirectSolrUpdateController.class.getName();
			} else {
				if ((outputArgs == null) || (!outputArgs.matches(".*http[s]?://.*"))) {
					String error = null;
					try {
						// URL u = MasterConfUtil.getSolrAddress(true, ds.getCollection());
						URL u = new URL(solrAddress + "/" + ds.getCollection());
						String address = u.toExternalForm();
						if (outputArgs != null)
							outputArgs = outputArgs + "," + address;
						else {
							outputArgs = address;
						}
						ds.setProperty("output_args", outputArgs);
					} catch (Exception e) {
						error = e.toString();
					}
					if (error != null) {
						LOG.warn("Failed to obtain SolrJ update URL, will use direct API (" + error + ")");
						ucClass = DirectSolrUpdateController.class.getName();
						outputArgs = null;
						ds.setProperty("output_args", outputArgs);
					}
				}
				if (ucClass == null)
					ucClass = SolrJUpdateController.class.getName();
			}
		} else {
			ucClass = outputType;
		}
		if (indexing) {
			UpdateController res = createUpdateController(ds, ucClass);
			if (caching) {
				if ((cc != null) && (cc.getBatchManager() != null)) {
					BatchStatus b = BatchManager.newBatch(ds.getCrawlerType(), ds.getCollection(), ds.getDataSourceId()
							.toString());
					b.descr = ds.getDisplayName();
					try {
						res = new BatchTeeUpdateController(cc.getBatchManager(), b, res, true);
					} catch (Exception e) {
						LOG.warn("Failed to create a batch UpdateController, using direct update", e);
					}
				} else {
					LOG.warn("Caching not supported - no batch manager for crawler " + cc);
				}
			}
			return res;
		}
		if ((cc == null) || (cc.getBatchManager() == null)) {
			return NULL_UPDATE_CONTROLLER;
		}
		try {
			BatchStatus batch = BatchManager.newBatch(ds.getCrawlerType(), ds.getCollection(), ds.getDataSourceId()
					.toString());
			batch.descr = ds.getDisplayName();

			cc.getBatchManager().saveBatchStatus(batch);
			return new BatchTeeUpdateController(cc.getBatchManager(), batch, null, true);
		} catch (Exception e) {
			LOG.warn("Failed to create a batch UpdateController, using regular update", e);
		}
		return createUpdateController(ds, ucClass);
	}

	private static UpdateController createUpdateController(DataSource ds, String ucClass) throws Exception {
		if ("NULL".equals(ucClass)) {
			return NULL_UPDATE_CONTROLLER;
		}

		Class<?> clz = Class.forName(ucClass, true, Thread.currentThread().getContextClassLoader());
		UpdateController update = null;
		if (Defaults.injector != null)
			update = (UpdateController) Defaults.injector.getInstance(clz);
		else {
			update = (UpdateController) clz.newInstance();
		}
		update.init(ds);
		return update;
	}

	public void start() throws Exception {
		started = true;
	}

	public void add(SolrInputDocument doc, String id) throws IOException {
		add(doc);
	}

	public abstract void add(SolrInputDocument paramSolrInputDocument) throws IOException;

	public abstract void delete(String paramString) throws IOException;

	public abstract void deleteByQuery(String paramString) throws IOException;

	public abstract void commit() throws IOException;

	public void finish(boolean commit) throws IOException {
		if (commit) {
			commit();
		}
		started = false;
	}

	public int getNumFailed() {
		return numFailed.get();
	}

	public void setNumFailed(int numFailed) {
		this.numFailed.set(numFailed);
	}

	public boolean isStarted() {
		return started;
	}

	public static final class NullUpdateController extends UpdateController {
		public void add(SolrInputDocument doc) throws IOException {
		}

		public void delete(String id) throws IOException {
		}

		public void deleteByQuery(String query) throws IOException {
		}

		public void commit() throws IOException {
		}

		public void init(DataSource ds) {
		}
	}
}
