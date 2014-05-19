package gaia.rules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesEngineManagerHandler extends RequestHandlerBase implements SolrCoreAware {
	private static transient Logger LOG = LoggerFactory.getLogger(RulesEngineManagerHandler.class);
	public static final String ENGINES = "engines";
	public static final String ENGINE = "engine";
	public static final String ENGINE_CLASS_NAME = "class";
	public static final String NAME = "name";
	protected SolrParams params;
	protected Map<String, RulesEngine> engines = new HashMap<String, RulesEngine>();

	public void init(NamedList args) {
		super.init(args);
		params = SolrParams.toSolrParams(args);
	}

	public void inform(SolrCore core) {
		NamedList enginesArgs = (NamedList) initArgs.get(ENGINES);
		if (enginesArgs != null) {
			List<NamedList> all = enginesArgs.getAll(ENGINE);
			for (NamedList engineArgs : all) {
				String name = (String) engineArgs.get(NAME);
				String className = engineArgs.get(ENGINE_CLASS_NAME).toString();
				Class<?> engineClass = core.getResourceLoader().findClass(className, RulesEngine.class);
				try {
					LOG.info("Loading " + name + " rules engine as class: " + className);
					RulesEngine engine = (RulesEngine) engineClass.asSubclass(RulesEngine.class).newInstance();
					engine.init(name, engineArgs, core);
					if (engine.hasRules()) {
						engines.put(name, engine);
					} else {
						LOG.info("Nor rules found for engine " + name + ". ");
						engines.put(name, new NoopRulesEngine());
					}
				} catch (InstantiationException e) {
					LOG.error("Exception", e);
					throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unable to instantiate rules engine: "
							+ className, e);
				} catch (IllegalAccessException e) {
					LOG.error("Exception", e);
					throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unable to instantiate rules engine: "
							+ className, e);
				} catch (Exception e) {
					LOG.error("Exception", e);
					throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unable to initialize rules engine: "
							+ className, e);
				}
			}
		}
	}

	public RulesEngine getEngine(String name) {
		return engines.get(name);
	}

	public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
		SolrParams params = req.getParams();
		String command = params.get("rulesCmd");
	}

	public String getVersion() {
		return "0.1";
	}

	public String getDescription() {
		return "Initializes and controls the RulesEngine and provides various other functionality related to it";
	}

	public SolrInfoMBean.Category getCategory() {
		return SolrInfoMBean.Category.OTHER;
	}

	public String getSource() {
		return "RulesRequestHandler.java";
	}

	public NamedList getStatistics() {
		NamedList result = null;
		return result;
	}
}
