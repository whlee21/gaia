package gaia.bigdata.api.client;

import gaia.commons.api.APIModule;

import org.restlet.security.Enroler;
import org.restlet.security.Verifier;

public class ClientAPIModule extends APIModule {
	protected void defineBindings() {
		bind(Verifier.class).to(ClientAPIVerifier.class);
		bind(Enroler.class).to(ClientAPIEnroler.class);
	}
}
