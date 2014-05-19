package gaia.rules;

import java.io.IOException;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.util.plugin.SolrCoreAware;

public class LandingPageComponent extends BaseRulesEngineComponent implements SolrCoreAware {
	public static final String COMPONENT_NAME = "landing";

	public void prepare(ResponseBuilder rb) throws IOException {
		SolrQueryRequest req = rb.req;
		SolrParams params = convertParams(rb, req.getParams());

		if (isComponentOn("landing", params, "prepare")) {
			RulesEngine engine = RulesHelper.getEngine(rb.req.getCore(), handlerName, engineName);
			if (engine != null) {
				rb.req.getContext().put("rulesPhase", "landing");
				rb.req.getContext().put("rulesHandle", handle);
				try {
					engine.findLandingPage(rb);
				} finally {
					rb.req.getContext().remove("rulesPhase");
					rb.req.getContext().remove("rulesHandle");
				}
			} else {
				throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unable to find engine with name " + engineName
						+ " on the Request Handler named: " + handlerName);
			}
		}
	}

	public void process(ResponseBuilder rb) throws IOException {
	}

	public void inform(SolrCore core) {
	}

	public String getDescription() {
		return "Calculates, using a RulesEngine, whether to short circuit this request and go to a landing page";
	}

	public String getSource() {
		return "$URL:  $";
	}

	public String getVersion() {
		return "$Revision:  $";
	}
}
