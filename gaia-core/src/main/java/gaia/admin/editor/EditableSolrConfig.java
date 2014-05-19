package gaia.admin.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.QueryElevationComponent;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.update.processor.LogUpdateProcessorFactory;
import org.apache.solr.update.processor.RunUpdateProcessorFactory;
import org.apache.solr.update.processor.SignatureUpdateProcessorFactory;
import org.apache.solr.util.DOMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import gaia.crawl.security.ConnectorsSecurityEnforcerComponent;
import gaia.handler.ClickAnalysisRequestHandler;
import gaia.handler.FieldMappingRequestHandler;
import gaia.handler.RoleBasedFilterComponent;
import gaia.security.ACLQueryFilterer;
import gaia.security.ACLTagProvider;
import gaia.security.AclBasedFilterComponent;
import gaia.solr.click.ClickDeletionPolicy;
import gaia.solr.click.ClickIndexReaderFactory;
import gaia.update.DistributedUpdateProcessorFactory;
import gaia.update.FieldMappingUpdateProcessorFactory;
import gaia.utils.XMLUtils;

public class EditableSolrConfig extends EditableConfig {
	private static transient Logger LOG = LoggerFactory.getLogger(EditableSolrConfig.class);
	private static final String GAIA_REQ_HANDLER = "/gaia";
	public static final String FILTER_CACHE = "filterCache";
	public static final String QUERY_RESULT_CACHE = "queryResultCache";
	public static final String DOCUMENT_CACHE = "documentCache";
	public static final String FIELD_VALUE_CACHE = "fieldValueCache";
	public static final String[] SOLR_CACHES = { FILTER_CACHE, QUERY_RESULT_CACHE, DOCUMENT_CACHE, FIELD_VALUE_CACHE };
	private String updateChain;

	public EditableSolrConfig(SolrCore core, String updateChain) {
		super(core);
		this.updateChain = updateChain;
	}

	public EditableSolrConfig(SolrCore core, String updateChain, ZkController zkController) {
		super(core, zkController);
		this.updateChain = updateChain;
	}

	public Node getSearchComponent(String name) {
		Node result;
		try {
			result = XMLUtils.getNode(new StringBuilder().append("/config/searchComponent[@name='").append(name).append("']")
					.toString(), configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	private NodeList getSearchComponents() {
		NodeList result;
		try {
			result = XMLUtils.getNodes("/config/searchComponent", configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public Node getPluginNode(String name, PluginType type) {
		Node pluginNode = null;

		switch (type) {
		// FIELD_TYPE, REQUEST_HANDLER, SEARCH_COMPONENT;
		case FIELD_TYPE:
			throw new IllegalArgumentException("Plugin not supported by this EditableConfig");
		case REQUEST_HANDLER:
			pluginNode = getRequestHandlerNode(name);
			break;
		case SEARCH_COMPONENT:
			pluginNode = getSearchComponent(name);
			break;
		default:
			throw new IllegalArgumentException(new StringBuilder().append("Unexpected type found:").append(type).toString());
		}

		return pluginNode;
	}

	public Element getRequestHandlerNode(String name) {
		Element result;
		try {
			result = (Element) XMLUtils.getNode(new StringBuilder().append("/config/requestHandler[@name='").append(name)
					.append("']").toString(), configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	private Element getRequestHandlerArrNode(String name, String arrName) {
		Element result;
		try {
			result = (Element) XMLUtils.getNode(new StringBuilder().append("/config/requestHandler[@name='").append(name)
					.append("']/arr[@name='").append(arrName).append("']").toString(), configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public List<String> getRegisteredComponents(String handlerName) {
		SolrRequestHandler reqHandler = core.getRequestHandler(handlerName);

		if ((reqHandler instanceof SearchHandler)) {
			SearchHandler searchHandler = (SearchHandler) reqHandler;
			Map<String, SearchComponent> searchComponents = core.getSearchComponents();
			List<String> names = new ArrayList<String>();
			for (SearchComponent component : searchHandler.getComponents()) {
				for (Map.Entry<String, SearchComponent> entry : searchComponents.entrySet())
					if (component.equals(entry.getValue())) {
						names.add(entry.getKey());
						break;
					}
			}
			return names;
		}
		return null;
	}

	public boolean setComponents(String handlerName, List<String> names) {
		Node requestHandler = getRequestHandlerNode(handlerName);
		if (requestHandler == null) {
			return false;
		}

		setValuesToRequestHandlerArray(handlerName, "components", (String[]) names.toArray(new String[0]), false);

		Node arrNode = getRequestHandlerArrNode(handlerName, "first-components");
		if (arrNode != null) {
			requestHandler.removeChild(arrNode);
		}
		arrNode = getRequestHandlerArrNode(handlerName, "last-components");
		if (arrNode != null) {
			requestHandler.removeChild(arrNode);
		}
		return true;
	}

	public boolean setLastComponents(String handlerName, List<String> names) {
		Node requestHandler = getRequestHandlerNode(handlerName);
		if (requestHandler == null) {
			return false;
		}

		setValuesToRequestHandlerArray(handlerName, "last-components", (String[]) names.toArray(new String[0]), false);

		Element arrNode = getRequestHandlerArrNode(handlerName, "components");
		if (arrNode != null) {
			requestHandler.removeChild(arrNode);
		}
		return true;
	}

	public String[] getArrayFromRequestHandler(String handlerName, String listName) {
		Element requestHandler = getRequestHandlerNode(handlerName);
		if (requestHandler == null) {
			return null;
		}
		String[] result = new String[0];
		Node rhArrNode = getRequestHandlerArrNode(handlerName, listName);
		if (null == rhArrNode) {
			return result;
		}

		NodeList nodelist = getRequestHandlerArrNode(handlerName, listName).getChildNodes();
		int size = nodelist.getLength();
		ArrayList<String> components = new ArrayList<String>();
		for (int i = 0; i < size; i++) {
			Node node = nodelist.item(i);
			if (node.getNodeType() == 1) {
				components.add(node.getTextContent().trim());
			}
		}

		result = components.toArray(new String[components.size()]);
		return result;
	}

	public void setValuesToRequestHandlerArray(String handlerName, String listName, String[] values,
			boolean checkComponents) {
		if (checkComponents) {
			for (String name : values) {
				getCore().getSearchComponent(name);
			}
		}

		Node arrNode = getRequestHandlerArrNode(handlerName, listName);

		Node requestHandler = getRequestHandlerNode(handlerName);

		if (arrNode != null) {
			requestHandler.removeChild(arrNode);
			arrNode.setNodeValue(null);
		}

		Element arrElement = configDoc.createElement("arr");
		arrElement.setAttribute("name", listName);
		requestHandler.appendChild(arrElement);

		for (String value : values) {
			Element stringElement = configDoc.createElement("str");
			stringElement.setTextContent(value);
			arrElement.appendChild(stringElement);
		}
	}

	public void updateRoleFilterComponent(Map<String, String[]> filters) throws DOMException, XPathExpressionException {
		Element component = (Element) getSearchComponent("filterbyrole");
		if (component == null) {
			component = initRolesConfig();
		}
		Element filterList = (Element) XMLUtils.getNode("./lst[@name='filters']", component);
		if (filterList == null) {
			filterList = configDoc.createElement("lst");
			component.appendChild(filterList);
			filterList.setAttribute("name", "filters");
		}

		removeChildElements(filterList, "str");

		if (filters != null) {
			Set<Entry<String, String[]>> entries = filters.entrySet();
			for (Map.Entry<String, String[]> entry : entries) {
				String name = (String) entry.getKey();
				String[] filterStrings = (String[]) entry.getValue();
				for (String filterString : filterStrings) {
					Element filterNode = configDoc.createElement("str");
					filterNode.setAttribute("name", name);
					filterNode.setTextContent(filterString);
					filterList.appendChild(filterNode);
				}
			}
		}
	}

	private Element initRolesConfig() {
		Element component = configDoc.createElement("searchComponent");
		configDoc.getDocumentElement().appendChild(component);
		component.setAttribute("name", "filterbyrole");
		component.setAttribute("class", RoleBasedFilterComponent.class.getName());
		Element defFilterEl = configDoc.createElement("str");
		component.appendChild(defFilterEl);
		defFilterEl.setAttribute("name", "default.filter");
		defFilterEl.appendChild(configDoc.createTextNode("-*:*"));

		List<String> searchComponents = getRegisteredComponents(GAIA_REQ_HANDLER);
		if (!searchComponents.contains("filterbyrole"))
			searchComponents.add(0, "filterbyrole");
		setComponents(GAIA_REQ_HANDLER, searchComponents);

		return component;
	}

	public void replaceHandlerParams(String handler, SolrParams params) {
		Node parent;
		try {
			parent = XMLUtils.getNode(
					new StringBuilder().append("/config/requestHandler[@name='").append(handler).append("']").toString(),
					configDoc);

			Node defaultsList = XMLUtils.getNode(new StringBuilder().append("/config/requestHandler[@name='").append(handler)
					.append("']/lst[@name='defaults']").toString(), configDoc);
			removeElementAndComment(defaultsList);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		Element listElement = configDoc.createElement("lst");
		listElement.setAttribute("name", "defaults");
		parent.appendChild(listElement);
		Iterator<String> iterator = params.getParameterNamesIterator();
		ArrayList<String> orderedParamsList = new ArrayList<String>();
		while (iterator.hasNext()) {
			String param = (String) iterator.next();
			orderedParamsList.add(param);
		}
		Collections.sort(orderedParamsList);
		for (String param : orderedParamsList) {
			String[] values = params.getParams(param);
			for (String value : values) {
				Element paramElement = configDoc.createElement("str");
				paramElement.setAttribute("name", param);
				paramElement.setTextContent(value);
				listElement.appendChild(paramElement);
			}
		}
	}

	public void setIndexSettings(Map<String, String> settings) {
		try {
			Element mainIndexElement = (Element) XMLUtils.getNode("/config/indexConfig", configDoc);
			if (mainIndexElement == null) {
				mainIndexElement = configDoc.createElement("indexConfig");
				configDoc.getDocumentElement().appendChild(mainIndexElement);
			}
			for (Map.Entry<String, String> entry : settings.entrySet()) {
				String key = (String) entry.getKey();
				String value = (String) entry.getValue();
				XMLUtils.addOrUpdateTextNodeElement(key, value, mainIndexElement, configDoc);
			}
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public void setUpdateHandlerSettings(UpdateHandlerSettings settings) {
		try {
			Element updateHandlerElement = (Element) XMLUtils.getNode("/config/updateHandler", configDoc);
			if (updateHandlerElement == null) {
				if ((settings.autoCommitMaxTime == null) && (settings.autoCommitMaxDocs == null)
						&& (settings.autoCommitOpenSearcher == null) && (settings.autoSoftCommitMaxTime == null)
						&& (settings.autoSoftCommitMaxDocs == null)) {
					return;
				}
				updateHandlerElement = configDoc.createElement("updateHandler");
				configDoc.getDocumentElement().appendChild(updateHandlerElement);
			}

			Element autoCommitElement = (Element) XMLUtils.getNode("autoCommit", updateHandlerElement);
			if (autoCommitElement == null) {
				autoCommitElement = configDoc.createElement("autoCommit");
				updateHandlerElement.appendChild(autoCommitElement);
			}

			if ((settings.autoCommitMaxTime == null) && (settings.autoCommitMaxDocs == null)
					&& (settings.autoCommitOpenSearcher == null)) {
				updateHandlerElement.removeChild(autoCommitElement);
			} else {
				if (settings.autoCommitMaxTime == null) {
					Element el = (Element) XMLUtils.getNode("maxTime", autoCommitElement);
					if (el != null)
						autoCommitElement.removeChild(el);
				} else {
					XMLUtils.addOrUpdateTextNodeElement("maxTime", Integer.toString(settings.autoCommitMaxTime.intValue()),
							autoCommitElement, configDoc);
				}
				if (settings.autoCommitMaxDocs == null) {
					Element el = (Element) XMLUtils.getNode("maxDocs", autoCommitElement);
					if (el != null)
						autoCommitElement.removeChild(el);
				} else {
					XMLUtils.addOrUpdateTextNodeElement("maxDocs", Integer.toString(settings.autoCommitMaxDocs.intValue()),
							autoCommitElement, configDoc);
				}
				if (settings.autoCommitOpenSearcher == null) {
					Element el = (Element) XMLUtils.getNode("openSearcher", autoCommitElement);
					if (el != null)
						autoCommitElement.removeChild(el);
				} else {
					XMLUtils.addOrUpdateTextNodeElement("openSearcher",
							Boolean.toString(settings.autoCommitOpenSearcher.booleanValue()), autoCommitElement, configDoc);
				}
			}

			Element autoSoftCommitElement = (Element) XMLUtils.getNode("autoSoftCommit", updateHandlerElement);
			if (autoSoftCommitElement == null) {
				autoSoftCommitElement = configDoc.createElement("autoSoftCommit");
				updateHandlerElement.appendChild(autoSoftCommitElement);
			}

			if ((settings.autoSoftCommitMaxTime == null) && (settings.autoSoftCommitMaxDocs == null)) {
				updateHandlerElement.removeChild(autoSoftCommitElement);
			} else {
				if (settings.autoSoftCommitMaxTime == null) {
					Element el = (Element) XMLUtils.getNode("maxTime", autoSoftCommitElement);
					if (el != null)
						autoSoftCommitElement.removeChild(el);
				} else {
					XMLUtils.addOrUpdateTextNodeElement("maxTime", Integer.toString(settings.autoSoftCommitMaxTime.intValue()),
							autoSoftCommitElement, configDoc);
				}
				if (settings.autoSoftCommitMaxDocs == null) {
					Element el = (Element) XMLUtils.getNode("maxDocs", autoSoftCommitElement);
					if (el != null)
						autoSoftCommitElement.removeChild(el);
				} else {
					XMLUtils.addOrUpdateTextNodeElement("maxDocs", Integer.toString(settings.autoSoftCommitMaxDocs.intValue()),
							autoSoftCommitElement, configDoc);
				}
			}
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public UpdateHandlerSettings getUpdateHandlerSettings() {
		Element updateHandlerElement;
		try {
			updateHandlerElement = (Element) XMLUtils.getNode("/config/updateHandler", configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}

		if (updateHandlerElement == null) {
			return new UpdateHandlerSettings();
		}
		try {
			boolean hasAutoCommitNode = XMLUtils.getNode("autoCommit", updateHandlerElement) != null;
			UpdateHandlerSettings settings = new UpdateHandlerSettings();
			settings.autoCommitMaxTime = XMLUtils.parseIntTextNode("autoCommit/maxTime/text()", updateHandlerElement,
					(Integer) null);
			settings.autoCommitMaxDocs = XMLUtils.parseIntTextNode("autoCommit/maxDocs/text()", updateHandlerElement,
					(Integer) null);
			if (hasAutoCommitNode) {
				settings.autoCommitOpenSearcher = XMLUtils.parseBooleanTextNode("autoCommit/openSearcher/text()",
						updateHandlerElement, Boolean.TRUE);
			}
			settings.autoSoftCommitMaxTime = XMLUtils.parseIntTextNode("autoSoftCommit/maxTime/text()", updateHandlerElement,
					(Integer) null);
			settings.autoSoftCommitMaxDocs = XMLUtils.parseIntTextNode("autoSoftCommit/maxDocs/text()", updateHandlerElement,
					(Integer) null);
			return settings;
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<String, String> getCacheConfig(String cacheName) {
		Element el;
		try {
			el = (Element) XMLUtils.getNode(new StringBuilder().append("/config/query/").append(cacheName).toString(),
					configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		if (el == null) {
			return null;
		}
		return DOMUtil.toMap(el.getAttributes());
	}

	public void createOrUpdateCacheConfig(String cacheName, Map<String, String> attribs) {
		try {
			String xpath;
			if (Arrays.asList(SOLR_CACHES).contains(cacheName))
				xpath = new StringBuilder().append("/config/query/").append(cacheName).toString();
			else {
				xpath = new StringBuilder().append("/config/query/cache[@name='").append(cacheName).append("']").toString();
			}
			Element el = (Element) XMLUtils.getNode(xpath, configDoc);
			if (el == null) {
				Element queryEl = (Element) XMLUtils.getNode("/config/query", configDoc);
				if (queryEl == null) {
					queryEl = configDoc.createElement("query");
					configDoc.getDocumentElement().appendChild(queryEl);
				}
				if (Arrays.asList(SOLR_CACHES).contains(cacheName)) {
					el = configDoc.createElement(cacheName);
				} else {
					el = configDoc.createElement("cache");
					el.setAttribute("name", cacheName);
				}
				queryEl.appendChild(el);
			}

			for (Map.Entry<String, String> entry : attribs.entrySet())
				if (entry.getValue() != null) {
					el.setAttribute((String) entry.getKey(), (String) entry.getValue());
				} else
					el.removeAttribute((String) entry.getKey());
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<String, Map<String, String>> getUserCaches() {
		Map<String, Map<String, String>> res = new HashMap<String, Map<String, String>>();
		NodeList nodes;
		try {
			nodes = XMLUtils.getNodes("/config/query/cache", configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		if (nodes == null) {
			return res;
		}
		for (int i = 0; i < nodes.getLength(); i++) {
			Element el = (Element) nodes.item(i);
			Map<String, String> map = DOMUtil.toMap(el.getAttributes());
			String name = (String) map.remove("name");
			if (name != null)
				res.put(name, map);
		}
		return res;
	}

	public void removeCacheConfig(String cacheName) {
		try {
			String xpath;
			if (Arrays.asList(SOLR_CACHES).contains(cacheName))
				xpath = new StringBuilder().append("/config/query/").append(cacheName).toString();
			else {
				xpath = new StringBuilder().append("/config/query/cache[@name='").append(cacheName).append("']").toString();
			}
			Element el = (Element) XMLUtils.getNode(xpath, configDoc);
			if (el == null) {
				LOG.warn(new StringBuilder().append("Could not remove not existent cache ").append(cacheName).toString());
				return;
			}
			el.getParentNode().removeChild(el);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public void removeDedupeUpdateProcess() {
		Element chainEl = getGaiaUpdateHandlerChain();
		Element dedupProcessorEl = getGaiaUpdateProcessor(SignatureUpdateProcessorFactory.class, chainEl);
		if (dedupProcessorEl != null)
			chainEl.removeChild(dedupProcessorEl);
	}

	public void setFieldMappingProcessor() {
		Element chainEl = getGaiaUpdateHandlerChain();
		Element processorEl = getGaiaUpdateProcessor(FieldMappingUpdateProcessorFactory.class, chainEl);
		if (processorEl == null) {
			processorEl = configDoc.createElement("processor");
			processorEl.setAttribute("class", FieldMappingUpdateProcessorFactory.class.getName());

			Element runUpdateProcEl = getGaiaUpdateProcessor(RunUpdateProcessorFactory.class, chainEl);
			if (runUpdateProcEl != null) {
				chainEl.insertBefore(processorEl, runUpdateProcEl);
			} else
				chainEl.insertBefore(processorEl, chainEl.getFirstChild());
		}
	}

	public void setFieldMappingRequestHandler() {
		try {
			Element el = (Element) XMLUtils.getNode("/config/requestHandler[@name='/fmap']", configDoc);
			if (el == null) {
				el = configDoc.createElement("requestHandler");
				el.setAttribute("name", "/fmap");
				configDoc.getDocumentElement().appendChild(el);
			}
			el.setAttribute("class", FieldMappingRequestHandler.class.getName());
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public void setDedupeUpdateProcess(Set<String> fields, boolean enabled, boolean overwriteDupes) {
		Element chainEl = getGaiaUpdateHandlerChain();

		Element oldProcessorEl = getGaiaUpdateProcessor(SignatureUpdateProcessorFactory.class, chainEl);
		if (oldProcessorEl != null) {
			chainEl.removeChild(oldProcessorEl);
		}

		Element newProcessorEl = configDoc.createElement("processor");
		newProcessorEl.setAttribute("class", SignatureUpdateProcessorFactory.class.getName());

		String enabledValue = "true";
		if (!enabled) {
			enabledValue = "false";
		}
		Element enableChild = configDoc.createElement("bool");
		enableChild.setAttribute("name", "enabled");
		enableChild.setTextContent(enabledValue);
		newProcessorEl.appendChild(enableChild);

		String overwriteDupesValue = "true";
		if (!overwriteDupes) {
			overwriteDupesValue = "false";
		}

		Element overwriteDupesChild = configDoc.createElement("bool");
		overwriteDupesChild.setAttribute("name", "overwriteDupes");
		overwriteDupesChild.setTextContent(overwriteDupesValue);
		newProcessorEl.appendChild(overwriteDupesChild);

		if (fields.size() > 0) {
			Element paramElement = configDoc.createElement("str");
			paramElement.setAttribute("name", "fields");

			StringBuilder fieldsSb = new StringBuilder();
			for (String field : fields) {
				if (fieldsSb.length() > 0) {
					fieldsSb.append(",");
				}
				fieldsSb.append(field);
			}

			paramElement.setTextContent(fieldsSb.toString());
			newProcessorEl.appendChild(paramElement);
		}

		Element fieldMappingProcEl = getGaiaUpdateProcessor(FieldMappingUpdateProcessorFactory.class, chainEl);
		Node nextChild = null;
		if (fieldMappingProcEl != null)
			nextChild = fieldMappingProcEl.getNextSibling();
		else {
			nextChild = chainEl.getFirstChild();
		}
		chainEl.insertBefore(newProcessorEl, nextChild);
	}

	public void setDistributedUpdate(String self, String shards) {
		Element chainEl = getGaiaUpdateHandlerChain();

		Element processorEl = getGaiaUpdateProcessor(DistributedUpdateProcessorFactory.class, chainEl);

		if (processorEl == null) {
			if (shards == null) {
				return;
			}

			processorEl = configDoc.createElement("processor");
			processorEl.setAttribute("class", DistributedUpdateProcessorFactory.class.getName());

			Element nextProcessorEl = getGaiaUpdateProcessor(LogUpdateProcessorFactory.class, chainEl);

			if (nextProcessorEl == null) {
				nextProcessorEl = getGaiaUpdateProcessor(RunUpdateProcessorFactory.class, chainEl);
			}

			chainEl.insertBefore(processorEl, nextProcessorEl);
		}

		removeChildElements(processorEl, "str");
		removeChildElements(processorEl, "arr");

		if (self != null) {
			Element paramElement = configDoc.createElement("str");
			paramElement.setAttribute("name", "self");
			paramElement.setTextContent(self);
			processorEl.appendChild(paramElement);
		}

		if (shards != null) {
			Element paramElement = configDoc.createElement("str");
			paramElement.setAttribute("name", "shards");
			paramElement.setTextContent(shards);
			processorEl.appendChild(paramElement);
		}
	}

	public void initClickAnalysisRequestHandler() {
		try {
			Element el = (Element) XMLUtils.getNode("/config/requestHandler[@name='/click']", configDoc);
			if (el == null) {
				el = configDoc.createElement("requestHandler");
				el.setAttribute("name", "/click");
				configDoc.getDocumentElement().appendChild(el);
			}
			el.setAttribute("class", ClickAnalysisRequestHandler.class.getName());
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public void initClickDeletionPolicy() {
		try {
			Element mainIndexEl = (Element) XMLUtils.getNode("/config/indexConfig", configDoc);
			if (mainIndexEl == null) {
				mainIndexEl = configDoc.createElement("indexConfig");
				configDoc.getDocumentElement().appendChild(mainIndexEl);
			}
			Element deletionPolicyEl = (Element) XMLUtils.getNode("./deletionPolicy", mainIndexEl);
			if (deletionPolicyEl == null) {
				deletionPolicyEl = configDoc.createElement("deletionPolicy");
				mainIndexEl.appendChild(deletionPolicyEl);
			}
			deletionPolicyEl.setAttribute("class", ClickDeletionPolicy.class.getName());
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public void setClickIndexReaderFactorySettings(boolean enabled, String docIdField, String boostData, String boostField) {
		Node factoryNode;
		try {
			factoryNode = XMLUtils.getNode(
					new StringBuilder().append("/config/indexReaderFactory[@class='")
							.append(ClickIndexReaderFactory.class.getName()).append("']").toString(), configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		if (factoryNode == null) {
			Element el = configDoc.createElement("indexReaderFactory");
			el.setAttribute("name", "IndexReaderFactory");
			el.setAttribute("class", ClickIndexReaderFactory.class.getName());
			configDoc.getDocumentElement().appendChild(el);
			factoryNode = el;
		}
		removeChildElements(factoryNode);

		Element child = configDoc.createElement("bool");
		child.setAttribute("name", "enabled");
		if (enabled)
			child.setTextContent("true");
		else {
			child.setTextContent("false");
		}
		factoryNode.appendChild(child);

		child = configDoc.createElement("str");
		child.setAttribute("name", "docIdField");
		child.setTextContent(docIdField);
		factoryNode.appendChild(child);

		child = configDoc.createElement("str");
		child.setAttribute("name", "boostData");
		child.setTextContent(boostData);
		factoryNode.appendChild(child);

		child = configDoc.createElement("str");
		child.setAttribute("name", "boostField");
		child.setTextContent(boostField);
		factoryNode.appendChild(child);
	}

	public String getName() {
		return core.getConfigResource();
	}

	public void setACLComponentConfig(String name, Map<String, Object> config) {
		Object providerClass = config.get("provider.class");
		if (providerClass == null) {
			throw new RuntimeException("provider.class must be specified");
		}

		Object filtererClass = config.get("filterer.class");
		if (filtererClass == null) {
			throw new RuntimeException("filterer.class must be specified");
		}

		loadClass(providerClass.toString(), ACLTagProvider.class);
		loadClass(filtererClass.toString(), ACLQueryFilterer.class);

		Node componentConfig = getSearchComponent(name);
		if (componentConfig != null) {
			componentConfig.getParentNode().removeChild(componentConfig);
		}

		Element searchComponent = getConfigDoc().createElement("searchComponent");
		try {
			Node configNode = XMLUtils.getNode("/config", getConfigDoc());
			configNode.appendChild(searchComponent);
		} catch (XPathExpressionException e) {
			throw new RuntimeException("Cannot find config element.");
		}
		searchComponent.setAttribute("class", AclBasedFilterComponent.class.getCanonicalName());
		searchComponent.setAttribute("name", name);
		addValueNode(searchComponent, "provider.class", providerClass);
		addValueNode(searchComponent, "filterer.class", filtererClass);

		Map<String, Object> pc = (Map) config.get("provider.config");
		addListNode(searchComponent, "provider.config", pc);
		addListNode(searchComponent, "filterer.config", (Map) config.get("filterer.config"));
	}

	private void loadClass(String name, Class<?> type) {
		try {
			Class<?> implClass = getCore().getResourceLoader().getClassLoader().loadClass(name);
			if (!type.isAssignableFrom(implClass))
				throw new RuntimeException(new StringBuilder().append("Wrong type, expecting ").append(type.getName())
						.append(" for ").append(name).toString());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(new StringBuilder().append("Cannot load class:").append(name).append(" because ")
					.append(e.getMessage()).toString());
		}
	}

	private void addValueNode(Node parent, String name, Object value) {
		String elemName;
		if ((value instanceof String)) {
			elemName = "str";
		} else {
			if ((value instanceof Integer)) {
				elemName = "int";
			} else {
				if ((value instanceof Boolean)) {
					elemName = "bool";
				} else {
					if ((value instanceof Float))
						elemName = "float";
					else
						throw new RuntimeException(new StringBuilder().append("unknown type:").append(value.getClass()).toString());
				}
			}
		}
		Element valueNode = getConfigDoc().createElement(elemName);
		parent.appendChild(valueNode);
		if (name != null) {
			valueNode.setAttribute("name", name);
		}
		valueNode.setTextContent(value.toString());
	}

	private void addListNode(Node parent, String name, Map<String, Object> values) {
		Element listNode = getConfigDoc().createElement("lst");
		parent.appendChild(listNode);
		if (name != null) {
			listNode.setAttribute("name", name);
		}
		if (values != null)
			for (Map.Entry<String, Object> entry : values.entrySet())
				addValueNode(listNode, (String) entry.getKey(), entry.getValue());
	}

	public Map<String, Object> getACLComponentConfig(String name) {
		try {
			HashMap<String, Object> config = new HashMap<String, Object>();
			Node component = getSearchComponent(name);

			if (component == null) {
				return null;
			}
			if (!isACLComponentNode(component)) {
				throw new RuntimeException("Not an ACL component.");
			}
			config.put("provider.class", XMLUtils.getNode("//str[@name='provider.class']", component).getTextContent());

			config.put("filterer.class", XMLUtils.getNode("//str[@name='filterer.class']", component).getTextContent());

			Map<String, Object> providerConfig = new HashMap<String, Object>();
			config.put("provider.config", providerConfig);

			Map<String, Object> filtererConfig = new HashMap<String, Object>();
			config.put("filterer.config", filtererConfig);

			NodeList children = component.getChildNodes();

			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if ((child.getNodeType() == 1) && ("lst".equals(child.getNodeName()))) {
					String lstName = child.getAttributes().getNamedItem("name").getTextContent();
					if ("provider.config".equals(lstName))
						populateMap(providerConfig, child.getChildNodes());
					else if ("filterer.config".equals(lstName)) {
						populateMap(filtererConfig, child.getChildNodes());
					}

				}

			}

			providerConfig.remove("java.naming.security.credentials");

			return config;
		} catch (XPathExpressionException e) {
			throw new RuntimeException(new StringBuilder().append("Could not get config: ").append(e.getMessage()).toString());
		}
	}

	private boolean isACLComponentNode(Node component) {
		return AclBasedFilterComponent.class.getName().equals(
				component.getAttributes().getNamedItem("class").getNodeValue());
	}

	private void populateMap(Map<String, Object> providerConfig, NodeList listValues) {
		for (int i = 0; i < listValues.getLength(); i++) {
			Node value = listValues.item(i);
			if (value.getNodeType() == 1)
				providerConfig.put(value.getAttributes().getNamedItem("name").getNodeValue(), getItemValue(value));
		}
	}

	private Object getItemValue(Node value) {
		String nodeName = value.getNodeName();
		if ("int".equals(nodeName)) {
			return Integer.valueOf(Integer.parseInt(value.getTextContent()));
		}
		if ("str".equals(nodeName)) {
			return value.getTextContent();
		}
		if ("bool".equals(nodeName)) {
			return Boolean.valueOf(Boolean.parseBoolean(value.getTextContent()));
		}

		return value.getTextContent();
	}

	public void deleteACLComponentConfig(String name) throws ComponentInUseException {
		try {
			NodeList result = XMLUtils.getNodes(
					new StringBuilder().append("/config/requestHandler/arr[contains(@name,'components')]/str[text() ='")
							.append(name).append("']").toString(), configDoc);

			if (result.getLength() > 0) {
				throw new ComponentInUseException();
			}
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}

		Node componentConfig = getSearchComponent(name);
		if (componentConfig != null)
			componentConfig.getParentNode().removeChild(componentConfig);
	}

	public List<String> getACLComponentNames() {
		NodeList components = getSearchComponents();
		ArrayList<String> names = new ArrayList<String>();
		for (int i = 0; i < components.getLength(); i++) {
			Node node = components.item(i);
			if (isACLComponentNode(node)) {
				String componentName = node.getAttributes().getNamedItem("name").getNodeValue();
				if (componentName != null) {
					names.add(componentName);
				}
			}
		}
		return names;
	}

	public boolean enableQueryElevation() {
		boolean updated = false;

		Element el = (Element) getSearchComponent("elevator");
		if (el == null) {
			updated = true;
			el = configDoc.createElement("searchComponent");
			configDoc.getDocumentElement().appendChild(el);
			el.setAttribute("name", "elevator");
			el.setAttribute("class", QueryElevationComponent.class.getName());
			Element qftEl = configDoc.createElement("str");
			el.appendChild(qftEl);
			qftEl.setAttribute("name", "queryFieldType");
			qftEl.appendChild(configDoc.createTextNode("string"));
			Element configEl = configDoc.createElement("str");
			el.appendChild(configEl);
			configEl.setAttribute("name", "config-file");
			configEl.appendChild(configDoc.createTextNode("elevate.xml"));
		}

		List<String> searchComponents = getRegisteredComponents(GAIA_REQ_HANDLER);
		if ((searchComponents != null) && (!searchComponents.contains("elevator")))
			searchComponents.add("elevator");
		setComponents(GAIA_REQ_HANDLER, searchComponents);

		return updated;
	}

	public boolean enableConnectorsSecurity() {
		boolean updated = false;
		Node configNode;
		try {
			configNode = XMLUtils.getNode("/config", getConfigDoc());
		} catch (XPathExpressionException e) {
			throw new RuntimeException("Cannot find config element.");
		}
		Element searchComponent;
		try {
			searchComponent = (Element) XMLUtils.getNode(new StringBuilder().append("/config/searchComponent[@class='")
					.append(ConnectorsSecurityEnforcerComponent.class.getCanonicalName()).append("']").toString(), configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		if (searchComponent == null) {
			updated = true;
			searchComponent = getConfigDoc().createElement("searchComponent");
			configNode.appendChild(searchComponent);
		}
		searchComponent.setAttribute("name", "connectorsSecurity");
		searchComponent.setAttribute("class", ConnectorsSecurityEnforcerComponent.class.getCanonicalName());

		List<String> searchComponents = getRegisteredComponents(GAIA_REQ_HANDLER);
		if ((searchComponents != null) && (!searchComponents.contains("connectorsSecurity"))) {
			updated = true;
			searchComponents.add(0, "connectorsSecurity");
			setComponents(GAIA_REQ_HANDLER, searchComponents);
		}
		return updated;
	}

	public boolean disableConnectorsSecurity() {
		boolean updated = false;
		Element searchComponent;
		try {
			searchComponent = (Element) XMLUtils.getNode(new StringBuilder().append("/config/searchComponent[@class='")
					.append(ConnectorsSecurityEnforcerComponent.class.getCanonicalName()).append("']").toString(), configDoc);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		if (searchComponent != null) {
			updated = true;
			searchComponent.getParentNode().removeChild(searchComponent);
		}

		List<String> searchComponents = getRegisteredComponents(GAIA_REQ_HANDLER);
		if ((searchComponents != null) && (searchComponents.contains("connectorsSecurity"))) {
			updated = true;
			searchComponents.remove("connectorsSecurity");
			setComponents(GAIA_REQ_HANDLER, searchComponents);
		}
		return updated;
	}

	public Element getGaiaUpdateHandlerChain() {
		try {
			Element chainEl = (Element) XMLUtils.getNode(
					new StringBuilder().append("/config/updateRequestProcessorChain[@name='").append(updateChain).append("']")
							.toString(), configDoc);
			if (chainEl == null) {
				throw new IllegalStateException(new StringBuilder().append("solrconfig must contain ").append(updateChain)
						.append(" update chain").toString());
			}
			return chainEl;
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public Element getGaiaUpdateProcessor(Class<?> clazz, Element chainEl) {
		try {
			NodeList processors = XMLUtils.getNodes("./processor", chainEl);
			for (int i = 0; i < processors.getLength(); i++) {
				Element processor = (Element) processors.item(i);
				String className = processor.getAttribute("class");
				if (className != null) {
					Class<?> processorClass = core.getResourceLoader().findClass(className, Object.class);
					if (clazz == processorClass) {
						return processor;
					}
				}
			}
			return null;
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public static class UpdateHandlerSettings {
		public Integer autoCommitMaxTime = null;
		public Integer autoCommitMaxDocs = null;
		public Boolean autoCommitOpenSearcher = null;
		public Integer autoSoftCommitMaxTime = null;
		public Integer autoSoftCommitMaxDocs = null;

		public UpdateHandlerSettings() {
		}

		public UpdateHandlerSettings(Integer autoCommitMaxTime, Integer autoCommitMaxDocs, Boolean autoCommitOpenSearcher,
				Integer autoSoftCommitMaxTime, Integer autoSoftCommitMaxDocs) {
			this.autoCommitMaxTime = autoCommitMaxTime;
			this.autoCommitMaxDocs = autoCommitMaxDocs;
			this.autoCommitOpenSearcher = autoCommitOpenSearcher;
			this.autoSoftCommitMaxTime = autoSoftCommitMaxTime;
			this.autoSoftCommitMaxDocs = autoSoftCommitMaxDocs;
		}
	}
}
