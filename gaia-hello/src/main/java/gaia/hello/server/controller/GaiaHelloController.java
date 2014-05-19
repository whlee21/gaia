package gaia.hello.server.controller;

import gaia.commons.server.GaiaException;
import gaia.commons.server.controller.RequestStatusRequest;
import gaia.commons.server.controller.RequestStatusResponse;

import java.util.Set;

public interface GaiaHelloController {
	public Set<RequestStatusResponse> getRequestStatus(
			RequestStatusRequest request) throws GaiaException;

	public Set<HelloResponse> getHelloMessage(Set<HelloRequest> helloRequests);
}
