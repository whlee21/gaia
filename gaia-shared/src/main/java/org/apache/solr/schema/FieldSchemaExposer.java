package org.apache.solr.schema;

import java.util.Map;

public class FieldSchemaExposer {
	public static int getProperties(SchemaField field) {
		return field.getProperties();
	}

	public static String[] getPropertyNames() {
		return FieldProperties.propertyNames;
	}

	public static Map<String, Integer> getPropertyMap() {
		return FieldProperties.propertyMap;
	}

	public static int calcProps(String name, FieldType ft, Map<String, String> props) {
		return SchemaField.calcProps(name, ft, props);
	}
}
