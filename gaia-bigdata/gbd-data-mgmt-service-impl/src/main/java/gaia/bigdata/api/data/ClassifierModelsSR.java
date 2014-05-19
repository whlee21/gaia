package gaia.bigdata.api.data;

import gaia.bigdata.api.State;
import gaia.bigdata.api.classification.ClassifierModel;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import org.restlet.data.Form;
import org.restlet.data.Status;

import com.google.inject.Inject;

public class ClassifierModelsSR extends BaseServerResource implements ClassifierModelsResource {
	protected DataManagementService dataMgmtService;

	@Inject
	public ClassifierModelsSR(Configuration configuration, DataManagementService dms) {
		super(configuration);
		dataMgmtService = dms;
		if (!dms.modelsSupported())
			throw new UnsupportedOperationException("The DataManagementService provided does not support model management");
	}

	public Collection<ClassifierModel> listModels() {
		Form form = getRequest().getResourceRef().getQueryAsForm();
		String regexStr = form.getValues("regex") != null ? form.getValues("regex") : null;
		if ((regexStr != null) && (!regexStr.isEmpty())) {
			Pattern pattern = Pattern.compile(regexStr);
			try {
				return dataMgmtService.listModels(pattern);
			} catch (IOException e) {
				setStatus(Status.SERVER_ERROR_INTERNAL, e);
			}
		}
		try {
			return dataMgmtService.listModels();
		} catch (IOException e) {
			setStatus(Status.SERVER_ERROR_INTERNAL, e);
		}
		return Collections.emptyList();
	}

	public State add(ClassifierModel toAdd) {
		return dataMgmtService.createModel(toAdd);
	}
}
