package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSchemaConfig;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class FieldTypeServerResource extends ServerResource implements FieldTypeResource {
	private CoreContainer cores;
	private CollectionManager cm;
	private String collection;
	private EditableSchemaConfig esc;
	private SolrCore solrCore;
	private String typeName;
	private Map<String, Object> type;

	@Inject
	public FieldTypeServerResource(CollectionManager cm, CoreContainer cores) {
		this.cores = cores;
		this.cm = cm;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));
		solrCore = cores.getCore(collection);

		setExisting(null != solrCore);

		if (!isExisting())
			return;

		esc = new EditableSchemaConfig(solrCore, cores.getZkController());
		typeName = ((String) getRequest().getAttributes().get("name"));

		type = esc.getFieldType(typeName);

		setExisting(null != type);
	}

	public void doRelease() {
		if (solrCore != null)
			solrCore.close();
	}

	@Get("json")
	public Map<String, Object> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		return type;
	}

	@Put("json")
	public void update(Map<String, Object> m) throws IOException, SAXException, ParserConfigurationException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}

		for (Map.Entry<String, Object> pair : m.entrySet()) {
			if ("name".equals(pair.getKey())) {
				throw ErrorUtils.statusExp(422, new Error("name", Error.E_FORBIDDEN_KEY,
						"The name of a FieldType can not be modified"));
			}

			if (null == pair.getValue()) {
				type.remove(pair.getKey());
			} else
				type.put(pair.getKey(), pair.getValue());
		}

		try {
			esc.addFieldType(type);
		} catch (IllegalArgumentException userError) {
			throw ErrorUtils.statusExp(422, new Error("", Error.E_EXCEPTION, userError.getMessage()));
		}

		try {
			esc.save();
		} catch (IOException e) {
			throw ErrorUtils.statusExp(e);
		}

		APIUtils.reloadCore(collection, cores);
	}

	@Delete("json")
	public void remove() throws IOException, SAXException, ParserConfigurationException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		if (esc.isFieldTypeInUse(typeName)) {
			throw ErrorUtils.statusExp(Response.Status.CONFLICT, new Error("name", Error.E_INVALID_VALUE,
					"Field Type currently in use: " + typeName));
		}

		esc.removeFieldType(typeName);
		try {
			esc.save();
		} catch (IOException e) {
			throw ErrorUtils.statusExp(e);
		}

		APIUtils.reloadCore(collection, cores);
	}
}
