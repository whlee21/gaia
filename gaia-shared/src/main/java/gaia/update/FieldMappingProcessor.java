package gaia.update;

import java.io.IOException;

import org.apache.solr.common.SolrInputField;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.FieldMappingUtil;

class FieldMappingProcessor extends UpdateRequestProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(FieldMappingProcessor.class);
	private static final String FIELD_MAPPING = "attr_field_mapping";
	FieldMapping fmap;
	IndexSchema schema;
	SolrCore core;

	public FieldMappingProcessor(SolrCore core, FieldMapping fmap, IndexSchema schema, UpdateRequestProcessor next) {
		super(next);
		this.core = core;
		this.fmap = fmap;
	}

	public void processAdd(AddUpdateCommand cmd) throws IOException {
		SolrInputField fld = cmd.solrDoc.getField(FIELD_MAPPING);
		if (fld != null) {
			LOG.debug("field mapping already applied: " + cmd.solrDoc);
			return;
		}
		FieldMappingUtil.mapFields(cmd.solrDoc, fmap, schema);
		FieldMappingUtil.normalizeFields(cmd.solrDoc, fmap);

		cmd.solrDoc.addField(FIELD_MAPPING, "true");

		super.processAdd(cmd);
	}
}
