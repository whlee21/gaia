package gaia.hello.server.controller;

import gaia.hello.server.configuration.Configuration;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Scopes;

public class GaiaHelloModule extends AbstractModule {
	private static final Logger LOG = LoggerFactory
			.getLogger(GaiaHelloModule.class);
	private final Configuration configuration;

	public GaiaHelloModule() {
		configuration = new Configuration();

	}

	public GaiaHelloModule(Properties properties) {
		configuration = new Configuration(properties);
	}

	public void init(Injector injector) {

	}

	@Override
	protected void configure() {
		LOG.debug("whlee21 GaiaHelloModule.configure()");
		bind(Configuration.class).toInstance(configuration);
		bind(Gson.class).in(Scopes.SINGLETON);
		bind(GaiaHelloController.class).to(GaiaHelloControllerImpl.class);
	}
}
