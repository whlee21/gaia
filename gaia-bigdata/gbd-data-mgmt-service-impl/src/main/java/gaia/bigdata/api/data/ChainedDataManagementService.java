package gaia.bigdata.api.data;

import gaia.bigdata.api.State;
import gaia.bigdata.api.Status;
import gaia.bigdata.api.classification.ClassifierModel;
import gaia.commons.api.Configuration;
import gaia.commons.services.ServiceLocator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ChainedDataManagementService extends BaseDataManagementService implements DataManagementService {
	private static transient Logger log = LoggerFactory.getLogger(ChainedDataManagementService.class);
	private Set<DataManagementService> services;
	private boolean supportsModels = false;

	@Inject
	public ChainedDataManagementService(Configuration config, Set<DataManagementService> services, ServiceLocator locator) {
		super(config, locator);
		this.services = services;
		for (DataManagementService service : services)
			if (service.modelsSupported()) {
				supportsModels = true;
				break;
			}
	}

	public State createCollection(String collectionName) {
		State result = new State(collectionName, collectionName);
		Map<String, Object> props = new HashMap<String, Object>();
		result.setProperties(props);

		for (DataManagementService service : services) {
			try {
				State tmp = service.createCollection(collectionName);
				result.addChild(tmp);
			} catch (Exception e) {
				handleException(collectionName, result, service, e);
			}
		}
		setStatus(result, Status.CREATED, services.size());
		return result;
	}

	private void handleException(String collectionName, State result, DataManagementService service, Exception e) {
		State tmp = new State(collectionName, collectionName);
		tmp.setStatus(Status.FAILED);
		if (e != null) {
			tmp.setThrowable(e);
		}
		tmp.addProperty("service-impl", service.getClass().getSimpleName());
		result.addChild(tmp);
	}

	private void setStatus(State result, Status successStatus, int expectedCorrect) {
		int numFailures = 0;
		int numNonExist = 0;
		int alreadyExists = 0;
		for (State state : result.getChildren()) {
			Status status = state.getStatus();
			switch (status) {
			case FAILED:
				numFailures++;
				break;
			case NON_EXISTENT:
				numNonExist++;
				break;
			case ALREADY_EXISTS:
				alreadyExists++;
			}
		}

		if (numFailures == expectedCorrect)
			result.setStatus(Status.FAILED);
		else if ((numFailures > 0) && (numFailures < expectedCorrect))
			result.setStatus(Status.INCOMPLETE);
		else if (numNonExist > 0)
			result.setStatus(Status.NON_EXISTENT);
		else if (alreadyExists > 0)
			result.setStatus(Status.ALREADY_EXISTS);
		else if (numFailures == 0)
			result.setStatus(successStatus);
	}

	public State deleteCollection(String collectionName) {
		State result = new State(collectionName, collectionName);
		for (DataManagementService service : services) {
			try {
				State tmp = service.deleteCollection(collectionName);
				result.addChild(tmp);
			} catch (Exception e) {
				handleException(collectionName, result, service, e);
			}
		}
		setStatus(result, Status.DELETED, services.size());
		return result;
	}

	public State lookupCollection(String collectionName) {
		State result = new State(collectionName, collectionName);
		for (DataManagementService service : services) {
			try {
				State tmp = service.lookupCollection(collectionName);

				if ((tmp != null) && (!tmp.getStatus().equals(Status.NOT_SUPPORTED)))
					result.addChild(tmp);
			} catch (Exception e) {
				handleException(collectionName, result, service, e);
			}
		}
		setStatus(result, Status.EXISTS, services.size());
		return result;
	}

	public List<State> listCollections(Pattern namesToMatch) {
		Map<String, State> states = new HashMap<String, State>();
		boolean hasFail = false;
		Set<State> failedServices = new HashSet<State>();
		for (DataManagementService service : services) {
			List<State> serviceStates = null;
			try {
				serviceStates = service.listCollections(namesToMatch);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if (serviceStates != null) {
				for (State serviceState : serviceStates) {
					if (!serviceState.getStatus().equals(Status.EXISTS)) {
						hasFail = true;
						failedServices.add(serviceState);
						break;
					}
					String coll = serviceState.getCollection();
					State exists = (State) states.get(coll);
					if (exists == null) {
						exists = new State(coll, coll);
						states.put(coll, exists);
						exists.setStatus(Status.EXISTS);
					}
					exists.addChild(serviceState);
				}
			}
		}

		if (hasFail) {
			State failed = createFailedState();
			failed.getChildren().addAll(failedServices);
			failed.getChildren().addAll(states.values());
			return Collections.singletonList(failed);
		}
		return new ArrayList<State>(states.values());
	}

	public boolean modelsSupported() {
		return supportsModels;
	}

	public List<ClassifierModel> listModels() throws IOException {
		return listModels(null);
	}

	public List<ClassifierModel> listModels(Pattern namesToMatch) throws IOException {
		Map<String, ClassifierModel> models = new HashMap<String, ClassifierModel>();

		for (DataManagementService service : services) {
			if (service.modelsSupported()) {
				List<ClassifierModel> serviceModels = null;
				try {
					serviceModels = service.listModels(namesToMatch);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				if (serviceModels != null) {
					for (ClassifierModel classifierModel : serviceModels) {
						if (classifierModel != null) {
							models.put(classifierModel.getName(), classifierModel);
						}
					}
				}
			}
		}
		return new ArrayList<ClassifierModel>(models.values());
	}

	public ClassifierModel lookupModel(String modelName) throws IOException {
		ClassifierModel result = null;
		for (DataManagementService service : services) {
			if (service.modelsSupported()) {
				result = service.lookupModel(modelName);
				if (result != null) {
					break;
				}
			}
		}
		return result;
	}

	public State createModel(ClassifierModel model) {
		State result = new State(model.getName(), model.getName());
		Map<String, Object> props = new HashMap<String, Object>();
		result.setProperties(props);
		int numSupporting = 0;
		for (DataManagementService service : services) {
			if (service.modelsSupported()) {
				numSupporting++;
				try {
					State tmp = service.createModel(model);
					if (tmp != null)
						result.addChild(tmp);
				} catch (Exception e) {
					handleException(model.getName(), result, service, e);
				}
			}
		}
		setStatus(result, Status.CREATED, numSupporting);
		return result;
	}

	public State updateModel(ClassifierModel model) {
		State result = new State(model.getName(), model.getName());
		int numSupporting = 0;
		for (DataManagementService service : services) {
			if (service.modelsSupported()) {
				numSupporting++;
				try {
					State tmp = service.updateModel(model);
					if (tmp != null)
						result.addChild(tmp);
				} catch (Exception e) {
					handleException(model.getName(), result, service, e);
				}
			}
		}
		setStatus(result, Status.SUCCEEDED, numSupporting);
		return result;
	}

	public State deleteModel(ClassifierModel model) {
		State result = new State(model.getName(), model.getName());
		int numSupporting = 0;
		for (DataManagementService service : services) {
			if (service.modelsSupported()) {
				numSupporting++;
				try {
					State tmp = service.deleteModel(model);
					if (tmp != null)
						result.addChild(tmp);
				} catch (Exception e) {
					handleException(model.getName(), result, service, e);
				}
			}
		}
		setStatus(result, Status.DELETED, numSupporting);
		return result;
	}
}
