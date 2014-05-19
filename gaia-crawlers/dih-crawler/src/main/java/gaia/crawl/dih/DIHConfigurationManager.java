package gaia.crawl.dih;

import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.utils.XMLUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class DIHConfigurationManager {
	private static transient Logger LOG = LoggerFactory.getLogger(DIHConfigurationManager.class);

	private static String SOLRCONFIG_FILENAME = "solrconfig.xml";
	private static String SCHEMACONFIG_FILENAME = "schema.xml";
	private static String SOLRXML_FILENAME = "solr.xml";
	private static String JDBC_LEAK_PREVENTION_JAR = "jdbc-leak-prevention.jar";

	private static final Map<String, String> ATTRIBS = new HashMap<String, String>();
	private File dir;
	private File jarsDir = null;

	public DIHConfigurationManager(File dir) {
		this.dir = dir;
	}

	public void initConfigs() throws IOException {
		dir.mkdirs();
		File solrXmlFile = new File(dir, SOLRXML_FILENAME);
		if (!solrXmlFile.exists()) {
			copyResourceToFile("/solr/" + SOLRXML_FILENAME, solrXmlFile);
		}
		File confDif = new File(dir, "/conf");
		confDif.mkdirs();
		File solrConfigFile = new File(confDif, SOLRCONFIG_FILENAME);
		if (!solrConfigFile.exists()) {
			copyResourceToFile("/solr/conf/" + SOLRCONFIG_FILENAME, solrConfigFile);
		}
		File schemaConfigFile = new File(confDif, SCHEMACONFIG_FILENAME);
		if (!schemaConfigFile.exists())
			copyResourceToFile("/solr/conf/" + SCHEMACONFIG_FILENAME, schemaConfigFile);
	}

	public void generateDihConfigs(DataSource dataSource) throws IOException {
		Document solrConfigDoc = readXmlConfig("conf" + File.separator + SOLRCONFIG_FILENAME);
		Element oldRequestHandlerElement = null;
		Element oldDataConfigElement = null;
		try {
			oldRequestHandlerElement = (Element) XMLUtils
					.getNode(
							"/config/requestHandler[@class='org.apache.solr.handler.dataimport.DataImportHandler'][@managedByGaia='true'][@name='/dataimport_"
									+ dataSource.getDataSourceId() + "']", solrConfigDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException("XPath exception while getting DIH request handler", e);
		}
		Element dataConfigElement;
		if (oldRequestHandlerElement != null) {
			String configFile = JDBCUtils.getConfig(oldRequestHandlerElement);
			Document dataConfigDoc;
			try {
				dataConfigDoc = readXmlConfig("conf" + File.separator + configFile);
			} catch (IOException e) {
				throw new RuntimeException("Couldn't load DIH data config", e);
			}
			oldDataConfigElement = dataConfigDoc.getDocumentElement();

			dataConfigElement = JDBCUtils.dataConfigXml(dataSource, dataConfigDoc, oldDataConfigElement);

			if (dataConfigElement == null) {
				dataConfigElement = JDBCUtils.dataConfigXml(dataSource, dataConfigDoc);
			}
			Element requestHandlerElement = JDBCUtils.requestHandlerXml(dataSource, solrConfigDoc,
					(Element) oldRequestHandlerElement.cloneNode(true));
			solrConfigDoc.getDocumentElement().replaceChild(requestHandlerElement, oldRequestHandlerElement);
		} else {
			Element requestHandlerElement = JDBCUtils.requestHandlerXml(dataSource, solrConfigDoc, null);
			solrConfigDoc.getDocumentElement().appendChild(requestHandlerElement);
			dataConfigElement = JDBCUtils.dataConfigXml(dataSource, solrConfigDoc);
		}
		String dataConfigFilename = JDBCUtils.dataConfigFilename(dataSource);

		saveConfig("conf" + File.separator + dataConfigFilename, dataConfigElement);
		saveConfig("conf" + File.separator + SOLRCONFIG_FILENAME, solrConfigDoc.getDocumentElement());
	}

	public void copyJdbcJars(File crawlerJarsDir) throws IOException {
		if (!crawlerJarsDir.exists()) {
			crawlerJarsDir.mkdirs();
		}

		jarsDir = new File(dir, "lib");
		jarsDir.mkdirs();

		File[] collectionJars = crawlerJarsDir.listFiles();
		File[] existingJars = jarsDir.listFiles();

		for (File f1 : collectionJars) {
			if (!f1.isDirectory()) {
				boolean isPresent = false;
				for (File f2 : existingJars) {
					if ((!f2.isDirectory()) && (f2.getName().equals(f1.getName())) && (f2.length() == f1.length())) {
						isPresent = true;
					}

				}

				if (!isPresent) {
					FileUtils.copyFileToDirectory(f1, jarsDir, true);
				}
			}
		}

		for (File f1 : existingJars) {
			if (!f1.isDirectory()) {
				boolean isPresent = false;
				for (File f2 : collectionJars) {
					if ((!f2.isDirectory()) && (f2.getName().equals(f1.getName())) && (f2.length() == f1.length())) {
						isPresent = true;
					}

				}

				if ((!isPresent) && (!JDBC_LEAK_PREVENTION_JAR.equals(f1.getName()))) {
					boolean status = FileUtils.deleteQuietly(f1);
					if (!status) {
						f1.deleteOnExit();
					}
				}

			}

		}

		File jdbcLeakJar = new File(jarsDir, JDBC_LEAK_PREVENTION_JAR);
		if (!jdbcLeakJar.exists()) {
			copyResourceToFile("/jdbc/jdbc-leak-prevention-jar.bin", jdbcLeakJar);
		}

		Document doc = readXmlConfig("conf" + File.separator + SOLRCONFIG_FILENAME);
		Element libElement = null;
		try {
			libElement = (Element) XMLUtils.getNode("/config/lib", doc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException("XPath exception while getting DIH request handler", e);
		}
		if (libElement == null) {
			libElement = doc.createElement("lib");
		}
		libElement.setAttribute("dir", jarsDir.getName());
		saveConfig("conf" + File.separator + SOLRCONFIG_FILENAME, doc.getDocumentElement());
	}

	public void removeConfigs() {
		if (dir.exists())
			FileUtils.deleteQuietly(dir);
	}

	public void removeDihPropFile(DataSourceId dsId) {
		if (!dir.exists()) {
			return;
		}
		File confDir = new File(dir, "conf");
		if (!confDir.exists()) {
			return;
		}
		File propFile = new File(confDir, "dataimport_" + dsId.getId() + ".properties");
		if (propFile.exists())
			propFile.delete();
	}

	public boolean isDihPropFileExists(DataSourceId dsId) {
		if (!dir.exists()) {
			return false;
		}
		File confDir = new File(dir, "conf");
		if (!confDir.exists()) {
			return false;
		}
		File propFile = new File(confDir, "dataimport_" + dsId.getId() + ".properties");
		if (propFile.exists()) {
			return true;
		}
		return false;
	}

	Document readXmlConfig(String fileName) throws IOException {
		File configFile = new File(dir, fileName);
		InputStream is = new FileInputStream(configFile);
		try {
			return XMLUtils.loadDocument(is);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Could not parse " + fileName, e);
		} catch (SAXException e) {
			throw new RuntimeException("Could not parse " + fileName, e);
		} finally {
			if (is != null)
				IOUtils.closeQuietly(is);
		}
	}

	private void saveConfig(String filename, Element rootElement) throws IOException {
		String content = null;
		try {
			content = XMLUtils.toString(rootElement, ATTRIBS);
		} catch (TransformerException e) {
			throw new RuntimeException("Could not save " + filename, e);
		}
		File file = new File(dir, filename);
		FileOutputStream os = new FileOutputStream(file);
		try {
			IOUtils.write(content, os);
		} finally {
			if (os != null)
				IOUtils.closeQuietly(os);
		}
	}

	private void copyResourceToFile(String resourcePath, File file) throws IOException {
		InputStream is = getClass().getResourceAsStream(resourcePath);
		OutputStream os = null;
		try {
			os = new FileOutputStream(file);
			IOUtils.copy(is, os);
		} finally {
			if (os != null)
				IOUtils.closeQuietly(os);
			if (is != null)
				IOUtils.closeQuietly(is);
		}
	}

	static {
		ATTRIBS.put("method", "xml");
		ATTRIBS.put("indent", "yes");
		ATTRIBS.put("{http://xml.apache.org/xslt}indent-amount", "2");
	}
}
