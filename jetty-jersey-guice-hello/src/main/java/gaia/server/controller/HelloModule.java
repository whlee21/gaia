package gaia.server.controller;

import gaia.server.api.model.GuicyInterface;
import gaia.server.api.model.GuicyInterfaceImpl;

import com.google.inject.AbstractModule;

public class HelloModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(GuicyInterface.class).to(GuicyInterfaceImpl.class);
	}

}
