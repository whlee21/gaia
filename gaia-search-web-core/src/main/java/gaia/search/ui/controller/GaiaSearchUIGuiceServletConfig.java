package gaia.search.ui.controller;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

public class GaiaSearchUIGuiceServletConfig extends GuiceServletContextListener {
	
	private static final Logger LOG = LoggerFactory.getLogger(GaiaSearchUIGuiceServletConfig.class);
	private static Injector injector;
//	private static final GaiaSearchModule module = new GaiaSearchModule();


//	@Inject
	public GaiaSearchUIGuiceServletConfig(Injector injector) throws SchedulerException {
		this.injector = injector;
//		((AdminScheduler) injector.getInstance(AdminScheduler.class))
//				.startAllSchedules();
	}
	
	@Override
	protected Injector getInjector() {
		return injector;
//		return Guice.createInjector(new GaiaSearchServletModule());
	}

//	public static Injector injectorHack() {
//		return injector;
//	}

//	public static void restartInjector() {
//		GaiaSearchModule module = new GaiaSearchModule();
//		injector = Guice.createInjector(new Module[] { module,
//				new GaiaSearchServletModule() });
//		module.init(injector);
//	}
	
//	static {
//		try {
//			injector = Guice.createInjector(new Module[] { module,
//					new GaiaSearchServletModule() });
//			module.init(injector);
//		} catch (Throwable t) {
//			String msg = "Error initializing Guice injector" + t.getMessage();
//			LOG.error(msg, t);
//			throw new RuntimeException(msg, t);
//		}
//	}
}
