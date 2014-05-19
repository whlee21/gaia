package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSolrConfig;
import gaia.utils.StringUtils;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.search.FastLRUCache;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class CacheServerResource extends CachesResourceBase implements CacheResource {
	private String cacheName;

	@Inject
	public CacheServerResource(CoreContainer cores, CollectionManager cm) {
		super(cores, cm);
	}

	public void doInit() {
		super.doInit();

		String apiCacheName = (String) getRequestAttributes().get("name");
		cacheName = StringUtils.convertUnderscoreToCamelCase(apiCacheName);

		setExisting((solrCore != null) && (currentCaches.containsKey(cacheName)));
	}

	public void doRelease() {
		super.doRelease();
		cacheName = null;
	}

	@Get("json")
	public Map<String, Object> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		return getCacheAttrs(cacheName);
	}

	@Put("json")
	public void update(Map<String, Object> m) throws ParserConfigurationException, IOException, SAXException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
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
	}

	@Delete
	public void remove() throws ParserConfigurationException, IOException, SAXException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		ecc.removeCacheConfig(cacheName);
		ecc.save();

		APIUtils.reloadCore(collection, cores);
	}
}
