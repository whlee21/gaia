package gaia.solr.click;

import java.io.IOException;

import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryLogComponent extends SearchComponent {
	private static final Logger LOG = LoggerFactory.getLogger("query.log");

	public String getDescription() {
		return "Query logging component";
	}

	public String getSource() {
		return "$URL$";
	}

	public String getVersion() {
		return "$Revision$";
	}

	public void prepare(ResponseBuilder rb) throws IOException {
	}

	public void process(ResponseBuilder rb) throws IOException {
		String q = rb.getQueryString();
		String addr = (String) rb.req.getContext().get("remote_addr");
		long start = rb.req.getStartTime();
		int numHits = rb.getResults().docList.matches();

		String reqId = Utils.createRequestId("none", start, q);
		LOG.info("Q\t" + addr + "\t" + start + "\t" + q + "\t" + numHits);
		rb.rsp.add("reqId", reqId);
	}
}
