package gaia.search.server.api.services;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.utils.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class CrawlerJobBatchService  extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(CrawlerJobBatchService.class);
	private static final Logger LOG = LoggerFactory.getLogger(CrawlerJobBatchService.class);
	
	private final String collection;
	private final String crawlerName;
	private final String batchId;
	private ConnectorManager cm;

	public CrawlerJobBatchService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName, String crawlerName,
			String batchId, ConnectorManager cm) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.crawlerName = crawlerName;
		this.batchId = batchId;
		this.cm = cm;
	}

	@GET
	@Produces("text/plain")
	public Response getCrawlerJobBatch(@Context HttpHeaders headers, @Context UriInfo ui) {
		
		LOG.debug("### hhokyung getCrawlerJobBatch 1");
		
		List<Error> errors = new ArrayList<Error>();
		if (crawlerName == null) {
			errors.add(new Error("crawler", Error.E_MISSING_VALUE));
		}
		if (errors.size() > 0)
			throw ErrorUtils.statusExp(422, errors);
		try {
			List<Map<String, Object>> res = cm.listJobs(crawlerName, true);
			List<Map<String, Object>> filtered = new LinkedList<Map<String, Object>>();
			for (Map<String, Object> st : res)
				if ((batchId == null) || (batchId.equals(st.get("job_id")))) {
					if (collection != null) {
						String id = (String) st.get("id");
						if (id != null) {
							DataSource ds = cm.getDataSource(new DataSourceId(id));
							if ((ds == null) || (!collection.equals(ds.getCollection())))
								;
						}

					} else {
						filtered.add(st);
					}
				}
			return buildResponse(Response.Status.OK, filtered);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	@PUT
	@Produces("text/plain")
	public Response startCrawlerJobBatch(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
		LOG.debug("### hhokyung startCrawlerJobBatch 1");
		List<Error> errors = new ArrayList<Error>();
		
//	String newDsId = (String) map.get("ds_id");

		RequestBody requestBody = getRequestBody(body);
		Map<String, Object> map = requestBody.getProperties();

   	String newDsId = (String) map.get("ds_id");
   	
		if (collection == null) {
			errors.add(new Error("collection", i18n.tr(Error.E_MISSING_VALUE)));
		}
		if (batchId == null) {
			errors.add(new Error("batch_id", i18n.tr(Error.E_MISSING_VALUE)));
		}
		if (crawlerName == null) {
			errors.add(new Error("crawler", i18n.tr(Error.E_MISSING_VALUE)));
		}
		String newCollection = collection;
		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}
		boolean parse = map.get("parse") != null ? StringUtils.getBoolean(map.get("parse")).booleanValue() : false;
		boolean index = map.get("index") != null ? StringUtils.getBoolean(map.get("index")).booleanValue() : true;

		DataSource ds = null;
		if (newDsId != null) {
			ds = cm.getDataSource(new DataSourceId(newDsId));
			if (ds == null) {
				throw ErrorUtils.statusExp(422, new Error("new_ds_id", Error.E_NOT_FOUND, i18n.tr("unknown data source id " + newDsId)));
			}
		}

		try {
			cm.startBatchJob(crawlerName, newCollection, batchId, ds, parse, index);
			return buildResponse(Response.Status.NO_CONTENT);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(422, e.toString());
		}
	}

	@DELETE
	@Produces("text/plain")
	public Response stopCrawlerJobBatch(@Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
		LOG.debug("### hhokyung stopCrawlerJobBatch 1");
		List<Error> errors = new ArrayList<Error>();
		if (batchId == null) {
			errors.add(new Error("batch_id", i18n.tr(Error.E_MISSING_VALUE)));
		}
		if (crawlerName == null) {
			errors.add(new Error("crawler", i18n.tr(Error.E_MISSING_VALUE)));
		}
		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}
		CrawlId cid = new CrawlId(batchId);
		Map<String, Object> status = cm.getStatus(crawlerName, cid);
		if ((status == null) || (status.get("crawl_state").equals("IDLE"))) {
			// setStatus(ResultStatus.STATUS.NOT_FOUND);
			return null;
		}
		try {
			cm.stopJob(crawlerName, cid, false, 5000L);
			return buildResponse(Response.Status.NO_CONTENT);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}
}
