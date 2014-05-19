package gaia.bigdata.api.hdfs;

import gaia.bigdata.api.client.ClientAPIEnroler;
import gaia.bigdata.api.client.ClientAPIVerifier;
import gaia.commons.api.APIModule;

import org.restlet.security.Enroler;
import org.restlet.security.Verifier;

public class WebHDFSProxyAPIModule extends APIModule {
	protected void defineBindings() {
		bind(Verifier.class).to(ClientAPIVerifier.class);
		bind(Enroler.class).to(ClientAPIEnroler.class);
	}
}
