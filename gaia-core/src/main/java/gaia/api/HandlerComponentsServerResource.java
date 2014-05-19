package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSolrConfig;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.quartz.SchedulerException;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class HandlerComponentsServerResource extends ServerResource implements HandlerComponentsResource {
	private static final Object INSTANCE_NAME = "name";
	private final CollectionManager cm;
	private String collection;
	private final CoreContainer cores;
	private SolrCore core;
	private String listName;
	private String listKey;
	private String handlerName;
	private static final Map<String, String> INSTANCE_ALIASES;// =
																														// Collections.unmodifiableMap(tmp);

	@Inject
	public HandlerComponentsServerResource(CollectionManager cm, CoreContainer cores) {
		this.cm = cm;
		this.cores = cores;
	}

	public void doInit() {
		collection = ((String) getRequestAttributes().get("coll_name"));
		listName = ((String) getRequestAttributes().get(INSTANCE_NAME));

		listKey = (INSTANCE_ALIASES.containsKey(listName) ? (String) INSTANCE_ALIASES.get(listName) : listName);

		core = cores.getCore(collection);
		setExisting(core != null);
		handlerName = getQuery().getFirstValue("handlerName", null);
	}

	public void doRelease() {
		if (core != null)
			core.close();
	}

	@Get("json")
	public String[] retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());

		if (ecc.getRequestHandlerNode(handlerName) == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Search components configuration does not exist: "
					+ listName);
		}

		String[] list = ecc.getArrayFromRequestHandler(handlerName, listKey);

		return list;
	}

	@Put("json")
	public void update(String[] values) throws DOMException, XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());

		ecc.setValuesToRequestHandlerArray(handlerName, listKey, values, true);

		ecc.save();
		APIUtils.reloadCore(collection, cores);
	}

	static {
		Map<String, String> tmp = new HashMap<String, String>();
		tmp.put("all", "components");
		tmp.put("first", "first-components");
		tmp.put("last", "last-components");
		INSTANCE_ALIASES = Collections.unmodifiableMap(tmp);
	}
}
