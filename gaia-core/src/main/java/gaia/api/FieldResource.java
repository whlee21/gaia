package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.utils.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.schema.IndexSchema;
import org.quartz.SchedulerException;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class FieldResource extends AbstractFieldServerResource {
	private FieldAttributeReader fieldAttribReader;

	public String getEntityLabel() {
		return "field";
	}

	@Inject
	public FieldResource(CollectionManager cm, CoreContainer cores) {
		super(cm, cores);
	}

	public void doInit() throws ResourceException {
		super.doInit();

		if (null == solrCore) {
			setExisting(false);
			return;
		}

		fieldAttribReader = new FieldAttributeReader(solrCore, cm.getUpdateChain());

		if (isExisting()) {
			IndexSchema schema = solrCore.getLatestSchema();

			if (!schema.getFields().containsKey(name)) {
				boolean includeDynamic = StringUtils.getBoolean(getQuery().getFirstValue("include_dynamic")).booleanValue();

				setExisting((includeDynamic) && (null != schema.getFieldOrNull(name)));
			}
		}
	}

	public FieldAttributeReader getAttributeReader() {
		return fieldAttribReader;
	}

	@Put("json")
	public void update(Map<String, Object> m) throws ParserConfigurationException, IOException, SAXException,
			SolrServerException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}
		Map<String, Object> existingAttrs = getAttributeReader().getAttributes(name);

		FieldAttribs fieldAttribs = new FieldAttribs(cores, solrCore, cm.getUpdateChain(),
				cm.getGaiaSearchHandler());

		FieldsServerResource.addOrUpdateField(cm, cores, solrCore, collection, name, m,
				FieldsServerResource.VALID_KEYS_UPDATE, AbstractFieldsResource.AddUpdateMode.UPDATE, existingAttrs,
				fieldAttribs);

		fieldAttribs.save();

		APIUtils.reloadCore(collection, cores);

		setStatus(Status.SUCCESS_NO_CONTENT);
		AuditLogger.log("updated field");
	}

	@Delete("json")
	public void remove() throws IOException, ParserConfigurationException, SAXException, SolrServerException,
			SchedulerException {
		if (!isExisting()) {
			 throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		FieldAttribs fieldAttribs = new FieldAttribs(cores, solrCore, cm.getUpdateChain(),
				cm.getGaiaSearchHandler());
		FieldsServerResource.addOrUpdateField(cm, cores, solrCore, collection, name,
				Collections.<String, Object> emptyMap(), Collections.<String> emptySet(),
				AbstractFieldsResource.AddUpdateMode.REMOVE, null, fieldAttribs);

		fieldAttribs.save();

		AuditLogger.log("removed field");

		APIUtils.reloadCore(collection, cores);
		setStatus(Status.SUCCESS_NO_CONTENT);
	}
}
