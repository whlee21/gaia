package gaia.bigdata.classification;

import gaia.bigdata.classification.mahout.HDFSSGDModelLoader;
import gaia.bigdata.classification.mahout.SGDClassifierService;
import gaia.bigdata.classification.mahout.SGDModelLoader;
import gaia.commons.api.APIModule;

public class ClassifierStateAPIModule extends APIModule {
	protected void defineBindings() {
		bind(ClassifierService.class).to(SGDClassifierService.class);
		bind(SGDModelLoader.class).to(HDFSSGDModelLoader.class);
	}
}
