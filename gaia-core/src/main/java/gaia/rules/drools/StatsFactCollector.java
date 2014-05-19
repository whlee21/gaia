package gaia.rules.drools;

import java.util.Collection;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;

public class StatsFactCollector extends FactCollector {
	protected SolrCore core;

	public void init(NamedList args, SolrCore core) {
		super.init(args, core);
		this.core = core;
	}

	protected void addSolrBasicFacts(ResponseBuilder rb, Collection<Object> facts) {
		super.addSolrBasicFacts(rb, facts);
		facts.add(core.getInfoRegistry());
	}
}
