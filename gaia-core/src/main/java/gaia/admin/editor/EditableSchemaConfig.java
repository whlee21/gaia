package gaia.admin.editor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.FieldTypePluginLoader;
import org.apache.solr.schema.SchemaAware;
import org.apache.solr.schema.SchemaField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import gaia.similarity.GaiaSimilarityFactory;
import gaia.utils.XMLUtils;

public class EditableSchemaConfig extends EditableConfig {
	private static final String XPATH_FIELDTYPES_SECTION = "/schema/types";
	private static final String XPATH_FIELDTYPES_PREFIX = "/schema/types/*[self::fieldType|self::fieldtype]";
	private static final String CLASS_KEY = "class";
	private static final String XML_ANALYZER_KEY = "analyzer";
	private static final String XML_CHAR_FILTER_KEY = "charFilter";
	private static final String XML_TOKENIZER_KEY = "tokenizer";
	private static final String XML_TOKEN_FILTER_KEY = "filter";
	private static final String MAP_ANALYZER_KEY = "analyzers";
	private static final String MAP_CHAR_FILTER_KEY = "char_filters";
	private static final String MAP_TOKENIZER_KEY = "tokenizer";
	private static final String MAP_TOKEN_FILTER_KEY = "token_filters";
	private static final Set<String> MAP_ANALYSIS_CHAIN_KEYS = new HashSet<String>();

	private static transient Logger LOG = LoggerFactory.getLogger(EditableSchemaConfig.class);

	public EditableSchemaConfig(SolrCore core) {
		super(core);
	}

	public EditableSchemaConfig(SolrCore core, ZkController zkController) {
		super(core, zkController);
	}

	public void addCopyField(String src, String dest) {
		if (copyFieldExists(src, dest))
			return;

		Element schemaElement = configDoc.getDocumentElement();
		Element copyFieldElement = configDoc.createElement("copyField");
		copyFieldElement.setAttribute("source", src);
		copyFieldElement.setAttribute("dest", dest);
		schemaElement.appendChild(copyFieldElement);
	}

	public void addPlugin(PluginInfo plugin) {
		try {
			Document newSnippet = XMLUtils
					.loadDocument(new ByteArrayInputStream(plugin.getConfigSnippet().getBytes("UTF-8")));

			addFieldType(newSnippet.getDocumentElement());
		} catch (IOException e) {
			LOG.error("Exception", e);
		} catch (SAXException e) {
			LOG.error("Exception", e);
		} catch (ParserConfigurationException e) {
			LOG.error("Exception", e);
		}
	}

	private void addFieldType(Element typeNode) {
		try {
			FieldTypePluginLoader verifier = new FieldTypePluginLoader(core.getLatestSchema(),
					new HashMap<String, FieldType>(), new ArrayList<SchemaAware>(1));

			FieldType t = (FieldType) verifier.loadSingle(core.getResourceLoader(), typeNode);

			if (null == t)
				throw new RuntimeException("Unknown error instantiating FieldType");
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to verify FieldType definition is valid", e);
		}

		Document doc = getConfigDoc();
		Node newNode = doc == typeNode.getOwnerDocument() ? typeNode : doc.importNode(typeNode, true);

		Node old = getFieldTypeNode(typeNode.getAttribute("name"));

		if (null == old)
			try {
				Node fieldTypes = XMLUtils.getNode(XPATH_FIELDTYPES_SECTION, doc);
				fieldTypes.appendChild(newNode);
			} catch (XPathExpressionException e) {
				throw new RuntimeException("filed type xpath failed?", e);
			}
		else
			old.getParentNode().replaceChild(newNode, old);
	}

	public Node getPluginNode(String name, PluginType type) {
		Node pluginNode = null;

		switch (type) {
		// FIELD_TYPE, REQUEST_HANDLER, SEARCH_COMPONENT
		case FIELD_TYPE:
			pluginNode = getFieldTypeNode(name);
			break;
		case REQUEST_HANDLER:
			throw new IllegalArgumentException("Plugin not supported by this EditableConfig");
		case SEARCH_COMPONENT:
			throw new IllegalArgumentException("Plugin not supported by this EditableConfig");
		default:
			throw new IllegalArgumentException("Unexpected type found:" + type);
		}

		return pluginNode;
	}

	public Document getConfigDoc() {
		return configDoc;
	}

	public SolrCore getCore() {
		return core;
	}

	NodeList getFieldsByType(String type) {
		NodeList nodes;
		try {
			nodes = XMLUtils.getNodes("/schema/fields/field[@type='" + type + "'] | /schema/fields/dynamicField[@type='"
					+ type + "']", configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		return nodes;
	}

	public boolean isFieldTypeInUse(String type) {
		return 0 != getFieldsByType(type).getLength();
	}

	public void addFieldType(Map<String, Object> type) {
		Element typeNode = getConfigDoc().createElement("fieldType");
		try {
			for (String key : type.keySet()) {
				Object val = type.get(key);
				Map<String, Object> analyzers;
				if (MAP_ANALYZER_KEY.equals(key)) {
					analyzers = (Map) val;

					if ((analyzers.containsKey(CLASS_KEY))
							|| (CollectionUtils.containsAny(MAP_ANALYSIS_CHAIN_KEYS, analyzers.keySet()))) {
						Element analyzer = convertAnalyzer(typeNode, analyzers);
						typeNode.appendChild(analyzer);
					} else {
						for (String anaType : analyzers.keySet()) {
							Map<String, Object> anaMap = (Map) analyzers.get(anaType);
							Element analyzer = convertAnalyzer(typeNode, anaMap);
							if (!"default".equals(anaType)) {
								analyzer.setAttribute("type", anaType);
							}
							typeNode.appendChild(analyzer);
						}
					}
				} else {
					setAttribute(typeNode, key, val);
				}
			}
		} catch (ClassCastException cause) {
			throw new IllegalArgumentException("FieldType structure is not syntactically correct", cause);
		}

		addFieldType(typeNode);
	}

	public Map<String, Object> getFieldType(String name) {
		return convertFieldType(getFieldTypeNode(name));
	}

	public List<Map<String, Object>> getFieldTypes() {
		NodeList fieldTypeNodes = getFieldTypeNodes();

		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(fieldTypeNodes.getLength());

		for (int i = 0; i < fieldTypeNodes.getLength(); i++) {
			Map<String, Object> fieldType = convertFieldType(fieldTypeNodes.item(i));
			result.add(fieldType);
		}

		return result;
	}

	Node getFieldTypeNode(String name) {
		Node result;
		try {
			result = XMLUtils.getNode(XPATH_FIELDTYPES_PREFIX + "[@name='" + name + "']",
					configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	NodeList getFieldTypeNodes() {
		NodeList result;
		try {
			result = XMLUtils.getNodes(XPATH_FIELDTYPES_PREFIX, configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	String getFieldTypeSnippet(String name) {
		String result = null;
		try {
			Node node = XMLUtils.getNode(XPATH_FIELDTYPES_PREFIX + "[@name='" + name + "']",
					configDoc);

			if (node != null) {
				result = XMLUtils.toString(node, XMLUtils.OMIT_XML);
			} else if (LOG.isDebugEnabled())
				LOG.debug("Couldn't find definition of " + name);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public List<String> getLinesFromConfigFile(String resource, boolean returnEmptyIfMissing) {
		try {
			return core.getResourceLoader().getLines(resource);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (RuntimeException e) {
			if (returnEmptyIfMissing) {
				return new ArrayList<String>();
			}
			throw e;
		}
	}

	public Map<String, String> getStopwordFiles() {
		Map<String, String> result = new HashMap<String, String>();
		try {
			NodeList nodes = XMLUtils.getNodes(
					XPATH_FIELDTYPES_PREFIX + "/analyzer/filter[@class='solr.StopFilterFactory']",
					configDoc);

			int len = nodes.getLength();
			for (int i = 0; i < len; i++) {
				Node node = nodes.item(i);
				NamedNodeMap attributes = node.getAttributes();
				Node item = attributes.getNamedItem("words");

				Node parentNode = node.getParentNode().getParentNode();
				String name = parentNode.getAttributes().getNamedItem("name").getNodeValue();

				result.put(name, item.getNodeValue());
			}
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public void removeCopyField(String src, String dest) {
		try {
			NodeList nodes = XMLUtils.getNodes("/schema/copyField", configDoc);
			if (nodes != null)
				for (int i = 0; i < nodes.getLength(); i++) {
					Element node = (Element) nodes.item(i);
					if (((null == src) || (src.equals(node.getAttribute("source"))))
							&& ((null == dest) || (dest.equals(node.getAttribute("dest"))))) {
						node.getParentNode().removeChild(node);
					}
				}
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean copyFieldExists(String sourceField, String destField) {
		try {
			return XMLUtils.exists("/schema/copyField[@source='" + sourceField + "' and @dest='" + destField + "']",
					configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Map<String, String>> getCopyFields() {
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		try {
			NodeList nodes = XMLUtils.getNodes("/schema/copyField", configDoc);
			if (nodes != null)
				for (int i = 0; i < nodes.getLength(); i++) {
					Element node = (Element) nodes.item(i);
					Map<String, String> item = new HashMap<String, String>();
					item.put("src", node.getAttribute("source"));
					item.put("dest", node.getAttribute("dest"));
					result.add(item);
				}
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public void removePlugin(PluginInfo info) {
		Node pluginNode = null;

		switch (info.getType()) {
		// FIELD_TYPE, REQUEST_HANDLER, SEARCH_COMPONENT;
		case FIELD_TYPE:
			pluginNode = getFieldTypeNode(info.getName());
			NodeList nodes = getFieldsByType(info.getName());
			if (nodes.getLength() > 0) {
				Node tmp = nodes.item(0);
				Node parentNode = tmp.getParentNode();
				for (int i = 0; i < nodes.getLength(); i++) {
					tmp = nodes.item(i);
					parentNode.removeChild(tmp);
				}
			}
			break;
		case REQUEST_HANDLER:
			throw new UnsupportedOperationException("not supported");
		case SEARCH_COMPONENT:
			throw new UnsupportedOperationException("not supported");
		}

		if (pluginNode != null) {
			Node parentNode = pluginNode.getParentNode();
			parentNode.removeChild(pluginNode);
		}
	}

	public void removeFieldType(String name) {
		if (null != name) {
			Node type = getFieldTypeNode(name);
			if (null != type)
				type.getParentNode().removeChild(type);
		}
	}

	public void replaceDynamicFields(Collection<SchemaField> prototypes) {
		replaceFields(prototypes, "dynamicField");
	}

	public void replaceFields(Collection<SchemaField> fields) {
		replaceFields(fields, "field");
	}

	private void replaceFields(Collection<SchemaField> fields, String elementName) {
		Element fieldsElement = (Element) configDoc.getDocumentElement().getElementsByTagName("fields").item(0);

		Map<String, SchemaField> fieldsMap = new TreeMap<String, SchemaField>();
		for (SchemaField field : fields) {
			fieldsMap.put(field.getName(), field);
		}

		List<Element> elementsToDelete = new ArrayList<Element>();
		NodeList currentFields = fieldsElement.getElementsByTagName(elementName);
		for (int i = 0; i < currentFields.getLength(); i++) {
			Element currentFieldEl = (Element) currentFields.item(i);
			String fieldName = currentFieldEl.getAttribute("name");
			if (fieldsMap.containsKey(fieldName)) {
				Element newFieldEl = convertSchemaField(fieldsElement, elementName, (SchemaField) fieldsMap.get(fieldName));
				fieldsElement.replaceChild(newFieldEl, currentFieldEl);
			} else {
				elementsToDelete.add(currentFieldEl);
			}

			fieldsMap.remove(fieldName);
		}

		for (Element el : elementsToDelete) {
			el.getParentNode().removeChild(el);
		}

		for (SchemaField field : fieldsMap.values()) {
			Element ele = convertSchemaField(fieldsElement, elementName, field);

			fieldsElement.appendChild(ele);
		}
	}

	private static Element convertSchemaField(Node parent, String elementName, SchemaField field) {
		Document doc = parent.getOwnerDocument();
		Element ele = doc.createElement(elementName);

		ele.setAttribute("name", field.getName());
		ele.setAttribute("type", field.getType().getTypeName());
		ele.setAttribute("indexed", Boolean.toString(field.indexed()));
		ele.setAttribute("stored", Boolean.toString(field.stored()));
		ele.setAttribute("multiValued", Boolean.toString(field.multiValued()));

		if (field.indexed()) {
			ele.setAttribute("termVectors", Boolean.toString(field.storeTermVector()));

			ele.setAttribute("omitNorms", Boolean.toString(field.omitNorms()));

			ele.setAttribute("omitTermFreqAndPositions", Boolean.toString(field.omitTermFreqAndPositions()));

			ele.setAttribute("omitPositions", Boolean.toString((field.omitTermFreqAndPositions()) || (field.omitPositions())));
		}

		if (field.getDefaultValue() != null) {
			ele.setAttribute("default", field.getDefaultValue());
		}

		return ele;
	}

	public void setSimilarityFactorySpecialFields(String fields) {
		try {
			Element simElem = (Element) XMLUtils.getNode("/schema/similarity", configDoc);
			if (simElem == null) {
				simElem = configDoc.createElement("similarity");
				simElem.setAttribute(CLASS_KEY, GaiaSimilarityFactory.class.getName());
				configDoc.getDocumentElement().appendChild(simElem);
			}
			Element specialFieldsElem = (Element) XMLUtils.getNode("./str[@name='special_fields']", simElem);
			if (specialFieldsElem == null) {
				specialFieldsElem = configDoc.createElement("str");
				specialFieldsElem.setAttribute("name", "special_fields");
				simElem.appendChild(specialFieldsElem);
			}
			specialFieldsElem.setTextContent(fields);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public String getName() {
		return core.getSchemaResource();
	}

	private static Map<String, Object> convertFieldType(Node typeNode) {
		if (null == typeNode)
			return null;

		Map<String, Object> result = attrsToMap(typeNode);

		Map<String, Object> analyzers = new LinkedHashMap<String, Object>();

		NodeList kids = typeNode.getChildNodes();
		for (int i = 0; i < kids.getLength(); i++) {
			Node kid = kids.item(i);

			if (XML_ANALYZER_KEY.equals(kid.getNodeName())) {
				Map<String, Object> analyzer = convertAnalyzer(kid);

				Object anaType = analyzer.remove("type");

				anaType = null == anaType ? "default" : anaType.toString();

				if (analyzers.containsKey(anaType)) {
					LOG.warn("Encountered duplicate analyzers of type=" + anaType + " for fieldType: " + result.get("name"));
				}

				analyzers.put(anaType.toString(), analyzer);
			}

		}

		if (!analyzers.isEmpty()) {
			result.put(MAP_ANALYZER_KEY, analyzers);
		}

		return result;
	}

	private static Element convertAnalyzer(Element parent, Map<String, Object> anaMap) {
		Document doc = parent.getOwnerDocument();
		Element anaNode = doc.createElement(XML_ANALYZER_KEY);

		if (anaMap.containsKey(CLASS_KEY)) {
			for (String key : anaMap.keySet()) {
				setAttribute(anaNode, key, anaMap.get(key));
			}

		} else {
			List<Map<String, Object>> charFilters = (List) anaMap.get(MAP_CHAR_FILTER_KEY);

			if (null != charFilters) {
				for (Map<String, Object> filt : charFilters) {
					Element charNode = doc.createElement(XML_CHAR_FILTER_KEY);
					for (String key : filt.keySet()) {
						setAttribute(charNode, key, filt.get(key));
					}
					anaNode.appendChild(charNode);
				}

			}

			Map<String, Object> tokMap = (Map) anaMap.get(MAP_TOKENIZER_KEY);

			if (null != tokMap) {
				Element tokNode = doc.createElement(XML_TOKENIZER_KEY);
				for (String key : tokMap.keySet()) {
					setAttribute(tokNode, key, tokMap.get(key));
				}
				anaNode.appendChild(tokNode);
			}

			List<Map<String, Object>> tokFilters = (List) anaMap.get(MAP_TOKEN_FILTER_KEY);

			if (null != tokFilters) {
				for (Map<String, Object> filt : tokFilters) {
					Element filtNode = doc.createElement(XML_TOKEN_FILTER_KEY);
					for (String key : filt.keySet()) {
						setAttribute(filtNode, key, filt.get(key));
					}
					anaNode.appendChild(filtNode);
				}
			}
		}

		return anaNode;
	}

	private static Map<String, Object> convertAnalyzer(Node anaNode) {
		Map<String, Object> result = attrsToMap(anaNode);

		if (result.containsKey(CLASS_KEY)) {
			return result;
		}

		NodeList kids = anaNode.getChildNodes();
		int kidsSize = kids.getLength();

		List<Map<String, Object>> charFilters = new ArrayList<Map<String, Object>>(kidsSize / 4);
		Object tokenizer = null;
		List<Map<String, Object>> tokenFilters = new ArrayList<Map<String, Object>>(kidsSize / 2);

		for (int i = 0; i < kidsSize; i++) {
			Node kid = kids.item(i);
			String name = kid.getNodeName();

			if (XML_TOKENIZER_KEY.equals(name))
				tokenizer = attrsToMap(kid);
			else if (XML_CHAR_FILTER_KEY.equals(name))
				charFilters.add(attrsToMap(kid));
			else if (XML_TOKEN_FILTER_KEY.equals(name)) {
				tokenFilters.add(attrsToMap(kid));
			}

		}

		result.put(MAP_CHAR_FILTER_KEY, charFilters);
		result.put(MAP_TOKENIZER_KEY, tokenizer);
		result.put(MAP_TOKEN_FILTER_KEY, tokenFilters);

		return result;
	}

	public static void setAttribute(Element node, String key, Object value) {
		try {
			if (null != value)
				node.setAttribute(key, value.toString());
		} catch (DOMException cause) {
			throw new IllegalArgumentException("Invalid " + node.getTagName() + " attribute: '" + key + "' ("
					+ cause.getMessage() + ")", cause);
		}

	}

	static {
		MAP_ANALYSIS_CHAIN_KEYS.add(MAP_CHAR_FILTER_KEY);
		MAP_ANALYSIS_CHAIN_KEYS.add(MAP_TOKENIZER_KEY);
		MAP_ANALYSIS_CHAIN_KEYS.add(MAP_TOKEN_FILTER_KEY);
	}
}
