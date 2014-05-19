package gaia.crawl.batch;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.Defaults;
import gaia.crawl.CrawlerController;
import gaia.crawl.batch.file.FileBatchManager;
import gaia.crawl.datasource.DataSource;

public abstract class BatchManager {
	protected static final Logger LOG = LoggerFactory.getLogger(BatchManager.class);
	public static final String BATCH_MANAGER_KEY = ".batch_manager";
	protected String ccType;

	public static BatchManager create(String ccType, ClassLoader cl) {
		return create(ccType, cl, FileBatchManager.class.getName());
	}

	public static BatchManager create(String ccType, ClassLoader cl, String defaultName) {
		String className = Defaults.INSTANCE.getString(Defaults.Group.crawlers, ccType + BATCH_MANAGER_KEY, defaultName);

		if ((className == null) || (className.trim().equals(""))) {
			className = defaultName;
		}

		if (cl == null)
			cl = BatchManager.class.getClassLoader();
		Class<?> clazz;
		try {
			clazz = Class.forName(className, true, cl);
		} catch (ClassNotFoundException cnfe) {
			LOG.warn("Can't find class " + className + ", using " + defaultName);
			clazz = FileBatchManager.class;
		}
		BatchManager instance = null;
		try {
			LOG.info("Creating " + clazz.getSimpleName() + " for '" + ccType + "'");
			Constructor<?> c = clazz.getConstructor(new Class[] { String.class });
			return (BatchManager) c.newInstance(new Object[] { ccType });
		} catch (Throwable t) {
			LOG.warn("Can't instantiate " + className + " for '" + ccType + "': " + t.getMessage() + ", using " + defaultName);

			instance = new FileBatchManager(ccType);
		}
		return instance;
	}

	protected BatchManager(String ccType) {
		this.ccType = ccType;
	}

	public String getCrawlerControllerType() {
		return ccType;
	}

	public static BatchStatus newBatch(String ccType, String collection, String dsId) {
		String batchId = UUID.randomUUID().toString().replaceAll("-", "");
		BatchStatus bs = new BatchStatus(ccType, collection, dsId, batchId);
		return bs;
	}

	public abstract DataSource newDataSourceTemplate(CrawlerController paramCrawlerController, String paramString1,
			String paramString2);

	public abstract List<BatchStatus> listBatchStatuses(String paramString) throws Exception;

	public abstract List<BatchStatus> listBatchStatuses(String paramString1, String paramString2) throws Exception;

	public abstract BatchStatus getBatchStatus(String paramString1, String paramString2) throws Exception;

	public abstract void saveBatchStatus(BatchStatus paramBatchStatus) throws Exception;

	public abstract boolean deleteBatch(BatchStatus paramBatchStatus) throws Exception;

	public abstract boolean deleteBatches(String paramString1, String paramString2) throws Exception;

	public abstract BatchContentWriter createContentWriter(BatchStatus paramBatchStatus, boolean paramBoolean)
			throws Exception;

	public abstract BatchContentReader openContentReader(BatchStatus paramBatchStatus) throws Exception;

	public abstract void deleteBatchContent(BatchStatus paramBatchStatus) throws Exception;

	public abstract BatchSolrWriter createSolrWriter(BatchStatus paramBatchStatus, boolean paramBoolean) throws Exception;

	public abstract BatchSolrReader openSolrReader(BatchStatus paramBatchStatus) throws Exception;

	public abstract void deleteBatchSolr(BatchStatus paramBatchStatus) throws Exception;
}
