package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.ComponentInUseException;
import gaia.admin.editor.EditableSolrConfig;
import gaia.security.ad.ADACLTagProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.quartz.SchedulerException;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class ACLFilteringServerResource extends ServerResource implements ACLFilteringResource {
	private static final Object INSTANCE_NAME = "name";
	public static final String CONFIG_GET_LIST = "config_get_list";
	private final CollectionManager cm;
	private String collection;
	private final CoreContainer cores;
	private SolrCore core;
	private String instanceName;

	@Inject
	public ACLFilteringServerResource(CollectionManager cm, CoreContainer cores) {
		this.cm = cm;
		this.cores = cores;
	}

	public void doInit() {
		collection = ((String) getRequestAttributes().get("coll_name"));
		instanceName = ((String) getRequestAttributes().get(INSTANCE_NAME));

		core = cores.getCore(collection);
		setExisting(core != null);
	}

	public void doRelease() {
		if (core != null)
			core.close();
	}

	@Get("json")
	public Map<String, Object> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());

		if (instanceName != null) {
			Map<String, Object> config = ecc.getACLComponentConfig(instanceName);
			if (config == null) {
				throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Configuration does not exist: " + instanceName);
			}

			return config;
		}

		List<String> aclNames = ecc.getACLComponentNames();
		Map<String, Object> configs = new HashMap<String, Object>();
		for (String name : aclNames) {
			configs.put(name, ecc.getACLComponentConfig(name));
		}
		return configs;
	}

	@Delete
	public void remove() throws DOMException, XPathExpressionException, IOException, ParserConfigurationException,
			SAXException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());

		Map<String, Object> config = ecc.getACLComponentConfig(instanceName);
		if (config == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Configuration does not exist: " + instanceName);
		}

		try {
			ecc.deleteACLComponentConfig(instanceName);
			ecc.save();
			APIUtils.reloadCore(collection, cores);
			setStatus(Status.SUCCESS_NO_CONTENT);
		} catch (ComponentInUseException e) {
			// setStatus(Response.Status.CONFLICT,
			// "Cannot delete a config that is currently in use.");
		}
	}

	@Post("json")
	public void create(Map<String, Object> m) throws DOMException, XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, SchedulerException {
		EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());

		Map<String, Object> config = ecc.getACLComponentConfig(instanceName);
		if (config != null) {
			throw ErrorUtils.statusExp(Response.Status.CONFLICT, "configuration already exists: " + instanceName);
		}

		validate(m);
		ecc.setACLComponentConfig(instanceName, m);
		ecc.save();
		APIUtils.reloadCore(collection, cores);
		setStatus(Status.SUCCESS_CREATED);
	}

	@Put("json")
	public void update(Map<String, Object> m) throws DOMException, XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, SchedulerException {
		validate(m);
		EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());

		Map<String, Object> config = ecc.getACLComponentConfig(instanceName);
		if (config == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Configuration not found: " + instanceName);
		}

		ecc.setACLComponentConfig(instanceName, m);
		ecc.save();
		APIUtils.reloadCore(collection, cores);
		setStatus(Status.SUCCESS_NO_CONTENT);
	}

	private void validate(Map<String, Object> m) {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		if (instanceName == null) {
			throw ErrorUtils.statusExp(417, "Name not specified");
		}

		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}

		Map<String, Object> config = (Map) m.get("provider.config");

		String providerClass = m.get("provider.class").toString();

		if (config != null) {
			Object url = config.get("java.naming.provider.url");

			if (ADACLTagProvider.class.getName().equals(providerClass)) {
				if ((url == null) || (url.toString().trim().length() == 0)) {
					throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("java.naming.provider.url",
							"java.naming.provider.url.no_url", "URL must be specified"));
				}

				try {
					URI u = new URI(url.toString());
					if ((!"ldap".equals(u.getScheme())) && (!"ldaps".equals(u.getScheme()))) {
						throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("java.naming.provider.url",
								"java.naming.provider.url.non_ldap_ldaps_url", "protocol must be ldap or ldaps: '" + url + "'"));
					}

				} catch (URISyntaxException e) {
					throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("java.naming.provider.url",
							"java.naming.provider.url.illegal:url", "Value is not legal url: '" + url + "' " + e.getMessage()));
				}

				Object principal = config.get("java.naming.security.principal");
				if (principal == null) {
					throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("java.naming.security.principal",
							"java.naming.security.principal.no_value", "Principal must be specified"));
				}

				if (!principal.toString().contains("@")) {
					throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST,
							new Error("java.naming.security.principal", "java.naming.security.principal.invalid_value",
									"Illegal username, no '@' found in: " + principal.toString()));
				}

				Object credentials = config.get("java.naming.security.credentials");
				if (credentials == null)
					throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("java.naming.security.credentials",
							"java.naming.security.credentials.no_value", "Credentials must be specified"));
			}
		}
	}
}
