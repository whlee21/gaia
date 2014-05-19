package gaia.crawl;

import java.util.List;

import org.apache.solr.common.SolrInputDocument;

import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.io.Content;

public abstract class ParserController {
	protected CrawlState state;
	protected UpdateController updateController;
	protected FieldMapping fieldMapping;

	public ParserController(UpdateController updateController) {
		this.updateController = updateController;
	}

	public void init(CrawlState state) {
		this.state = state;
		fieldMapping = (state.ds != null ? state.ds.getFieldMapping() : null);
	}

	public abstract List<SolrInputDocument> parse(Content paramContent) throws Exception;
}
