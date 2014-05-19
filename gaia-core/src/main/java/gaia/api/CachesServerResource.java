package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.utils.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.core.CoreContainer;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class CachesServerResource extends CachesResourceBase implements CachesResource {
	@Inject
	public CachesServerResource(CoreContainer cores, CollectionManager cm) {
		super(cores, cm);
	}

	@Post("json")
	public Map<String, Object> add(Map<String, Object> m) throws IOException, ParserConfigurationException, SAXException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		Object obj = m.remove("name");
		if ((obj == null) || ((!(obj instanceof String)) && ("".equals(obj)))) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_MISSING_VALUE));
		}
		String name = StringUtils.convertUnderscoreToCamelCase((String) obj);

		createOrUpdateCache(name, m, null, true);

		getResponse().setLocationRef("caches/" + URLEncoder.encode(name, "UTF-8"));
		return new HashMap<String, Object>();
	}

	@Get("json")
	public List<Map<String, Object>> retrieve() {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (String cache : this.currentCaches.keySet()) {
			Map<String, Object> cacheAttrs = getCacheAttrs(cache);
			result.add(cacheAttrs);
		}
		return result;
	}
}
