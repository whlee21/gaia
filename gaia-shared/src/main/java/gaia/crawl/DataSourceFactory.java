package gaia.crawl;

import gaia.api.Error;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.GenericSpec;
import gaia.spec.SpecProperty;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class DataSourceFactory {
	protected final Map<String, DataSourceSpec> types = new HashMap<String, DataSourceSpec>();
	protected CrawlerController cc;
	protected Set<String> enabledTypes = new HashSet<String>();
	protected Set<String> restrictedTypes = new HashSet<String>();

	protected DataSourceFactory(CrawlerController cc) {
		this.cc = cc;
	}

	public void setEnabledTypes(String typesList) {
		enabledTypes.clear();
		if (StringUtils.isBlank(typesList)) {
			return;
		}
		String[] list = typesList.split("[\\s,]+");
		if (list.length > 0)
			enabledTypes.addAll(Arrays.asList(list));
	}

	public void setEnabledTypes(Set<String> enabledTypes) {
		enabledTypes.clear();
		enabledTypes.addAll(enabledTypes);
	}

	public Set<String> getEnabledTypes() {
		return Collections.unmodifiableSet(enabledTypes);
	}

	public void setRestrictedTypes(String typesList) {
		restrictedTypes.clear();
		if (StringUtils.isBlank(typesList)) {
			return;
		}
		String[] list = typesList.split("[\\s,]+");
		if (list.length > 0)
			restrictedTypes.addAll(Arrays.asList(list));
	}

	public void setRestrictedTypes(Set<String> restrictedTypes) {
		restrictedTypes.clear();
		restrictedTypes.addAll(restrictedTypes);
	}

	public Set<String> getRestrictedTypes() {
		return Collections.unmodifiableSet(restrictedTypes);
	}

	public Map<String, DataSourceSpec> getDataSourceSpecs() {
		if (enabledTypes.isEmpty()) {
			return Collections.unmodifiableMap(types);
		}

		HashMap<String, DataSourceSpec> res = new HashMap<String, DataSourceSpec>();
		for (String t : enabledTypes) {
			if (types.containsKey(t)) {
				res.put(t, types.get(t));
			}
		}
		return res;
	}

	public DataSourceSpec getSpec(String type) {
		if (type == null) {
			return null;
		}
		return (DataSourceSpec) types.get(type);
	}

	public DataSource create(Map<String, Object> m, String collection) throws DataSourceFactoryException {
		Map<String, Object> map = new HashMap<String, Object>();
		map.putAll(m);

		String dsType = (String) map.get("type");
		map.remove("type");
		String id = map.get("id") != null ? String.valueOf(map.get("id")) : null;
		DataSource ds = null;
		DataSourceSpec conf = null;
		if (dsType != null) {
			if ((!enabledTypes.isEmpty()) && (!enabledTypes.contains(dsType))) {
				throw new DataSourceFactoryException("Unsupported DataSource type: " + dsType);
			}
			conf = (DataSourceSpec) types.get(dsType.toLowerCase());
			if (conf == null) {
				throw new DataSourceFactoryException("Unsupported DataSource type: " + dsType);
			}
			try {
				Class<?> clz = DataSource.class;
				Constructor<?> constr = null;
				try {
					constr = clz.getConstructor(new Class[] { String.class, String.class, String.class });
					ds = (DataSource) constr.newInstance(new Object[] { dsType, cc.getClass().getName(), collection });
				} catch (Throwable t) {
					constr = clz.getConstructor(new Class[] { String.class, String.class });
					if (constr == null) {
						throw new Exception(
								"DataSource must have either (dsType,ccType,collection) or (ccType,collection) constructor!");
					}
					ds = (DataSource) constr.newInstance(new Object[] { cc.getClass().getName(), collection });
				}
				if (id != null) {
					ds.setDataSourceId(new DataSourceId(id));
				}
			} catch (Throwable t) {
				throw new DataSourceFactoryException("Failed to create " + dsType, t);
			}
		} else {
			throw new DataSourceFactoryException("Missing data source type.", new Error("type", Error.E_NULL_VALUE));
		}

		if (ds == null) {
			ds = new DataSource(dsType, cc.getClass().getName(), collection);
			if (id != null) {
				ds.setDataSourceId(new DataSourceId(id));
			}
			conf = new GenericSpec();
		}
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (conf != null) {
				SpecProperty spec = conf.getSpecProperty((String) entry.getKey());
//				if ((spec != null) && (spec.readOnly))
//					;
				
				if(spec != null && spec.required){
					ds.setProperty((String) entry.getKey(), entry.getValue());
				}
			} else if (!"mapping".equals(entry.getKey())) {
				ds.setProperty((String) entry.getKey(), entry.getValue());
			}
		}

		if (conf != null) {
			List<Error> err = conf.validate(ds.getProperties());
			if (!err.isEmpty()) {
				throw new DataSourceFactoryException("Validation errors", err);
			}
		}

		conf.cast(ds.getProperties());

		ds.setFieldMapping(conf.getDefaultFieldMapping());

		Map<String, Object> mapping = (Map) m.get("mapping");
		if ((mapping != null) && (!mapping.isEmpty())) {
			FieldMapping.fromMap(ds.getFieldMapping(), mapping);
		}

		return ds;
	}
}
