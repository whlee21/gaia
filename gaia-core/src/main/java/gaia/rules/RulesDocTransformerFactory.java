package gaia.rules;

import java.io.IOException;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.transform.DocTransformer;
import org.apache.solr.response.transform.TransformerFactory;

public class RulesDocTransformerFactory extends TransformerFactory {
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

	public DocTransformer create(String field, SolrParams params, SolrQueryRequest req) {
		RulesEngine engine = RulesHelper.getEngine(req.getCore(), handlerName, engineName);
		if (engine != null) {
			return new RulesDocTransformer(field, engine, req);
		}
		throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unable to find engine with name " + engineName
				+ " on the Request Handler named: " + handlerName);
	}

	class RulesDocTransformer extends DocTransformer {
		private String name;
		private RulesEngine engine;
		private SolrQueryRequest req;

		RulesDocTransformer(String name, RulesEngine engine, SolrQueryRequest req) {
			this.name = name;
			this.engine = engine;
			this.req = req;
		}

		public String getName() {
			return name;
		}

		public void transform(SolrDocument doc, int docid) throws IOException {
			try {
				req.getContext().put("rulesPhase", "docTransformation");
				req.getContext().put("rulesHandle", handle);
				engine.transformDocument(doc, docid);
			} finally {
				req.getContext().remove("rulesPhase");
				req.getContext().remove("rulesHandle");
			}
		}
	}
}
