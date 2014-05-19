/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gaia.search.server.api.services.parsers;

import gaia.search.server.api.services.RequestBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.BooleanNode;
import org.codehaus.jackson.node.IntNode;
import org.codehaus.jackson.node.LongNode;
import org.codehaus.jackson.node.ValueNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON parser which parses a JSON string into a map of properties and values.
 */
public class JsonRequestBodyParser implements RequestBodyParser {
	/**
	 * Logger instance.
	 */
	private final static Logger LOG = LoggerFactory.getLogger(JsonRequestBodyParser.class);

	@Override
	public RequestBody parse(String body) throws BodyParseException {

		// Set<RequestBody> requestBodySet = new HashSet<RequestBody>();
		// RequestBody requestBody = null;
		RequestBody requestBody = new RequestBody();
		requestBody.setBody(body);

		if (body != null && body.length() != 0) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode root = mapper.readTree(ensureArrayFormat(body));

				Iterator<JsonNode> iterator = root.getElements();
				if (iterator.hasNext()) {
					LOG.debug("whlee21 iterator.hasNext()");
					JsonNode node = iterator.next();
					// Map<String, Object> mapProperties = new HashMap<String, Object>();
					// Map<String, Object> requestInfoProps = new HashMap<String,
					// Object>();
					// NamedPropertySet propertySet = new NamedPropertySet("",
					// mapProperties);
					Map<String, Object> properties = new HashMap<String, Object>();

					processNode(node, "", properties);

					// If this node does not have request info properties
					// then add it
					// as a new property set to the root request body
					// if (!propertySet.getProperties().isEmpty()) {
					// LOG.debug("whlee21 requestInfoProps.isEmpty()");
					// requestBody.addPropertySet(propertySet);
					// }
					requestBody.setProperties(properties);
					LOG.debug("whlee21 requestInfoProps.isEmpty()");
					// requestBody = rootBody;
				}
			} catch (IOException e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Caught exception parsing msg body.");
					LOG.debug("Message Body: " + body, e);
				}
				throw new BodyParseException(e);
			}
		}
		// if (requestBodySet.isEmpty()) {
		// requestBodySet.add(rootBody);
		// }

		return requestBody;
	}

	private void processNode(JsonNode node, String path, Map<String, Object> properties) {

		// LOG.debug("whlee21 (path, node) = (" + path + ", "+ node + ")");
		Iterator<String> iterator = node.getFieldNames();

		while (iterator.hasNext()) {
			String name = iterator.next();
			JsonNode child = node.get(name);
			// LOG.debug("whlee21 (name, child) = (" + name + ", "+ child + ")");
			if (child.isArray()) {
				// LOG.debug("whlee21 child is array : " + child);
				// array
				Iterator<JsonNode> arrayIter = child.getElements();
				// Set<Map<String, Object>> arraySet = new HashSet<Map<String,
				// Object>>();
				// while (arrayIter.hasNext()) {
				// LOG.debug("whlee21 child next" + arrayIter.next());
				// NamedPropertySet arrayPropertySet = new NamedPropertySet(name, new
				// HashMap<String, Object>());
				// processNode(arrayIter.next(), "", arrayPropertySet,
				// requestInfoProps);
				// arraySet.add(arrayPropertySet.getProperties());
				// LOG.debug("whlee21 child aaa" + arraySet);
				// }

				List<Object> arrayList = new ArrayList<Object>();
				while (arrayIter.hasNext()) {
					JsonNode grandChild = arrayIter.next();
					if (grandChild instanceof ValueNode) {
						Object grandChildValue;
						if (grandChild instanceof IntNode) {
							grandChildValue = grandChild.asInt();
						} else if (grandChild instanceof LongNode) {
							grandChildValue = grandChild.asLong();
						} else if (grandChild instanceof BooleanNode) {
							grandChildValue = grandChild.asBoolean();
						} else {
							grandChildValue = grandChild.asText();
						}
						arrayList.add(grandChildValue);
					} else {
//						NamedPropertySet arrayPropertySet = new NamedPropertySet(name, new HashMap<String, Object>());
						Map<String, Object> childProperties = new HashMap<String, Object>();
						processNode(arrayIter.next(), "", childProperties);
						arrayList.add(childProperties);
					}
				}
				// propertySet.getProperties().put(PropertyHelper.getPropertyId(path,
				// name), arraySet);
				LOG.debug("whlee21 (path, name, arraySet) = (" + path + ", " + name + ", " + arrayList + ")");
				properties.put(name, arrayList);
			} else if (child.isContainerNode()) {
				// object
				if (name.equals(BODY_TITLE)) {
					name = "";
				}
				LOG.debug("whlee21 child.isContainerNode()");
				processNode(child, path.isEmpty() ? name : path + '/' + name, properties);
			} else {
				// field
				if (path.startsWith(REQUEST_INFO_PATH)) {
					// if (child instanceof IntNode) {
					// LOG.debug("whlee21 " + child.asText() + " is IntNode");
					// } else if (child instanceof LongNode) {
					// LOG.debug("whlee21 " + child.asText() + " is LongNode");
					// } else if (child instanceof BooleanNode) {
					// LOG.debug("whlee21 " + child.asText() + " is BooleanNode");
					// } else if (child instanceof TextNode) {
					// LOG.debug("whlee21 " + child.asText() + " is TextNode");
					// }
					// if (child.isBoolean()) {
					// LOG.debug("whlee21 parseNode " + child.asText() + " is boolean.");
					// }
					// if (child.isNumber()) {
					// LOG.debug("whlee21 parseNode " + child.asText() + " is number.");
					// // child.is
					// }
					// if (child.isTextual()) {
					// LOG.debug("whlee21 parseNode " + child.asText() + " is textual.");
					// }
					// requestInfoProps.put(PropertyHelper.getPropertyId(path.substring(REQUEST_INFO_PATH.length()),
					// name),
					// child.asText());
					LOG.debug("whlee21 (path, name, child) = (" + path + ", " + name + ", " + child.asText() + ")");
//					propertySet.getProperties().put(name, child.asText());
					if ("null".equalsIgnoreCase(child.asText())) {
						properties.put(name, null);
					} else {
						properties.put(name, child.asText());
					}
				} else {

					// if (child instanceof IntNode) {
					// LOG.debug("whlee21 " + child.asText() + " is IntNode");
					// } else if (child instanceof LongNode) {
					// LOG.debug("whlee21 " + child.asText() + " is LongNode");
					// } else if (child instanceof BooleanNode) {
					// LOG.debug("whlee21 " + child.asText() + " is BooleanNode");
					// } else if (child instanceof TextNode) {
					// LOG.debug("whlee21 " + child.asText() + " is TextNode");
					// }
					// if (child.isBoolean()) {
					// LOG.debug("whlee21 parseNode " + child.asText() + " is boolean.");
					// }
					// if (child.isNumber()) {
					// LOG.debug("whlee21 parseNode " + child.asText() + " is number.");
					// }
					// if (child.isTextual()) {
					// LOG.debug("whlee21 parseNode " + child.asText() + " is textual.");
					// }
					Object childValue;
					if (child instanceof IntNode) {
						childValue = child.asInt();
					} else if (child instanceof LongNode) {
						childValue = child.asLong();
					} else if (child instanceof BooleanNode) {
						childValue = child.asBoolean();
					} else {
						childValue = child.asText();
					}
					// propertySet.getProperties().put(PropertyHelper.getPropertyId(path.equals(BODY_TITLE)
					// ? "" : path, name),
					// childValue);
					LOG.debug("whlee21 (path, name, childValue) = (" + path + ", " + name + ", " + childValue + ")");
//					propertySet.getProperties().put(name, childValue);
//					properties.put(name,  childValue);
					if ("null".equalsIgnoreCase(childValue.toString())) {
						properties.put(name, null);
					} else {
						properties.put(name, childValue);
					}
				}
			}
		}
	}

	private String ensureArrayFormat(String s) {
		return s.startsWith("[") ? s : '[' + s + ']';
	}
}
