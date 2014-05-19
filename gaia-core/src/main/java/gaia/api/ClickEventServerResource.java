package gaia.api;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.codehaus.jackson.map.ObjectMapper;
import org.restlet.representation.InputRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ClickEventServerResource extends ServerResource implements ClickEventResource {
	private static final Logger LOG = LoggerFactory.getLogger(ClickEventServerResource.class);
	public static final String F_BUFFERING = "buffering";
	public static final String F_TYPE = "type";
	public static final String F_REQID = "req";
	public static final String F_QUERY = "q";
	public static final String F_QTIME = "qt";
	public static final String F_CTIME = "ct";
	public static final String F_POS = "pos";
	public static final String F_HITS = "hits";
	public static final String F_DOCID = "doc";
	public static final String F_USER = "u";
	static Layout layout = new PatternLayout("%m%n");

	@Inject
	private ClickLoggingContext ctx;

	@Get("json")
	public Map<String, Object> getStats() {
		Map<String, Object> stats = new HashMap<String, Object>();
		stats.put("buffering", Boolean.valueOf(ctx.buffering));
		stats.put("buffer_size", Integer.valueOf(ctx.buffer.size()));
		stats.put("query_count", Integer.valueOf(ctx.queryCount));
		stats.put("click_count", Integer.valueOf(ctx.clickCount));
		stats.put("logs_count", Integer.valueOf(ctx.appenders.size()));
		return stats;
	}

	@Put("json")
	public void recordEvent(InputRepresentation object) throws IOException {
		List<Map<String, Object>> events = null;
		String data = IOUtils.toString(object.getReader());

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
			throw ErrorUtils.statusExp(422, "Entity was neither a single field, or a map of fields, to add");
		}

		if ((events == null) || (events.isEmpty())) {
			return;
		}

		String collection = (String) getRequestAttributes().get("coll_name");
		if (collection == null) {
			throw ErrorUtils.statusExp(422, new Error("collection", Error.E_MISSING_VALUE));
		}

		for (Map<String, Object> ev : events) {
			if (ev.containsKey("buffering")) {
				boolean newBuffering = ((Boolean) ev.get("buffering")).booleanValue();
				if ((ctx.buffering) && (!newBuffering)) {
					ctx.flushBuffer();
				}
				ctx.buffering = newBuffering;
			}
			if (ctx.buffering) {
				Map<String, Object> bufMap = new HashMap<String, Object>(ev);
				bufMap.put("coll_name", collection);
				ctx.buffer.add(bufMap);
			} else {
				ctx.log(collection, ev);
			}
		}
	}
}
