package gaia.api;

import gaia.ssl.SSLConfigManager;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class SSLConfigServerResource extends ServerResource implements SSLConfigResource {
	private static final Logger LOG = LoggerFactory.getLogger(SSLConfigServerResource.class);
	private final SSLConfigManager manager;
	private List<String> hideKeys = Arrays.asList(new String[0]);

	@Inject
	public SSLConfigServerResource(SSLConfigManager manager) {
		this.manager = manager;
	}

	@Get("json")
	public Map<String, Object> retrieve() {
		setStatus(Status.SUCCESS_OK);
		Map<String, Object> config = manager.getConfig();
		for (String hideKey : hideKeys) {
			config.remove(hideKey);
		}

		return config;
	}

	@Put("json")
	public void set(Map<String, Object> config) {
		// List<Error> errors = manager.setConfig(config);
		//
		// if (errors.size() > 0) {
		// throw ErrorUtils.statusExp(ResultStatus.STATUS.UNPROCESSABLE_ENTITY,
		// errors);
		// }
		//
		// setStatus(Status.SUCCESS_NO_CONTENT);
	}
}
