package gaia.rules;

import java.io.Closeable;
import java.io.IOException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.update.AddUpdateCommand;

public abstract class RulesEngine implements Closeable {
	protected NamedList initArgs;
	protected SolrCore core;
	protected String engineName;
	protected boolean hasRules;

	public void init(String engineName, NamedList args, SolrCore core) throws Exception {
		this.engineName = engineName;
		this.initArgs = args;
		this.core = core;
		RulesEngineCloseHook hook = new RulesEngineCloseHook(this);
		core.addCloseHook(hook);
	}

	public void close() throws IOException {
	}

	public abstract void findLandingPage(ResponseBuilder paramResponseBuilder);

	public abstract void prepareSearch(ResponseBuilder paramResponseBuilder);

	public abstract void postSearch(ResponseBuilder paramResponseBuilder);

	public abstract void transformDocument(SolrDocument paramSolrDocument, int paramInt);

	public abstract void prepareDocument(AddUpdateCommand paramAddUpdateCommand);

	public boolean hasRules() {
		return hasRules;
	}
}
