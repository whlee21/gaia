package gaia.rules;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.update.AddUpdateCommand;

public class NoopRulesEngine extends RulesEngine {
	public void findLandingPage(ResponseBuilder rb) {
	}

	public void prepareSearch(ResponseBuilder rb) {
	}

	public void postSearch(ResponseBuilder rb) {
	}

	public void transformDocument(SolrDocument doc, int docId) {
	}

	public void prepareDocument(AddUpdateCommand cmd) {
	}

	public boolean hasRules() {
		return false;
	}
}
