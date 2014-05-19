package gaia.hello.server.controller;

import gaia.commons.server.GaiaException;
import gaia.commons.server.controller.RequestStatusRequest;
import gaia.commons.server.controller.RequestStatusResponse;

import java.util.Collections;
import java.util.Set;

public class GaiaHelloControllerImpl implements GaiaHelloController {

	@Override
	public Set<RequestStatusResponse> getRequestStatus(
			RequestStatusRequest request) throws GaiaException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Set<HelloResponse> getHelloMessage(Set<HelloRequest> helloRequest) {
		return Collections.singleton(new HelloResponse("Response: Hello World!"));
	}

}
