package gaia.bigdata.api.data;

import gaia.bigdata.api.data.gaiasearch.GaiaSearchDataManagementService;
import gaia.bigdata.api.data.hadoop.HadoopDataManagementService;
import gaia.bigdata.api.data.hbase.HBaseDataManagementService;
import gaia.commons.api.APIModule;

import com.google.inject.multibindings.Multibinder;

public class DataManagementAPIModule extends APIModule {
	protected void defineBindings() {
		Multibinder<DataManagementService> dmsBinder = Multibinder.newSetBinder(binder(), DataManagementService.class);
		dmsBinder.addBinding().to(GaiaSearchDataManagementService.class);
		dmsBinder.addBinding().to(HadoopDataManagementService.class);
		dmsBinder.addBinding().to(HBaseDataManagementService.class);
		bind(DataManagementService.class).to(ChainedDataManagementService.class);
		bind(SDACollectionsResource.class).to(SDACollectionsServerResource.class);
		bind(SDACollectionResource.class).to(SDACollectionServerResource.class);
		bind(ClassifierModelsResource.class).to(ClassifierModelsSR.class);
		bind(ClassifierModelResource.class).to(ClassifierModelSR.class);
	}
}
