package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSchemaConfig;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.quartz.SchedulerException;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class FieldTypesServerResource extends ServerResource implements FieldTypesResource {
	public static final String FIELD_TYPE_NAME_PROP = "name";
	public static final String FIELD_TYPE_CLASS_PROP = "class";
	private CoreContainer cores;
	private CollectionManager cm;
	private String collection;
	private EditableSchemaConfig esc;
	private SolrCore solrCore;

	@Inject
	public FieldTypesServerResource(CollectionManager cm, CoreContainer cores) {
		this.cores = cores;
		this.cm = cm;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));
		solrCore = cores.getCore(collection);
		setExisting(solrCore != null);

		if (!isExisting())
			return;

		esc = new EditableSchemaConfig(solrCore, cores.getZkController());
	}

	public void doRelease() {
		if (solrCore != null)
			solrCore.close();
	}

	@Get("json")
	public List<Map<String, Object>> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		return esc.getFieldTypes();
	}

	@Post("json")
	public Map<String, Object> add(Map<String, Object> m) throws IOException, ParserConfigurationException, SAXException,
			SolrServerException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		if (!m.containsKey("name")) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_MISSING_VALUE, "name must be specified"));
		}

		if (!m.containsKey("class")) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_MISSING_VALUE, "class must be specified"));
		}

		String name = gaia.utils.StringUtils.getString(m.get("name"));
		String clazz = gaia.utils.StringUtils.getString(m.get("class"));

		if (org.apache.commons.lang.StringUtils.isBlank(name)) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_EMPTY_VALUE, "A non-blank name must be specified"));
		}

		if (org.apache.commons.lang.StringUtils.isBlank(clazz)) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_EMPTY_VALUE, "A non-blank class must be specified"));
		}

		if (null != esc.getFieldType(name)) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_EXISTS,
					"An explicit field type already exists with the name:" + name));
		}

		try {
			esc.addFieldType(m);
		} catch (IllegalArgumentException userError) {
			throw ErrorUtils.statusExp(422, new Error("", Error.E_EXCEPTION, userError.getMessage()));
		}

		try {
			esc.save();
		} catch (IOException e) {
			throw ErrorUtils.statusExp(e);
		}

		APIUtils.reloadCore(collection, cores);

		Map<String, Object> data = esc.getFieldType(name);

		setStatus(Status.SUCCESS_CREATED);
		getResponse().setLocationRef("fieldtypes/" + URLEncoder.encode(name, "UTF-8"));
		return data;
	}
}
