package gaia.bigdata.api.data;

import gaia.bigdata.api.State;
import gaia.bigdata.api.classification.ClassifierModel;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import java.io.IOException;

import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ClassifierModelSR extends BaseServerResource implements ClassifierModelResource {
	private static transient Logger log = LoggerFactory.getLogger(ClassifierModelSR.class);
	protected DataManagementService dataMgmtService;
	protected String modelName;

	@Inject
	public ClassifierModelSR(Configuration configuration, DataManagementService dms) {
		super(configuration);
		dataMgmtService = dms;
		if (!dms.modelsSupported())
			throw new UnsupportedOperationException("The DataManagementService provided does not support model management");
	}

	protected void doInit() throws ResourceException {
		modelName = ((String) getRequest().getAttributes().get("modelName"));
	}

	public ClassifierModel getModel() {
		try {
			return dataMgmtService.lookupModel(modelName);
		} catch (IOException e) {
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL, e);
		}
		return null;
	}

	public State updateModel(ClassifierModel model) {
		try {
			model.setName(modelName);
			return dataMgmtService.updateModel(model);
		} catch (Exception e) {
			log.error("Exception", e);
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL, e);
		}
		return null;
	}

	public State deleteModel() {
		ClassifierModel model = null;
		try {
			model = dataMgmtService.lookupModel(modelName);
		} catch (IOException e) {
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL, e);
		}
		State state;
		if (model != null) {
			state = dataMgmtService.deleteModel(model);
			state.setStatus(gaia.bigdata.api.Status.DELETED);
		} else {
			state = new State(modelName, modelName);
			state.setStatus(gaia.bigdata.api.Status.FAILED);
		}
		return state;
	}
}
