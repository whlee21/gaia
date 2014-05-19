package gaia.bigdata.classification.mahout;

import gaia.bigdata.api.classification.ClassifierModel;
import java.io.IOException;

public class SGDModelLoader {
	SGDClassifierModel load(ClassifierModel model) throws IOException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public long size(ClassifierModel model) throws IOException, UnsupportedOperationException {
		return (model.getNumCategories() - 1) * model.getNumFeatures() * 10 + model.getNumFeatures() * 4 + 150;
	}
}
