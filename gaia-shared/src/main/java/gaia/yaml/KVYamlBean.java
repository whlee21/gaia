package gaia.yaml;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.persistence.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class KVYamlBean extends YamlBean {
	private static final Logger LOG = LoggerFactory.getLogger(KVYamlBean.class);
	private Map<String, Object> values;

	@Transient
	private boolean initCalled = false;

	@Transient
	private boolean readOnly = true;

	public KVYamlBean() {
		values = new TreeMap<String, Object>();
		readOnly = true;
	}

	public KVYamlBean(String file, boolean readOnly) {
		super(file, false, YamlBean.LocationIfRelative.CONF);
		this.readOnly = readOnly;

		if (!initCalled)
			init();
	}

	protected void init() {
		initCalled = true;
		if (values == null) {
			values = new TreeMap<String, Object>();
		}

		initDefaultValues();

		if (readOnly) {
			return;
		}
		save();
	}

	protected abstract void initDefaultValues();

	protected void load(YamlBean yamlBean) {
		KVYamlBean defaults = (KVYamlBean) yamlBean;

		values = defaults.values;
	}

	public synchronized void set(Group group, String key, Object value) {
		String fullKey = group.toString() + "." + key;
		if (value == null)
			values.remove(fullKey);
		else {
			values.put(fullKey, value);
		}
		if (readOnly) {
			return;
		}
		save();
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public synchronized void init(Group group, String key, Object value) {
		String fullKey = group.toString() + "." + key;
		if (value == null) {
			if (values.get(fullKey) != null)
				values.remove(key);
			else {
				values.put(fullKey, null);
			}

		} else if (values.get(fullKey) == null)
			values.put(fullKey, value);
	}

	public int getInt(Group group, String key) {
		return getInt(group, key, Integer.valueOf(0));
	}

	public synchronized int getInt(Group group, String key, Integer defValue) {
		Object v = getObject(group, key, defValue);
		if ((v instanceof Number))
			return ((Number) v).intValue();
		try {
			return Integer.parseInt(v.toString());
		} catch (NumberFormatException nfe) {
			LOG.warn("Invalid integer default value '" + v + "', replacing with " + defValue);
			set(group, key, defValue);
		}
		return defValue.intValue();
	}

	public float getFloat(Group group, String key) {
		return getFloat(group, key, Float.valueOf(0.0F));
	}

	public synchronized float getFloat(Group group, String key, Float defValue) {
		Object v = getObject(group, key, defValue);
		if ((v instanceof Number))
			return ((Number) v).floatValue();
		try {
			return Float.parseFloat(v.toString());
		} catch (NumberFormatException nfe) {
			LOG.warn("Invalid float default value '" + v + "', replacing with " + defValue);
			set(group, key, defValue);
		}
		return defValue.floatValue();
	}

	public long getLong(Group group, String key) {
		return getLong(group, key, Long.valueOf(0L));
	}

	public synchronized long getLong(Group group, String key, Long defValue) {
		Object v = getObject(group, key, defValue);
		if ((v instanceof Number))
			return ((Number) v).longValue();
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException nfe) {
			LOG.warn("Invalid long default value '" + v + "', replacing with " + defValue);
			set(group, key, defValue);
		}
		return defValue.longValue();
	}

	public boolean getBoolean(Group group, String key) {
		return getBoolean(group, key, Boolean.FALSE);
	}

	public synchronized boolean getBoolean(Group group, String key, Boolean defValue) {
		Object v = getObject(group, key, defValue);
		if ((v instanceof Boolean)) {
			return ((Boolean) v).booleanValue();
		}
		return Boolean.parseBoolean(v.toString());
	}

	public String getString(Group group, String key) {
		return getString(group, key, null);
	}

	public synchronized String getString(Group group, String key, String defValue) {
		Object v = getObject(group, key, defValue);
		if (v == null) {
			return null;
		}
		return v.toString();
	}

	public synchronized Object getObject(Group group, String key, Object defValue) {
		String fullKey = group.toString() + "." + key;
		Object v = values.get(fullKey);
		if (v == null) {
			set(group, key, defValue);
			return defValue;
		}
		return v;
	}

	public List getList(Group group, String key, List defValue) {
		Object v = getObject(group, key, defValue);
		if (v == null) {
			return defValue;
		}
		if ((v instanceof List))
			return (List) v;
		if ((v instanceof String)) {
			return Arrays.asList(((String) v).split("\\|"));
		}
		return defValue;
	}

	public static interface Group {
	}
}
