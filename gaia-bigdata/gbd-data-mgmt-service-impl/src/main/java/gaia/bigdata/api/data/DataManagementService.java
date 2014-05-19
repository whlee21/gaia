package gaia.bigdata.api.data;

import gaia.bigdata.api.State;
import gaia.bigdata.api.classification.ClassifierModel;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public interface DataManagementService {
	public State createCollection(String collection);

	public State deleteCollection(String collection);

	public State lookupCollection(String collection);

	public List<State> listCollections();

	public List<State> listCollections(Pattern pattern);

	public boolean modelsSupported();

	public List<ClassifierModel> listModels() throws IOException;

	public List<ClassifierModel> listModels(Pattern pattern) throws IOException;

	public ClassifierModel lookupModel(String model) throws IOException;

	public State createModel(ClassifierModel model);

	public State deleteModel(ClassifierModel model);

	public State updateModel(ClassifierModel model);
}