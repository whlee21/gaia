package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSolrConfig;
import gaia.admin.roles.Role;
import gaia.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.quartz.SchedulerException;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class RoleServerResource extends ServerResource implements RoleResource {
	private CollectionManager cm;
	private String collection;
	private String roleName;
	private CoreContainer cores;
	private SolrCore core;

	@Inject
	public RoleServerResource(CollectionManager cm, CoreContainer cores) {
		this.cm = cm;
		this.cores = cores;
	}

	public void doInit() {
		collection = ((String) getRequestAttributes().get("coll_name"));
		roleName = ((String) getRequestAttributes().get("name"));
		core = cores.getCore(collection);
		setExisting(core != null);
	}

	public void doRelease() {
		if (core != null)
			core.close();
	}

	@Get("json")
	public Map<String, ?> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		Map<String, String[]> filters = RolesServerResource.getFilters(core);

		Role role = cm.getRole(collection, roleName);
		if (role == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Role does not exist: " + roleName);
		}

		Map<String, Object> r = RolesServerResource.fillRole(filters, role);

		return r;
	}

	@Delete
	public void remove() throws DOMException, XPathExpressionException, IOException, ParserConfigurationException,
			SAXException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		Role role = cm.getRole(collection, roleName);
		if (role == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Role does not exists" + roleName);
		}

		cm.removeRole(collection, roleName);
		Map<String, String[]> filters = RolesServerResource.getFilters(core);
		if (filters.keySet().contains(roleName)) {
			filters.remove(roleName);
			EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());
			ecc.updateRoleFilterComponent(filters);
			ecc.save();
			APIUtils.reloadCore(collection, cores);
		}

		setStatus(Status.SUCCESS_NO_CONTENT);
	}

	@Put("json")
	public void update(Map<String, Object> m) throws DOMException, XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}

		List<Error> errors = new ArrayList<Error>();

		Set<String> validKeys = new HashSet<String>(RolesServerResource.VALID_KEYS.size());
		validKeys.addAll(RolesServerResource.VALID_KEYS);
		validKeys.remove("name");
		errors.addAll(RolesServerResource.validate(m, validKeys));

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}

		Role role = cm.getRole(collection, roleName);
		if (role == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, new Error(roleName, Error.E_INVALID_VALUE,
					"Role does not exist:" + roleName));
		}

		List<String> users = StringUtils.getList(String.class, m.get("users"));
		List<String> groups = StringUtils.getList(String.class, m.get("groups"));
		List<String> filters = StringUtils.getList(String.class, m.get("filters"));

		if (users == null) {
			users = role.getUsers();
		}

		if (groups == null) {
			groups = role.getGroups();
		}

		boolean reloadCore = true;
		if (filters == null) {
			reloadCore = false;
			filters = RolesServerResource.getFilters(RolesServerResource.getFilters(core), roleName);
		}

		role = RolesServerResource.addUpdateRole(roleName, cores, collection, core, cm.getUpdateChain(), users, groups,
				filters);

		cm.addRole(collection, role);
		setStatus(Status.SUCCESS_NO_CONTENT);
		if (reloadCore) {
			APIUtils.reloadCore(collection, cores);
		}
		AuditLogger.log("updated role");
	}
}
