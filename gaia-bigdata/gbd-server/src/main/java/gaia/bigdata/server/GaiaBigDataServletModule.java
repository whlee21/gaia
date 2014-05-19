package gaia.bigdata.server;

import java.util.HashMap;
import java.util.Map;

import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class GaiaBigDataServletModule extends JerseyServletModule {
	private static final String JERSEY_CONFIG_PROPERTY_RESOURCECONFIGCLASS = "com.sun.jersey.config.property.resourceConfigClass";
	private static final String JERSEY_API_JSON_POJO_MAPPING_FEATURE = "com.sun.jersey.api.json.POJOMappingFeature";
	private static final String JERSEY_CONFIG_PROPERTY_PACKAGES = "com.sun.jersey.config.property.packages";

	@Override
	protected void configureServlets() {
		bind(GuiceContainer.class);

		Map<String, String> params = new HashMap<String, String>();
		params.put(JERSEY_CONFIG_PROPERTY_RESOURCECONFIGCLASS, "com.sun.jersey.api.core.PackagesResourceConfig");
		params.put(JERSEY_CONFIG_PROPERTY_PACKAGES, "gaia.bigdata.api.services;" + "gaia.bigdata.api");
		params.put(JERSEY_API_JSON_POJO_MAPPING_FEATURE, "true");
		// Route all requests through GuiceContainer
		serve("/gbd/v1/*").with(GuiceContainer.class, params);

		params = new HashMap<String, String>();
		params.put(JERSEY_CONFIG_PROPERTY_RESOURCECONFIGCLASS, "com.sun.jersey.api.core.PackagesResourceConfig");
		params.put(JERSEY_CONFIG_PROPERTY_PACKAGES, "gaia.bigdata.api.services;" + "gaia.bigdata.api");
		params.put(JERSEY_API_JSON_POJO_MAPPING_FEATURE, "true");
		serve("/resources/*").with(GuiceContainer.class, params);
	}
}
