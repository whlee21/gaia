package gaia.bigdata.api.data;

import gaia.bigdata.api.State;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class SDACollectionServerResource extends BaseServerResource implements SDACollectionResource {
	private static transient Logger log = LoggerFactory.getLogger(SDACollectionServerResource.class);
	protected DataManagementService dataMgmtService;
	protected String id;

	@Inject
	public SDACollectionServerResource(Configuration configuration, DataManagementService dms) {
		super(configuration);
		dataMgmtService = dms;
	}

	protected void doInit() throws ResourceException {
		id = ((String) getRequest().getAttributes().get("id"));
	}

	public State retrieve() {
		State result = null;
		try {
			result = dataMgmtService.lookupCollection(id);
			handleStatus(result);
		} catch (Exception e) {
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL, e);
		}
		return result;
	}

	private void handleStatus(State result) {
		if (result != null) {
			gaia.bigdata.api.Status status = result.getStatus();
			switch (status) {
			case ALREADY_EXISTS:
			case CREATED:
				setStatus(org.restlet.data.Status.SUCCESS_OK);
			case DELETED:
			case EXISTS:
				break;
			case FAILED:
			case INCOMPLETE:
			case KILLED:
				setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL, status.name());
				break;
			}
		} else {
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL, "no result returned");
		}
	}

	public State remove() {
		State result = null;
		try {
			result = dataMgmtService.deleteCollection(id);
			handleStatus(result);
		} catch (Exception e) {
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL, e);
		}
		return result;
	}
}
