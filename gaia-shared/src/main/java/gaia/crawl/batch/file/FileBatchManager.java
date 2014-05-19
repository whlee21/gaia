package gaia.crawl.batch.file;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.Constants;
import gaia.crawl.CrawlerController;
import gaia.crawl.batch.BatchContentWriter;
import gaia.crawl.batch.BatchManager;
import gaia.crawl.batch.BatchSolrReader;
import gaia.crawl.batch.BatchSolrWriter;
import gaia.crawl.batch.BatchStatus;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.FieldMappingUtil;
import gaia.utils.DeepCopy;
import gaia.utils.FileUtils;

public class FileBatchManager extends BatchManager {
	private static final Logger LOG = LoggerFactory.getLogger(FileBatchManager.class);
	public static final String CONTENT_NAME = "content.raw";
	public static final String SOLR_NAME = "solr.json";
	public static final String STATUS_NAME = "batch.status";
	public static final String BATCH_DIR_NAME = "batches";
	private static final FileFilter DIR_FILTER = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	};
	File homeDir;

	public FileBatchManager(String ccType) {
		super(ccType);
		homeDir = new File(Constants.GAIA_DATA_HOME + File.separator + BATCH_DIR_NAME + File.separator + ccType);

		homeDir.mkdirs();
	}

	public List<BatchStatus> listBatchStatuses(String collection) throws Exception {
		return listBatchStatuses(collection, null);
	}

	public List<BatchStatus> listBatchStatuses(String collection, String dsId) throws Exception {
		File[] colDirs;
		if (collection != null) {
			File colDir = new File(homeDir, collection);
			if ((!colDir.exists()) || (colDir.isFile())) {
				return Collections.emptyList();
			}
			colDirs = new File[] { colDir };
		} else {
			colDirs = homeDir.listFiles();
		}
		List<BatchStatus> res = new ArrayList<BatchStatus>();
		for (File colDir : colDirs) {
			File[] batchDirs = colDir.listFiles(DIR_FILTER);
			if ((batchDirs != null) && (batchDirs.length != 0)) {
				for (File batchDir : batchDirs) {
					File bs = new File(batchDir, STATUS_NAME);
					if ((bs.exists()) && (!bs.isDirectory())) {
						try {
							BatchStatus status = BatchStatus.read(new FileInputStream(bs));
							if ((dsId == null) || (dsId.equals(status.dsId))) {
								res.add(status);
							}
						} catch (IOException ioe) {
							LOG.warn("Error reading batch status from " + bs, ioe);
						}
					}
				}
			}
		}
		return res;
	}

	private File makeDirName(String collection, String batch, boolean create) {
		File dir = null;
		if (collection == null) {
			String[] collections = homeDir.list();
			if (collections == null) {
				return null;
			}
			for (String col : collections) {
				dir = new File(homeDir, col + File.separator + batch);
				if (dir.exists())
					return dir;
			}
		} else {
			dir = new File(homeDir, collection + File.separator + batch);
			if ((!dir.exists()) && (create)) {
				dir.mkdirs();
			}
		}
		return dir;
	}

	private File makeContentFilename(BatchStatus batch, boolean create) {
		File out = new File(makeDirName(batch.collection, batch.batchId, create), CONTENT_NAME);
		return out;
	}

	private File makeSolrFilename(BatchStatus batch, boolean create) {
		File out = new File(makeDirName(batch.collection, batch.batchId, create), SOLR_NAME);
		return out;
	}

	private File makeStatusFilename(String collection, String batchId, boolean create) {
		File out = new File(makeDirName(collection, batchId, create), STATUS_NAME);
		return out;
	}

	public BatchContentWriter createContentWriter(BatchStatus batch, boolean overwrite) throws Exception {
		File out = makeContentFilename(batch, true);
		if (out.exists()) {
			if (overwrite)
				out.delete();
			else {
				throw new Exception("Content file already exists: " + out);
			}
		}
		return new ContentFileWriter(out);
	}

	public ContentFileReader openContentReader(BatchStatus batch) throws Exception {
		File in = makeContentFilename(batch, false);
		if (!in.exists()) {
			return null;
		}
		return new ContentFileReader(in);
	}

	public void deleteBatchContent(BatchStatus batch) throws Exception {
		File f = makeContentFilename(batch, false);
		f.delete();
	}

	public BatchSolrWriter createSolrWriter(BatchStatus batch, boolean overwrite) throws Exception {
		File out = makeSolrFilename(batch, true);
		if (out.exists()) {
			if (overwrite)
				out.delete();
			else {
				throw new Exception("Solr file already exists: " + out);
			}
		}
		return new SolrFileWriter(out);
	}

	public BatchSolrReader openSolrReader(BatchStatus batch) throws Exception {
		File in = makeSolrFilename(batch, false);
		if (!in.exists()) {
			return null;
		}
		return new SolrFileReader(in);
	}

	public void deleteBatchSolr(BatchStatus batch) throws Exception {
		File in = makeSolrFilename(batch, false);
		in.delete();
	}

	public BatchStatus getBatchStatus(String collection, String batch) throws Exception {
		File in = makeStatusFilename(collection, batch, false);
		if (!in.exists()) {
			return null;
		}
		return BatchStatus.read(new FileInputStream(in));
	}

	public void saveBatchStatus(BatchStatus result) throws Exception {
		File out = makeStatusFilename(result.collection, result.batchId, true);
		FileOutputStream fos = new FileOutputStream(out);
		result.write(fos);
		fos.flush();
		fos.close();
	}

	public boolean deleteBatch(BatchStatus batch) throws Exception {
		File dir = makeDirName(batch.collection, batch.batchId, false);
		if (!dir.exists()) {
			return false;
		}
		FileUtils.emptyDirectory(dir);
		return dir.delete();
	}

	public boolean deleteBatches(String collection, String batchId) throws Exception {
		if (collection == null) {
			throw new Exception("collection argument must not be null");
		}
		File rootDir = new File(homeDir, collection);
		if (batchId != null) {
			rootDir = new File(rootDir, batchId);
		}
		if (!rootDir.exists()) {
			return false;
		}
		FileUtils.emptyDirectory(rootDir);
		return rootDir.delete();
	}

	public DataSource newDataSourceTemplate(CrawlerController cc, String collection, String dsId) {
		DataSource res = null;
		if (dsId != null) {
			res = cc.getDataSourceRegistry().getDataSource(new DataSourceId(dsId));
			if (res != null) {
				res = (DataSource) DeepCopy.copy(res);
				res.setCollection(collection);
				res.setDisplayName("batch processing template");
			}
		}
		if (res == null) {
			res = new DataSource("generic", ccType, collection);
			res.setFieldMapping(new FieldMapping());
			res.setDataSourceId(new DataSourceId(dsId));
			res.setDisplayName("(batch processing template)");
			FieldMappingUtil.addTikaFieldMapping(res.getFieldMapping(), false);
		}
		return res;
	}
}
