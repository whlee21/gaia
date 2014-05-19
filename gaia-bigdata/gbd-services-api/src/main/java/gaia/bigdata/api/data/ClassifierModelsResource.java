package gaia.bigdata.api.data;

import gaia.bigdata.api.State;
import gaia.bigdata.api.classification.ClassifierModel;
import org.restlet.resource.Post;

public  interface ClassifierModelsResource extends
		ModelsListingResource {
	@Post
	public  State add(ClassifierModel paramClassifierModel);
}
