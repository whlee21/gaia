package gaia.commons.services;

import java.net.URI;
import java.util.Collections;

public abstract class BaseServiceLocator implements ServiceLocator {
	public void registerService(String name, URI endpoint) {
		registerService(name, endpoint, Collections.<String, String> emptyMap());
	}
}
