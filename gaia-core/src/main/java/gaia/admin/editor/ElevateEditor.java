package gaia.admin.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.solr.cloud.ZkController;
import org.apache.solr.core.SolrCore;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import gaia.utils.StringUtils;
import gaia.utils.XMLUtils;

public class ElevateEditor extends EditableConfig {
	public ElevateEditor(SolrCore core) {
		super(core, null, true);
	}

	public ElevateEditor(SolrCore core, ZkController zkController) {
		super(core, zkController, true);
	}

	public Map<String, List<Map<String, Object>>> getElevations() {
		Map<String, List<Map<String, Object>>> elevations = new HashMap<String, List<Map<String, Object>>>();
		try {
			NodeList nodes = XMLUtils.getNodes("/elevate/query", this.configDoc);

			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				Node textNode = node.getAttributes().getNamedItem("text");
				String query = textNode.getTextContent();

				NodeList docs = XMLUtils.getNodes("./doc", node);
				List<Map<String, Object>> docMaps = new ArrayList<Map<String, Object>>();
				elevations.put(query, docMaps);
				for (int j = 0; j < docs.getLength(); j++) {
					Node docNode = docs.item(j);
					Node idNode = docNode.getAttributes().getNamedItem("id");
					Node excludeNode = docNode.getAttributes().getNamedItem("exclude");
					Map<String, Object> docMap = new HashMap<String, Object>();
					if (excludeNode != null) {
						String excludeVal = excludeNode.getTextContent();
						if (excludeVal.equalsIgnoreCase("true"))
							docMap.put("exclude", Boolean.valueOf(true));
						else {
							docMap.put("exclude", Boolean.valueOf(false));
						}
					}
					String idText = idNode.getTextContent();

					docMap.put("doc", idText);
					docMaps.add(docMap);
				}
			}
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		return elevations;
	}

	public void addElevations(Map<String, List<Map<String, Object>>> elevations) {
		Node mainNode;
		try {
			mainNode = XMLUtils.getNode("/elevate", this.configDoc);
			if (mainNode == null) {
				mainNode = this.configDoc.createElement("elevate");
				this.configDoc.appendChild(mainNode);
			}
			removeChildElements(mainNode);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		for (Map.Entry<String, List<Map<String, Object>>> entry : elevations.entrySet()) {
			Element queryChild = this.configDoc.createElement("query");
			queryChild.setAttribute("text", (String) entry.getKey());

			List<Map<String, Object>> docMaps = entry.getValue();
			for (Map<String, Object> docMap : docMaps) {
				String doc = (String) docMap.get("doc");
				Boolean exclude = StringUtils.getBoolean(docMap.get("exclude"));
				if (exclude == null) {
					exclude = Boolean.valueOf(false);
				}

				Element idChild = this.configDoc.createElement("doc");
				idChild.setAttribute("id", doc);
				if (exclude.booleanValue()) {
					idChild.setAttribute("exclude", "true");
				}
				queryChild.appendChild(idChild);
			}

			mainNode.appendChild(queryChild);
		}
	}

	public Node getPluginNode(String name, PluginType type) {
		return null;
	}

	public String getName() {
		return "elevate.xml";
	}
}
