package gaia.crawl.dih;

import gaia.Defaults;
import gaia.crawl.CrawlId;
import gaia.crawl.CrawlerControllerRegistry;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardingUpdateProcessorFactory extends UpdateRequestProcessorFactory {
	private static final Logger LOG = LoggerFactory.getLogger(ForwardingUpdateProcessorFactory.class);
	public static final String DATASOURCE = "ds";

	public UpdateRequestProcessor getInstance(SolrQueryRequest req, SolrQueryResponse rsp, UpdateRequestProcessor next) {
		SolrParams params = req.getParams();
		String dsId = params.get(DATASOURCE);
		if (dsId != null) {
			CrawlerControllerRegistry ccr = (CrawlerControllerRegistry) Defaults.injector
					.getInstance(CrawlerControllerRegistry.class);
			DIHCrawlerController controller = (DIHCrawlerController) ccr.get(DIHCrawlerController.class.getName());
			DIHCrawlState state = (DIHCrawlState) controller.getCrawlState(new CrawlId(dsId));
			if (state == null) {
				LOG.info("Could not find {} data source running - ingoring all documents.", dsId);
			}
			return new ForwardingUpdateProcessor(state, next);
		}
		LOG.info("No {} request parameter is passed - ingoring all documents.", DATASOURCE);
		return new ForwardingUpdateProcessor(null, next);
	}
}
