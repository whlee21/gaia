package gaia.commons.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.commons.services.URIPayload;

public class RestletUtil {
	private static transient Logger LOG = LoggerFactory.getLogger(RestletUtil.class);

	public static <T> RestletContainer<T> wrap(Class<? extends T> resourceInterface, URIPayload serviceLocation,
			String path) {
		if (serviceLocation != null) {
			return wrap(resourceInterface, serviceLocation.uri, path, Collections.<String, String> emptyMap());
		}
		LOG.warn("No serviceLocation available for " + resourceInterface.getName());

		return null;
	}

	public static <T> RestletContainer<T> wrap(Class<? extends T> resourceInterface, URI serviceLocation, String path) {
		if (serviceLocation != null) {
			return wrap(resourceInterface, serviceLocation, path, Collections.<String, String> emptyMap());
		}
		LOG.warn("No serviceLocation available for " + resourceInterface.getName());

		return null;
	}

	public static <T> RestletContainer<T> wrap(Class<? extends T> resourceInterface, URI serviceLocation, String path,
			Map<String, String> params) {
		LOG.debug("Wrapping {} for URI {}", resourceInterface.getName(), serviceLocation + path);
		Object result = null;
		RestletContainer<T> restletContainer = null;
		if (serviceLocation != null) {
			URI thePath = null;
			try {
				thePath = new URI(serviceLocation.toString() + path);
			} catch (URISyntaxException e) {
				LOG.error("Exception", e);
			}
			ClientResource resource = new ClientResource(thePath);

			if ((params != null) && (!params.isEmpty())) {
				for (Map.Entry<String, String> entry : params.entrySet()) {
					resource.addQueryParameter((String) entry.getKey(), (String) entry.getValue());
				}
			}
			result = resource.wrap(resourceInterface);
			restletContainer = new RestletContainer(resource, result);
		} else {
			LOG.warn("No serviceLocation available for " + resourceInterface.getName());
		}

		return restletContainer;
	}

	public static <T> RestletContainer<T> wrap(Class<? extends T> resourceInterface, URIPayload serviceLocation,
			String path, Map<String, String> params) {
		if (serviceLocation != null) {
			return wrap(resourceInterface, serviceLocation.uri, path, params);
		}
		LOG.warn("No serviceLocation available for " + resourceInterface.getName());

		return null;
	}

	public static <T> RestletContainer<T> wrap(Class<? extends T> resourceInterface, URI serviceLocation, String path,
			Form params) {
		Object result = null;
		RestletContainer<T> restletContainer = null;

		if (serviceLocation != null) {
			URI thePath = null;
			try {
				thePath = new URI(serviceLocation.toString() + path);
			} catch (URISyntaxException e) {
				LOG.error("Exception", e);
			}
			ClientResource resource = new ClientResource(thePath);
			if (params != null) {
				Set<String> names = params.getNames();
				for (String name : names) {
					String[] vals = params.getValuesArray(name);
					for (int i = 0; i < vals.length; i++) {
						String val = vals[i];
						resource.addQueryParameter(name, val);
					}
				}
			}
			result = resource.wrap(resourceInterface);
			restletContainer = new RestletContainer(resource, result);
		} else {
			LOG.warn("No serviceLocation available for " + resourceInterface.getName());
		}
		return restletContainer;
	}

	public static <T> RestletContainer<T> wrap(Class<? extends T> resourceInterface, URIPayload serviceLocation,
			String path, Form params) {
		if (serviceLocation != null) {
			return wrap(resourceInterface, serviceLocation.uri, path, params);
		}
		LOG.warn("No serviceLocation available for " + resourceInterface.getName());

		return null;
	}

	public static <T> RestletContainer<T> wrap(Class<? extends T> resourceInterface, URIPayload serviceLocation,
			String path, String id, String secret) {
		if (serviceLocation != null) {
			return wrap(resourceInterface, serviceLocation.uri, path, id, secret);
		}
		LOG.warn("No serviceLocation available for " + resourceInterface.getName());

		return null;
	}

	public static <T> RestletContainer<T> wrap(Class<? extends T> resourceInterface, URI serviceLocation, String path,
			String id, String secret) {
		Object result = null;
		RestletContainer<T> restletContainer = null;
		if (serviceLocation != null) {
			URI thePath = null;
			try {
				thePath = new URI(serviceLocation.toString() + path);
			} catch (URISyntaxException e) {
				LOG.error("Exception", e);
			}
			ClientResource resource = new ClientResource(thePath);
			resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC, id, secret);
			result = resource.wrap(resourceInterface);
			restletContainer = new RestletContainer(resource, result);
		}
		return restletContainer;
	}

	public static void release(RestletContainer container) {
		Representation rep = container.getClientResource().getResponseEntity();
		if (rep != null) {
			try {
				rep.exhaust();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			rep.release();
		}
	}
}
