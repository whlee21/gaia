package gaia.hello.server.servlet;

import gaia.hello.server.controller.GaiaHelloModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceServletContextListener;

public class GaiaHelloGuiceServletConfig extends GuiceServletContextListener {
	
	private static final Logger LOG = LoggerFactory.getLogger(GaiaHelloGuiceServletConfig.class);
	private static Injector injector;
	private static final GaiaHelloModule module = new GaiaHelloModule();

	@Override
	protected Injector getInjector() {
		return injector;
	}

	public static Injector injectorHack() {
		return injector;
	}

	public static void restartInjector() {
		GaiaHelloModule module = new GaiaHelloModule();
		injector = Guice.createInjector(new Module[] { module,
				new GaiaHelloServletModule() });
		module.init(injector);
	}
	
	static {
		try {
			injector = Guice.createInjector(new Module[] { module,
					new GaiaHelloServletModule() });
			module.init(injector);
		} catch (Throwable t) {
			String msg = "Error initializing Guice injector" + t.getMessage();
			LOG.error(msg, t);
			throw new RuntimeException(msg, t);
		}
	}
}
