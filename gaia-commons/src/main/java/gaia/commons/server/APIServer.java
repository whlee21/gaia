package gaia.commons.server;

import gaia.commons.api.API;
import gaia.commons.api.APIModule;
import gaia.commons.api.Configuration;
import gaia.commons.api.MemoryConfiguration;
import gaia.commons.api.ResourceFinder;
import gaia.commons.management.JMXMonitoredMap;
import gaia.commons.management.MonitoringAPIModule;
import gaia.commons.services.ChainedServiceLocator;
import gaia.commons.services.PropertyServiceLocator;
import gaia.commons.services.ServiceLocator;
import gaia.commons.services.URIPayload;
import gaia.commons.services.ZooKeeperServiceLocator;
import gaia.commons.util.CLI;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.cli2.Option;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.routing.VirtualHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class APIServer {
	private static transient Logger LOG = LoggerFactory.getLogger(APIServer.class);
	protected APIServerConfig config;
	protected Component component;
	protected ServiceLocator locator;
	protected Set<API> apis;
	private Protocol protocol;
	private long serverStart;

	@Inject
	public APIServer(Set<API> apis, APIServerConfig config, ServiceLocator locator) throws Exception {
		this.config = config;
		this.locator = locator;
		this.apis = apis;
		component = createComponent();
		createServer();
	}

	protected void createServer() throws URISyntaxException {
		Server server;
		if (config.useSSL == true) {
			protocol = Protocol.HTTPS;
			server = new Server(protocol, config.address, config.port);
		} else {
			protocol = Protocol.HTTP;
			server = new Server(protocol, config.address, config.port);
		}
		component.getServers().add(server);
		Context context = server.getContext();
		if (config.useSSL == true) {
			context.getParameters().add("keystorePath", config.keystorePath);
			context.getParameters().add("keystorePassword", config.keystorePassword);
			context.getParameters().add("keyPassword", config.keyPassword);
		}

		server.getContext().getParameters().add("minThreads", String.valueOf(config.minThreads));
		server.getContext().getParameters().add("maxThreads", String.valueOf(config.maxThreads));

		for (API api : apis) {
			String apiRoot = api.getAPIRoot();
			if ((apiRoot == null) || (apiRoot.equals(""))) {
				throw new RuntimeException("API must provide non-null, non-empty getAPIRoot implementation");
			}

			locator.registerService(api.getAPIName(), new URI(protocol.getSchemeName(), null, config.address, config.port,
					config.apiBase + apiRoot, null, null));
			URIPayload uriPayload = locator.getServiceURI(api.getAPIName());
			if (uriPayload != null) {
				LOG.info("Registered {} at {}", api.getAPIName(), uriPayload.uri);
			} else
				LOG.warn("Couldn't find service that was just registered: " + api.getAPIName());
		}
	}

	protected Component createComponent() throws URISyntaxException {
		Component component = new Component();
		VirtualHost apivhost = new VirtualHost(component.getContext());
		component.getDefaultHost().attach(config.apiBase, apivhost);
		component.getClients().add(Protocol.HTTP);
		component.getClients().add(Protocol.HTTPS);

		for (API api : apis) {
			String apiRoot = api.getAPIRoot();
			if ((apiRoot == null) || (apiRoot.equals(""))) {
				throw new RuntimeException("API must provide non-null, non-empty getAPIRoot implementation");
			}
			apivhost.attach(apiRoot, api);
			LOG.info("Attached " + api.getAPIName() + " to " + apiRoot);
		}
		return component;
	}

	public Set<API> getApis() {
		return Collections.unmodifiableSet(apis);
	}

	public String getAddress() {
		return config.address;
	}

	public void setAddress(String address) {
		config.address = address;
	}

	public int getPort() {
		return config.port;
	}

	public void setPort(int port) {
		config.port = port;
	}

	public boolean isUseSSL() {
		return config.useSSL;
	}

	public void setUseSSL(boolean useSSL) {
		config.useSSL = useSSL;
	}

	public String getApiBase() {
		return config.apiBase;
	}

	public void setApiBase(String apiBase) {
		config.apiBase = apiBase;
	}

	public synchronized void start() throws Exception {
		LOG.info("Starting " + apis + " on " + config.address + ":" + config.port + config.apiBase);
		serverStart = System.currentTimeMillis();
		component.start();
		LOG.info("Started");
	}

	public synchronized void stop() throws Exception {
		LOG.info("Stopping " + config.address + ":" + config.port);
		try {
			for (API api : apis)
				locator.unregisterService(api.getAPIName(), new URI(protocol.getSchemeName(), null, config.address,
						config.port, config.apiBase + api.getAPIRoot(), null, null));
		} catch (Throwable e) {
			LOG.error("Exception", e);
		}
		component.stop();
		LOG.info("APIServer Stopped.  Server ran for: " + (System.currentTimeMillis() - serverStart) + " ms");
	}

	public Component getComponent() {
		return component;
	}

	public static void main(String[] args) throws Exception {
		System.setProperty("org.restlet.engine.loggerFacadeClass", "org.restlet.ext.slf4j.Slf4jLoggerFacade");

		CLI cmdLine = new CLI();
		cmdLine.addOption("port", "p", "The port to listen on", true);
		cmdLine.addOption("address", "x", "The address to bind to", true);
		cmdLine.addOption("apiBase", "b", "The base of the URL name, e.g. /sda or /api", "/sda/v1");
		cmdLine.addFlag("ssl", "s", "Use SSL");

		cmdLine.addOption("threadPoolSize", "tps",
				"The number of threads to use in the fixed size Thread Pool for use by services", "5");
		cmdLine.addOption("config", "c", "The path to the configuration file", false);
		cmdLine
				.addOption(
						"jmxServiceUrl",
						"u",
						"Optional.  Pass in the service URL of the JMX service to connect to.  If this value or jmxAgentId is not set, then a MBeanServer will be created",
						false);
		cmdLine.addOption("jmxAgentId", "a", "Optional. The JMX Agent Id to connect to.", false);
		cmdLine.addOption("minServerThreads", "minThr",
				"Optional.  The minimum number of threads to use to service incoming requests in Jetty", "5");
		cmdLine.addOption("maxServerThreads", "maxThr",
				"Optional.  The maximum number of threads to use to service incoming requests in Jetty", "500");
		cmdLine.addOption("curatorNamespace", "cn",
				"Optional.  The namespace in ZooKeeper to use for registering the API services.  Default is 'sda'", "sda");
		Option opt = CLI.buildOption("api", "a",
				"The fully qualified class name extending the API class or the shortcut name", true, 1, Integer.MAX_VALUE,
				true, null);

		cmdLine.addOption(opt);
		Map<String, List<String>> argMap = cmdLine.parseArguments(args);
		if (argMap == null) {
			System.out.println("Couldn't parse args: " + Arrays.asList(args));
			return;
		}
		Properties props = new Properties();

		String cfg = cmdLine.getOption("config");
		if (cfg != null) {
			File configFile = new File(cfg);
			if (!configFile.exists())
				System.err.println("Can't find config file at " + configFile);
			else {
				props.load(new FileReader(configFile));
			}

		}

		String allApis = cmdLine.getOption("api");
		Class<?>[] classes;
		int i;
		if ((allApis != null) && (allApis.equalsIgnoreCase("properties"))) {
			String apiProp = props.getProperty("apis");
			if ((apiProp != null) && (!apiProp.isEmpty())) {
				String[] apis = apiProp.split(",");
				classes = (Class[]) Array.newInstance(Class.class, apis.length);
				for (i = 0; i < apis.length; i++) {
					Class<?> clazz = Class.forName(apis[i]).asSubclass(APIModule.class);
					classes[i] = clazz;
				}
			} else {
				throw new Exception(
						"'apis' is not set in the configuration file, but you specified --api properties when launching");
			}
		} else {
			i = 0;
			classes = (Class[]) Array.newInstance(Class.class, cmdLine.getOptions("api").size());
			for (String apiClassName : cmdLine.getOptions("api")) {
				Class<?> clazz = Class.forName(apiClassName).asSubclass(APIModule.class);
				classes[(i++)] = clazz;
			}
		}

		String curatorNamespace = cmdLine.getOption("curatorNamespace", "sda");
		final SLInfo slInfo = setupServiceLocator(props, curatorNamespace);

		final Configuration configuration = new MemoryConfiguration(props, argMap);
		final APIServerConfig sdaConfig = new APIServerConfig();
		sdaConfig.address = cmdLine.getOption("address");
		sdaConfig.port = Integer.parseInt(cmdLine.getOption("port"));
		sdaConfig.apiBase = cmdLine.getOption("apiBase", "/sda/v1");
		sdaConfig.useSSL = cmdLine.hasOption("ssl");
		int numThreads = Integer.parseInt(cmdLine.getOption("threadPoolSize", "5"));
		sdaConfig.minThreads = Integer.parseInt(cmdLine.getOption("minServerThreads", "5"));
		sdaConfig.maxThreads = Integer.parseInt(cmdLine.getOption("maxServerThreads", "500"));
		String serviceUrl = cmdLine.getOption("jmxServiceUrl");
		String agentId = cmdLine.getOption("jmxAgentId");

		JMXMonitoredMap jmx = createJMX("/" + sdaConfig.apiBase, serviceUrl, agentId);

		APIModule[] modules = new APIModule[classes.length + 2];
		for (i = 0; i < classes.length; i++) {
			Class<?> module = classes[i];
			modules[i] = ((APIModule) module.newInstance());
		}
		if (sdaConfig.useSSL == true) {
			sdaConfig.keystorePath = configuration.getProperties().getProperty("keystorePath");
			sdaConfig.keystorePassword = configuration.getProperties().getProperty("keystorePassword");
			checkObfuscated(sdaConfig.keystorePassword);
			sdaConfig.keyPassword = configuration.getProperties().getProperty("keyPassword");
			checkObfuscated(sdaConfig.keyPassword);
		}
		final ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

		modules[classes.length] = new MonitoringAPIModule(jmx);
		modules[(classes.length + 1)] = new APIModule() {
			protected void configure() {
				bind(APIServer.class);
				bind(ServiceLocator.class).toInstance(slInfo.serviceLocator);
				if (slInfo.curator != null) {
					bind(CuratorFramework.class).toInstance(slInfo.curator);
				}
				bind(Configuration.class).toInstance(configuration);
				bind(APIServerConfig.class).toInstance(sdaConfig);
				bind(ExecutorService.class).toInstance(executorService);
			}

			protected void defineBindings() {
			}
		};
		Injector injector = Guice.createInjector(modules);
		ResourceFinder finder = new ResourceFinder(injector);
		injector.injectMembers(finder);
		for (APIModule module : modules) {
			injector.injectMembers(module);
		}
		for (APIModule module : modules) {
			module.initInjectorDependent();
		}

		APIServer sdaServer = (APIServer) injector.getInstance(APIServer.class);
		for (API api : sdaServer.getApis()) {
			jmx.put(api.getAPIName(), api);
		}
		Runtime.getRuntime().addShutdownHook(new ShutdownThread(sdaServer));
		LOG.info("Registered Shutdown Hook");

		sdaServer.start();
	}

	private static void checkObfuscated(String password) {
		if (!password.startsWith("OBF:"))
			throw new RuntimeException(
					"Passwords must be Obfuscated and start with 'OBF:'.  See http://docs.codehaus.org/display/JETTY/How+to+configure+SSL");
	}

	private static SLInfo setupServiceLocator(Properties props, String curatorNamespace) throws Exception {
		SLInfo info = new SLInfo();
		final ServiceLocator locator;
		if (props.containsKey("zkhost")) {
			final CuratorFramework curator = CuratorFrameworkFactory.builder().connectString(props.getProperty("zkhost"))
					.namespace(curatorNamespace).retryPolicy(new ExponentialBackoffRetry(50, 100)).build();

			curator.start();
			// while (!curator.isStarted()) {
			// Thread.sleep(10L);
			// }
			while (curator.getState() != CuratorFrameworkState.STARTED) {
				Thread.sleep(10L);
			}
			curator.newNamespaceAwareEnsurePath("/services").ensure(curator.getZookeeperClient());
			ServiceDiscovery<Map> discovery = ServiceDiscoveryBuilder.builder(Map.class).basePath("/services")
					.client(curator).build();
			discovery.start();
			locator = new ChainedServiceLocator(new ServiceLocator[] {
					new ZooKeeperServiceLocator(discovery, new RoundRobinStrategy<Map>()), new PropertyServiceLocator(props) });

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						curator.close();
						locator.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
			info.curator = curator;
		} else {
			locator = new PropertyServiceLocator(props);
		}
		info.serviceLocator = locator;
		return info;
	}

	public static JMXMonitoredMap createJMX(String root, String serviceUrl, String agentId) {
		JMXMonitoredMap result = null;
		MBeanServer server = null;
		if (serviceUrl == null) {
			List<MBeanServer> servers = null;
			if (agentId == null) {
				servers = MBeanServerFactory.findMBeanServer(null);
			} else if (agentId != null) {
				servers = MBeanServerFactory.findMBeanServer(agentId);

				if ((servers == null) || (servers.isEmpty())) {
					throw new RuntimeException("No JMX Servers found with agentId: " + agentId);
				}
			}
			if ((servers == null) || (servers.isEmpty())) {
				LOG.info("No JMX servers found, not exposing SDA information with JMX.");
				return null;
			}
			server = (MBeanServer) servers.get(0);
			LOG.info("JMX monitoring is enabled. Adding mbeans to JMX Server: " + server);
		} else {
			try {
				server = MBeanServerFactory.createMBeanServer();
				JMXConnectorServer connector = JMXConnectorServerFactory.newJMXConnectorServer(new JMXServiceURL(serviceUrl),
						null, server);

				connector.start();
				LOG.info("JMX monitoring is enabled at " + serviceUrl);
			} catch (Exception e) {
				server = null;
				throw new RuntimeException("Could not start JMX monitoring ", e);
			}
		}
		result = new JMXMonitoredMap(root, server);

		return result;
	}

	private static class SLInfo {
		CuratorFramework curator;
		ServiceLocator serviceLocator;
	}

	private static class ShutdownThread extends Thread {
		private APIServer server;

		private ShutdownThread(APIServer server) {
			this.server = server;
		}

		public void run() {
			System.out.println("Shutting down APIServer");
			System.out.flush();
			try {
				server.stop();
			} catch (Exception e) {
				APIServer.LOG.error("Exception", e);
			}
		}
	}
}
