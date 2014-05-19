package gaia.crawl;

import gaia.api.LWEStatusService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restlet.Application;
import org.restlet.Restlet;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class RestAPI extends Application {
	private static transient Log log = LogFactory.getLog(RestAPI.class);
	public static final String COLLECTION_PARAM = "coll_name";
	private LWERouter router;

	@Inject
	public RestAPI(Injector injector) {
		setStatusService(new LWEStatusService());
	}

	public void stop() throws Exception {
		if (router != null)
			router.stop();
	}

	public synchronized Restlet createInboundRoot() {
		log.info("Starting /control api");

		getTunnelService().setEnabled(true);
		getTunnelService().setExtensionsTunnel(true);

		LWERouter router = new LWERouter(getContext());
		this.router = router;
		router.attach("/config/lock-updates", LockUpdatesResource.class);
		return router;
	}
}
