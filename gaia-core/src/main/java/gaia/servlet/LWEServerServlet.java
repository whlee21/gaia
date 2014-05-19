package gaia.servlet;

import gaia.api.RestAPI;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.ext.servlet.ServerServlet;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LWEServerServlet extends ServerServlet {
	private RestAPI application;

	@Inject
	public LWEServerServlet(RestAPI application) {
		this.application = application;
	}

	protected Application createApplication(Context parentContext) {
		application.setContext(parentContext.createChildContext());
		getComponent().getClients().add(Protocol.HTTP);
		getComponent().getClients().add(Protocol.HTTPS);
		return application;
	}
}
