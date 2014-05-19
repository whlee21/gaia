package gaia.bigdata.api.data.hbase;

import gaia.bigdata.api.State;
import gaia.bigdata.api.Status;
import gaia.bigdata.api.classification.ClassifierModel;
import gaia.bigdata.api.data.BaseDataManagementService;
import gaia.bigdata.api.data.DataManagementService;
import gaia.bigdata.hbase.documents.DocumentTable;
import gaia.bigdata.hbase.metrics.MetricTable;
import gaia.bigdata.hbase.models.ClassifierModelTable;
import gaia.commons.api.Configuration;
import gaia.commons.services.ServiceLocator;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class HBaseDataManagementService extends BaseDataManagementService implements DataManagementService {
	private static transient Logger log = LoggerFactory.getLogger(HBaseDataManagementService.class);

	@Inject
	protected HBaseDataManagementService(Configuration config, ServiceLocator locator) {
		super(config, locator);
	}

	public State createCollection(String collectionName) {
		State result = new State(collectionName, collectionName);
		result.setStatus(Status.CREATED);
		result.addProperty("service-impl", getClass().getSimpleName());
		return result;
	}

	public State deleteCollection(String collectionName) {
		State result = new State(collectionName, collectionName);
		String zkConnect = config.getProperties().getProperty("hbase.zk.connect");
		if (zkConnect == null) {
			result.setStatus(Status.FAILED);
			result.setErrorMsg("Missing required config value: hbase.zk.connect");
			return result;
		}

		MetricTable table = new MetricTable(zkConnect);
		try {
			table.deleteMetrics(collectionName);
			result.setStatus(Status.DELETED);
			table.close();
		} catch (IOException e) {
			result.setStatus(Status.FAILED);
			result.setErrorMsg("Unable to delete HBase metrics for collection: " + collectionName);
		}

		DocumentTable docTable = new DocumentTable(zkConnect);
		try {
			docTable.deleteAllDocuments(collectionName);
			result.setStatus(Status.DELETED);
			docTable.close();
		} catch (IOException e) {
			result.setStatus(Status.FAILED);
			result.setErrorMsg("Unable to delete HBase documents for collection: " + collectionName);
		}

		result.addProperty("service-impl", getClass().getSimpleName());
		return result;
	}

	public State lookupCollection(String collectionName) {
		State result = new State(collectionName, collectionName);
		result.setStatus(Status.NOT_SUPPORTED);
		result.addProperty("service-impl", getClass().getSimpleName());
		return result;
	}

	public List<State> listCollections(Pattern namesToMatch) {
		return Collections.emptyList();
	}

	public boolean modelsSupported() {
		return true;
	}

	public State updateModel(ClassifierModel model) {
		if (model != null) {
			State result = new State(model.getName(), model.getName());
			String zkConnect = config.getProperties().getProperty("hbase.zk.connect");
			if (zkConnect == null) {
				result.setStatus(Status.FAILED);
				result.setErrorMsg("Missing required config value: hbase.zk.connect");
				return result;
			}
			ClassifierModelTable table = new ClassifierModelTable(zkConnect);
			ClassifierModel toUpdate = null;
			try {
				toUpdate = table.getModel(model.getName());
				table.close();
			} catch (IOException e) {
				log.error("Model {} cannot be updated since it does not exist", model.getName(), e);
				result.setStatus(Status.FAILED);
				result.setErrorMsg("Unable to update the HBase model because it doesn't exist: " + model.getName());
				return result;
			}
			if (toUpdate != null) {
				try {
					update(model, toUpdate);
					table.putModel(toUpdate);
					result.setStatus(Status.SUCCEEDED);
				} catch (IOException e) {
					result.setStatus(Status.FAILED);
					result.setErrorMsg("Unable to update the HBase model: " + model.getName());
				}
			}
			return result;
		}
		State result = new State();
		result.setStatus(Status.FAILED);
		result.setErrorMsg("Unable to update the HBase model: " + model.getName());
		return result;
	}

	private void update(ClassifierModel src, ClassifierModel toUpdate) {
		if (src.getDesiredReplication() != toUpdate.getDesiredReplication()) {
			toUpdate.setDesiredReplication(src.getDesiredReplication());
		}
		if ((src.getLocation() != null) && (!src.getLocation().equals(toUpdate.getLocation()))) {
			toUpdate.setLocation(src.getLocation());
		}
		if ((src.getMetadata() != null) && (!src.getMetadata().equals(toUpdate.getMetadata()))) {
			if (toUpdate.getMetadata() == null)
				toUpdate.setMetadata(src.getMetadata());
			else
				toUpdate.getMetadata().putAll(src.getMetadata());
		} else if ((src.getMetadata() == null) && (toUpdate.getMetadata() != null)) {
			toUpdate.getMetadata().clear();
		}
		if (src.getNumCategories() != toUpdate.getNumCategories()) {
			toUpdate.setNumCategories(src.getNumCategories());
		}
		if (src.getNumFeatures() != toUpdate.getNumFeatures()) {
			toUpdate.setNumFeatures(src.getNumFeatures());
		}
		if ((src.getProvider() != null) && (!src.getProvider().equals(toUpdate.getProvider()))) {
			toUpdate.setProvider(src.getProvider());
		}
		if ((src.getType() != null) && (!src.getType().equals(toUpdate.getType())))
			toUpdate.setType(src.getType());
	}

	public List<ClassifierModel> listModels() throws IOException {
		return listModels(null);
	}

	public List<ClassifierModel> listModels(Pattern namesToMatch) throws IOException {
		List<ClassifierModel> result = null;
		String zkConnect = config.getProperties().getProperty("hbase.zk.connect");
		if (zkConnect == null) {
			throw new IOException("Unable to connect to ZooKeeper");
		}
		ClassifierModelTable table = new ClassifierModelTable(zkConnect);

		if (namesToMatch != null)
			result = Lists.newArrayList(table.grepModelByName(namesToMatch.pattern()));
		else {
			result = Lists.newArrayList(table.listModels());
		}
		table.close();
		return result;
	}

	public ClassifierModel lookupModel(String modelName) throws IOException {
		String zkConnect = config.getProperties().getProperty("hbase.zk.connect");
		if (zkConnect == null) {
			throw new IOException("Unable to connect to ZooKeeper");
		}
		ClassifierModelTable table = new ClassifierModelTable(zkConnect);
		ClassifierModel model = table.getModel(modelName);
		table.close();
		return model;
	}

	public State createModel(ClassifierModel model) {
		if (model != null) {
			State result = new State(model.getName(), model.getName());
			String zkConnect = config.getProperties().getProperty("hbase.zk.connect");
			if (zkConnect == null) {
				result.setStatus(Status.FAILED);
				result.setErrorMsg("Missing required config value: hbase.zk.connect");
				return result;
			}
			ClassifierModelTable table = new ClassifierModelTable(zkConnect);
			try {
				table.putModel(model);
				result.setStatus(Status.SUCCEEDED);
				table.close(); // by whlee21
			} catch (IOException e) {
				result.setStatus(Status.FAILED);
				result.setErrorMsg("Unable to update the HBase model: " + model.getName());
			}
			return result;
		}
		State result = new State();
		result.setStatus(Status.FAILED);
		result.setErrorMsg("Unable to update the HBase model: " + model.getName());
		return result;
	}

	public State deleteModel(ClassifierModel model) {
		State result = new State(model.getName(), model.getName());
		String zkConnect = config.getProperties().getProperty("hbase.zk.connect");
		if (zkConnect == null) {
			result.setStatus(Status.FAILED);
			result.setErrorMsg("Missing required config value: hbase.zk.connect");
			return result;
		}
		ClassifierModelTable table = new ClassifierModelTable(zkConnect);
		try {
			table.deleteModel(model.getName());
			table.close(); // by whlee21
		} catch (IOException e) {
			result.setStatus(Status.FAILED);
			result.setErrorMsg("Unable to update the HBase model: " + model.getName());
		}
		return result;
	}
}
