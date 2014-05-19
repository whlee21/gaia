package gaia.upgrade.metadata;

import gaia.utils.StreamEaterThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.output.NullOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class MetadataUpgradeTool {
	public static final String NEW_APP_DIR_OVERRIDE_SYSPROP = "new_app_dir";
	public static final String NEW_CONF_DIR_OVERRIDE_SYSPROP = "new_conf_dir";
	public static final String JVM_CMD_OVERRIDE_SYSPROP = "java_cmd";
	private static final PrintStream NULL_STREAM = new PrintStream(NullOutputStream.NULL_OUTPUT_STREAM);

	private static final URL TOOL_JAR_URL = MetadataUpgradeTool.class.getProtectionDomain().getCodeSource().getLocation();
	private final File appDir;
	private final File confDir;
	private final File dataDir;
	private final PrintStream err;
	private final PrintStream out;

	public MetadataUpgradeTool(PrintStream out, PrintStream err, File appDir, File confDir, File dataDir) {
		this.out = out;
		this.err = err;
		this.appDir = appDir;
		this.confDir = confDir;
		this.dataDir = dataDir;
	}

	public int upgrade() {
		try {
			int result = 0;

			result += upgradeJettyConfigs();

			result += copyLog4jConnectorsConfIfMissing();

			result += upgradeMasterConf();

			result += upgradeCollectionsYml();

			result += upgradeDefaultsYml();

			result += upgradeSchema();

			result += upgradeFieldMappingConfig();

			result += fixHighlightingConfig();

			result += upgradeDihConfigs();

			result += removeAutocompleteDirs();

			result += removeCommitWithinUpdateProcessorFactory();

			result += removeIndexDefaults();

			result += upgradeMainIndexToIndexConfigInSolrConfig();

			int uiResult = moveUiSqlDb();

			if (0 == uiResult)
				uiResult += upgradeUiDb();

			return result + uiResult;
		} catch (Exception e) {
			err.println("Upgrade aborted due to fatal error: " + e.getMessage());
			e.printStackTrace(err);
		}
		return -1;
	}

	public int upgradeMasterConf() {
		boolean core = isCoreEnabled();
		boolean connectors = isConnectorsEnabled();
		boolean enabled = (core) || (connectors);

		PrintStream errOut = enabled ? err : out;
		out.println("### Upgrading master.conf...");
		File conf = new File(confDir, "master.conf");
		Properties props = loadMasterDotConf();
		List<String> lines = null;
		try {
			lines = FileUtils.readLines(conf, "UTF-8");
		} catch (IOException e) {
			errOut.println("Unable to upgrade master.conf: " + e.toString());
			return onlyProblemIf(errOut, enabled, "connectors");
		}
		boolean modified = false;
		if (props.getProperty("lweconnectors.enabled") == null) {
			addDefaultConnectorParams(lines, props.getProperty("lwecore.jvm.params"));
			modified = true;
		} else {
			int idx = -1;
			for (int i = 0; i < lines.size(); i++) {
				if (((String) lines.get(i)).startsWith("lweconnectors.jvm.params=")) {
					idx = i;
					break;
				}
			}
			if (idx == -1) {
				addDefaultConnectorParams(lines, props.getProperty("lwecore.jvm.params"));
				modified = true;
			} else {
				String line = (String) lines.get(idx);
				if (line.indexOf("-Dmapr.home") == -1) {
					line = line + " -Dmapr.home=/opt/mapr";
					lines.set(idx, line);
					modified = true;
				}
			}
		}
		if (props.getProperty("lweui.jvm.params") != null) {
			int idx = -1;
			for (int i = 0; i < lines.size(); i++) {
				if (((String) lines.get(i)).startsWith("lweui.jvm.params=")) {
					idx = i;
					break;
				}
			}
			if (idx != -1) {
				String line = (String) lines.get(idx);
				if (line.indexOf("-XX:MaxPermSize=") == -1) {
					line = line + " -XX:MaxPermSize=256M";
					lines.set(idx, line);
					modified = true;
				}
			}
		}
		if (modified) {
			try {
				FileUtils.writeLines(conf, "UTF-8", lines);
			} catch (IOException e) {
				errOut.println("Unable to upgrade master.conf: " + e.toString());
				return onlyProblemIf(errOut, enabled, "connectors");
			}
		}
		errOut.close();
		return 0;
	}

	private void addDefaultConnectorParams(List<String> lines, String defaultParams) {
		lines.add("");
		lines.add("#--- upgrade: added default connectors configuration ---");
		lines.add("lweconnectors.enabled=true");
		lines.add("lweconnectors.address=http://127.0.0.1:8765");
		String connParams = defaultParams;
		if (connParams == null) {
			connParams = "";
		}
		if (connParams.indexOf("-Dmapr.home") == -1) {
			connParams = connParams + " -Dmapr.home=/opt/mapr";
		}
		lines.add("lweconnectors.jvm.params=" + connParams);
	}

	public int upgradeUiDb() {
		out.println("### Upgrading UI DB...");

		boolean ui = isUiEnabled();

		PrintStream errOut = ui ? err : out;

		File workDir = null;
		try {
			File newAppDir = getNewAppDir();
			if ((null != newAppDir) && (newAppDir.exists()))
				workDir = FileUtils.getFile(newAppDir, new String[] { "webapps", "admin", "WEB-INF" });
		} catch (URISyntaxException e) {
			errOut.println("Unable to compute path of UI DB upgrade code: " + e.getMessage());
			return onlyProblemIf(errOut, ui, "UI");
		}

		if ((null == workDir) || (!workDir.exists())) {
			errOut.println("Unable to determine valid path of UI DB upgrade code directory: " + workDir);
			return onlyProblemIf(errOut, ui, "UI");
		}

		ProcessBuilder procBuilder = new ProcessBuilder(new String[] { System.getProperty("java_cmd", "java"), "-Xss2048k",
				"-cp", getJrubyClasspath(workDir), "-Dlucidworks_data_home=" + dataDir.getAbsolutePath(),
				"-Dlog4j.configuration=log4j-upgrader.xml", "org.jruby.Main", getRakePath(workDir), "-s", "db:migrate" });

		procBuilder.directory(workDir);

		procBuilder.environment().put("BUNDLE_WITHOUT", "development:test:assets");
		procBuilder.environment().put("GEM_HOME", "gems");
		procBuilder.environment().put("GEM_PATH", "gems");
		procBuilder.environment().put("RAILS_ENV", "production");
		try {
			out.println("Executing UI DB upgrade");
			Process proc = procBuilder.start();

			InputStream procOut = proc.getInputStream();
			InputStream procErr = proc.getErrorStream();
			try {
				StreamEaterThread eatOut = new StreamEaterThread(procOut, out);
				eatOut.start();
				StreamEaterThread eatErr = new StreamEaterThread(procErr, errOut);
				eatErr.start();
				int status = proc.waitFor();

				if (0 != status) {
					errOut.println("UI DB Upgrade command did not exit cleanly: " + status);
					return onlyProblemIf(errOut, ui, "UI");
				}
			} finally {
				IOUtils.closeQuietly(procOut);
				IOUtils.closeQuietly(procErr);
			}
		} catch (IOException e1) {
			errOut.println("Unable to execute UI DB Upgrade command: " + e1.getMessage());
			return onlyProblemIf(errOut, ui, "UI");
		} catch (InterruptedException e2) {
			errOut.println("UI DB Upgrade command interrupted: " + e2.getMessage());
			return onlyProblemIf(errOut, ui, "UI");
		}

		return 0;
	}

	public int moveUiSqlDb() {
		out.println("### Upgrading UI DB location...");

		File correctDbDir = new File(dataDir, "gaia-web");
		File correctDbFile = new File(correctDbDir, "production.sqlite3");

		boolean ui = isUiEnabled();

		PrintStream errOut = ui ? err : out;

		if (correctDbFile.exists()) {
			errOut.close();
			return 0;
		}

		if (null != appDir) {
			File oldDbFile = FileUtils.getFile(appDir, new String[] { "webapps", "gaia-web", "WEB-INF", "db",
					"production.sqlite3" });

			if (oldDbFile.exists()) {
				try {
					FileUtils.moveFileToDirectory(oldDbFile, correctDbDir, true);
					out.println("Moved " + oldDbFile.getAbsolutePath() + " to " + correctDbDir.getAbsolutePath());
					errOut.close();
					return 0;
				} catch (IOException e) {
					errOut.println("Unable to move UI DB file...");
					errOut.println("  From: " + oldDbFile.getAbsolutePath());
					errOut.println("  To:   " + correctDbDir.getAbsolutePath());
					errOut.println("  Err:  " + e.getMessage());

					return onlyProblemIf(errOut, ui, "UI");
				}

			}

		}

		errOut.println("The user db file was not found at the expected location: " + correctDbFile);

		if (null == appDir) {
			errOut.println("   In some older versions of GaiaSearch, the db was located in the app dir,");
			errOut.println("   but since -lwe_app_dir was not specified, this check could not be performed.");
		} else {
			errOut.println("  It was not found in any location used in older versions of GaiaSearch.");
		}
		if (ui) {
			errOut.println("  The UI and User related functions will not work w/o this DB.");
		}
		return onlyProblemIf(errOut, ui, "LWE UI");
	}

	public int upgradeJettyConfigs() {
		out.println("### Upgrading Jetty Configs ...");

		File jettyConfDir = FileUtils.getFile(confDir, new String[] { "jetty" });
		if (!jettyConfDir.exists()) {
			err.println("Unable to locate jetty config dir: " + jettyConfDir.getAbsolutePath());

			return -1;
		}

		String expected = "http://www.eclipse.org/jetty/configure.dtd";
		File coreContext = FileUtils.getFile(jettyConfDir, new String[] { "search", "contexts", "gaia-search.xml" });

		File uiContext = FileUtils.getFile(jettyConfDir, new String[] { "gaia-web", "contexts", "gaia-web.xml" });

		boolean confsNeedReplaced = false;

		for (File context : new File[] { coreContext, uiContext }) {
			if (context.exists()) {
				try {
					String fileContents = FileUtils.readFileToString(context, "UTF-8");

					if (!fileContents.contains(expected)) {
						confsNeedReplaced = true;
						break;
					}
				} catch (IOException e) {
					err.println("Error reading jetty config file: " + context.getAbsolutePath() + " - " + e.getMessage());

					return -1;
				}
			}

		}

		File adminContext = FileUtils.getFile(jettyConfDir, new String[] { "gaia-web", "contexts", "admin.xml" });
		File launchpadContext = FileUtils.getFile(jettyConfDir, new String[] { "gaia-web", "contexts", "launchpad.xml" });
		File quickstartContext = FileUtils.getFile(jettyConfDir, new String[] { "gaia-web", "contexts", "quickstart.xml" });
		File relevancyContext = FileUtils.getFile(jettyConfDir, new String[] { "gaia-web", "contexts", "relevancy.xml" });

		for (File context : new File[] { adminContext, launchpadContext, quickstartContext, relevancyContext }) {
			if (!context.exists()) {
				confsNeedReplaced = true;
			}
		}

		if (confsNeedReplaced) {
			out.println("Replacing out of date jetty config files");

			File newJettyConfDir = null;
			try {
				newJettyConfDir = getNewConfDir();
			} catch (URISyntaxException e) {
				err.println("Unable to compute path of new jetty configs: " + e.getMessage());
				return -1;
			}

			if (!newJettyConfDir.exists()) {
				err.println("Unable to locate new jetty config dir to copy files from: " + newJettyConfDir.getAbsolutePath());

				return -1;
			}
			try {
				FileUtils.deleteDirectory(jettyConfDir);
			} catch (IOException e) {
				err.println("Unable to delete jetty config dir: " + jettyConfDir.getAbsolutePath() + " - " + e.getMessage());

				return -1;
			}
			try {
				FileUtils.copyDirectory(newJettyConfDir, jettyConfDir);
			} catch (IOException e) {
				err.println("Unable to copy files into jetty config dir: " + jettyConfDir.getAbsolutePath() + " - "
						+ e.getMessage());

				return -1;
			}
		}
		return 0;
	}

	public int copyLog4jConnectorsConfIfMissing() {
		out.println("### Copying connectors log4j config if missing ...");

		String filename = "log4j-connectors.xml";

		if (!confDir.exists()) {
			err.println("Unable to locate config dir: " + confDir.getAbsolutePath());
			return -1;
		}

		File log4jFile = FileUtils.getFile(confDir, new String[] { filename });
		if (log4jFile.exists()) {
			out.println("Found log4j-connectors.xml file in conf dir, skipping this step...");
			return 0;
		}

		File file = null;
		try {
			file = FileUtils.getFile(getNewConfDir(), new String[] { "..", filename });
		} catch (URISyntaxException e) {
			err.println("Unable to compute path of new connectors log4j config: " + e.getMessage());
			return -1;
		}
		if (!file.exists()) {
			err.println("Unable to locate new connectors log4j config to copy file from: " + log4jFile.getAbsolutePath());
			return -1;
		}
		try {
			FileUtils.copyFile(file, log4jFile);
		} catch (IOException e) {
			err.println("Unable to copy log4j config file for connectors module: " + e.getMessage());
			return -1;
		}

		return 0;
	}

	public int upgradeDefaultsYml() {
		String defFile = "defaults.yml";
		out.println("### Upgrading format of defaults.yml ...");
		boolean core = isCoreEnabled();

		PrintStream errOut = core ? err : out;

		File yml = new File(new File(confDir, "search"), defFile);
		if ((!yml.exists()) || (yml.length() == 0L)) {
			out.println(yml + " not found or empty, not upgrading format: " + yml.getAbsolutePath());
			errOut.close();
			return 0;
		}
		String yamlStr;
		try {
			yamlStr = FileUtils.readFileToString(yml, "UTF-8");
		} catch (IOException e) {
			errOut.println("Could not read source file " + yml.getAbsolutePath() + ": " + e.getMessage());
			return onlyProblemIf(errOut, core, "Core");
		}
		SimpleDefaultsYaml yaml = new SimpleDefaultsYaml();
		Object yamlObj = null;
		try {
			yamlObj = yaml.load(yamlStr);
		} catch (RuntimeException e) {
			errOut.println("Could not parse YAML file " + yml.getAbsolutePath() + ": " + e.getMessage());
			return onlyProblemIf(errOut, core, "Core");
		}
		if (!(yamlObj instanceof Map)) {
			errOut.println("Wrong type of top-level object, expected Map but got " + yamlObj.getClass().getName());
			return onlyProblemIf(errOut, core, "Core");
		}
		StringWriter w = new StringWriter();
		try {
			yaml.dump(yamlObj, w);
			yamlStr = w.toString();
		} catch (RuntimeException e) {
			errOut.println("Could not parse YAML file " + yml.getAbsolutePath() + ": " + e.getMessage());
			return onlyProblemIf(errOut, core, "Core");
		} finally {
			IOUtils.closeQuietly(w);
		}

		try {
			out.println("Writing upgraded defaults.yml");
			FileUtils.writeStringToFile(yml, yamlStr, "UTF-8");
		} catch (IOException e) {
			errOut.println("Could not save corrected file " + yml.getAbsolutePath() + ": " + e.getMessage());
			return onlyProblemIf(errOut, core, "Core");
		}
		errOut.close();
		return 0;
	}

	public int upgradeCollectionsYml() {
		String colFile = "collections.yml";
		String dsFile = "datasources.yml";
		out.println("### Upgrading format of collections.yml ...");

		boolean core = isCoreEnabled();

		PrintStream errOut = core ? err : out;

		File cyml = new File(new File(dataDir, "search"), colFile);
		File dyml = new File(new File(dataDir, "search"), dsFile);

		if (!cyml.exists()) {
			out.println("collections.yml not found, not upgrading format: " + cyml.getAbsolutePath());
			errOut.close();
			return 0;
		}
		String yamlStr;
		try {
			yamlStr = FileUtils.readFileToString(cyml, "UTF-8");
		} catch (IOException e) {
			errOut.println("Could not read source file " + cyml.getAbsolutePath() + ": " + e.getMessage());
			return onlyProblemIf(errOut, core, "Core");
		}

		boolean fixS3DataSourceTypeMadness = false;
		Version oldVersion = getOldVersion();
		if ((null != oldVersion) && (null != oldVersion.major) && (null != oldVersion.minor)) {
			if ((oldVersion.major.intValue() < 2)
					|| ((oldVersion.major.intValue() == 2) && (oldVersion.minor.intValue() < 1))) {
				fixS3DataSourceTypeMadness = true;
			}
		}

		SimpleCollectionsYaml yaml = new SimpleCollectionsYaml(fixS3DataSourceTypeMadness);
		Object yamlObj = null;
		try {
			yamlObj = yaml.load(yamlStr);
		} catch (RuntimeException e) {
			errOut.println("Could not parse YAML file " + cyml.getAbsolutePath() + ": " + e.getMessage());
			return onlyProblemIf(errOut, core, "Core");
		}
		if (!(yamlObj instanceof Map)) {
			errOut.println("Wrong type of top-level object, expected Map but got " + yamlObj.getClass().getName());
			return onlyProblemIf(errOut, core, "Core");
		}
		Map<String, Object> map = (Map) yamlObj;
		Map<String, Object> datasources = extractDataSources(map);
		fixScheduledCommands(map);
		StringWriter w = new StringWriter();
		try {
			yaml.dump(yamlObj, w);
			yamlStr = w.toString();
		} catch (RuntimeException e) {
			errOut.println("Could not parse YAML file " + cyml.getAbsolutePath() + ": " + e.getMessage());
			return onlyProblemIf(errOut, core, "Core");
		} finally {
			IOUtils.closeQuietly(w);
		}

		try {
			out.println("Writing upgraded collections.yml");
			FileUtils.writeStringToFile(cyml, yamlStr, "UTF-8");
		} catch (IOException e) {
			errOut.println("Could not save corrected file " + cyml.getAbsolutePath() + ": " + e.getMessage());
			return onlyProblemIf(errOut, core, "Core");
		}

		w = new StringWriter();
		try {
			yaml.dump(datasources, w);
			yamlStr = w.toString();
		} catch (RuntimeException e) {
			errOut.println("Could not parse datasources YAML file " + dyml.getAbsolutePath() + ": " + e.getMessage());
			return onlyProblemIf(errOut, core, "Core");
		} finally {
			IOUtils.closeQuietly(w);
		}
		try {
			out.println("Writing extracted datasources.yml");
			FileUtils.writeStringToFile(dyml, yamlStr, "UTF-8");
		} catch (IOException e) {
			errOut.println("Could not save datasources file " + dyml.getAbsolutePath() + ": " + e.getMessage());
			return onlyProblemIf(errOut, core, "Core");
		}
		errOut.close();
		return 0;
	}

	private Map<String, Object> extractDataSources(Map<String, Object> yamlMap) {
		Map<String, Object> colsMap = (Map) yamlMap.get("collectionsMap");
		List<SimpleCollectionsYaml.SimpleDataSource> res = new ArrayList<SimpleCollectionsYaml.SimpleDataSource>();
		for (Map.Entry<String, Object> e : colsMap.entrySet()) {
			Map<String, Object> colMap = (Map) e.getValue();

			Set<SimpleCollectionsYaml.SimpleDataSource> dataSources = (Set) colMap.remove("dataSources");
			if ((dataSources != null) && (!dataSources.isEmpty())) {
				for (SimpleCollectionsYaml.SimpleDataSource ds : dataSources) {
					String newId;
					if (ds.id != 0) {
						newId = String.valueOf(ds.id);
					} else {
						newId = (String) ds.properties.get("id");
						if (newId == null) {
							newId = (String) ds.dsId.get("id");
							if (newId == null) {
								newId = "0";
							}
						}
					}
					ds.dsId.put("id", newId);
					ds.dsId.put("user", "");
					ds.properties.put("id", newId);
					if ((ds.schedule != null) && (!ds.schedule.isEmpty())) {
						Map<String, Object> cmd = new HashMap<String, Object>();
						cmd.put("createDate", new Date());
						cmd.put("lastModified", new Date());
						cmd.put("id", newId);
						cmd.put("name", "crawl " + newId + " (upgraded)");
						ds.schedule.put("activity", "crawl");
						cmd.put("schedule", ds.schedule);
						Map<String, Object> cmds = (Map) colMap.get("cmds");
						cmds.put(newId, cmd);
					}
					res.add(ds);
				}
			}
		}
		Map<String, Object> dss = new HashMap<String, Object>();
		dss.put("datasources", res);
		dss.put("location", "DATA");
		dss.put("file", "datasources.yml");
		return dss;
	}

	private void fixScheduledCommands(Map<String, Object> yamlMap) {
		Map<String, Object> colsMap = (Map) yamlMap.get("collectionsMap");
		for (Map.Entry<String, Object> e : colsMap.entrySet()) {
			Map<String, Object> colMap = (Map) e.getValue();
			Map<String, Object> cmdsMap = (Map) colMap.get("cmds");
			if (cmdsMap != null) {
				for (Map.Entry<String, Object> e1 : cmdsMap.entrySet()) {
					Map<String, Object> cmd = (Map) e1.getValue();
					cmd.remove("description");
					String name = (String) cmd.get("name");
					if (name == null) {
						name = "";
					}
					name = name + " (cleaned)";
					cmd.put("name", name);
				}
			}
		}
	}

	public int removeAutocompleteDirs() {
		out.println("### Removing old autocomplete data files...");

		boolean core = isCoreEnabled();
		PrintStream errOut = core ? err : out;

		if (null == dataDir) {
			out.println("dataDir unknown, skipping autocomplete data file removal");
			errOut.close();
			return 0;
		}

		List<File> autocompleteDirs = new ArrayList<File>(10);
		findSubDirsByName(dataDir, "autocomplete", autocompleteDirs);
		boolean failure = false;

		for (File dir : autocompleteDirs) {
			try {
				FileUtils.deleteDirectory(dir);
			} catch (IOException e) {
				errOut.println("Unable to delete autocomplete dir: " + dir.getAbsolutePath());
				e.printStackTrace(err);
				failure = true;
			}
		}

		return failure ? onlyProblemIf(errOut, core, "Core") : 0;
	}

	public int upgradeDihConfigs() {
		out.println("### Upgrading dih configuration files...");

		boolean failure = false;

		boolean core = isCoreEnabled();
		PrintStream errOut = core ? err : out;

		File solrConfDir = new File(confDir, "solr");
		if ((!solrConfDir.exists()) || (!solrConfDir.isDirectory())) {
			errOut.println("solr configuration directory not found, not upgrading dih configs: "
					+ solrConfDir.getAbsolutePath());
			return onlyProblemIf(errOut, core, "Core");
		}

		Collection<File> confFiles = FileUtils.listFiles(solrConfDir,
				FileFilterUtils.makeFileOnly(new WildcardFileFilter("dataconfig_*.xml")), TrueFileFilter.INSTANCE);

		XPathFactory xpathFactory = XPathFactory.newInstance();
		for (File confFile : confFiles) {
			out.println("Processing " + confFile.getAbsolutePath() + " ...");

			InputStream is = null;
			FileOutputStream os = null;
			try {
				is = new FileInputStream(confFile);
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document document = builder.parse(is);

				boolean upgraded = false;

				String[] fields = { "data_source", "data_source_type", "data_source_name" };

				for (String field : fields) {
					XPath xpath = xpathFactory.newXPath();
					Element el = (Element) xpath.evaluate("/dataConfig/document/entity[@name='root']/field[@column='" + field
							+ "']", document, XPathConstants.NODE);
					if (el != null) {
						upgraded = true;
						el.getParentNode().removeChild(el);
					}
				}

				if (upgraded) {
					os = writeXML(confFile, document);

					out.println("Writing upgraded " + confFile.getAbsolutePath());
				} else {
					out.println("No need to upgrade " + confFile.getAbsolutePath());
				}
			} catch (Exception e) {
				errOut.println("Exception upgrading " + confFile.getAbsolutePath() + ": " + e.getMessage());
				e.printStackTrace(errOut);
				errOut.println();
				failure = true;
			} finally {
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(os);
			}
		}

		return failure ? onlyProblemIf(errOut, core, "Core") : 0;
	}

	public int fixHighlightingConfig() {
		out.println("### Upgrading highlighting configuration in solrconfig ...");

		boolean core = isCoreEnabled();
		PrintStream errOut = core ? err : out;

		File solrConfDir = new File(confDir, "solr");
		if ((!solrConfDir.exists()) || (!solrConfDir.isDirectory())) {
			errOut.println("solr configuration directory not found, not upgrading: " + solrConfDir.getAbsolutePath());
			return onlyProblemIf(errOut, core, "Core");
		}

		Collection<File> confFiles = FileUtils.listFiles(solrConfDir,
				FileFilterUtils.makeFileOnly(FileFilterUtils.nameFileFilter("solrconfig.xml")), TrueFileFilter.INSTANCE);

		boolean failure = false;

		for (File confFile : confFiles) {
			out.println("Processing " + confFile.getAbsolutePath() + " ...");
			try {
				fixHighlightingConfig(confFile);
			} catch (Exception e) {
				errOut.println("Exception upgrading " + confFile.getAbsolutePath() + ": " + e.getMessage());
				e.printStackTrace(errOut);
				errOut.println();
				failure = true;
			}
		}

		return failure ? onlyProblemIf(errOut, core, "Core") : 0;
	}

	public void fixHighlightingConfig(File confFile) throws Exception {
		InputStream is = null;
		FileOutputStream os = null;
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			XPath xpath = XPathFactory.newInstance().newXPath();

			is = new FileInputStream(confFile);
			Document document = builder.parse(is);

			boolean hasHighlightComponent = ((Boolean) xpath.evaluate("//searchComponent[@name='highlight']", document,
					XPathConstants.BOOLEAN)).booleanValue();

			if (hasHighlightComponent) {
				out.println("Highlight component found, no need to upgrade " + confFile.getAbsolutePath());
			} else {
				Element h = (Element) xpath.evaluate("//highlighting", document, XPathConstants.NODE);

				if (h == null) {
					out.println("no <highlighting/> found, no need to upgrade " + confFile.getAbsolutePath());
				} else {
					Element component = document.createElement("searchComponent");
					component.setAttribute("class", "solr.HighlightComponent");
					component.setAttribute("name", "highlight");

					h.getParentNode().replaceChild(component, h);
					component.appendChild(h);

					os = writeXML(confFile, document);
					out.println("Writing upgraded " + confFile.getAbsolutePath());
				}
			}
		} finally {
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(os);
		}
	}

	public int upgradeFieldMappingConfig() {
		out.println("### Upgrading field mapping configuration in solrconfig ...");

		boolean core = isCoreEnabled();
		PrintStream errOut = core ? err : out;

		File solrConfDir = new File(confDir, "solr");
		if ((!solrConfDir.exists()) || (!solrConfDir.isDirectory())) {
			errOut.println("solr configuration directory not found, not upgrading: " + solrConfDir.getAbsolutePath());
			return onlyProblemIf(errOut, core, "Core");
		}

		boolean failure = false;

		Collection<File> confFiles = FileUtils.listFiles(solrConfDir,
				FileFilterUtils.makeFileOnly(FileFilterUtils.nameFileFilter("solrconfig.xml")), TrueFileFilter.INSTANCE);

		XPathFactory xpathFactory = XPathFactory.newInstance();
		for (File confFile : confFiles) {
			out.println("Processing " + confFile.getAbsolutePath() + " ...");

			InputStream is = null;
			FileOutputStream os = null;
			try {
				is = new FileInputStream(confFile);
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document document = builder.parse(is);

				boolean upgraded = false;

				XPath xpath = xpathFactory.newXPath();
				Element el = (Element) xpath.evaluate("/config/requestHandler[@name='/fmap']", document, XPathConstants.NODE);
				if (el == null) {
					upgraded = true;

					el = document.createElement("requestHandler");
					el.setAttribute("name", "/fmap");
					el.setAttribute("class", "gaia.handler.FieldMappingRequestHandler");
					document.getDocumentElement().appendChild(el);
				}

				Element chainEl = (Element) xpath.evaluate("/config/updateRequestProcessorChain[@name='gaia-update-chain']",
						document, XPathConstants.NODE);
				if (chainEl != null) {
					Element processorEl = (Element) xpath.evaluate(
							"./processor[@class='gaia.update.FieldMappingUpdateProcessorFactory']", chainEl, XPathConstants.NODE);
					if (processorEl == null) {
						upgraded = true;

						processorEl = document.createElement("processor");
						processorEl.setAttribute("class", "gaia.update.FieldMappingUpdateProcessorFactory");

						Element runProcessorEl = (Element) xpath.evaluate(
								"./processor[@class='solr.DistributedUpdateProcessorFactory']", chainEl, XPathConstants.NODE);
						if (runProcessorEl != null) {
							chainEl.insertBefore(processorEl, runProcessorEl);
						} else {
							chainEl.insertBefore(processorEl, chainEl.getFirstChild());
						}
					}
				}

				if (upgraded) {
					os = writeXML(confFile, document);

					out.println("Writing upgraded " + confFile.getAbsolutePath());
				} else {
					out.println("No need to upgrade " + confFile.getAbsolutePath());
				}
			} catch (Exception e) {
				errOut.println("Exception upgrading " + confFile.getAbsolutePath() + ": " + e.getMessage());
				e.printStackTrace(errOut);
				errOut.println();
				failure = true;
			} finally {
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(os);
			}
		}

		return failure ? onlyProblemIf(errOut, core, "Core") : 0;
	}

	public int upgradeSchema() {
		out.println("### Upgrading Solr schema ...");

		boolean core = isCoreEnabled();
		PrintStream errOut = core ? err : out;

		File solrConfDir = new File(confDir, "solr");
		if ((!solrConfDir.exists()) || (!solrConfDir.isDirectory())) {
			errOut.println("solr configuration directory not found, not upgrading similarity configuration: "
					+ solrConfDir.getAbsolutePath());
			return onlyProblemIf(errOut, core, "Core");
		}

		Collection<File> schemaFiles = FileUtils.listFiles(solrConfDir,
				FileFilterUtils.makeFileOnly(FileFilterUtils.nameFileFilter("schema.xml")), TrueFileFilter.INSTANCE);

		for (File schemaFile : schemaFiles) {
			out.println("Upgrading " + schemaFile.getAbsolutePath());
			String content;
			try {
				content = FileUtils.readFileToString(schemaFile, "UTF-8");
			} catch (IOException e) {
				errOut.println("Could not read file " + schemaFile.getAbsolutePath() + ": " + e.getMessage());
				errOut.close();
				return onlyProblemIf(errOut, core, "Core");
			}

			boolean upgraded = false;
			if ((content.contains("<similarityProvider")) || (content.contains("</similarityProvider"))
					|| (content.contains("gaia.admin.collection.GaiaSimilarityFactory"))) {
				content = content.replace("<similarityProvider", "<similarity");
				content = content.replace("</similarityProvider", "</similarity");
				content = content.replace("gaia.admin.collection.GaiaSimilarityFactory",
						"gaia.similarity.GaiaSimilarityFactory");
				upgraded = true;
			}
			if (content.contains(" class=\"SnowballPorterFilterFactory\"")) {
				content = content.replace(" class=\"SnowballPorterFilterFactory\"",
						" class=\"solr.SnowballPorterFilterFactory\"");

				upgraded = true;
			}
			if (upgraded)
				try {
					out.println("Writing upgraded " + schemaFile.getAbsolutePath());
					FileUtils.writeStringToFile(schemaFile, content, "UTF-8");
				} catch (IOException e) {
					errOut.println("Could not save corrected file " + schemaFile.getAbsolutePath() + ": " + e.getMessage());
					errOut.close();
					return onlyProblemIf(errOut, core, "Core");
				}
			else {
				out.println("No need to upgrade " + schemaFile.getAbsolutePath());
			}
		}

		errOut.close();
		return 0;
	}

	int removeCommitWithinUpdateProcessorFactory() {
		out.println("### Removing CommitWithinUpdateProcessorFactory in solrconfig ...");

		boolean core = isCoreEnabled();
		PrintStream errOut = core ? err : out;

		File solrConfDir = new File(confDir, "solr");
		if ((!solrConfDir.exists()) || (!solrConfDir.isDirectory())) {
			errOut.println("solr configuration directory not found, not upgrading: " + solrConfDir.getAbsolutePath());
			return onlyProblemIf(errOut, core, "Core");
		}

		boolean failure = false;

		Collection<File> confFiles = FileUtils.listFiles(solrConfDir,
				FileFilterUtils.makeFileOnly(FileFilterUtils.nameFileFilter("solrconfig.xml")), TrueFileFilter.INSTANCE);

		XPathFactory xpathFactory = XPathFactory.newInstance();
		for (File confFile : confFiles) {
			out.println("Processing " + confFile.getAbsolutePath() + " ...");

			InputStream is = null;
			FileOutputStream os = null;
			try {
				is = new FileInputStream(confFile);
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document document = builder.parse(is);

				boolean upgraded = false;

				XPath xpath = xpathFactory.newXPath();

				Element commitWithinFactory = (Element) xpath.evaluate(
						"/config/updateRequestProcessorChain/processor[@class='gaia.update.CommitWithinUpdateProcessorFactory']",
						document, XPathConstants.NODE);
				if (commitWithinFactory != null) {
					out.println("Removing CommitWithinUpdateProcessorFactory.");
					commitWithinFactory.getParentNode().removeChild(commitWithinFactory);
					upgraded = true;
				}

				if (upgraded) {
					os = writeXML(confFile, document);

					out.println("Writing upgraded " + confFile.getAbsolutePath());
				} else {
					out.println("No need to upgrade " + confFile.getAbsolutePath());
				}
			} catch (Exception e) {
				errOut.println("Exception upgrading " + confFile.getAbsolutePath() + ": " + e.getMessage());
				e.printStackTrace(errOut);
				errOut.println();
				failure = true;
			} finally {
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(os);
			}
		}

		return failure ? onlyProblemIf(errOut, core, "Core") : 0;
	}

	int removeIndexDefaults() {
		out.println("### Removing indexDefaults in solrconfig ...");

		boolean core = isCoreEnabled();
		PrintStream errOut = core ? err : out;

		File solrConfDir = new File(confDir, "solr");
		if ((!solrConfDir.exists()) || (!solrConfDir.isDirectory())) {
			errOut.println("solr configuration directory not found, not upgrading: " + solrConfDir.getAbsolutePath());

			return onlyProblemIf(errOut, core, "Core");
		}

		boolean failure = false;

		Collection<File> confFiles = FileUtils.listFiles(solrConfDir,
				FileFilterUtils.makeFileOnly(FileFilterUtils.nameFileFilter("solrconfig.xml")), TrueFileFilter.INSTANCE);

		XPathFactory xpathFactory = XPathFactory.newInstance();
		for (File confFile : confFiles) {
			out.println("Processing " + confFile.getAbsolutePath() + " ...");

			InputStream is = null;
			FileOutputStream os = null;
			try {
				is = new FileInputStream(confFile);
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

				Document document = builder.parse(is);

				boolean upgraded = false;

				XPath xpath = xpathFactory.newXPath();

				Element indexDefaults = (Element) xpath.evaluate("/config/indexDefaults", document, XPathConstants.NODE);

				if (indexDefaults != null) {
					out.println("Removing indexDefaults.");
					indexDefaults.getParentNode().removeChild(indexDefaults);
					upgraded = true;
				}

				if (upgraded) {
					os = writeXML(confFile, document);

					out.println("Writing upgraded " + confFile.getAbsolutePath());
				} else {
					out.println("No need to upgrade " + confFile.getAbsolutePath());
				}
			} catch (Exception e) {
				errOut.println("Exception upgrading " + confFile.getAbsolutePath() + ": " + e.getMessage());

				e.printStackTrace(errOut);
				errOut.println();
				failure = true;
			} finally {
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(os);
			}
		}

		return failure ? onlyProblemIf(errOut, core, "Core") : 0;
	}

	public int upgradeMainIndexToIndexConfigInSolrConfig() {
		out.println("### Upgrading indexConfig configuration in Solr config ...");

		boolean core = isCoreEnabled();
		PrintStream errOut = core ? err : out;

		File solrConfDir = new File(confDir, "solr");
		if ((!solrConfDir.exists()) || (!solrConfDir.isDirectory())) {
			errOut.println("solr configuration directory not found, not upgrading indexConfig configuration: "
					+ solrConfDir.getAbsolutePath());

			return onlyProblemIf(errOut, core, "Core");
		}

		Collection<File> configFiles = FileUtils.listFiles(solrConfDir,
				FileFilterUtils.makeFileOnly(FileFilterUtils.nameFileFilter("solrconfig.xml")), TrueFileFilter.INSTANCE);

		for (File configFile : configFiles) {
			out.println("Upgrading indexConfig configuration in " + configFile.getAbsolutePath());
			String content;
			try {
				content = FileUtils.readFileToString(configFile, "UTF-8");
			} catch (IOException e) {
				errOut.println("Could not read file " + configFile.getAbsolutePath() + ": " + e.getMessage());
				errOut.close();
				return onlyProblemIf(errOut, core, "Core");
			}
			if ((content.contains("<mainIndex")) || (content.contains("</mainIndex"))) {
				content = content.replaceAll("<mainIndex", "<indexConfig");
				content = content.replaceAll("</mainIndex", "</indexConfig");
				try {
					out.println("Writing upgraded " + configFile.getAbsolutePath());
					FileUtils.writeStringToFile(configFile, content, "UTF-8");
				} catch (IOException e) {
					errOut.println("Could not save corrected file " + configFile.getAbsolutePath() + ": " + e.getMessage());
					errOut.close();
					return onlyProblemIf(errOut, core, "Core");
				}
			} else {
				out.println("No need to upgrade " + configFile.getAbsolutePath());
			}
		}

		errOut.close();
		return 0;
	}

	private FileOutputStream writeXML(File confFile, Document document) throws FileNotFoundException,
			TransformerFactoryConfigurationError, TransformerConfigurationException, TransformerException {
		FileOutputStream os = new FileOutputStream(confFile);
		Result result = new StreamResult(os);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty("method", "xml");
		transformer.setOutputProperty("indent", "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		DOMSource source = new DOMSource(document);
		transformer.transform(source, result);
		return os;
	}

	private Properties loadMasterDotConf() {
		try {
			return loadMasterDotConf(confDir);
		} catch (IOException e) {
			throw new RuntimeException("Can't parse master.conf in " + confDir.getAbsolutePath() + ": " + e.getMessage(), e);
		}
	}

	private int onlyProblemIf(PrintStream errOut, boolean enabled, String system) {
		if (enabled)
			return -1;

		errOut.println("  (This should not cause problems unless you edit master.conf in the future to enable the LWE "
				+ system + ")");
		return 0;
	}

	private boolean isCoreEnabled() {
		return Boolean.parseBoolean(loadMasterDotConf().getProperty("lwecore.enabled", "false"));
	}

	private boolean isUiEnabled() {
		return Boolean.parseBoolean(loadMasterDotConf().getProperty("lweui.enabled", "false"));
	}

	private boolean isConnectorsEnabled() {
		return Boolean.parseBoolean(loadMasterDotConf().getProperty("lweconnectors.enabled", "false"));
	}

	public static void main(String[] args) {
		System.exit(exec(args));
	}

	public static int exec(String[] args) {
		int i = 0;
		boolean fail = false;

		File lweAppDir = null;
		File lweDataDir = null;
		File lweConfDir = null;
		PrintStream out = NULL_STREAM;

		while (i < args.length) {
			String arg = args[(i++)];

			if (arg.equals("-verbose")) {
				out = System.out;
			} else {
				if (arg.equals("-help")) {
					printUsage();
					return 0;
				}

				if (arg.equals("-lwe_app_dir")) {
					if (i < args.length) {
						lweAppDir = new File(args[(i++)]);
					} else {
						System.err.println("-lwe_app_dir requires a value");
						fail = true;
					}

				} else if (arg.equals("-lwe_data_dir")) {
					if (i < args.length) {
						lweDataDir = new File(args[(i++)]);
					} else {
						System.err.println("-lwe_data_dir requires a value");
						fail = true;
					}

				} else if (arg.equals("-lwe_conf_dir")) {
					if (i < args.length) {
						lweConfDir = new File(args[(i++)]);
					} else {
						System.err.println("-lwe_conf_dir requires a value");
						fail = true;
					}
				} else {
					System.err.println("unexpected argument: " + arg);
					fail = true;
				}
			}
		}
		if (null == lweDataDir) {
			System.err.println("-lwe_data_dir must be specified");
			fail = true;
		} else if (!lweDataDir.exists()) {
			System.err.println("-lwe_data_dir must exist: " + lweDataDir.getAbsolutePath());
			fail = true;
		}
		if (null == lweConfDir) {
			System.err.println("-lwe_conf_dir must be specified");
			fail = true;
		} else if (!lweConfDir.exists()) {
			System.err.println("-lwe_conf_dir must exist: " + lweConfDir.getAbsolutePath());
			fail = true;
		}
		if (null == lweAppDir) {
			System.err.println("-lwe_app_dir must be specified");
			fail = true;
		} else if (!lweAppDir.exists()) {
			System.err.println("-lwe_app_dir must exist: " + lweAppDir.getAbsolutePath());
			fail = true;
		}

		if (fail) {
			printUsage();
			return -1;
		}

		MetadataUpgradeTool tool = new MetadataUpgradeTool(out, System.err, lweAppDir, lweConfDir, lweDataDir);

		return tool.upgrade();
	}

	public static void printUsage() {
		PrintStream o = System.out;
		o.println("Help:");

		o.println(" -help                  prints this message to stdout and exits");
		o.println(" -verbose               causes progress info to be written to stdout");
		o.println(" -lwe_conf_dir <path>   conf directory of your existing installation");
		o.println(" -lwe_data_dir <path>   data directory of your existing installation");
		o.println(" -lwe_app_dir <path>    app directory of your existing installation");
	}

	public static Properties loadMasterDotConf(File confDir) throws IOException {
		FileInputStream fis = new FileInputStream(new File(confDir, "master.conf"));
		Properties conf = new Properties();
		try {
			conf.load(fis);
			fis.close();
		} finally {
			IOUtils.closeQuietly(fis);
		}
		return conf;
	}

	public static String getJrubyClasspath(File workDir) {
		File warlib = getGlobedRelativeFile(workDir, new String[] { "lib" });
		File conf = getGlobedRelativeFile(workDir, new String[] { "config" });

		return conf.getPath() + File.pathSeparatorChar + warlib.getPath() + File.separatorChar + "*";
	}

	public static String getRakePath(File workDir) {
		return getGlobedRelativeFile(workDir, new String[] { "gems", "gems", "rake-*", "bin", "rake" }).getPath();
	}

	public static File getGlobedRelativeFile(File directory, String[] globs) {
		if (directory == null) {
			throw new NullPointerException("directory must not be null");
		}
		if (globs == null) {
			throw new NullPointerException("globs must not be null");
		}
		File relFile = new File(".");
		File absFile = directory;
		for (String glob : globs) {
			// File[] subFiles = absFile.listFiles(new WildcardFileFilter(glob));

			File[] subFiles = FileUtils.listFiles(absFile, new WildcardFileFilter(glob), null).toArray(new File[0]);
			int num = subFiles.length;
			if (1 != num) {
				throw new IllegalStateException("Found " + num + " files matching " + glob + " in " + absFile);
			}

			absFile = subFiles[0];
			relFile = new File(relFile, absFile.getName());
		}
		return relFile;
	}

	public static void findSubDirsByName(File baseDir, String dirName, Collection<File> dirs) {
		for (File subdir : FileUtils.listFiles(baseDir, null, DirectoryFileFilter.DIRECTORY)) {
			// baseDir.listFiles(DirectoryFileFilter.DIRECTORY)) {
			if (dirName.equals(subdir.getName())) {
				dirs.add(subdir);
			}
			findSubDirsByName(subdir, dirName, dirs);
		}
	}

	private static File getNewAppDir() throws URISyntaxException {
		String newAppDirOverride = System.getProperty("new_app_dir");

		if (null != newAppDirOverride) {
			return new File(newAppDirOverride);
		}

		File thisJar = new File(TOOL_JAR_URL.toURI());

		return thisJar.getParentFile().getParentFile();
	}

	private static File getNewConfDir() throws URISyntaxException {
		String newConfDirOverride = System.getProperty("new_conf_dir");

		if (null != newConfDirOverride) {
			return new File(newConfDirOverride);
		}

		File newAppDir = getNewAppDir();
		if (null == newAppDir)
			return null;

		return FileUtils.getFile(newAppDir, new String[] { "..", "conf", "jetty" });
	}

	private Version getOldVersion() {
		return new Version(appDir);
	}

	private static final class Version {
		public final Properties props;
		public final String version;
		public final Integer major;
		public final Integer minor;
		public final Integer bugfix;

		public Version(File appDir) {
			Properties propsT = null;
			String versionT = null;
			Integer majorT = null;
			Integer minorT = null;
			Integer bugfixT = null;
			try {
				if (null != appDir) {
					File versionFile = new File(appDir, "VERSION.txt");
					if (versionFile.exists()) {
						FileInputStream fis = new FileInputStream(versionFile);
						try {
							Properties p = new Properties();
							p.load(fis);
							fis.close();
							propsT = p;
						} finally {
							IOUtils.closeQuietly(fis);
						}
						versionT = propsT.getProperty("version");
						if (null != versionT) {
							String[] parts = versionT.split("\\.");
							if (0 < parts.length) {
								majorT = Integer.valueOf(parts[0]);
							}
							if (1 < parts.length) {
								minorT = Integer.valueOf(parts[1]);
							}
							if (2 < parts.length) {
								bugfixT = Integer.valueOf(parts[2]);
							}
						}
					}
				}
			} catch (Exception e) {
			}
			props = propsT;
			version = versionT;
			major = majorT;
			minor = minorT;
			bugfix = bugfixT;
		}
	}
}
