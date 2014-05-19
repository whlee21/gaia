package gaia.search.server.api.services;

import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.ScheduledSolrCommand;
import gaia.admin.collection.SolrCmdHistory;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class ActivityHistoryService extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(ActivityHistoryService.class);
	private static transient Logger LOG = LoggerFactory.getLogger(ActivityHistoryService.class);

	private String activityId;
	
	private static final Object ID = "id";
	private String id;
	private ScheduledSolrCommand cmd;
	private Collection<ScheduledSolrCommand> cmds;
	private CollectionManager cm;
	private String collection;
	private SolrCore core;
	private SolrCmdHistory cmdHistory;
	private volatile boolean existing = true;

	public ActivityHistoryService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName,
			String activityId, 	CollectionManager cm, CoreContainer cores, SolrCmdHistory cmdHistory) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.activityId = activityId;
		this.cmdHistory = cmdHistory;
		core = cores.getCore(collection);
		
		setExisting(cm.getCollectionNames().contains(collection));
		try {
			id = activityId;
			if (id.equals("all")) {
				cmds = cm.getAllScheduledSolrCommands(collection);				
			}else {
				cmd = cm.getScheduledSolrCommand(collection, id);
				if (cmd != null) {
					cmds = new ArrayList<ScheduledSolrCommand>();
					cmds.add(cmd);					
				}else {
					throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("Activity " + id + " not found in collection "
							+ collection));
				}
			}
		} catch (NumberFormatException e) {
		} finally {
			setExisting((cmds != null) && (isExisting()));
		}
	}

	public void setExisting(boolean exists) {
		this.existing = exists;
	}
	
	public boolean isExisting() {
		return this.existing;
	}

	@GET
	@Produces("text/plain")
	public Response getActivityHistory(@Context HttpHeaders headers, @Context UriInfo ui) throws SolrServerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		List<Map<String, Object>> activityStatus =  getHistory(cmds, cmdHistory);
		
		return buildResponse(Response.Status.OK, activityStatus);
	}

	static List<Map<String, Object>> getHistory(Collection<ScheduledSolrCommand> cmd, SolrCmdHistory cmdHistory)
			throws SolrServerException {
		List<Map<String, Object>> allHistory = new ArrayList<Map<String, Object>>();
		
		for (ScheduledSolrCommand cmdObj : cmd) {
      System.out.println(cmdObj);
      List<Map<String, Object>> history = cmdHistory.getHistory(cmdObj.getId());
      if (history != null) {
      if (history.size() > 0) {
      	 for(Map<String, Object> hisobj : history) {
      	  	allHistory.add(hisobj);
      	 }
       }
      
      }
   }
		
		return allHistory;
	}

	// protected ResourceInstance createActivityStatusResource(String
	// collectionName, String activityId) {
	// Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
	// mapIds.put(Resource.Type.Collection, collectionName);
	// mapIds.put(Resource.Type.ActivityHistory, activityId);
	// return createResource(Resource.Type.ActivityHistory, mapIds);
	// }
}
