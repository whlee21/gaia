package gaia.crawl.datasource;

import gaia.Defaults;

import java.io.Reader;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.DateField;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldMappingUtil {
	private static final Logger LOG = LoggerFactory.getLogger(FieldMappingUtil.class);

	private static FieldMapping defaultMapping;
	private static FieldMapping tikaDefaultMapping;
	private static FieldMapping apertureDefaultMapping;
	private static FieldMapping emptyMapping = new FieldMapping();

	public static void addApertureFieldMapping(FieldMapping mapping, boolean overwrite) {
		mapping.setFrom(apertureDefaultMapping, overwrite);
	}

	public static void addTikaFieldMapping(FieldMapping mapping, boolean overwrite) {
		mapping.setFrom(tikaDefaultMapping, overwrite);
	}

	public static void verifySchema(FieldMapping mapping, SolrCore core) throws Exception {
		if (core == null) {
			LOG.warn("No SolrCore - can't verify field mapping with schema.");
			return;
		}
		IndexSchema schema = core.getLatestSchema();

		Map<String, SchemaField> schemaFields = new HashMap<String, SchemaField>(schema.getFields());

		SchemaField uniqueKeyField = schema.getUniqueKeyField();
		String uniqueKeyFieldName = "";
		if (uniqueKeyField != null) {
			uniqueKeyFieldName = uniqueKeyField.getName();
			schemaFields.remove(uniqueKeyField.getName());
		} else {
			LOG.warn(new StringBuilder().append("No unique key field for collection ").append(core.getName())
					.append("; this may cause problems for some Solr features").toString());
		}
		if ((mapping.getUniqueKey() != null) && (!mapping.getUniqueKey().equals(uniqueKeyFieldName))) {
			mapping.defineMapping(mapping.getUniqueKey(), uniqueKeyFieldName);
		}

		mapping.setUniqueKey(uniqueKeyFieldName);

		if (mapping.getDefaultField() != null) {
			SchemaField fld = schema.getFieldOrNull(mapping.getDefaultField());
			if (fld == null) {
				LOG.warn(new StringBuilder().append("FieldMapping defaultField '").append(mapping.getDefaultField())
						.append("' not present in schema! Disabling.").toString());

				mapping.setDefaultField(null);
			} else {
				if (fld.multiValued()) {
					mapping.setMultivalued(fld.getName(), true);
				}
				schemaFields.remove(fld.getName());
			}
		}

		SchemaField fld = schema.getFieldOrNull(mapping.getDatasourceField());
		if (fld == null) {
			LOG.warn(new StringBuilder().append("FieldMapping datasourceField '").append(mapping.getDatasourceField())
					.append("' not present in schema!").toString());
		} else {
			if (fld.multiValued()) {
				mapping.setMultivalued(fld.getName(), true);
			}
			schemaFields.remove(fld.getName());
		}

		if (mapping.getDynamicField() != null) {
			SchemaField field = schema.getFieldOrNull(new StringBuilder().append(mapping.getDynamicField()).append("_any")
					.toString());
			if (field == null) {
				LOG.warn(new StringBuilder().append("FieldMapping dynamicField '").append(mapping.getDynamicField())
						.append("' not present in schema! Disabling.").toString());

				mapping.setDynamicField(null);
			} else {
				if (field.multiValued()) {
					mapping.setMultivalued(mapping.getDynamicField(), true);
				}
				schemaFields.remove(fld.getName());
			}
		}
		HashSet<String> srcNames = new HashSet<String>(mapping.getMappings().keySet());
		for (String srcName : srcNames) {
			String tgtName = (String) mapping.getMappings().get(srcName);

			schemaFields.remove(tgtName);
			if (tgtName != null) {
				fld = schema.getFieldOrNull(tgtName);
				if (fld == null) {
					LOG.warn(new StringBuilder().append("FieldMapping '").append(srcName).append("'->'").append(tgtName)
							.append("' target field not present in schema! Disabling.").toString());

					mapping.removeMapping(srcName);
				} else {
					adjustTypeMapping(fld, mapping);
				}
			}
		}
		for (SchemaField sf : schemaFields.values())
			adjustTypeMapping(sf, mapping);
	}

	private static void adjustTypeMapping(SchemaField fld, FieldMapping mapping) {
		if (mapping.isMultivalued(fld.getName()) == null) {
			mapping.setMultivalued(fld.getName(), fld.multiValued());
		} else if (!fld.multiValued()) {
			mapping.setMultivalued(fld.getName(), false);
		} else if (mapping.isMultivalued(fld.getName()).booleanValue())
			;
		if (mapping.getTypes().containsKey(fld.getName())) {
			return;
		}

		FieldType type = fld.getType();
		if ((type instanceof DateField))
			mapping.defineType(fld.getName(), FieldMapping.FType.DATE);
		else if (type.getClass().getName().endsWith("DoubleField"))
			mapping.defineType(fld.getName(), FieldMapping.FType.DOUBLE);
		else if (type.getClass().getName().endsWith("FloatField"))
			mapping.defineType(fld.getName(), FieldMapping.FType.FLOAT);
		else if (type.getClass().getName().endsWith("IntField"))
			mapping.defineType(fld.getName(), FieldMapping.FType.INT);
		else if (type.getClass().getName().endsWith("ShortField"))
			mapping.defineType(fld.getName(), FieldMapping.FType.INT);
		else if (type.getClass().getName().endsWith("LongField"))
			mapping.defineType(fld.getName(), FieldMapping.FType.LONG);
	}

	public static void normalizeFields(SolrInputDocument doc, FieldMapping mapping) {
		HashSet<String> fieldNames = new HashSet<String>(doc.getFieldNames());

		for (Map.Entry<String, String> e : mapping.getLiterals().entrySet()) {
			doc.addField((String) e.getKey(), e.getValue());
		}

		for (String name : fieldNames)
			if (name == null) {
				doc.remove(name);
			} else {
				Collection<Object> values = doc.getFieldValues(name);
				if ((values == null) || (values.size() == 0)) {
					doc.remove(name);
				} else {
					if ((name.equals("mimeType")) && (mapping.isMultivalued("mimeType") != null)
							&& (!mapping.isMultivalued("mimeType").booleanValue())) {
						doc.remove(name);
						String last = null;
						for (Object o : values) {
							String v = o.toString();

							int pos = v.indexOf(59);
							if (pos != -1) {
								v = v.substring(0, pos);
							}
							if ((last == null) || (last.length() < v.length())) {
								last = v;
							}
						}
						doc.addField(name, last);
					}
					Boolean multi;
					boolean set;

					if (mapping.checkType(name) == FieldMapping.FType.DATE) {
						doc.remove(name);
						multi = mapping.isMultivalued(name);
						set = false;
						for (Object o : values) {
							if ((o instanceof Date)) {
								doc.addField(name, o);
								set = true;
							} else {
								String dateTime = o.toString();
								Date validDate = null;
								try {
									validDate = DateUtil.parseDate(dateTime);
								} catch (ParseException e) {
									LOG.warn(new StringBuilder().append("Invalid date in field ").append(name).append(": '")
											.append(dateTime).append("' removed: ").append(e.toString()).toString());
								}
								if (validDate != null) {
									doc.addField(name, validDate);
									set = true;
								}
							}
							if ((multi != null) && (!multi.booleanValue()) && (set))
								break;
						}
					} else {
						if (mapping.checkType(name) == FieldMapping.FType.INT) {
							doc.remove(name);
							for (Object o : values) {
								if ((o instanceof Number))
									doc.addField(name, Long.valueOf(((Number) o).longValue()));
								else {
									try {
										Double D = Double.valueOf(Double.parseDouble(String.valueOf(o)));
										doc.addField(name, Integer.valueOf(D.intValue()));
									} catch (Exception e) {
										LOG.warn(new StringBuilder().append("Invalid integer in field ").append(name).append(": '")
												.append(o).append("' removed.").toString());
									}
								}
							}
						} else if (mapping.checkType(name) == FieldMapping.FType.LONG) {
							doc.remove(name);
							for (Object o : values) {
								if ((o instanceof Number))
									doc.addField(name, Long.valueOf(((Number) o).longValue()));
								else {
									try {
										Double D = Double.valueOf(Double.parseDouble(String.valueOf(o)));
										doc.addField(name, Long.valueOf(D.longValue()));
									} catch (Exception e) {
										LOG.warn(new StringBuilder().append("Invalid long in field ").append(name).append(": '").append(o)
												.append("' removed.").toString());
									}
								}
							}
						}
						if ((mapping.isMultivalued(name) != null) && (!mapping.isMultivalued(name).booleanValue())
								&& (values.size() > 1)) {
							doc.remove(name);
							if (mapping.checkType(name) == FieldMapping.FType.STRING) {
								LinkedHashSet<String> uniq = new LinkedHashSet<String>(values.size());
								for (Object o : values) {
									uniq.add(o.toString());
								}
								StringBuilder sb = new StringBuilder();
								for (String s : uniq) {
									if (sb.length() > 0)
										sb.append(' ');
									sb.append(s);
								}
								doc.setField(name, sb.toString());
							} else {
								doc.setField(name, values.iterator().next());
							}
						}
					}
				}
			}
		String uniqueKey = mapping.getUniqueKey();
		if (uniqueKey != null) {
			Collection<Object> vals = doc.getFieldValues(uniqueKey);
			if ((vals != null) && (vals.size() != 0)) {
				if (vals.size() > 0) {
					Object id = null;
					for (Object i : vals) {
						id = i;
					}
					doc.remove(uniqueKey);
					doc.addField(uniqueKey, id);
				}
			}
		}
	}

	public static void addGaiaSearchFields(FieldMapping template, DataSourceAPI ds) {
		FieldMapping src = ds.getFieldMapping();
		String dataSourceField = src != null ? src.getDatasourceField() : emptyMapping.getDatasourceField();
		if (dataSourceField == null) {
			LOG.warn(new StringBuilder()
					.append("addGaiaworksFields is true, but dataSourceField is null ??? replacing with ")
					.append(emptyMapping.getDatasourceField()).toString());
			dataSourceField = emptyMapping.getDatasourceField();
		}
		template.setLiteral(dataSourceField, ds.getDataSourceId().toString());
		String type = ds.getCategory();
		if (type == null) {
			type = (String) ds.getProperty("type");
		}
		template.setLiteral(new StringBuilder().append(dataSourceField).append("_type").toString(), type);
		template.setLiteral(new StringBuilder().append(dataSourceField).append("_name").toString(), ds.getDisplayName());
	}

	public static void ensureGaiaFields(SolrInputDocument doc, DataSourceAPI ds) {
		FieldMapping mapping = ds.getFieldMapping();
		if (mapping == null) {
			mapping = emptyMapping;
		}

		if (doc.getField(mapping.getDatasourceField()) == null) {
			doc.addField(mapping.getDatasourceField(), ds.getDataSourceId().toString());
		}

		String typeField = new StringBuilder().append(mapping.getDatasourceField()).append("_type").toString();
		if (doc.getField(typeField) == null) {
			if (ds.getCategory() != null)
				doc.addField(typeField, ds.getCategory());
			else {
				doc.addField(typeField, ds.getProperty("type"));
			}
		}

		String nameField = new StringBuilder().append(mapping.getDatasourceField()).append("_name").toString();
		if (doc.getField(nameField) == null) {
			doc.addField(nameField, ds.getDisplayName());
		}

		String lastModified = "lastModified";
		if (doc.getField(lastModified) == null)
			doc.addField(lastModified, new Date());
	}

	public static void generateUniqueKey(SolrInputDocument doc, String uniqueKey) {
		if (doc.getField(uniqueKey) == null) {
			UUID uuid = UUID.randomUUID();
			doc.addField(uniqueKey, uuid.toString());
		}
	}

	public static String map(String name, FieldMapping mapping, IndexSchema schema) {
		String key = name.toLowerCase();
		if (mapping != null) {
			Map<String, String> mappings = mapping.getMappings();
			String res = (String) mappings.get(key);
			if (mappings.containsKey(key)) {
				if (StringUtils.isBlank(res)) {
					return null;
				}
				return res;
			}
		}
		if (schema != null) {
			if (schema.getFieldOrNull(name) != null) {
				return name;
			}
		}
		if (mapping != null) {
			if (mapping.getDynamicField() != null)
				return toDynamic(name, mapping.getDynamicField());
			if (mapping.getDefaultField() != null) {
				return mapping.getDefaultField();
			}
		}
		return name;
	}

	private static String toDynamic(String name, String dynamicPrefix) {
		name = name.trim().replaceAll("[\\W]", "_").toLowerCase();
		return new StringBuilder().append(dynamicPrefix).append("_").append(name).toString();
	}

	public static void mapFields(SolrInputDocument doc, FieldMapping mapping, IndexSchema schema) {
		HashSet<String> fieldNames = new HashSet<String>(doc.getFieldNames());
		if (mapping == null) {
			return;
		}
		for (String sourceName : fieldNames) {
			if ((!sourceName.equals(mapping.getUniqueKey()))
					&& ((!sourceName.equals("_version_")) || (mapping.getMappings().containsKey("_version_")))) {
				String targetFieldName = map(sourceName, mapping, schema);
				if (targetFieldName == null) {
					doc.removeField(sourceName);
				} else if (!targetFieldName.equals(sourceName)) {
					Collection<Object> values = doc.getFieldValues(sourceName);
					doc.removeField(sourceName);
					if (values != null)
						for (Object obj : values) {
							doc.addField(targetFieldName, obj);
						}
				}
			}
		}
		String dynamicField = mapping.getDynamicField() != null ? mapping.getDynamicField() : "attr";
		String dynAuthor = new StringBuilder().append(dynamicField).append("_author").toString();
		String mappedAuthor = map("author", mapping, schema);
		if ((!doc.containsKey(mappedAuthor)) && (doc.containsKey(dynAuthor))) {
			for (Object value : doc.getFieldValues(dynAuthor)) {
				doc.addField(mappedAuthor, value);
			}
			doc.remove(dynAuthor);
		}
	}

	public static String toJSON(FieldMapping mapping) throws Exception {
		LOG.info(new StringBuilder().append("mapping: ").append(mapping).toString());
		ObjectMapper mapper = new ObjectMapper();
		StringWriter sw = new StringWriter();
		mapper.writeValue(sw, mapping.toMap());
		return sw.toString();
	}

	public static FieldMapping fromJSON(Reader json, FieldMapping template) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = (Map) mapper.readValue(json, Map.class);
		FieldMapping fmap = new FieldMapping();
		if (template != null) {
			fmap.setFrom(template, true);
		}
		FieldMapping.fromMap(fmap, map);
		return fmap;
	}

	public static String mappingsToJSON(Map<String, Map<String, FieldMapping>> mappings) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		StringWriter sw = new StringWriter();
		HashMap<String, Object> map = new HashMap<String, Object>();
		for (Map.Entry<String, Map<String, FieldMapping>> e : mappings.entrySet()) {
			HashMap<String, Object> m = new HashMap<String, Object>();
			map.put(e.getKey(), m);
			for (Map.Entry<String, FieldMapping> em : e.getValue().entrySet())
				m.put(em.getKey(), ((FieldMapping) em.getValue()).toMap());
		}
		mapper.writeValue(sw, map);
		return sw.toString();
	}

	public static Map<String, Map<String, FieldMapping>> mappingsFromJSON(String json) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = (Map) mapper.readValue(json, Map.class);
		HashMap<String, Map<String, FieldMapping>> mappings = new HashMap<String, Map<String, FieldMapping>>();
		for (Map.Entry<String, Object> e : map.entrySet()) {
			Map<String, FieldMapping> perDs = (Map) e.getValue();
			Map<String, FieldMapping> perDsRes = new HashMap<String, FieldMapping>();
			mappings.put(e.getKey(), perDsRes);
			for (Map.Entry<String, FieldMapping> em : perDs.entrySet()) {
				Map<String, Object> mapping = (Map) em.getValue();
				FieldMapping fmap = new FieldMapping();
				FieldMapping.fromMap(fmap, mapping);
				perDsRes.put(em.getKey(), fmap);
			}
		}
		return mappings;
	}

	static {
		DateUtil.DEFAULT_DATE_FORMATS.add("yyyy-MM-dd'T'HH:mm:ss.000'Z'");

		defaultMapping = FieldMapping.defaultFieldMapping();
		apertureDefaultMapping = (FieldMapping) Defaults.INSTANCE.getObject(Defaults.Group.datasource,
				FieldMapping.MAPPING_APERTURE_KEY, defaultMapping);
		tikaDefaultMapping = (FieldMapping) Defaults.INSTANCE.getObject(Defaults.Group.datasource,
				FieldMapping.MAPPING_TIKA_KEY, defaultMapping);
	}
}
