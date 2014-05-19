package gaia.rules;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;

public abstract class BaseRulesEngineComponent extends SearchComponent {
	protected String handlerName;
	protected String engineName;
	protected String handle;

	public void init(NamedList args) {
		super.init(args);
		SolrParams params = SolrParams.toSolrParams(args);
		handlerName = params.get("requestHandler");
		if ((handlerName == null) || (handlerName.equals(""))) {
			throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
					"Unable to determine RulesEngineManagerHandler for requestHandler=" + handlerName);
		}
		engineName = params.get("engine");
		if (engineName == null) {
			throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unable to determine engineName for=" + engineName);
		}
		handle = params.get("handle");
	}

	protected boolean isComponentOn(String compName, SolrParams params, String phase) {
		boolean handleParam = params.getBool(compName + "." + handle, true);
		return (params.getBool(compName, false)) && (params.getBool(compName + "." + phase, true)) && (handleParam == true);
	}

	protected SolrParams convertParams(ResponseBuilder rb, SolrParams params) {
		if (!(params instanceof ModifiableSolrParams)) {
			params = new ModifiableSolrParams(params);
			rb.req.setParams(params);
		}
		return params;
	}
}
