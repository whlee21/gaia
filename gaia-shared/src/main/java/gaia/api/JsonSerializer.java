package gaia.api;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonSerializer implements ObjectSerializer {

	private static final Logger LOG = LoggerFactory.getLogger(JsonSerializer.class);

	/**
	 * Factory used to create JSON generator.
	 */
	// JsonFactory m_factory = new JsonFactory();

	// ObjectMapper m_mapper = new ObjectMapper(m_factory);
	// ObjectMapper m_mapper = new ObjectMapper();

	/**
	 * Generator which writes JSON.
	 */
//	JsonGenerator m_generator;

	@Override
	public Object serialize(Object obj) {
		try {
			// ByteArrayOutputStream bytesOut = init();
			//
			// if (result.getStatus().isErrorState()) {
			// return serializeError(result.getStatus());
			// }
			//
			// processNode(result.getResultTree());
			// if (result instanceof List) {
			// m_generator.writeStartObject();
			// Iterator<Object> iter = ((List)result).iterator();
			// while (iter.hasNext()) {
			//
			// }
			// m_generator.writeEndObject();
			// }
			// m_mapper.defaultPrettyPrintingWriter().writeValueAsString(value);

			// m_generator.close();
			// return bytesOut.toString("UTF-8");
			// m_mapper.defaultPrettyPrintingWriter();
			// ObjectWriter ow = m_mapper.writer().withDefaultPrettyPrinter();
			ObjectWriter m_writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
			return m_writer.writeValueAsString(obj);
		} catch (IOException e) {
			// todo: exception handling. Create ResultStatus 500 and call
			// serializeError
			throw new RuntimeException("Unable to serialize to json: " + e, e);
		}
	}

	// @Override
	// public Object serializeError(ResultStatus error) {
	// try {
	// ByteArrayOutputStream bytesOut = init();
	// // m_mapper.writeValue(m_generator, error);
	// m_generator.writeStartObject();
	// m_generator.writeNumberField("status", error.getStatus()
	// .getStatus());
	// m_generator.writeStringField("message", error.getMessage());
	// m_generator.writeFieldName("errors");
	// m_generator.writeStartArray();
	// for (Error err : error.getErrors()) {
	// m_generator.writeStartObject();
	// m_generator.writeStringField("message", err.getMessage());
	// m_generator.writeStringField("key", err.getKey());
	// m_generator.writeStringField("code", err.getCode());
	// m_generator.writeEndObject();
	// }
	// m_generator.writeEndArray();
	// m_generator.writeEndObject();
	// m_generator.close();
	// return bytesOut.toString("UTF-8");
	//
	// } catch (IOException e) {
	// // todo: exception handling
	// throw new RuntimeException("Unable to serialize to json: " + e, e);
	// }
	// }

	// private ByteArrayOutputStream init() throws IOException {
	// ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
	// m_generator = createJsonGenerator(bytesOut);
	//
	// DefaultPrettyPrinter p = new DefaultPrettyPrinter();
	// p.indentArraysWith(new DefaultPrettyPrinter.Lf2SpacesIndenter());
	// m_generator.setPrettyPrinter(p);
	//
	// return bytesOut;
	// }

	// private void processNode(TreeNode<Resource> node) throws IOException {
	// String name = node.getName();
	// Resource r = node.getObject();
	//
	// if (r == null) {
	// if (name != null) {
	// if (node.getParent() == null) {
	// m_generator.writeStartObject();
	// writeHref(node);
	// }
	// m_generator.writeArrayFieldStart(name);
	// }
	// } else {
	// m_generator.writeStartObject();
	// writeHref(node);
	// // resource props
	// handleResourceProperties(getTreeProperties(r.getPropertiesMap()));
	// }
	//
	// for (TreeNode<Resource> child : node.getChildren()) {
	// processNode(child);
	// }
	//
	// if (r == null) {
	// if (name != null) {
	// m_generator.writeEndArray();
	// if (node.getParent() == null) {
	// m_generator.writeEndObject();
	// }
	// }
	// } else {
	// m_generator.writeEndObject();
	// }
	// }
	//
	// private TreeNode<Map<String, Object>> getTreeProperties(
	// Map<String, Map<String, Object>> propertiesMap) {
	// TreeNode<Map<String, Object>> treeProperties = new TreeNodeImpl<Map<String,
	// Object>>(
	// null, new HashMap<String, Object>(), null);
	//
	// for (Map.Entry<String, Map<String, Object>> entry : propertiesMap
	// .entrySet()) {
	// String category = entry.getKey();
	// TreeNode<Map<String, Object>> node;
	// if (category == null) {
	// node = treeProperties;
	// } else {
	// node = treeProperties.getChild(category);
	// if (node == null) {
	// String[] tokens = category.split("/");
	// node = treeProperties;
	// for (String t : tokens) {
	// TreeNode<Map<String, Object>> child = node.getChild(t);
	// if (child == null) {
	// child = node.addChild(
	// new HashMap<String, Object>(), t);
	// }
	// node = child;
	// }
	// }
	// }
	//
	// Map<String, Object> properties = entry.getValue();
	//
	// for (Map.Entry<String, Object> propertyEntry : properties
	// .entrySet()) {
	// node.getObject().put(propertyEntry.getKey(),
	// propertyEntry.getValue());
	// }
	// }
	// return treeProperties;
	// }
	//
	// private void handleResourceProperties(TreeNode<Map<String, Object>> node)
	// throws IOException {
	// String category = node.getName();
	//
	// if (category != null) {
	// m_generator.writeFieldName(category);
	// m_generator.writeStartObject();
	// }
	//
	// for (Map.Entry<String, Object> entry : node.getObject().entrySet()) {
	// m_generator.writeFieldName(entry.getKey());
	// m_mapper.writeValue(m_generator, entry.getValue());
	// }
	//
	// for (TreeNode<Map<String, Object>> n : node.getChildren()) {
	// handleResourceProperties(n);
	// }
	//
	// if (category != null) {
	// m_generator.writeEndObject();
	// }
	// }

	// private JsonGenerator createJsonGenerator(ByteArrayOutputStream baos)
	// throws IOException {
	// JsonGenerator generator = m_factory
	// .createJsonGenerator(new OutputStreamWriter(baos, Charset
	// .forName("UTF-8").newEncoder()));
	//
	// DefaultPrettyPrinter p = new DefaultPrettyPrinter();
	// p.indentArraysWith(new DefaultPrettyPrinter.Lf2SpacesIndenter());
	// generator.setPrettyPrinter(p);
	//
	// return generator;
	// }

	// private void writeHref(TreeNode<Resource> node) throws IOException {
	// String hrefProp = node.getProperty("href");
	// if (hrefProp != null) {
	// m_generator.writeStringField("href", hrefProp);
	// }
	// }
}
