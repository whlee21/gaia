package gaia.hello.server.servlet;

import gaia.commons.server.api.resources.ResourceInstanceFactory;
import gaia.hello.server.api.resources.ResourceInstanceFactoryImpl;
import gaia.hello.server.api.services.HelloGuice;
import gaia.hello.server.controller.GuicyInterface;
import gaia.hello.server.controller.GuicyInterfaceImpl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class GaiaHelloServletModule extends JerseyServletModule {

	private static final Logger LOG = LoggerFactory
			.getLogger(GaiaHelloServletModule.class);
	private static final String JERSEY_CONFIG_PROPERTY_RESOURCECONFIGCLASS = "com.sun.jersey.config.property.resourceConfigClass";
	private static final String JERSEY_API_JSON_POJO_MAPPING_FEATURE = "com.sun.jersey.api.json.POJOMappingFeature";
	private static final String JERSEY_CONFIG_PROPERTY_PACKAGES = "com.sun.jersey.config.property.packages";

	// public GaiaHelloServletModule() {
	// TODO Auto-generated constructor stub
	// }

	@Override
	protected void configureServlets() {

		LOG.debug("whlee21 GaiaHelloServletModule.configureServlets()");
		// Must configure at least one JAX-RS resource or the
		// server will fail to start.

		// bind(HelloService.class);
		bind(HelloGuice.class);
		bind(GuicyInterface.class).to(GuicyInterfaceImpl.class);
		bind(ResourceInstanceFactory.class).to(ResourceInstanceFactoryImpl.class);

		bind(GuiceContainer.class);

		// bind(JacksonJsonProvider.class).in(Scopes.SINGLETON);

		final Map<String, String> params = new HashMap<String, String>();

		params.put(JERSEY_CONFIG_PROPERTY_RESOURCECONFIGCLASS,
				"com.sun.jersey.api.core.PackagesResourceConfig");
		params.put(JERSEY_CONFIG_PROPERTY_PACKAGES,
				"gaia.hello.server.api.services;" + "gaia.hello.server.api");
		params.put(JERSEY_API_JSON_POJO_MAPPING_FEATURE, "true");

		// Route all requests through GuiceContainer
		serve("/api/v1/*").with(GuiceContainer.class, params);
		// serve("/*").with(GuiceContainer.class);
	}
}
