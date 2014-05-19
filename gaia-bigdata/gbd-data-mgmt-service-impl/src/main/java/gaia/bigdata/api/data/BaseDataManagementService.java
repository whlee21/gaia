package gaia.bigdata.api.data;

import gaia.commons.api.Configuration;
import gaia.commons.services.BaseService;
import gaia.commons.services.ServiceLocator;
import gaia.bigdata.Constants;
import gaia.bigdata.api.State;
import gaia.bigdata.api.Status;
import gaia.bigdata.api.classification.ClassifierModel;
import gaia.bigdata.services.ServiceType;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public abstract class BaseDataManagementService extends BaseService implements DataManagementService {
	public BaseDataManagementService(Configuration config, ServiceLocator locator) {
		super(config, locator);
	}

	public String getType() {
		return ServiceType.DATA_MANAGEMENT.name();
	}

	public List<State> listCollections() {
		return listCollections(Constants.MATCH_ALL);
	}

	public boolean modelsSupported() {
		return false;
	}

	public State updateModel(ClassifierModel model) {
		State state = new State(model.getName(), model.getName());
		state.setStatus(Status.NOT_SUPPORTED);
		return state;
	}

	public List<ClassifierModel> listModels() throws IOException {
		return listModels(null);
	}

	public List<ClassifierModel> listModels(Pattern namesToMatch) throws IOException {
		return Collections.emptyList();
	}

	public ClassifierModel lookupModel(String modelName) throws IOException {
		return null;
	}

	public State createModel(ClassifierModel model) {
		return null;
	}

	public State deleteModel(ClassifierModel model) {
		return null;
	}

	protected List<State> createFailedList(Throwable e) {
		State state = createFailedState(e);
		return Collections.singletonList(state);
	}

	protected State createFailedState() {
		return createFailedState((Throwable) null);
	}

	protected State createFailedState(Throwable e) {
		State state = new State();
		state.setStatus(Status.FAILED);
		state.addProperty("service-impl", getClass().getSimpleName());
		if (e != null) {
			state.setThrowable(e);
		}
		return state;
	}
}
