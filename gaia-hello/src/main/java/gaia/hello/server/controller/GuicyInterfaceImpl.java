package gaia.hello.server.controller;

public class GuicyInterfaceImpl implements GuicyInterface {

	@Override
	public String get() {
		return GuicyInterfaceImpl.class.getName();
	}

}
