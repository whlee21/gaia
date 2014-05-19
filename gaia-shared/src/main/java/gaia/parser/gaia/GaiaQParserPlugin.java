package gaia.parser.gaia;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

public class GaiaQParserPlugin extends QParserPlugin {
	public GaiaQueryParserPluginStatics parserStatics = null;

	public void init(NamedList args) {
	}

	public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
		GaiaQParser p = new GaiaQParser(qstr, localParams, params, req);
		p.setGaiaQParserPlugin(this);
		return p;
	}

	public GaiaQueryParserPluginStatics getParserStatics(IndexSchema schema) {
		if (parserStatics == null)
			parserStatics = new GaiaQueryParserPluginStatics(schema);
		else if (parserStatics.schema != schema)
			parserStatics.reInit(schema);
		return parserStatics;
	}
}
