package gaia.search.server.api.services;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gaia.api.ClickLoggingContext;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.search.server.api.services.parsers.BodyParseException;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class ClickService extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(ClickService.class);
	private static final Logger LOG = LoggerFactory.getLogger(ClickService.class);
	private final String collectionName;
	private ClickLoggingContext clickLogCtx;

	public ClickService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName, ClickLoggingContext ctx) {
		super(serializer, bodyParser);
		this.collectionName = collectionName;
		this.clickLogCtx = ctx;
	}

	@GET
	@Produces("text/plain")
	public Response getClicks(@Context HttpHeaders headers, @Context UriInfo ui) {

		LOG.debug("**** getClick collectionName:" + collectionName);

		Map<String, Object> stats = new HashMap<String, Object>();
		stats.put("buffering", Boolean.valueOf(clickLogCtx.buffering));
		stats.put("buffer_size", Integer.valueOf(clickLogCtx.buffer.size()));
		stats.put("query_count", Integer.valueOf(clickLogCtx.queryCount));
		stats.put("click_count", Integer.valueOf(clickLogCtx.clickCount));
		stats.put("logs_count", Integer.valueOf(clickLogCtx.appenders.size()));
		return buildResponse(Response.Status.OK, stats);
	}

	@PUT
	@Produces("text/plain")
	public Response updateClick(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws JsonParseException, 
				JsonMappingException, IOException {
		Map<String, Object> requestProperties = null;
		try {
			RequestBody requestBody = getRequestBody(body);
			requestProperties = requestBody.getProperties();
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		}
		
		List<Map<String, Object>> events = null;
		String data = body;

		ObjectMapper mapper = new ObjectMapper();
		Object o = null;
		if (data.startsWith("["))
			o = mapper.readValue(data, List.class);
		else if (data.startsWith("{"))
			o = mapper.readValue(data, Map.class);
		else {
			o = data;
		}
		if ((o instanceof List)) {
			events = (List) o;
		} else if ((o instanceof Map)) {
			events = Collections.singletonList((Map<String, Object>) o);
		} else {
			throw ErrorUtils.statusExp(422, i18n.tr("Entity was neither a single field, or a map of fields, to add"));
		}

		
		if ((events == null) || (events.isEmpty())) {
			return null;
		}

		String collection = collectionName;
		if (collection == null) {
			throw ErrorUtils.statusExp(422, new Error("collection", i18n.tr(Error.E_MISSING_VALUE)));
		}

		for (Map<String, Object> ev : events) {
			if (ev.containsKey("buffering")) {
				boolean newBuffering = ((Boolean) ev.get("buffering")).booleanValue();
				if ((clickLogCtx.buffering) && (!newBuffering)) {
					clickLogCtx.flushBuffer();
				}
				clickLogCtx.buffering = newBuffering;
			}
			if (clickLogCtx.buffering) {
				Map<String, Object> bufMap = new HashMap<String, Object>(ev);
				bufMap.put("coll_name", collection);
				clickLogCtx.buffer.add(bufMap);
			} else {
				clickLogCtx.log(collection, ev);
			}
		}
		return buildResponse(Response.Status.NO_CONTENT);
	}

	// protected ResourceInstance createRoleResource(String collectionName) {
	// return createResource(Resource.Type.Click,
	// Collections.singletonMap(Resource.Type.Click, collectionName));
	// }
}
