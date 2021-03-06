package gaia.api;

import java.io.IOException;
import java.util.Map;
import org.quartz.SchedulerException;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface ActivityResource {
	@Delete
	public void remove() throws SchedulerException;

	@Put("json")
	public void update(Map<String, Object> paramMap) throws IOException, SchedulerException;

	@Get("json")
	public Map<String, Object> retrieve();
}
