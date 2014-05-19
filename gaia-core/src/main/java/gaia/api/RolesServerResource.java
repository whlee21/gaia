package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSolrConfig;
import gaia.admin.roles.Role;
import gaia.handler.RoleBasedFilterComponent;
import gaia.utils.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SearchComponent;
import org.quartz.SchedulerException;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class RolesServerResource extends ServerResource implements RolesResource {
	static final String GROUPS = "groups";
	static final String USERS = "users";
	private static final String FILTERBYROLE_COMPONENT = "filterbyrole";
	static final String ROLE_NAME = "name";
	static final String FILTERS = "filters";
	private CollectionManager cm;
	private String collection;
	private CoreContainer cores;
	private SolrCore core;
	static final Set<String> VALID_KEYS = new HashSet<String>();

	@Inject
	public RolesServerResource(CollectionManager cm, CoreContainer cores) {
		this.cm = cm;
		this.cores = cores;
	}

	public void doInit() {
		collection = ((String) getRequestAttributes().get("coll_name"));
		core = cores.getCore(collection);
		setExisting(core != null);
	}

	public void doRelease() {
		if (core != null)
			core.close();
	}

	@Post("json")
	public Map<String, Object> add(Map<String, Object> m) throws DOMException, XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}

		List<Error> errors = new ArrayList<Error>();

		errors.addAll(validate(m, VALID_KEYS));

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}

		String r = (String) m.get("name");

		Role role = cm.getRole(collection, r);
		if (role != null) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_EXISTS, "Role already exists: " + role.getName()));
		}

		List<String> users = StringUtils.getList(String.class, m.get("users"));
		List<String> groups = StringUtils.getList(String.class, m.get("groups"));
		List<String> filters = StringUtils.getList(String.class, m.get("filters"));

		role = addUpdateRole(r, cores, collection, core, cm.getUpdateChain(), users, groups, filters);

		cm.addRole(collection, role);
		AuditLogger.log("added Role");
		setStatus(Status.SUCCESS_CREATED);
		getResponse().setLocationRef("roles/" + URLEncoder.encode(r, "UTF-8"));

		if (filters != null) {
			APIUtils.reloadCore(collection, cores);
		}
		SolrCore lastestCore = cores.getCore(collection);
		Map<String, Object> roles;
		try {
			roles = fillRole(getFilters(lastestCore), role);
		} finally {
			lastestCore.close();
		}
		return roles;
	}

	static Role addUpdateRole(String r, CoreContainer cores, String collection, SolrCore core, String updateChain,
			List<String> users, List<String> groups, List<String> filters) throws XPathExpressionException, IOException {
		Role role = new Role(r);
		if (users != null) {
			role.setUsers(users);
		}
		if (groups != null) {
			role.setGroups(groups);
		}

		if (filters != null) {
			Map<String, String[]> f = getFilters(core);
			f.put(r, filters.toArray(new String[0]));

			EditableSolrConfig ecc = new EditableSolrConfig(core, updateChain, cores.getZkController());
			ecc.updateRoleFilterComponent(f);
			ecc.save();
		}
		return role;
	}

	@Get("json")
	public List<Map<String, Object>> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		List<Map<String, Object>> roleList = new ArrayList<Map<String, Object>>();
		Collection<Role> roles = cm.getRoles(collection);
		Map<String, String[]> filters = getFilters(core);

		for (Role role : roles) {
			Map<String, Object> r = fillRole(filters, role);
			roleList.add(r);
		}

		return roleList;
	}

	static Map<String, Object> fillRole(Map<String, String[]> filters, Role role) {
		Map<String, Object> r = new HashMap<String, Object>();
		r.put("name", role.getName());
		r.put("users", role.getUsers());
		r.put("groups", role.getGroups());

		List<String> filterList = getFilters(filters, role.getName());

		r.put("filters", filterList);
		return r;
	}

	static List<String> getFilters(Map<String, String[]> filters, String role) {
		String[] f = (String[]) filters.get(role);
		List<String> filterList;
		if (f == null)
			filterList = Collections.emptyList();
		else {
			filterList = Arrays.asList(f);
		}
		return filterList;
	}

	static Map<String, String[]> getFilters(SolrCore core) {
		Map<String, String[]> f = new HashMap<String, String[]>();

		Map<String, SearchComponent> components = core.getSearchComponents();
		if (components.containsKey(FILTERBYROLE_COMPONENT)) {
			SearchComponent component = (SearchComponent) components.get(FILTERBYROLE_COMPONENT);
			if (component != null) {
				RoleBasedFilterComponent filterByRoleComponent = (RoleBasedFilterComponent) component;
				Map<String, String[]> filters = filterByRoleComponent.getFilters();
				f.putAll(filters);
			}

		} else {
			f.put("DEFAULT", new String[] { "*:*" });
		}

		return f;
	}

	static List<Error> validate(Map<String, Object> m, Set<String> validKeys) {
		List<Error> errors = new ArrayList<Error>();

		String name = (String) m.get("name");
		if (name != null) {
			Matcher matcher = APIUtils.ALPHANUM.matcher(name);
			if (!matcher.matches()) {
				errors.add(new Error("name", Error.E_INVALID_VALUE, "name must consist of only A-Z a-z 0-9 - _"));
			}
		}

		Set<String> keys = m.keySet();
		for (String key : keys) {
			if (!validKeys.contains(key)) {
				errors.add(new Error(key, Error.E_FORBIDDEN_KEY, "Unknown or dissallowed key found:" + key));
			}
		}

		return errors;
	}

	static {
		VALID_KEYS.add("name");
		VALID_KEYS.add("users");
		VALID_KEYS.add("groups");
		VALID_KEYS.add("filters");
	}
}
