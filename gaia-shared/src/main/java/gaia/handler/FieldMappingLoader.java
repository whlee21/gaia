package gaia.handler;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.cloud.ZkSolrResourceLoader;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.FieldMappingUtil;
import gaia.utils.OSFileWriter;
import gaia.utils.StringUtils;

class FieldMappingLoader {
	private static final Logger LOG = LoggerFactory.getLogger(FieldMappingLoader.class);
	static final String FILE_NAME = "field-mapping.json";
	boolean zkMode = false;
	String confDir = null;
	ZkController zkController;
	String zkPath;

	FieldMappingLoader(SolrCore core) {
		SolrResourceLoader resLoader = core.getResourceLoader();
		if ((resLoader instanceof ZkSolrResourceLoader)) {
			ZkSolrResourceLoader zkLoader = (ZkSolrResourceLoader) resLoader;
			try {
				Field zkField = ZkSolrResourceLoader.class.getDeclaredField("zkController");

				zkField.setAccessible(true);
				zkController = ((ZkController) zkField.get(zkLoader));
				String confName = zkController
						.readConfigName(core.getCoreDescriptor().getCloudDescriptor().getCollectionName());

				zkPath = new StringBuilder().append("/configs/").append(confName).append("/").append("field-mapping.json")
						.toString();
				zkMode = true;
			} catch (Exception e) {
				LOG.warn(new StringBuilder().append("Can't obtain a reference to ZkController, ZK save/load won't work: ")
						.append(e.toString()).toString(), e);
				zkMode = false;
			}
		} else {
			zkMode = false;
			confDir = resLoader.getConfigDir();
		}
	}

	Map<String, Map<String, FieldMapping>> loadMappings() throws Exception {
		Map<String, Map<String, FieldMapping>> res = null;
		if (zkMode)
			res = zkLoadMappings();
		else {
			res = localLoadMappings();
		}
		if (res != null) {
			int chains = res.size();
			int maps = 0;
			for (Map<String, FieldMapping> e : res.values()) {
				maps += e.size();
			}
			LOG.info(new StringBuilder()
					.append("Loaded ")
					.append(maps)
					.append(" mapping(s) for ")
					.append(chains)
					.append(" update chain(s) from ")
					.append(
							zkMode ? new StringBuilder().append("ZK:").append(zkController.getZkServerAddress()).toString()
									: new File(confDir, "field-mapping.json").getAbsolutePath()).toString());
		}

		return res;
	}

	private Map<String, Map<String, FieldMapping>> localLoadMappings() throws Exception {
		File f = new File(confDir, "field-mapping.json");
		if ((!f.exists()) || (!f.canRead()) || (f.length() == 0L)) {
			return null;
		}
		String json = FileUtils.readFileToString(f);
		return FieldMappingUtil.mappingsFromJSON(json);
	}

	private Map<String, Map<String, FieldMapping>> zkLoadMappings() {
		if (zkController == null) {
			return null;
		}
		String json = null;
		try {
			if (zkController.pathExists(zkPath)) {
				byte[] bytes = zkController.getZkClient().getData(zkPath, null, null, true);
				json = new String(bytes, StringUtils.UTF_8.name());
			}
			if (json != null) {
				return FieldMappingUtil.mappingsFromJSON(json);
			}
			return null;
		} catch (Exception e) {
			LOG.warn(new StringBuilder().append("Error reading field mappings from ZK: ").append(e.toString()).toString(), e);
		}
		return null;
	}

	synchronized void saveMappings(Map<String, Map<String, FieldMapping>> mappings) throws Exception {
		if (zkMode)
			zkSaveMappings(mappings);
		else
			localSaveMappings(mappings);
	}

	private void localSaveMappings(Map<String, Map<String, FieldMapping>> mappings) throws Exception {
		String json = FieldMappingUtil.mappingsToJSON(mappings);
		File f = new File(confDir, "field-mapping.json");
		OSFileWriter writer = new OSFileWriter(f);
		FileUtils.writeStringToFile(writer.getWriteFile(), json, StringUtils.UTF_8.name());

		writer.flush();
	}

	private void zkSaveMappings(Map<String, Map<String, FieldMapping>> mappings) throws Exception {
		if (zkController == null) {
			return;
		}
		String json = FieldMappingUtil.mappingsToJSON(mappings);
		try {
			SolrZkClient cli = zkController.getZkClient();
			byte[] bytes = json.getBytes(StringUtils.UTF_8);
			if (!cli.exists(zkPath, true).booleanValue()) {
				cli.makePath(zkPath, false, true);
			}
			cli.setData(zkPath, bytes, true);
		} catch (Exception e) {
			LOG.warn(new StringBuilder().append("Error writing field mappings to ZK: ").append(e.toString()).toString(), e);
		}
	}
}
