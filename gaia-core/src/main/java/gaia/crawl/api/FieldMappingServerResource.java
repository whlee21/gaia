package gaia.crawl.api;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.datasource.FieldMapping;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class FieldMappingServerResource extends ServerResource implements FieldMappingResource {

	private static final Logger LOG = LoggerFactory.getLogger(FieldMappingServerResource.class);
	public static final String PART_ATTR = "part";
	public static final String KEY_ATTR = "key";
	private ConnectorManager ccm;
	private String collection;
	private DataSourceId dsId;
	private DataSource ds;

	@Inject
	public FieldMappingServerResource(ConnectorManager ccm) {
		this.ccm = ccm;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));

		String id = (String) getRequest().getAttributes().get("id");
		dsId = new DataSourceId(id);
		try {
			try {
				ds = ccm.getDataSource(dsId);
			} catch (NumberFormatException e) {
				LOG.warn("Invalid dsId '" + dsId + "'");
			} catch (Exception e) {
				throw ErrorUtils.statusExp(422, new Error("getDataSource", Error.E_EXCEPTION, e.toString()));
			}
		} finally {
			setExisting(ds != null);
		}
		setExisting((ds != null) && (ds.getCollection().equals(collection)));
	}

	@Get("json")
	public Object retrieve() throws IOException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Data source id '" + dsId + " not found");
		}
		FieldMapping mapping = ds.getFieldMapping();
		if (mapping == null) {
			return Collections.emptyMap();
		}
		String part = (String) getRequestAttributes().get("part");
		String key = (String) getRequestAttributes().get("key");
		Map<String, Object> res = mapping.toMap();
		if (part == null) {
			return res;
		}
		Object o = res.get(part);
		if (o == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Nonexistent field mapping part '" + part + "'");
		}
		if (!(o instanceof Map)) {
			if ((o instanceof String)) {
				// o = new JacksonRepresentation(o.toString());
			}
			return o;
		}
		res = (Map) o;
		if (key != null) {
			o = res.get(key);
			if ((o instanceof String)) {
				// o = new JacksonRepresentation(o.toString());
			}
			return o;
		}
		return res;
	}

	@Delete("json")
	public void remove() throws IOException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Data source id '" + dsId + " not found");
		}
		String part = (String) getRequestAttributes().get("part");
		String key = (String) getRequestAttributes().get("key");
		if (ds.getFieldMapping() == null) {
			return;
		}
		FieldMapping mapping = ds.getFieldMapping();
		if (part == null) {
			ds.setFieldMapping(new FieldMapping());
			try {
				ccm.updateDataSource(ds);
			} catch (Exception e) {
				throw ErrorUtils.statusExp(e);
			}
		} else {
			if (part.equals("mappings")) {
				if (key == null) {
					mapping.getMappings().clear();
				} else
					mapping.getMappings().remove(key);
			} else if (part.equals("literals")) {
				if (key == null) {
					mapping.getLiterals().clear();
				} else
					mapping.getLiterals().remove(key);
			} else if (part.equals("types")) {
				if (key == null)
					mapping.getTypes().clear();
				else
					mapping.getTypes().remove(key);
			} else if (part.equals("multi_val")) {
				if (key == null)
					mapping.getMultivalued().clear();
				else
					mapping.getMultivalued().remove(key);
			} else
				throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Nonexistent or non-removable field mapping part '"
						+ part + "'");
			try {
				ccm.updateDataSource(ds);
			} catch (Exception e) {
				throw ErrorUtils.statusExp(e);
			}
		}
	}

	@Put("json")
	public void update(Map<String, Object> args) throws IOException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Data source id '" + dsId + " not found");
		}
		if (args == null) {
			return;
		}

		String part = (String) getRequestAttributes().get("part");
		String key = (String) getRequestAttributes().get("key");
		if ((key != null) || (part != null)) {
			throw ErrorUtils.statusExp(422, "Expected JSON map of updated parts/keys/values, instead got '" + args + "'");
		}
		FieldMapping mapping = new FieldMapping();
		FieldMapping.fromMap(mapping, args);
		ds.setFieldMapping(mapping);
		try {
			ccm.updateDataSource(ds);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}
}
