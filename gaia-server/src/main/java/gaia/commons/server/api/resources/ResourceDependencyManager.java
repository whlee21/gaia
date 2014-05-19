package gaia.commons.server.api.resources;

import gaia.commons.server.controller.spi.Resource;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Singleton;

@Singleton
public class ResourceDependencyManager {

	private Map<Resource.Type, Class<? extends ResourceDefinition>> dependencies = new HashMap<Resource.Type, Class<? extends ResourceDefinition>>();

	public ResourceDependencyManager() {
		// TODO Auto-generated constructor stub
	}

	public void registerResourceDefinition(Resource.Type type,
			Class<? extends ResourceDefinition> clazz) {
		dependencies.put(type, clazz);
	}

	public Class<? extends ResourceDefinition> getResourceDefinition(
			Resource.Type type) {
		return dependencies.get(type);
	}
}
