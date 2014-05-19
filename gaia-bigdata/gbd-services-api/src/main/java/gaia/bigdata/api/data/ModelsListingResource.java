package gaia.bigdata.api.data;

import gaia.bigdata.api.classification.ClassifierModel;
import java.util.Collection;
import org.restlet.resource.Get;

public interface ModelsListingResource {
	@Get
	public Collection<ClassifierModel> listModels();
}
