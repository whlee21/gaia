package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.IndexDeletionPolicyWrapper;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;

public class CollectionInfoServerResource extends ServerResource implements CollectionInfoResource {
	static final String INDEX_LAST_MODIFIED = "index_last_modified";
	static final String INDEX_DIRECTORY = "index_directory";
	static final String INDEX_HAS_DELETIONS = "index_has_deletions";
	static final String INDEX_IS_CURRENT = "index_is_current";
	static final String INDEX_IS_OPTIMIZED = "index_is_optimized";
	static final String INDEX_VERSION = "index_version";
	static final String INDEX_MAX_DOC = "index_max_doc";
	static final String INDEX_NUM_DOCS = "index_num_docs";
	static final String TOTAL_DISK_SPACE = "total_disk_space";
	static final String FREE_DISK_SPACE = "free_disk_space";
	static final String TOTAL_DISK_BYTES = "total_disk_bytes";
	static final String FREE_DISK_BYTES = "free_disk_bytes";
	static final String INDEX_SIZE = "index_size";
	static final String INDEX_SIZE_BYTES = "index_size_bytes";
	static final String DATA_DIR = "data_dir";
	static final String ROOT_DIR = "root_dir";
	static final String COLLECTION_NAME = "collection_name";
	static final String INSTANCE_DIR = "instance_dir";
	private String collection;
	private SolrCore core;
	private String keyName;
	private CoreContainer cores;
	private CollectionManager cm;
	private static Set<String> VALID_KEYS = new HashSet<String>();

	@Inject
	public CollectionInfoServerResource(CollectionManager cm, CoreContainer cores) {
		this.cores = cores;
		this.cm = cm;
	}

	public void doInit() {
		collection = ((String) getRequestAttributes().get("coll_name"));
		keyName = ((String) getRequestAttributes().get("name"));
		core = cores.getCore(collection);
	}

	public void doRelease() {
		if (core != null)
			core.close();
	}

	@Get("json")
	public Map<String, Object> retrieve() throws IOException, SolrServerException {
		if (core == null)
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Could not find SolrCore:" + collection);
		java.util.Collection<String> reqKeys;
		if (keyName == null) {
			reqKeys = VALID_KEYS;
		} else {
			reqKeys = Arrays.asList(keyName.split(","));
			List<Error> errors = new ArrayList<Error>();
			for (String key : reqKeys) {
				if (!VALID_KEYS.contains(key)) {
					errors.add(new Error(keyName, Error.E_MISSING_VALUE, "The given key does not exist:" + keyName));
				}
			}

			if (errors.size() > 0) {
				throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, errors);
			}

		}

		Map<String, Object> stats = new HashMap<String, Object>();
		RefCounted<SolrIndexSearcher> searcher = core.getNewestSearcher(true);
		Long freeDiskSpace = null;
		Long totalDiskSpace = null;
		Long indexSize = null;
		File solrDir = new File(core.getDataDir());
		try {
			DirectoryReader reader = ((SolrIndexSearcher) searcher.get()).getIndexReader();

			for (String key : reqKeys)
				if (key.equals("index_num_docs")) {
					stats.put("index_num_docs", Integer.valueOf(reader.numDocs()));
				} else if (key.equals("index_max_doc")) {
					stats.put("index_max_doc", Integer.valueOf(reader.maxDoc()));
				} else if (key.equals("index_version")) {
					stats.put("index_version", Long.valueOf(reader.getVersion()));
				} else if (key.equals("index_is_optimized")) {
					boolean isOptimized;

					if (reader.hasDeletions()) {
						isOptimized = false;
					} else {
						List<AtomicReaderContext> leaves = reader.leaves();
						isOptimized = leaves.size() == 1;
					}
					stats.put("index_is_optimized", Boolean.valueOf(isOptimized));
				} else if (key.equals("index_is_current")) {
					stats.put("index_is_current", Boolean.valueOf(reader.isCurrent()));
				} else if (key.equals("index_has_deletions")) {
					stats.put("index_has_deletions", Boolean.valueOf(reader.hasDeletions()));
				} else if (key.equals("index_directory")) {
					Directory dir = reader.directory();
					stats.put("index_directory", dir.getClass().getName());
				} else if (key.equals("index_last_modified")) {
					stats.put("index_last_modified",
							StringUtils.formatDate(new Date(IndexDeletionPolicyWrapper.getCommitTimestamp(reader.getIndexCommit()))));
				} else if (key.equals("root_dir")) {
					stats.put("root_dir", gaia.utils.FileUtils.findRoot(solrDir).getAbsoluteFile());
				} else if (key.equals("data_dir")) {
					stats.put("data_dir", solrDir.getAbsoluteFile());
				} else if (key.equals("index_size_bytes")) {
					if (indexSize == null) {
						indexSize = Long.valueOf(org.apache.commons.io.FileUtils.sizeOfDirectory(solrDir));
					}

					stats.put("index_size_bytes", indexSize);
				} else if (key.equals("free_disk_bytes")) {
					if (null == freeDiskSpace) {
						freeDiskSpace = Long.valueOf(solrDir.getFreeSpace());
					}
					stats.put("free_disk_bytes", freeDiskSpace);
				} else if (key.equals("total_disk_bytes")) {
					if (null == totalDiskSpace) {
						totalDiskSpace = Long.valueOf(solrDir.getTotalSpace());
					}
					stats.put("total_disk_bytes", totalDiskSpace);
				} else if (key.equals("index_size")) {
					if (indexSize == null) {
						indexSize = Long.valueOf(org.apache.commons.io.FileUtils.sizeOfDirectory(solrDir));
					}

					stats.put("index_size", gaia.utils.FileUtils.humanReadableUnits(indexSize.longValue()));
				} else if (key.equals("free_disk_space")) {
					if (null == freeDiskSpace) {
						freeDiskSpace = Long.valueOf(solrDir.getFreeSpace());
					}
					stats.put("free_disk_space", gaia.utils.FileUtils.humanReadableUnits(freeDiskSpace.longValue()));
				} else if (key.equals("total_disk_space")) {
					if (null == totalDiskSpace) {
						totalDiskSpace = Long.valueOf(solrDir.getTotalSpace());
					}
					stats.put("total_disk_space", gaia.utils.FileUtils.humanReadableUnits(totalDiskSpace.longValue()));
				} else if (key.equals("collection_name")) {
					stats.put("collection_name", core.getName());
				} else if (key.equals("instance_dir")) {
					stats.put("instance_dir", cm.getCollection(collection).getInstanceDir());
				}
		} finally {
			searcher.decref();
		}
		return stats;
	}

	static {
		VALID_KEYS.add("data_dir");
		VALID_KEYS.add("free_disk_bytes");
		VALID_KEYS.add("free_disk_space");
		VALID_KEYS.add("index_directory");
		VALID_KEYS.add("index_has_deletions");
		VALID_KEYS.add("index_is_current");
		VALID_KEYS.add("index_is_optimized");
		VALID_KEYS.add("index_last_modified");
		VALID_KEYS.add("index_max_doc");
		VALID_KEYS.add("index_num_docs");
		VALID_KEYS.add("index_size");
		VALID_KEYS.add("index_size_bytes");
		VALID_KEYS.add("index_version");
		VALID_KEYS.add("total_disk_bytes");
		VALID_KEYS.add("total_disk_space");
		VALID_KEYS.add("root_dir");
		VALID_KEYS.add("data_dir");
		VALID_KEYS.add("instance_dir");
		VALID_KEYS.add("collection_name");
	}
}
