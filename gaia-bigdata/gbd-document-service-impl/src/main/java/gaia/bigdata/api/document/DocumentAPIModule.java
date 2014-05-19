package gaia.bigdata.api.document;

import gaia.bigdata.api.document.gaiasearch.GaiaDocumentService;
import gaia.bigdata.api.document.hbase.HBaseDocumentService;
import gaia.commons.api.APIModule;

import com.google.inject.multibindings.Multibinder;

public class DocumentAPIModule extends APIModule {
	protected void defineBindings() {
		Multibinder<DocumentService> dsBinder = Multibinder.newSetBinder(binder(), DocumentService.class);

		dsBinder.addBinding().to(GaiaDocumentService.class);
		dsBinder.addBinding().to(HBaseDocumentService.class);

		bind(DocumentResource.class).to(DocumentServerResource.class);
		bind(DocumentsResource.class).to(DocumentsServerResource.class);
		bind(DocumentsRetrievalResource.class).to(DocumentsRetrievalServerResource.class);
	}
}
