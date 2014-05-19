package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSolrConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.common.SolrException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.CacheRegenerator;
import org.apache.solr.search.FastLRUCache;
import org.apache.solr.search.LRUCache;
import org.apache.solr.search.SolrCache;
import org.restlet.resource.ServerResource;
import org.xml.sax.SAXException;

public class CachesResourceBase extends ServerResource {
	public static final String NAME = "name";
	public static final String CLASS = "class";
	public static final String REGENERATOR = "regenerator";
	public static final String SIZE = "size";
	public static final String INITIAL_SIZE = "initialSize";
	public static final String AUTOWARM_COUNT = "autowarmCount";
	public static final String[] LRU_CACHE_ATTRS = { "size", "initialSize", "autowarmCount" };
	public static final String MIN_SIZE = "minSize";
	public static final String ACCEPTABLE_SIZE = "acceptableSize";
	public static final String CLEANUP_THREAD = "cleanupThread";
	public static final String SHOW_ITEMS = "showItems";
	public static final String[] FAST_LRU_CACHE_ATTRS = { "minSize", "acceptableSize", "cleanupThread", "showItems" };
	protected CoreContainer cores;
	protected CollectionManager cm;
	protected String collection;
	protected SolrCore solrCore;
	protected Map<String, Map<String, String>> currentCaches;
	EditableSolrConfig ecc;

	public CachesResourceBase(CoreContainer cores, CollectionManager cm) {
		this.cores = cores;
		this.cm = cm;
	}

	public void doInit() {
		collection = ((String) getRequestAttributes().get("coll_name"));
		solrCore = cores.getCore(collection);
		setExisting(solrCore != null);

		ecc = new EditableSolrConfig(solrCore, cm.getUpdateChain(), cores.getZkController());
		currentCaches = getCaches();
	}

	public void doRelease() {
		ecc = null;
		collection = null;
		if (solrCore != null) {
			solrCore.close();
		}
		solrCore = null;
		currentCaches = null;
	}

	protected Map<String, Map<String, String>> getCaches() {
		Map<String, Map<String, String>> res = new HashMap<String, Map<String, String>>();

		Map<String, Map<String, String>> userCaches = ecc.getUserCaches();
		res.putAll(userCaches);

		for (String name : EditableSolrConfig.SOLR_CACHES) {
			Map<String, String> attrs = ecc.getCacheConfig(name);
			if (attrs != null) {
				res.put(name, attrs);
			}
		}

		if (!res.containsKey(EditableSolrConfig.FIELD_VALUE_CACHE)) {
			Map<String, String> attrs = new HashMap<String, String>();
			attrs.put("class", "solr.FastLRUCache");
			attrs.put("size", "10000");
			attrs.put("showItems", "-1");
			attrs.put("initialSize", "10");
			res.put(EditableSolrConfig.FIELD_VALUE_CACHE, attrs);
		}

		return res;
	}

	public void createOrUpdateCache(String name, Map<String, Object> m, String existingClassName, boolean createMode)
			throws ParserConfigurationException, IOException, SAXException {
		Map<String, String> attrs = new HashMap<String, String>();

		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}

		if (createMode) {
			if (currentCaches.containsKey(name)) {
				throw ErrorUtils.statusExp(422, new Error("name", Error.E_FORBIDDEN_VALUE,
						"An explicit cache already exists with the name: " + name));
			}

		}

		Object obj = m.remove("class");

		Class<?> solrCacheClass = null;
		if ((createMode) || (obj != null)) {
			if ((obj == null) || (!(obj instanceof String)) || ("".equals(obj))) {
				throw ErrorUtils.statusExp(422, new Error("class", Error.E_MISSING_VALUE));
			}
			String solrCacheClassName = (String) obj;
			try {
				solrCacheClass = solrCore.getResourceLoader().findClass(solrCacheClassName, Object.class);
			} catch (SolrException e) {
				throw ErrorUtils.statusExp(422, new Error("class", Error.E_FORBIDDEN_VALUE, "Could not find specified class: "
						+ solrCacheClassName));
			}

			if (!SolrCache.class.isAssignableFrom(solrCacheClass)) {
				throw ErrorUtils.statusExp(422, new Error("class", Error.E_FORBIDDEN_VALUE,
						"Specified class does not implement SolrCache interface: " + solrCacheClassName));
			}

			attrs.put("class", solrCacheClassName);
		}

		if ((solrCacheClass == null) && (existingClassName != null)) {
			try {
				solrCacheClass = solrCore.getResourceLoader().findClass(existingClassName, Object.class);
			} catch (SolrException e) {
			}
		}

		obj = m.remove("regenerator");

		if (obj != null) {
			if ((!(obj instanceof String)) || ("".equals(obj))) {
				throw ErrorUtils.statusExp(422, new Error("regenerator", Error.E_MISSING_VALUE));
			}
			String regeneratorClassName = (String) obj;
			Class<?> clazz;
			try {
				clazz = solrCore.getResourceLoader().findClass(regeneratorClassName, Object.class);
			} catch (SolrException e) {
				throw ErrorUtils.statusExp(422, new Error("regenerator", Error.E_FORBIDDEN_VALUE,
						"Could not find specified class: " + regeneratorClassName));
			}

			if (!CacheRegenerator.class.isAssignableFrom(clazz)) {
				throw ErrorUtils.statusExp(422, new Error("regenerator", Error.E_FORBIDDEN_VALUE,
						"Specified class does not implement CacheRegenerator interface: " + regeneratorClassName));
			}

			attrs.put("regenerator", regeneratorClassName);
		}

		if ((LRUCache.class == solrCacheClass) || (FastLRUCache.class == solrCacheClass)) {
			if ((createMode) && ((!m.containsKey("size")) || (m.get("size") == null))) {
				throw ErrorUtils.statusExp(422, new Error("size", Error.E_MISSING_VALUE));
			}

			for (String attr : LRU_CACHE_ATTRS) {
				String attrApiKey = gaia.utils.StringUtils.convertCamelCaseToUnderscore(attr);
				Object value = m.remove(attrApiKey);
				if ((value != null) && (!"".equals(value))) {
					String strVal = value.toString();
					if (("autowarmCount".equals(attr)) && (strVal.indexOf("%") == strVal.length() - 1))
						strVal = strVal.substring(0, strVal.length() - 1);
					try {
						Integer intVal = Integer.valueOf(Integer.parseInt(strVal));
						attrs.put(attr, value.toString());
					} catch (NumberFormatException e) {
						String message = "should be an integer";
						if ("autowarmCount".equals(attr))
							message = "should be an integer or percentage (i.e. 80%)";
						throw ErrorUtils.statusExp(422, new Error(attrApiKey, Error.E_FORBIDDEN_VALUE, message));
					}
				}

			}

		}

		if (FastLRUCache.class == solrCacheClass) {
			for (String attr : FAST_LRU_CACHE_ATTRS) {
				String attrApiKey = gaia.utils.StringUtils.convertCamelCaseToUnderscore(attr);
				Object value = m.remove(attrApiKey);
				if ((value != null) && (!"".equals(value))) {
					if ("cleanupThread".equals(attr)) {
						try {
							Boolean boolVal = Boolean.valueOf(Boolean.parseBoolean(value.toString()));
							attrs.put(attr, boolVal.toString());
						} catch (NumberFormatException e) {
							throw ErrorUtils.statusExp(422, new Error(attrApiKey, Error.E_FORBIDDEN_VALUE, "should be a boolean"));
						}
					} else {
						try {
							Integer intVal = Integer.valueOf(Integer.parseInt(value.toString()));
							attrs.put(attr, intVal.toString());
						} catch (NumberFormatException e) {
							throw ErrorUtils.statusExp(422, new Error(attrApiKey, Error.E_FORBIDDEN_VALUE, "should be an integer"));
						}
					}

				}

			}

		}

		if (((LRUCache.class == solrCacheClass) || (FastLRUCache.class == solrCacheClass)) && (m.size() > 0)) {
			String unknownKeys = org.apache.commons.lang.StringUtils.join(m.keySet(), ",");
			throw ErrorUtils.statusExp(422, new Error(unknownKeys, Error.E_FORBIDDEN_KEY, "Unknown or dissallowed key found:"
					+ unknownKeys));
		}

		for (Map.Entry<String, Object> entry : m.entrySet()) {
			attrs.put(entry.getKey(), entry.getValue().toString());
		}

		ecc.createOrUpdateCacheConfig(name, attrs);
		ecc.save();

		APIUtils.reloadCore(collection, cores);
	}

	protected Map<String, Object> getCacheAttrs(String cacheName) {
		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, String> cacheAttrs = currentCaches.get(cacheName);

		cacheAttrs.put("name", gaia.utils.StringUtils.convertCamelCaseToUnderscore(cacheName));

		for (Map.Entry<String, String> entry : cacheAttrs.entrySet()) {
			result.put(gaia.utils.StringUtils.convertCamelCaseToUnderscore((String) entry.getKey()), entry.getValue());
		}

		String className = (String) cacheAttrs.get("class");
		if ((equalsClass(className, LRUCache.class)) || (equalsClass(className, FastLRUCache.class))) {
			for (String prop : LRU_CACHE_ATTRS) {
				String propUnderscored = gaia.utils.StringUtils.convertCamelCaseToUnderscore(prop);
				if (!result.containsKey(propUnderscored)) {
					result.put(propUnderscored, null);
				}
			}
		}
		if (equalsClass(className, FastLRUCache.class)) {
			for (String prop : FAST_LRU_CACHE_ATTRS) {
				String propUnderscored = gaia.utils.StringUtils.convertCamelCaseToUnderscore(prop);
				if (!result.containsKey(propUnderscored)) {
					result.put(propUnderscored, null);
				}
			}
		}
		return result;
	}

	protected boolean equalsClass(String className, Class<?> expectedClass) {
		try {
			Class<?> clazz = solrCore.getResourceLoader().findClass(className, Object.class);
			if (clazz == expectedClass)
				return true;
		} catch (SolrException e) {
		}

		return false;
	}
}
