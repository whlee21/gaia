package gaia.parser.gaia;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.solr.schema.FieldType;

class GaiaSchemaFieldTypes {
	public GaiaQueryParser parser = null;

	public GaiaSchemaFields schemaFields = null;

	public Map<String, GaiaSchemaFieldType> schemaFieldTypes = new HashMap<String, GaiaSchemaFieldType>();
	public static final String defaultTextPrefix = "text";
	public String textPrefix = "text";

	public GaiaSchemaFieldTypes(GaiaQueryParser parser, GaiaSchemaFields qf) {
		this.parser = parser;
		schemaFields = qf;
	}

	public GaiaSchemaFieldType get(FieldType ft) {
		if ((schemaFieldTypes == null) || (ft == null)) {
			return null;
		}
		String typeName = ft.getTypeName();
		GaiaSchemaFieldType t = (GaiaSchemaFieldType) schemaFieldTypes.get(typeName);
		if (t == null) {
			t = new GaiaSchemaFieldType(parser, schemaFields, ft);
			schemaFieldTypes.put(typeName, t);
		}

		return t;
	}

	public void setTextPrefix(String prefix) {
		textPrefix = prefix;

		if (schemaFieldTypes != null) {
			Iterator<GaiaSchemaFieldType> it = schemaFieldTypes.values().iterator();
			while (it.hasNext()) {
				GaiaSchemaFieldType t = (GaiaSchemaFieldType) it.next();
				t.determineFieldType();
			}
		}
	}
}
