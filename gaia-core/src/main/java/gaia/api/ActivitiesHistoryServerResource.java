package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.ScheduledSolrCommand;
import gaia.admin.collection.SolrCmdHistory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.CoreContainer;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;

public class ActivitiesHistoryServerResource extends ServerResource implements ActivitiesHistoryResource {
	private CollectionManager cm;
	private String collection;
	private CoreContainer cores;
	private SolrCmdHistory cmdHistory;

	@Inject
	public ActivitiesHistoryServerResource(CollectionManager cm, CoreContainer cores, SolrCmdHistory cmdHistory) {
		this.cm = cm;
		this.cores = cores;
		this.cmdHistory = cmdHistory;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));
		setExisting(APIUtils.coreExists(cores, collection));
	}

	@Get("json")
	public List<Map<String, Object>> retrieve() throws SolrServerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		List<Map<String, Object>> histories = new ArrayList<Map<String, Object>>();

		for (ScheduledSolrCommand cmd : cm.getAllScheduledSolrCommands(collection)) {
			Map<String, Object> entry = new HashMap<String, Object>();
			List<Map<String, Object>> history = ActivityHistoryServerResource.getHistory(cmd, cmdHistory);
			entry.put("id", cmd.getId());
			entry.put("history", history);
			histories.add(entry);
		}
		return histories;
	}
}
