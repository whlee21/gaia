package gaia.bigdata.classification;

import gaia.bigdata.api.classification.ClassifierModel;
import java.io.IOException;
import java.util.Collection;
import org.apache.mahout.math.Vector;

public interface ClassifierService {
	public void train(ClassifierModel paramClassifierModel, int paramInt, Vector paramVector)
			throws UnsupportedOperationException;

	public Vector classify(ClassifierModel paramClassifierModel, Vector paramVector) throws IOException,
			UnsupportedOperationException;

	public void loadModel(ClassifierModel paramClassifierModel) throws IOException, UnsupportedOperationException;

	public void unloadModel(ClassifierModel paramClassifierModel) throws IOException, UnsupportedOperationException;

	public int hasRoom(ClassifierModel paramClassifierModel) throws IOException;

	public boolean isSupported(ClassifierModel paramClassifierModel);

	public Collection<? extends ClassifierModel> loadedModels();

	public ClassifierModel getModel(String paramString);
}
