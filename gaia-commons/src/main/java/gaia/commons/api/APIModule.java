package gaia.commons.api;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;

public abstract class APIModule extends AbstractModule {

	@Inject
	protected Injector injector;

	protected Class<? extends API> getAPIClass() {
		String className = getClass().getName();
		className = className.replace("Module", "");
		try {
			return Class.forName(className).asSubclass(API.class);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	protected abstract void defineBindings();

	protected void configure() {
		Multibinder<API> apiBinder = Multibinder.newSetBinder(binder(), API.class);
		apiBinder.addBinding().to(getAPIClass());
		defineBindings();
	}

	public void initInjectorDependent() {
	}
}
