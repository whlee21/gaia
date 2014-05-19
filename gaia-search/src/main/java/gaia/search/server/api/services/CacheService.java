package gaia.search.server.api.services;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSolrConfig;
import gaia.api.APIUtils;
import gaia.api.CachesResourceBase;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.search.server.api.services.parsers.BodyParseException;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.search.server.configuration.Configuration;
import gaia.utils.StringUtils;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.common.SolrException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.CacheRegenerator;
import org.apache.solr.search.FastLRUCache;
import org.apache.solr.search.LRUCache;
import org.apache.solr.search.SolrCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xnap.commons.i18n.I18nFactory;

public class CacheService extends BaseService {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(CacheService.class);
	private static final Logger LOG = LoggerFactory.getLogger(CacheService.class);
	
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

	private volatile boolean existing = true;
	private CoreContainer cores;
	private CollectionManager cm;
	private final String collection;
	private SolrCore solrCore;
	private Map<String, Map<String, String>> currentCaches;
	EditableSolrConfig ecc;
	private Configuration configuration;

	public CacheService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName,  CollectionManager cm
			, CoreContainer cores, Configuration configuration) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.cm = cm;
		this.cores = cores;
		this.configuration = configuration;
		
		solrCore = cores.getCore(
				collection);
		setExisting(solrCore != null);

		ecc = new EditableSolrConfig(solrCore, cm.getUpdateChain(), cores.getZkController());
		currentCaches = getCaches();
	}

	public void setExisting(boolean exists) {
		this.existing = exists;
	}
	
	public boolean isExisting() {
		return this.existing;
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
	

	@GET
	@Produces("text/plain")
	public Response getCaches(@Context HttpHeaders headers, @Context UriInfo ui) {
		LOG.debug("hhokyung CacheService getCache query param(collection): " + collection);
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		Map<String, Map<String, Object>> results = getAllCacheAttrs();
		return buildResponse(Response.Status.OK, results);
	}
	
	@GET
	@Path("{cacheName}")
	@Produces("text/plain")
	public Response getCacheObj(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("cacheName") String cacheName) {
		LOG.debug("hhokyung CacheService getCache query param(collection): " + collection);
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		Map<String, Object> results = getCacheAttrs(cacheName);
		return buildResponse(Response.Status.OK, results);
	}
	

//	@GET
//	@Path("{cacheName}")
//	@Produces("text/plain")
//	public Response getCache(@Context HttpHeaders headers, @Context UriInfo ui, @PathParam("cacheName") String cacheName) {
//		LOG.debug("hhokyung CacheService getCache query param(collection): " + collection);
//		if (!isExisting()) {
//			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
//		}
//
//		return getCacheAttrs(cacheName);
//	}

	@POST
	@Produces("text/plain")
	public Response createCache(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws IOException, 
			ParserConfigurationException, SAXException, URISyntaxException {
		LOG.debug("hhokyung CacheService createCache query param(collection): " + collection);
		try {
			RequestBody requestBody = getRequestBody(body);
			return addCache(requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		}
	}
	
	public Response addCache(Map<String, Object> m) throws IOException, ParserConfigurationException, SAXException, 
			URISyntaxException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}

		Object obj = m.remove("name");
		if ((obj == null) || ((!(obj instanceof String)) && ("".equals(obj)))) {
			throw ErrorUtils.statusExp(422, new Error("name", i18n.tr(Error.E_MISSING_VALUE)));
		}
		String name = StringUtils.convertUnderscoreToCamelCase((String) obj);
		
		createOrUpdateCache(name, m, null, true);
		Map<String, Object> data = new HashMap<String, Object>();
		
		URI seeOther = configuration.getCollectionUri(collection + "/caches/" + URLEncoder.encode(name, "UTF-8"));
		Response response = buildResponse(Response.Status.CREATED, seeOther, data);
		
		return response;
	}

	@PUT
	@Path("{cacheName}")
	@Produces("text/plain")
	public Response updateCache(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("cacheName") String cacheName) throws ParserConfigurationException, IOException, SAXException {
		LOG.debug("hhokyung CacheService updateCache query param(collection): " + collection);
		try {
			RequestBody requestBody = getRequestBody(body);
			return updateCache(cacheName, requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		}
	}


	public Response updateCache(String cacheName, Map<String, Object> m) throws ParserConfigurationException, IOException, SAXException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}

		Map<String, Object> map = getCacheAttrs(cacheName);
		String existingClass = (String) map.get("class");

		if ((EditableSolrConfig.FIELD_VALUE_CACHE.equals(cacheName)) && (ecc.getCacheConfig(cacheName) == null)) {
			map.remove("name");

			String className = (String) m.get("class");
			if ((className != null) && (!equalsClass(className, FastLRUCache.class))) {
				for (String attr : CachesResourceBase.FAST_LRU_CACHE_ATTRS) {
					map.remove(StringUtils.convertCamelCaseToUnderscore(attr));
				}
			}

			map.putAll(m);
			m = map;
		}

		createOrUpdateCache(cacheName, m, existingClass, false);
		return buildResponse(Response.Status.NO_CONTENT);
	}
	
	@DELETE
	@Path("{cacheName}")
	@Produces("text/plain")
	public Response deleteCache(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("cacheName") String cacheName) throws IOException, ParserConfigurationException, SAXException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		ecc.removeCacheConfig(cacheName);
		ecc.save();

		APIUtils.reloadCore(collection, cores);
		return buildResponse(Response.Status.NO_CONTENT);
	}

	public void createOrUpdateCache(String name, Map<String, Object> m, String existingClassName, boolean createMode)
			throws ParserConfigurationException, IOException, SAXException {
		Map<String, String> attrs = new HashMap<String, String>();

		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}

		if (createMode) {
			if (currentCaches.containsKey(name)) {
				throw ErrorUtils.statusExp(422, new Error("name", Error.E_FORBIDDEN_VALUE,
						i18n.tr("An explicit cache already exists with the name: " + name)));
			}

		}

		Object obj = m.remove("class");

		Class<?> solrCacheClass = null;
		if ((createMode) || (obj != null)) {
			if ((obj == null) || (!(obj instanceof String)) || ("".equals(obj))) {
				throw ErrorUtils.statusExp(422, new Error("class", i18n.tr(Error.E_MISSING_VALUE)));
			}
			String solrCacheClassName = (String) obj;
			try {
				solrCacheClass = solrCore.getResourceLoader().findClass(solrCacheClassName, Object.class);
			} catch (SolrException e) {
				throw ErrorUtils.statusExp(422, new Error("class", Error.E_FORBIDDEN_VALUE, i18n.tr("Could not find specified class: "
						+ solrCacheClassName)));
			}

			if (!SolrCache.class.isAssignableFrom(solrCacheClass)) {
				throw ErrorUtils.statusExp(422, new Error("class", Error.E_FORBIDDEN_VALUE,
						i18n.tr("Specified class does not implement SolrCache interface: " + solrCacheClassName)));
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
				throw ErrorUtils.statusExp(422, new Error("regenerator", i18n.tr(Error.E_MISSING_VALUE)));
			}
			String regeneratorClassName = (String) obj;
			Class<?> clazz;
			try {
				clazz = solrCore.getResourceLoader().findClass(regeneratorClassName, Object.class);
			} catch (SolrException e) {
				throw ErrorUtils.statusExp(422, new Error("regenerator", Error.E_FORBIDDEN_VALUE,
						i18n.tr("Could not find specified class: " + regeneratorClassName)));
			}

			if (!CacheRegenerator.class.isAssignableFrom(clazz)) {
				throw ErrorUtils.statusExp(422, new Error("regenerator", Error.E_FORBIDDEN_VALUE,
						i18n.tr("Specified class does not implement CacheRegenerator interface: " + regeneratorClassName)));
			}

			attrs.put("regenerator", regeneratorClassName);
		}

		if ((LRUCache.class == solrCacheClass) || (FastLRUCache.class == solrCacheClass)) {
			if ((createMode) && ((!m.containsKey("size")) || (m.get("size") == null))) {
				throw ErrorUtils.statusExp(422, new Error("size", i18n.tr(Error.E_MISSING_VALUE)));
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
						throw ErrorUtils.statusExp(422, new Error(attrApiKey, Error.E_FORBIDDEN_VALUE, i18n.tr(message)));
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
							throw ErrorUtils.statusExp(422, new Error(attrApiKey, Error.E_FORBIDDEN_VALUE, i18n.tr("should be a boolean")));
						}
					} else {
						try {
							Integer intVal = Integer.valueOf(Integer.parseInt(value.toString()));
							attrs.put(attr, intVal.toString());
						} catch (NumberFormatException e) {
							throw ErrorUtils.statusExp(422, new Error(attrApiKey, Error.E_FORBIDDEN_VALUE, i18n.tr("should be an integer")));
						}
					}

				}

			}

		}

		if (((LRUCache.class == solrCacheClass) || (FastLRUCache.class == solrCacheClass)) && (m.size() > 0)) {
			String unknownKeys = org.apache.commons.lang.StringUtils.join(m.keySet(), ",");
			throw ErrorUtils.statusExp(422, new Error(unknownKeys, Error.E_FORBIDDEN_KEY, i18n.tr("Unknown or dissallowed key found:"
					+ unknownKeys)));
		}

		for (Map.Entry<String, Object> entry : m.entrySet()) {
			attrs.put(entry.getKey(), entry.getValue().toString());
		}

		ecc.createOrUpdateCacheConfig(name, attrs);
		ecc.save();

		APIUtils.reloadCore(collection, cores);
	}

	protected Map<String, Map<String, Object>> getAllCacheAttrs() {
		Map<String, Map<String, Object>> results = new HashMap<String, Map<String, Object>>();
		
		Iterator<Entry<String, Map<String, String>>> iter = currentCaches.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<String, Map<String, String>> entry = iter.next();
			String cacheName = entry.getKey();

			Map<String, Object> result = getCacheAttrs(cacheName);

			results.put(cacheName, result);
		}
		
		return results;
	}
	
	
	
	protected Map<String, Object> getCacheAttrs(String cacheName) {
		Map<String, Object> result = new HashMap<String, Object>();
		LOG.debug("getCacheAttrs11 : "+cacheName);
		Map<String, String> cacheAttrs = currentCaches.get(cacheName);
		
		LOG.debug("getCacheAttrs12 cacheAttrs : "+cacheAttrs);

		LOG.debug("getCacheAttrs13 convertCamelCaseToUnderscore : "+gaia.utils.StringUtils.convertCamelCaseToUnderscore(cacheName));
		cacheAttrs.put("name", gaia.utils.StringUtils.convertCamelCaseToUnderscore(cacheName));

		LOG.debug("getCacheAttrs14 : "+cacheName);
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

	// ResourceInstance createCacheResource(String collection, String
	// cacheName) {
	// Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
	// mapIds.put(Resource.Type.Collection, collection);
	// mapIds.put(Resource.Type.Cache, cacheName);
	//
	// LOG.debug("hhokyung CacheService createCacheResource[Cache]: collection["+collection+"] cacheName["+cacheName+"]");
	//
	// return createResource(Resource.Type.Cache, mapIds);
	// }
}
