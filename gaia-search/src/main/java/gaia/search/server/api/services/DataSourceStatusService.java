package gaia.search.server.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.xnap.commons.i18n.I18nFactory;

public class DataSourceStatusService  extends BaseService  {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(DataSourceStatusService.class);
	private String collection;
	private String dataSourceId;
	private ConnectorManager crawlerManager;
	
	public DataSourceStatusService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName, String dataSourceId, ConnectorManager crawlerManager) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.dataSourceId = dataSourceId;
		this.crawlerManager = crawlerManager;
	}

	@GET
	@Produces("text/plain")
	public Response getDataSourceStatus(@Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
		Response resultObj = null;
		
		Object statusObj = status();
		
		return buildResponse(Response.Status.OK, statusObj);
	}
	
	private DataSource getDataSource(String collection) throws Exception {
		String idStr = dataSourceId;
		if (idStr == null) {
			return null;
		}
		DataSourceId dsId = new DataSourceId(idStr);
		DataSource ds = null;
		try {
			ds = crawlerManager.getDataSource(dsId);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, e.toString());
		}
		if (ds == null)
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		if ((collection != null) && (!collection.equals(ds.getCollection()))) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("Data source " + idStr + " not found in collection "
					+ collection));
		}

		return ds;
	}

	public Object status() throws Exception {
		return doStatus(collection, getDataSource(collection), crawlerManager);
	}

	static Object doStatus(String collection, DataSource ds, ConnectorManager crawlerManager) throws Exception {
		if (ds == null) {
			List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();

			for (DataSource dataSource : crawlerManager.getDataSources(collection)) {
				Map<String, Object> stat = crawlerManager.getStatus(dataSource.getCrawlerType(),
						new CrawlId(dataSource.getDataSourceId()));

				if (stat != null) {
					res.add(stat);
				}
			}
			return res;
		}
		return crawlerManager.getStatus(ds.getCrawlerType(), new CrawlId(ds.getDataSourceId()));
	}
	
}
