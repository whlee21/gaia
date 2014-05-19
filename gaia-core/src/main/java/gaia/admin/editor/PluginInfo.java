package gaia.admin.editor;

import javax.persistence.Entity;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import gaia.IdObject;
import gaia.utils.XMLUtils;

@Entity
public class PluginInfo extends IdObject {
	private PluginType type;
	private boolean provided;
	private boolean readOnly;
	private String className;
	private String name;
	private boolean enabled;
	private String configSnippet;

	public PluginInfo() {
	}

	public PluginInfo(String configSnippet) {
		this(configSnippet, true, false);
	}

	public PluginInfo(String configSnippet, boolean enabled, boolean readOnly) {
		setConfigSnippet(configSnippet);
		this.enabled = enabled;
		this.readOnly = readOnly;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isProvided() {
		return provided;
	}

	public void setProvided(boolean provided) {
		this.provided = provided;
	}

	public PluginType getType() {
		return type;
	}

	public String getConfigSnippet() {
		return configSnippet;
	}

	public void setConfigSnippet(String configSnippet) {
		this.configSnippet = configSnippet;
		try {
			Node node = XMLUtils.loadDocument(configSnippet).getDocumentElement();
			NamedNodeMap attributes = node.getAttributes();
			className = attributes.getNamedItem("class").getNodeValue();
			if ((className != null)
					&& ((className.startsWith("org.apache.lucene")) || (className.startsWith("org.apache.solr")) || (className
							.startsWith("solr")))) {
				provided = true;
			}
			name = attributes.getNamedItem("name").getNodeValue();
			String nodeName = node.getNodeName();
			if (nodeName.equals("searchComponent"))
				type = PluginType.SEARCH_COMPONENT;
			else if (nodeName.equals("requestHandler"))
				type = PluginType.REQUEST_HANDLER;
			else if (nodeName.equals("fieldType"))
				type = PluginType.FIELD_TYPE;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getClassName() {
		return className;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return "PluginInfo{className='" + className + '\'' + ", type=" + type + ", provided=" + provided + ", readOnly="
				+ readOnly + ", name='" + name + '\'' + ", enabled=" + enabled + ", configSnippet='" + configSnippet + '\''
				+ '}';
	}
}
