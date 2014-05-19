package gaia.rules;

import java.io.IOException;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

class RulesUpdateProcessor extends UpdateRequestProcessor {
	RulesEngine engine;

	RulesUpdateProcessor(UpdateRequestProcessor next, RulesEngine engine) {
		super(next);
		this.engine = engine;
	}

	public void processAdd(AddUpdateCommand cmd) throws IOException {
		engine.prepareDocument(cmd);
		super.processAdd(cmd);
	}
}
