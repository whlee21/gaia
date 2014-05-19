package gaia.search.ui.controller;


import gaia.search.ui.configuration.Configuration;

import java.net.MalformedURLException;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.servlets.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class GaiaProxyServlet extends ProxyServlet {

	private static Logger LOG = LoggerFactory.getLogger(GaiaProxyServlet.class);
	private static Injector injector;

	@Inject
	public static void init(Injector instance) {
		injector = instance;
	}
	
	@Override
	protected HttpURI proxyHttpURI(String scheme, String serverName, int serverPort, String uri)
			throws MalformedURLException {
		Configuration config = injector.getInstance(Configuration.class);
		String originalURI = scheme + serverName + ":" + serverPort + uri;
		String proxyedURI = scheme + config.getDestServerName() + ":" + config.getDestServerPort() + uri;
		LOG.info("originalURI = " + originalURI);
		LOG.info("proxyedURI = " + proxyedURI);
		return super.proxyHttpURI(scheme, config.getDestServerName(), config.getDestServerPort(), uri);
	}


}
