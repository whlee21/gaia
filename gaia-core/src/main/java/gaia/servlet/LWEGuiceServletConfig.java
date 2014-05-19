package gaia.servlet;


//public class LWEGuiceServletConfig extends GuiceServletContextListener {
//	private static transient Logger LOG = LoggerFactory.getLogger(GuiceServletContextListener.class);
//
//	private static final LWEModule module = new LWEModule();
//	private static Injector injector;
//
//	public LWEGuiceServletConfig() throws SchedulerException {
//		((AdminScheduler) injector.getInstance(AdminScheduler.class)).startAllSchedules();
//	}
//
//	protected Injector getInjector() {
//		return injector;
//	}
//
//	public static Injector injectorHack() {
//		return injector;
//	}
//
//	public static void restartInjector() {
//		LWEModule module = new LWEModule();
//		injector = Guice.createInjector(new Module[] { module, new LWEServletModule() });
//		module.init(injector);
//	}
//
//	static {
//		try {
//			injector = Guice.createInjector(new Module[] { module, new LWEServletModule() });
//			module.init(injector);
//		} catch (Throwable t) {
//			String msg = "Error initializing Guice injector" + t.getMessage();
//			LOG.error(msg, t);
//			throw new RuntimeException(msg, t);
//		}
//	}
//}
public class LWEGuiceServletConfig {
	
}