package gaia.admin.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.core.SolrCore;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import gaia.utils.OSFileWriter;
import gaia.utils.StringUtils;
import gaia.utils.XMLUtils;

public abstract class EditableConfig {
	private static final int NUM_BACKUP_FILES_KEPT = 10;
	private static transient Logger LOG = LoggerFactory.getLogger(EditableConfig.class);

	public static final Map<String, String> ATTRIBS = new HashMap<String, String>();
	protected SolrCore core;
	protected Document configDoc;
	protected boolean tolerateMissing = false;
	private ConfigFileUpdater configFileUpdater;
	private ZkController zkController;

	public EditableConfig(SolrCore core) {
		this(core, null);
	}

	public EditableConfig(SolrCore core, ZkController zkController) {
		this(core, zkController, false);
	}

	public EditableConfig(SolrCore core, ZkController zkController, boolean tolerateMissing) {
		if (core == null) {
			throw new NullPointerException("SolrCore cannot be null");
		}
		this.core = core;
		this.zkController = zkController;
		this.tolerateMissing = tolerateMissing;

		boolean zooKeeperEnabled = zkController != null;

		if (!zooKeeperEnabled) {
			configFileUpdater = new LocalConfigFileUpdater(getBackupDir());
		} else {
			if ((core.getCoreDescriptor() == null) || (core.getCoreDescriptor().getCloudDescriptor() == null)) {
				throw new IllegalArgumentException(
						"You cannot use a SolrCore without a valid Core and Cloud Descriptor in ZooKeeper mode - EditableCoreConfig must be able to access the CloudDescriptor");
			}

			configFileUpdater = new ZKConfigFileUpdater(zkController, core);
		}
		try {
			configDoc = loadConfigFile(getName());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Document loadConfigFile(String fileName) throws IOException {
		Document document;
		synchronized (EditableConfig.class) {
			InputStream is = null;
			try {
				is = core.getResourceLoader().openResource(fileName);
			} catch (IOException e) {
				if (!tolerateMissing)
					throw e;
			}
			try {
				if ((is == null) && (tolerateMissing)) {
					DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					document = builder.newDocument();
				} else {
					document = XMLUtils.loadDocument(is);
				}
			} catch (SAXException e) {
				throw new RuntimeException(e);
			} catch (ParserConfigurationException e) {
				throw new RuntimeException(e);
			}
		}
		return document;
	}

	protected void removeChildElements(Node parent) {
		NodeList oldChildren = parent.getChildNodes();
		int numElements = oldChildren.getLength();
		for (int i = numElements - 1; i >= 0; i--) {
			Node node = oldChildren.item(i);
			if (node.getNodeType() == 1)
				parent.removeChild(node);
		}
	}

	public abstract Node getPluginNode(String paramString, PluginType paramPluginType);

	public Document getConfigDoc() {
		return configDoc;
	}

	public SolrCore getCore() {
		return core;
	}

	private File getBackupDir() {
		String dir = core.getResourceLoader().getConfigDir();
		File backupDir = new File(dir, "backup");
		return backupDir;
	}

	public void replacePlugins(Collection<PluginInfo> plugins, Collection<String> oldNames) {
		if (plugins.size() != oldNames.size()) {
			throw new RuntimeException("size mismatch between plugins and oldNames");
		}
		Iterator<String> oldIter = oldNames.iterator();
		for (PluginInfo plugin : plugins) {
			String oldName = (String) oldIter.next();
			try {
				Node pluginNode = getPluginNode(oldName, plugin.getType());

				if (pluginNode != null) {
					Node parentNode = pluginNode.getParentNode();

					Document newSnippet = XMLUtils.loadDocument(new ByteArrayInputStream(plugin.getConfigSnippet().getBytes(
							"UTF-8")));

					parentNode.removeChild(pluginNode);
					Node newNode = configDoc.importNode(newSnippet.getDocumentElement(), true);
					if (newNode != null)
						parentNode.appendChild(newNode);
				}
			} catch (IOException e) {
				LOG.error("Exception", e);
			} catch (SAXException e) {
				LOG.error("Exception", e);
			} catch (ParserConfigurationException e) {
				LOG.error("Exception", e);
			}
		}
	}

	public void replacePlugin(PluginInfo plugin, String oldName) {
		replacePlugins(Collections.singletonList(plugin), Collections.singletonList(oldName));
	}

	public void writeConfigFile(Collection<String> lines, File file) {
		try {
			configFileUpdater.writeLines(lines, file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void deleteConfigFile(File file) {
		configFileUpdater.deleteFile(file);
	}

	public void save() throws IOException {
		File outfile;
		if (zkController == null) {
			String configDir = core.getResourceLoader().getConfigDir();
			outfile = new File(configDir, getName());
		} else {
			outfile = new File(getName());
		}

		configFileUpdater.writeXml(configDoc, outfile);
	}

	public abstract String getName();

	protected void removeChildElements(Node parent, String elementName) {
		NodeList oldChildren = parent.getChildNodes();
		int numElements = oldChildren.getLength();
		for (int i = numElements - 1; i >= 0; i--) {
			Node node = oldChildren.item(i);
			if ((node != null) && (node.getNodeType() == 1) && (elementName.equals(node.getNodeName()))) {
				removeElementAndComment(node);
			}
		}
	}

	protected void removeElementAndComment(Node node) {
		if (node == null)
			return;
		Node parent = node.getParentNode();
		Node sibling = node.getPreviousSibling();
		parent.removeChild(node);

		Node commentSibling = null;

		if ((sibling != null) && (sibling.getNodeType() == 3) && ("".equals(sibling.getNodeValue().trim()))) {
			commentSibling = sibling.getPreviousSibling();
			parent.removeChild(sibling);
		}

		if ((commentSibling != null) && (commentSibling.getNodeType() == 8))
			parent.removeChild(commentSibling);
	}

	protected static Map<String, Object> attrsToMap(Node node) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();

		NamedNodeMap attrs = node.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			Node attr = attrs.item(i);
			result.put(attr.getNodeName(), attr.getNodeValue());
		}

		return result;
	}

	public final ZkController getZkController() {
		return zkController;
	}

	static {
		ATTRIBS.put("method", "xml");
		ATTRIBS.put("indent", "yes");
		ATTRIBS.put("{http://xml.apache.org/xslt}indent-amount", "2");
	}

	public static class ZKConfigFileUpdater extends EditableConfig.ConfigFileUpdater {
		private ZkController zkController;
		private SolrCore core;

		public ZKConfigFileUpdater(ZkController zkController, SolrCore core) {
			this.zkController = zkController;
			this.core = core;
		}

		void writeString(String string, File file) throws IOException {
			OutputStreamWriter writer = null;
			try {
				String confName = zkController
						.readConfigName(core.getCoreDescriptor().getCloudDescriptor().getCollectionName());

				byte[] bytes = string.getBytes(StringUtils.UTF_8);

				String path = "/configs/" + confName + "/" + file.getName();

				if (!zkController.getZkClient().exists(path, false).booleanValue())
					try {
						zkController.getZkClient().makePath(path, false);
					} catch (KeeperException.NodeExistsException e) {
					}
				zkController.getZkClient().setData(path, bytes, true);
			} catch (KeeperException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				if (writer != null)
					writer.close();
			}
		}

		void deleteFile(File file) {
			try {
				String confName = zkController
						.readConfigName(core.getCoreDescriptor().getCloudDescriptor().getCollectionName());

				String path = "/configs/" + confName + "/" + file.getName();

				zkController.getZkClient().delete(path, -1, false);
			} catch (KeeperException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		void writeXml(Node doc, File file) throws IOException {
			OutputStreamWriter writer = null;
			try {
				String confName = zkController
						.readConfigName(core.getCoreDescriptor().getCloudDescriptor().getCollectionName());

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				writer = new OutputStreamWriter(baos, StringUtils.UTF_8);
				Result result = new StreamResult(writer);
				XMLUtils.transform(doc, result, EditableConfig.ATTRIBS);

				zkController.getZkClient().setData("/configs/" + confName + "/" + file.getName(), baos.toByteArray(), true);
			} catch (TransformerException e) {
				throw new RuntimeException(e);
			} catch (KeeperException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				if (writer != null)
					writer.close();
			}
		}
	}

	public static class LocalConfigFileUpdater extends EditableConfig.ConfigFileUpdater {
		public LocalConfigFileUpdater() {
		}

		public LocalConfigFileUpdater(File backupDir) {
			super();
		}

		void writeString(String string, File file) throws IOException {
			OSFileWriter fw = new OSFileWriter(file);

			synchronized (EditableConfig.class) {
				backupFile(file, backupDir, new Date());

				FileUtils.writeStringToFile(fw.getWriteFile(), string, StringUtils.UTF_8.name());

				fw.flush();
			}
		}

		void deleteFile(File file) {
			file.delete();
		}

		void writeXml(Node doc, File file) throws IOException {
			OSFileWriter fw = new OSFileWriter(file);

			synchronized (EditableConfig.class) {
				backupFile(file, backupDir, new Date());

				FileOutputStream fis = new FileOutputStream(fw.getWriteFile());
				OutputStreamWriter writer = new OutputStreamWriter(fis, StringUtils.UTF_8);
				try {
					Result result = new StreamResult(writer);
					XMLUtils.transform(doc, result, EditableConfig.ATTRIBS);
				} catch (TransformerException e) {
					throw new RuntimeException(e);
				} finally {
					writer.close();
					fis.close();
				}

				fw.flush();
			}
		}
	}

	public static abstract class ConfigFileUpdater {
		protected File backupDir;

		public ConfigFileUpdater() {
		}

		public ConfigFileUpdater(File backupDir) {
			this.backupDir = backupDir;
		}

		void writeLines(Collection<String> lines, File file) throws IOException {
			StringWriter writer = new StringWriter();
			IOUtils.writeLines(lines, null, writer);
			writeString(writer.getBuffer().toString(), file);
			writer.close();
		}

		abstract void writeString(String paramString, File paramFile) throws IOException;

		abstract void writeXml(Node paramNode, File paramFile) throws IOException;

		abstract void deleteFile(File paramFile);

		protected void backupFile(File file, File backupDir, Date timestamp) throws IOException {
			if (backupDir == null) {
				return;
			}
			if ((!file.isFile()) || (!file.canRead())) {
				return;
			}
			backupDir.mkdirs();
			FileUtils.copyFile(file, new File(backupDir, file.getName() + "-" + timestamp.getTime()));

			final List<File> backupFiles = new ArrayList<File>();
			final Pattern backupFilePattern = Pattern.compile(file.getName() + "-\\d+");
			backupDir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					Matcher m = backupFilePattern.matcher(name);
					if (m.matches()) {
						backupFiles.add(new File(dir, name));
						return true;
					}
					return false;
				}
			});
			if (backupFiles.size() > NUM_BACKUP_FILES_KEPT) {
				Collections.sort(backupFiles);

				((File) backupFiles.get(0)).delete();
			}
		}
	}
}
