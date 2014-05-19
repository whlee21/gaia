package gaia.api;

import java.io.IOException;
import java.util.Map;
import org.restlet.representation.InputRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface ClickEventResource {
	@Get("json")
	public Map<String, Object> getStats();

	@Put("json")
	public void recordEvent(InputRepresentation paramInputRepresentation) throws IOException;
}
