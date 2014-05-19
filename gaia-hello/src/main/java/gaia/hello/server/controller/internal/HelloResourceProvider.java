package gaia.hello.server.controller.internal;

import gaia.commons.server.GaiaException;
import gaia.commons.server.controller.internal.ResourceImpl;
import gaia.commons.server.controller.spi.NoSuchParentResourceException;
import gaia.commons.server.controller.spi.NoSuchResourceException;
import gaia.commons.server.controller.spi.Predicate;
import gaia.commons.server.controller.spi.Request;
import gaia.commons.server.controller.spi.RequestStatus;
import gaia.commons.server.controller.spi.Resource;
import gaia.commons.server.controller.spi.ResourceAlreadyExistsException;
import gaia.commons.server.controller.spi.SystemException;
import gaia.commons.server.controller.spi.UnsupportedPropertyException;
import gaia.commons.server.controller.utilities.PredicateHelper;
import gaia.commons.server.controller.utilities.PropertyHelper;
import gaia.hello.server.controller.GaiaHelloController;
import gaia.hello.server.controller.HelloRequest;
import gaia.hello.server.controller.HelloResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HelloResourceProvider extends AbstractControllerResourceProvider {

	// ----- Property ID constants ---------------------------------------------
	// Requests
	protected static final String HELLO_MESSAGE_PROPERTY_ID = PropertyHelper
			.getPropertyId("Hellos", "message");

	private static Set<String> pkPropertyIds = new HashSet<String>(
			Arrays.asList(new String[] { HELLO_MESSAGE_PROPERTY_ID }));

	HelloResourceProvider(Set<String> propertyIds,
			Map<Resource.Type, String> keyPropertyIds,
			GaiaHelloController helloController) {
		super(propertyIds, keyPropertyIds, helloController);
	}

	@Override
	public RequestStatus createResources(Request request)
			throws SystemException, UnsupportedPropertyException,
			ResourceAlreadyExistsException, NoSuchParentResourceException {
		throw new UnsupportedOperationException("Not currently supported.");
	}

	@Override
	public Set<Resource> getResources(Request request, Predicate predicate)
			throws SystemException, UnsupportedPropertyException,
			NoSuchResourceException, NoSuchParentResourceException {

		final HelloRequest helloRequest = getRequest(PredicateHelper
				.getProperties(predicate));
		Set<String> requestedIds = getRequestPropertyIds(request, predicate);

		// TODO : handle multiple requests
		Set<HelloResponse> responses = getResources(new Command<Set<HelloResponse>>() {
			@Override
			public Set<HelloResponse> invoke() throws GaiaException {
				return getManagementController().getHelloMessage(Collections.singleton(helloRequest));
			}
		});

		Set<Resource> resources = new HashSet<Resource>();
		for (HelloResponse response : responses) {
			Resource resource = new ResourceImpl(Resource.Type.Hello);
			setResourceProperty(resource, HELLO_MESSAGE_PROPERTY_ID,
					response.getMessage(), requestedIds);

			resources.add(resource);
		}
		return resources;
	}

	@Override
	public RequestStatus updateResources(Request request, Predicate predicate)
			throws SystemException, UnsupportedPropertyException,
			NoSuchResourceException, NoSuchParentResourceException {
		throw new UnsupportedOperationException("Not currently supported.");
	}

	@Override
	public RequestStatus deleteResources(Predicate predicate)
			throws SystemException, UnsupportedPropertyException,
			NoSuchResourceException, NoSuchParentResourceException {
		throw new UnsupportedOperationException("Not currently supported.");
	}

	@Override
	protected Set<String> getPKPropertyIds() {
		throw new UnsupportedOperationException("Not currently supported.");
	}

	private HelloRequest getRequest(Map<String, Object> properties) {
		HelloRequest cr = new HelloRequest(
				(String) properties.get(HELLO_MESSAGE_PROPERTY_ID));

		return cr;
	}
}
