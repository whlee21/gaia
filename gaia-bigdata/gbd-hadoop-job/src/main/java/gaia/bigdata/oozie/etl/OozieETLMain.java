package gaia.bigdata.oozie.etl;

import java.net.URI;

import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.bigdata.api.State;
import gaia.bigdata.api.connector.ConnectorResource;

public final class OozieETLMain {
	private static final Logger LOG = LoggerFactory.getLogger(OozieETLMain.class);

	public static void main(String[] args) throws Exception {
		URI connectorServiceURI = new URI(args[0]);
		String collectionName = args[1];
		String dataSourceId = args[2];
		int poll = Integer.parseInt(args[3]);
		boolean wait = Boolean.parseBoolean(args[4]);

		ClientResource cr = new ClientResource(connectorServiceURI + "/" + collectionName + "/" + dataSourceId);
		ConnectorResource connectorResource = (ConnectorResource) cr.wrap(ConnectorResource.class);
		State state = connectorResource.execute(null);
		cr.getResponseEntity().exhaust();
		cr.getResponseEntity().release();

		LOG.info("Crawler: {}", state);
		if (wait) {
			while (true) {
				Thread.sleep(poll);
				String crawlState = (String) connectorResource.status().getProperties().get("crawl_state");
				LOG.info("Crawl state: {}", crawlState);
				if ((crawlState.equals("FINISHED")) || (crawlState.equals("STOPPED")))
					System.exit(0);
				else if ((crawlState.equals("ABORTED")) || (crawlState.equals("EXCEPTION"))) {
					System.exit(1);
				}
			}

		}

		System.exit(0);
	}
}
