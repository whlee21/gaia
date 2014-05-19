package gaia.crawl.solrxml;

import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;
import gaia.crawl.DataSourceFactoryException;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceSpec;

import java.util.Map;

public class SolrXmlDataSourceFactory extends DataSourceFactory {
	public SolrXmlDataSourceFactory(CrawlerController cc) {
		super(cc);
		types.put(DataSourceSpec.Type.solrxml.toString(), new SolrXMLSpec());
	}

	public DataSource create(Map<String, Object> m, String collection) throws DataSourceFactoryException {
		DataSource ds = super.create(m, collection);

		if (ds.getBoolean("include_datasource_metadata", true)) {
			ds.getFieldMapping().defineMapping("batch_id", "batch_id");
		}

		if (ds.getBoolean("include_datasource_metadata"))
			ds.getFieldMapping().setAddGaiaSearchFields(true);
		else {
			ds.getFieldMapping().setAddGaiaSearchFields(false);
		}

		return ds;
	}
}
