package gaia.server.api.model;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class GuicyInterfaceImpl implements GuicyInterface {

	private Injector injector;

	@Inject
	public GuicyInterfaceImpl(Injector injector) {
		this.injector = injector;
	}

	@Override
	public String get() {
		return GuicyInterfaceImpl.class.getName();
	}

	public Injector getInjector() {
		return injector;
	}
}
