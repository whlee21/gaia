package gaia.rules;

import org.apache.solr.core.SolrCore;

public class RulesHelper {
	public static RulesEngine getEngine(SolrCore core, String handlerName, String engineName) {
		RulesEngineManagerHandler handler = (RulesEngineManagerHandler) core.getRequestHandler(handlerName);
		RulesEngine engine = null;
		if (handler != null) {
			engine = handler.getEngine(engineName);
		}
		return engine;
	}
}
