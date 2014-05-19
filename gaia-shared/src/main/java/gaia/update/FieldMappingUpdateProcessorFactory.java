package gaia.update;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaAware;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.crawl.datasource.FieldMapping;

public class FieldMappingUpdateProcessorFactory extends UpdateRequestProcessorFactory implements SchemaAware {
	private static final Logger LOG = LoggerFactory.getLogger(FieldMappingUpdateProcessorFactory.class);

	private Map<String, FieldMapping> mappings = Collections.synchronizedMap(new HashMap<String, FieldMapping>());
	private IndexSchema currentSchema = null;

	public UpdateRequestProcessor getInstance(SolrQueryRequest req, SolrQueryResponse rsp, UpdateRequestProcessor next) {
		SolrParams params = req.getParams();
		String dsId = params.get("fm.ds");
		if (dsId == null) {
			return next;
		}
		FieldMapping fmap = mappings.get(dsId);
		if (fmap == null) {
			if (dsId.equals("-1")) {
				if (mappings.size() == 1)
					fmap = mappings.entrySet().iterator().next().getValue();
				else
					throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
							"parameter fm.ds=-1 is valid only when a single mapping exists, current mappings: " + mappings.keySet());
			} else {
				throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "parameter fm.ds(=" + dsId
						+ ") exists but does not match any existing mapping");
			}
		}
		return new FieldMappingProcessor(req.getCore(), fmap, currentSchema, next);
	}

	public Map<String, FieldMapping> getMappings() {
		return mappings;
	}

	public void inform(IndexSchema schema) {
		currentSchema = schema;
	}
}
