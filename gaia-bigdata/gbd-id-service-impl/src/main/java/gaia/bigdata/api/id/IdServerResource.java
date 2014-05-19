package gaia.bigdata.api.id;

import com.google.inject.Inject;
import org.restlet.resource.ServerResource;

public class IdServerResource extends ServerResource implements IdResource {
	private final IdGeneratorService service;

	@Inject
	public IdServerResource(IdGeneratorService service) {
		this.service = service;
	}

	public String generateId() {
		return service.generateIdAsString();
	}
}
