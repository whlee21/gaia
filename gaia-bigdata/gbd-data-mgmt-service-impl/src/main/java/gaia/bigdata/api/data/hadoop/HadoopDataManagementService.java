package gaia.bigdata.api.data.hadoop;

import gaia.bigdata.api.State;
import gaia.bigdata.api.Status;
import gaia.bigdata.api.data.BaseDataManagementService;
import gaia.bigdata.util.HadoopUtil;
import gaia.commons.services.ServiceLocator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HadoopDataManagementService extends BaseDataManagementService {
	private static transient Logger log = LoggerFactory.getLogger(HadoopDataManagementService.class);
	protected Path parentPath;
	protected Path collectionsPath;
	protected FileSystem parentFS;
	protected org.apache.hadoop.conf.Configuration hConfig;
	protected File hadoopConfDir;
	protected String hadoopJobTracker;
	protected String fsName;

	@Inject
	public HadoopDataManagementService(gaia.commons.api.Configuration config, ServiceLocator locator) throws IOException {
		super(config, locator);
		Properties props = config.getProperties();
		parentPath = new Path(props.getProperty("hadoop.base.path", "hdfs://data"));
		String hConfDir = props.getProperty("hadoop.conf.dir", System.getProperty("HADOOP_CONF_DIR"));
		if (hConfDir != null) {
			hadoopConfDir = new File(hConfDir);
		}
		hadoopJobTracker = props.getProperty("job.tracker");
		fsName = props.getProperty("fs.name");
		if ((hadoopConfDir != null) && (hadoopConfDir.exists()))
			hConfig = HadoopUtil.createConfig(hadoopConfDir);
		else if ((hadoopJobTracker != null) && (!hadoopJobTracker.isEmpty()) && (fsName != null)
				&& (!fsName.isEmpty())) {
			hConfig = HadoopUtil.createConfig(hadoopJobTracker, fsName);
		} else
			throw new IOException(
					"Can't create Hadoop configuration.  Either set hadoop.conf.dir in your SDA Configuration or the HADOOP_CONF_DIR environment variable or specify job.tracker and fs.name");

		parentFS = parentPath.getFileSystem(hConfig);
		parentFS.mkdirs(parentPath);
		collectionsPath = new Path(parentPath, "collections");
		parentFS.mkdirs(collectionsPath);
	}

	public State createCollection(String collectionName) {
		State result = new State();
		Path newColl = new Path(collectionsPath, collectionName);
		try {
			if (!parentFS.exists(newColl)) {
				Path newCollocations = new Path(collectionsPath, collectionName + "_collocations");
				mkDir(newColl);
				result.setCollection(collectionName);
				result.setId(collectionName);
				result.setStatus(Status.CREATED);
				result.addProperty("path", newColl.toUri().toString());
				mkDir(newCollocations);
			} else {
				result.setStatus(Status.ALREADY_EXISTS);
			}
		} catch (IOException e) {
			log.error("Exception", e);
			result.setThrowable(e);
			result.setStatus(Status.FAILED);
		}
		result.addProperty("service-impl", getClass().getSimpleName());
		return result;
	}

	private void mkDir(Path dir) throws IOException {
		parentFS.mkdirs(dir);
		parentFS.mkdirs(new Path(dir, "logs"));
		parentFS.mkdirs(new Path(dir, "content"));
		parentFS.mkdirs(new Path(dir, "tmp"));
	}

	public State deleteCollection(String collectionName) {
		State result = new State(collectionName, collectionName);
		result.setCollection(collectionName);
		try {
			Path collToDelete = new Path(collectionsPath, collectionName);
			if (parentFS.exists(collToDelete) == true) {
				Path collocationsToDelete = new Path(collectionsPath, collectionName + "_collocations");
				HadoopUtil.delete(hConfig, new Path[] { collToDelete, collocationsToDelete });
				Map<String, Object> props = new HashMap<String, Object>();
				props.put("path", collToDelete.toUri().toString());
				result.setProperties(props);
				result.setStatus(Status.DELETED);
			} else {
				result.setStatus(Status.NON_EXISTENT);
			}
		} catch (IOException e) {
			result.setStatus(Status.FAILED);
			result.setThrowable(e);
		}
		result.addProperty("service-impl", getClass().getSimpleName());
		return result;
	}

	public State lookupCollection(String collectionName) {
		State result = new State();
		result.setCollection(collectionName);
		result.addProperty("service-impl", getClass().getSimpleName());
		Path collPath = new Path(collectionsPath, collectionName);
		try {
			if (parentFS.exists(collPath)) {
				result.setStatus(Status.EXISTS);
				Map<String, Object> props = new HashMap<String, Object>();
				props.put("path", collPath.toUri().toString());
				result.setProperties(props);
			} else {
				result.setStatus(Status.NON_EXISTENT);
			}
		} catch (IOException e) {
			result.setStatus(Status.FAILED);
			result.setThrowable(e);
		}
		return result;
	}

	public List<State> listCollections(Pattern namesToMatch) {
		List<State> result = null;
		try {
			FileStatus[] statuses = parentFS.listStatus(collectionsPath);
			if ((statuses != null) && (statuses.length > 0)) {
				result = new ArrayList<State>(statuses.length);
				for (int i = 0; i < statuses.length; i++) {
					FileStatus status = statuses[i];
					Path path = status.getPath();
					String name = path.getName();
					if (!name.endsWith("_collocations")) {
						Matcher matcher = namesToMatch.matcher(name);
						if (matcher.matches()) {
							State tmp = new State(name, name);
							tmp.setStatus(Status.EXISTS);
							tmp.addProperty("path", path.toUri().toString());
							tmp.addProperty("service-impl", getClass().getSimpleName());
							result.add(tmp);
						}
					}
				}
			} else {
				result = Collections.emptyList();
			}
		} catch (IOException e) {
			return createFailedList(e);
		}
		return result;
	}
}
