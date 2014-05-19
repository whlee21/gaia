package gaia.api;

import gaia.admin.collection.CollectionManager;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.CoreContainer;
import org.quartz.SchedulerException;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class DynamicFieldResource extends AbstractFieldServerResource {
	private DynamicFieldAttributeReader attribReader;

	public String getEntityLabel() {
		return "dynamic field";
	}

	@Inject
	public DynamicFieldResource(CollectionManager cm, CoreContainer cores) {
		super(cm, cores);
	}

	public void doInit() throws ResourceException {
		super.doInit();

		if (null != solrCore)
			attribReader = new DynamicFieldAttributeReader(solrCore);
	}

	public DynamicFieldAttributeReader getAttributeReader() {
		return attribReader;
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

		DynamicFieldAttribs fieldAttribs = new DynamicFieldAttribs(cores, solrCore);

		DynamicFieldsServerResource.addOrUpdateDynamicField(cm, cores, solrCore, collection, name,
				m, DynamicFieldsServerResource.VALID_KEYS_UPDATE, AbstractFieldsResource.AddUpdateMode.UPDATE, existingAttrs,
				fieldAttribs);

		fieldAttribs.save();

		APIUtils.reloadCore(collection, cores);

		setStatus(Status.SUCCESS_NO_CONTENT);
		AuditLogger.log("updated dynamicField");
	}

	@Delete("json")
	public void remove() throws IOException, ParserConfigurationException, SAXException, SolrServerException,
			SchedulerException {
		if (!isExisting()) {
			 throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		DynamicFieldAttribs fieldAttribs = new DynamicFieldAttribs(cores, solrCore);

		DynamicFieldsServerResource.addOrUpdateDynamicField(cm, cores, solrCore, collection, name,
				Collections.<String, Object> emptyMap(), Collections.<String> emptySet(),
				AbstractFieldsResource.AddUpdateMode.REMOVE, null, fieldAttribs);

		fieldAttribs.save();

		AuditLogger.log("removed dynamicField");

		APIUtils.reloadCore(collection, cores);
		setStatus(Status.SUCCESS_NO_CONTENT);
	}
}
