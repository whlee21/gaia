package gaia.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLUtils {
	public static final Map<String, String> INDENT;
	public static final Map<String, String> OMIT_XML;
	private static final XPathFactory xpathFactory = XPathFactory.newInstance();

	public static void transform(Node node, Result result, boolean indent) throws TransformerException {
		transform(node, result, indent == true ? INDENT : Collections.<String, String> emptyMap());
	}

	public static void transform(Node node, Result result, Map<String, String> attributes) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		for (Map.Entry<String, String> entry : attributes.entrySet()) {
			transformer.setOutputProperty((String) entry.getKey(), (String) entry.getValue());
		}

		DOMSource source = new DOMSource(node);

		transformer.transform(source, result);
	}

	public static String toString(Node node, Map<String, String> attributes) throws TransformerException {
		String result = null;
		StringWriter writer = new StringWriter();
		StreamResult sr = new StreamResult(writer);
		transform(node, sr, attributes);
		result = writer.toString();
		return result;
	}

	public static Document loadDocument(InputStream is) throws IOException, SAXException, ParserConfigurationException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

		return builder.parse(is);
	}

	public static Document loadDocument(InputSource is) throws IOException, SAXException, ParserConfigurationException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

		return builder.parse(is);
	}

	public static Document loadDocument(String content) throws IOException, ParserConfigurationException, SAXException {
		return loadDocument(new InputSource(new StringReader(content)));
	}

	public static Node getNode(String xpathStr, Node node) throws XPathExpressionException {
		XPath xpath = xpathFactory.newXPath();
		return (Node) xpath.evaluate(xpathStr, node, XPathConstants.NODE);
	}

	public static String getTextNode(String xpathStr, Node node) throws XPathExpressionException {
		XPath xpath = xpathFactory.newXPath();
		Text textNode = (Text) xpath.evaluate(xpathStr, node, XPathConstants.NODE);
		if (textNode == null)
			return null;
		return textNode.getNodeValue();
	}

	public static String getAttrValue(String xpathStr, Node node) throws XPathExpressionException {
		XPath xpath = xpathFactory.newXPath();
		Attr attr = (Attr) xpath.evaluate(xpathStr, node, XPathConstants.NODE);
		if (attr == null)
			return null;
		return attr.getValue();
	}

	public static Integer parseIntTextNode(String xpathStr, Node node, Integer defValue) throws XPathExpressionException {
		String str = getTextNode(xpathStr, node);
		if (str == null)
			return defValue;
		return Integer.valueOf(Integer.parseInt(str));
	}

	public static Boolean parseBooleanTextNode(String xpathStr, Node node, Boolean defValue)
			throws XPathExpressionException {
		String str = getTextNode(xpathStr, node);
		if (str == null)
			return defValue;
		return Boolean.valueOf(Boolean.parseBoolean(str));
	}

	public static void addOrUpdateTextNodeElement(String nodeName, String value, Node parentNode, Document doc)
			throws XPathExpressionException {
		Element el = (Element) getNode(nodeName, parentNode);
		if (el == null) {
			el = doc.createElement(nodeName);
			parentNode.appendChild(el);
		}
		Text textNode = (Text) getNode("text()", el);
		if (textNode == null) {
			textNode = doc.createTextNode(value);
			el.appendChild(textNode);
		} else {
			textNode.setData(value);
		}
	}

	public static NodeList getNodes(String xpathStr, Node node) throws XPathExpressionException {
		XPath xpath = xpathFactory.newXPath();
		return (NodeList) xpath.evaluate(xpathStr, node, XPathConstants.NODESET);
	}

	public static boolean exists(String xpathStr, Node node) throws XPathExpressionException {
		XPath xpath = xpathFactory.newXPath();
		return ((Boolean) xpath.evaluate(xpathStr, node, XPathConstants.BOOLEAN)).booleanValue();
	}

	static {
		INDENT = new HashMap<String, String>();
		INDENT.put("indent", "yes");
		OMIT_XML = new HashMap<String, String>();
		OMIT_XML.put("omit-xml-declaration", "yes");
	}
}
