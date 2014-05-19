package gaia.search.server.api.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gaia.admin.collection.AdminScheduler;
import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.ScheduledSolrCommand;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.solr.client.solrj.SolrServerException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class ActivityStatusService  extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(ActivityStatusService.class);
	private static final Logger LOG = LoggerFactory.getLogger(ActivityStatusService.class);
	private String activityId;
	private CollectionManager cm;
	private AdminScheduler adminScheduler;
	private String collection;
	private volatile boolean existing = true;

	public ActivityStatusService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName, String activityId,
			CollectionManager cm, AdminScheduler adminScheduler) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.activityId = activityId;
		this.cm = cm;
		this.adminScheduler = adminScheduler;
		
		setExisting(cm.getCollectionNames().contains(collection));
	}
	

	public void setExisting(boolean exists) {
		this.existing = exists;
	}
	
	public boolean isExisting() {
		return this.existing;
	}

	@GET
	@Produces("text/plain")
	public Response getActivityStatus(@Context HttpHeaders headers, @Context UriInfo ui) throws SolrServerException, SchedulerException, ParseException {
		LOG.debug("hhokyung getActivityStatus() (collectionName, activityId) = (" + collection+ ", " + activityId
				+ ")");
		
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		List<Map<String, Object>> activitiesStatus = new ArrayList<Map<String, Object>>();

		for (ScheduledSolrCommand cmd : cm.getAllScheduledSolrCommands(collection)) {
			Map<String, Object> temp = adminScheduler.getStatus(cmd);
			if (temp.get("id").equals(activityId)) 
				activitiesStatus.add(adminScheduler.getStatus(cmd));
		}
		return buildResponse(Response.Status.OK, activitiesStatus);
	}

//	protected ResourceInstance createActivityStatusResource(String collectionName, String activityId) {
//		Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
//		mapIds.put(Resource.Type.Collection, collectionName);
//		mapIds.put(Resource.Type.ActivityStatus, activityId);
//		return createResource(Resource.Type.ActivityStatus, mapIds);
//	}
}
