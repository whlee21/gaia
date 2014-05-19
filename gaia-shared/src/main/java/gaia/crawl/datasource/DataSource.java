package gaia.crawl.datasource;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public final class DataSource implements Serializable, DataSourceAPI {
	private static final Logger LOG = LoggerFactory.getLogger(DataSource.class);
	private Map<String, Object> properties = new HashMap<String, Object>();
	private DataSourceId dsId;
	private FieldMapping fieldMapping;
	private Date createDate;
	private Date lastModified;

	protected DataSource() {
		setDataSourceId(new DataSourceId("", UUID.randomUUID().toString()));
	}

	public DataSource(String dsType, String crawlerType, String collection) {
		this();
		Date now = new Date();
		setCreateDate(now);
		setLastModified(now);
		setProperty("type", dsType);
		setCrawlerType(crawlerType);
		setCollection(collection);
	}

	public DataSource(DataSource template) {
		dsId = new DataSourceId(template.dsId.toString());
		if (template.fieldMapping != null) {
			fieldMapping = new FieldMapping();
			fieldMapping.setFrom(template.fieldMapping, true);
		} else {
			fieldMapping = null;
		}
		if (template.createDate != null)
			createDate = ((Date) template.createDate.clone());
		if (template.lastModified != null) {
			lastModified = ((Date) template.lastModified.clone());
		}
		properties.putAll(template.properties);
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public String getCategory() {
		return (String) getProperty("category");
	}

	public void setCategory(String cat) {
		setProperty("category", cat);
	}

	public DataSourceId getDataSourceId() {
		return dsId;
	}

	public void setDataSourceId(DataSourceId dsId) {
		this.dsId = dsId;
		setProperty("id", dsId.toString());
	}

	public String getDisplayName() {
		return (String) properties.get("name");
	}

	public void setDisplayName(String displayName) {
		setProperty("name", displayName);
	}

	public void setCrawlerType(String crawlerClass) {
		setProperty("crawler", crawlerClass);
	}

	public String getCrawlerType() {
		return (String) getProperty("crawler");
	}

	public String getType() {
		return (String) getProperty("type");
	}

	public String getCollection() {
		return (String) getProperty("collection");
	}

	public void setCollection(String collection) {
		setProperty("collection", collection);
	}

	public FieldMapping getFieldMapping() {
		return fieldMapping;
	}

	public void setFieldMapping(FieldMapping mapping) {
		fieldMapping = mapping;
	}

	public boolean isSecurityTrimmingEnabled() {
		return getBoolean("enable_security_trimming", false);
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void clearProperties() {
		properties.clear();
	}

	public Object getProperty(String name) {
		return properties.get(name);
	}

	public Object getProperty(String name, Object defVal) {
		Object res = properties.get(name);
		if (res != null)
			return res;
		return defVal;
	}

	public Object setProperty(String name, Object value) {
		if (value == null) {
			return properties.remove(name);
		}
		return properties.put(name, value);
	}

	public void setProperties(Map<String, Object> props) {
		if ((props == null) || (props.isEmpty())) {
			return;
		}
		properties.putAll(props);
	}

	public int getInt(String name) {
		return getInt(name, 0);
	}

	public int getInt(String name, int defVal) {
		Object o = properties.get(name);
		if (o == null) {
			return defVal;
		}
		if ((o instanceof Number))
			return ((Number) o).intValue();
		if ((o instanceof String)) {
			try {
				return Integer.parseInt((String) o);
			} catch (Exception e) {
				return defVal;
			}
		}
		return defVal;
	}

	public long getLong(String name) {
		return getLong(name, 0L);
	}

	public long getLong(String name, long defVal) {
		Object o = properties.get(name);
		if (o == null) {
			return defVal;
		}
		if ((o instanceof Number))
			return ((Number) o).longValue();
		if ((o instanceof String)) {
			try {
				return Long.parseLong((String) o);
			} catch (Exception e) {
				return defVal;
			}
		}
		return defVal;
	}

	public String getString(String name) {
		return getString(name, null);
	}

	public String getString(String name, String defVal) {
		Object o = properties.get(name);
		if (o == null) {
			return defVal;
		}
		return o.toString();
	}

	public boolean getBoolean(String name) {
		return getBoolean(name, false);
	}

	public boolean getBoolean(String name, boolean defVal) {
		Object o = properties.get(name);
		if (o == null) {
			return defVal;
		}
		if ((o instanceof Boolean))
			return ((Boolean) o).booleanValue();
		if ((o instanceof Number))
			return ((Number) o).intValue() != 0;
		if ((o instanceof String)) {
			try {
				return Boolean.parseBoolean((String) o);
			} catch (Exception e) {
				return defVal;
			}
		}
		return defVal;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.putAll(getProperties());

		m.put("id", getDataSourceId().toString());

		if ((fieldMapping != null) && (fieldMapping.toMap().size() > 0)) {
			m.put("mapping", fieldMapping.toMap());
		}
		return m;
	}

	public static DataSource fromMap(Map<String, Object> map) {
		if (map == null) {
			return null;
		}
		DataSource res = new DataSource();

		Map fmap = (Map) map.remove("mapping");
		res.properties.putAll(map);
		res.dsId = new DataSourceId(res.getString("id"));

		if (fmap != null) {
			FieldMapping mapping = new FieldMapping();
			FieldMapping.fromMap(mapping, fmap);
			res.setFieldMapping(mapping);
		}
		return res;
	}

	public String toString() {
		return toMap().toString();
	}

	public int hashCode() {
		int prime = 31;
		int result = prime + (dsId == null ? 0 : dsId.hashCode());
		result = prime * result + (fieldMapping == null ? 0 : fieldMapping.hashCode());

		result = prime * result + (properties == null ? 0 : properties.hashCode());

		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		DataSource other = (DataSource) obj;
		if (dsId == null) {
			if (other.dsId != null)
				return false;
		} else if (!dsId.equals(other.dsId))
			return false;
		if (fieldMapping == null) {
			if (other.fieldMapping != null)
				return false;
		} else if (!fieldMapping.equals(other.fieldMapping))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else {
			for (Map.Entry<String, Object> e : properties.entrySet()) {
				String k = e.getKey();
				Object v = e.getValue();
				Object cmpV = other.properties.get(k);
				if (v == null) {
					if (cmpV != null)
						return false;
				} else {
					boolean res;
					if ((v instanceof Number))
						res = String.valueOf(v).equals(String.valueOf(cmpV));
					else {
						res = v.equals(cmpV);
					}
					if (!res)
						return false;
				}
			}
		}
		return true;
	}
}
