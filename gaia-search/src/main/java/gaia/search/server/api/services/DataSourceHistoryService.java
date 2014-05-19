package gaia.search.server.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class DataSourceHistoryService extends BaseService {
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(DataSourceHistoryService.class);
	private static final Logger LOG = LoggerFactory.getLogger(DataSourceHistoryService.class);

	private String collection;
	private String dataSourceId;
	private ConnectorManager ccm;
	private volatile boolean existing = true;
	private DataSource ds;

	public DataSourceHistoryService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName, String dataSourceId 
			, ConnectorManager crawlerManager) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.dataSourceId = dataSourceId;
		this.ccm = crawlerManager;
	}


	public void setExisting(boolean exists) {
		this.existing = exists;
	}
	

	public boolean isExisting() {
		return this.existing;
	}
	
	public void doInit() {
		if (dataSourceId == null) {
			setExisting(false);
			return;
		}
		DataSourceId dsId = new DataSourceId(dataSourceId);
		try {
			ds = ccm.getDataSource(dsId);
		} catch (Exception e) {
		} finally {
			boolean existing = ds != null;
			if ((existing) && (collection != null)) {
				existing = collection.equals(ds.getCollection());
			}

			setExisting(existing);
		}
	}
	
	@GET
	@Produces("text/plain")
	public Response getDataSourceHistory(@Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
		LOG.debug("whlee21 getDataSourceHistory (collectionName, dataSourceId) = (" + collection + ", " + dataSourceId
				+ ")");
		doInit();
		return retrieve();
	}

	public Response retrieve() throws Exception {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		List<Map<String, Object>> histories = new ArrayList<Map<String, Object>>();

		for (DataSource ds : ccm.getDataSources(collection)) {
			List<Map<String, Object>> history = getHistory(ds, ccm);
			Map<String, Object> entry = new HashMap<String, Object>();
			entry.put("id", ds.getDataSourceId().toString());
			entry.put("history", history);
			histories.add(entry);
		}
		return buildResponse(Response.Status.OK, histories);
	}
	
	static List<Map<String, Object>> getHistory(DataSource ds, ConnectorManager ccm) throws Exception {
		String id = ds.getDataSourceId().toString();
		List<Map<String, Object>> history = ccm.getHistory(id);
		if (history == null) {
			return new ArrayList<Map<String, Object>>(0);
		}

		return history;
	}
}
