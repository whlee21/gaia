package gaia.update;

import gaia.Defaults;
import gaia.Settings;

import java.io.IOException;

import org.apache.solr.common.SolrException;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

public class BlockUpdatesUpdateProcessorFactory extends UpdateRequestProcessorFactory {
	public UpdateRequestProcessor getInstance(SolrQueryRequest req, SolrQueryResponse rsp, UpdateRequestProcessor next) {
		return new BlockUpdatesUpdateProcessor(req, rsp, this, next);
	}

	class BlockUpdatesUpdateProcessor extends UpdateRequestProcessor {
		private Settings settings = (Settings) Defaults.injector.getInstance(Settings.class);

		public BlockUpdatesUpdateProcessor(SolrQueryRequest req, SolrQueryResponse rsp,
				BlockUpdatesUpdateProcessorFactory factory, UpdateRequestProcessor next) {
			super(next);
		}

		public void processAdd(AddUpdateCommand cmd) throws IOException {
			if (settings.getBoolean(Settings.Group.control, "blockUpdates")) {
				throw new SolrException(SolrException.ErrorCode.FORBIDDEN,
						"Doc failed: This index has been blocked from updates");
			}

			if (next != null)
				next.processAdd(cmd);
		}
	}
}
