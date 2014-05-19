package gaia.bigdata.api.data;

import gaia.bigdata.api.State;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.restlet.data.Form;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class SDACollectionsServerResource extends BaseServerResource implements SDACollectionsResource {
	private static transient Logger log = LoggerFactory.getLogger(SDACollectionsServerResource.class);
	private DataManagementService dataMgmtService;

	@Inject
	public SDACollectionsServerResource(Configuration configuration, DataManagementService dms) {
		super(configuration);
		dataMgmtService = dms;
	}

	@Get
	public List<State> listCollections() {
		try {
			Form form = getRequest().getResourceRef().getQueryAsForm();
			String regexStr = form.getValues("regex") != null ? form.getValues("regex") : null;
			if ((regexStr != null) && (!regexStr.isEmpty())) {
				Pattern pattern = Pattern.compile(regexStr);
				return dataMgmtService.listCollections(pattern);
			}
			return dataMgmtService.listCollections();
		} catch (Exception e) {
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL, e);
			State tmp = new State();
			tmp.setStatus(gaia.bigdata.api.Status.FAILED);
			tmp.setThrowable(e);
			return Collections.singletonList(tmp);
		}
	}

	@Post
	public State add(Map<String, Object> body) {
		State result = null;
		try {
			Object collection = body.get("collection");

			if (collection != null) {
				result = dataMgmtService.createCollection(collection.toString());
				if ((!result.getStatus().equals(gaia.bigdata.api.Status.CREATED))
						&& (!result.getStatus().equals(gaia.bigdata.api.Status.ALREADY_EXISTS)))
					handleBadStatus(result);
			} else {
				setStatus(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, "No collection specified in the request");
			}
		} catch (Exception e) {
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL, e);
		}
		return result;
	}

	private void handleBadStatus(State result) {
		gaia.bigdata.api.Status status = result.getStatus();
		if (status != null) {
			switch (status) {
			case ALREADY_EXISTS:
			case CREATED:
			case DELETED:
				setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL, status.name());
			}
		}
	}
}
