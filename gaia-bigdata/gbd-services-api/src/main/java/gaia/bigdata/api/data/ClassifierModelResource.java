package gaia.bigdata.api.data;

import gaia.bigdata.api.State;
import gaia.bigdata.api.classification.ClassifierModel;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface ClassifierModelResource {
	@Get
	public ClassifierModel getModel();

	@Put
	public State updateModel(ClassifierModel paramClassifierModel);

	@Delete
	public State deleteModel();
}
