package gaia.parser.gaia;

import org.apache.solr.schema.IndexSchema;

public class GaiaQueryParserPluginStatics {
	public IndexSchema schema = null;

	public void reInit(IndexSchema sch) {
		schema = sch;
	}

	public GaiaQueryParserPluginStatics(IndexSchema sch) {
		reInit(sch);
	}

	public void setSchema(IndexSchema sch) {
		if (schema != sch)
			reInit(sch);
	}
}
