package gaia.search.server.api.services;

import gaia.api.AuditLogger;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import javax.ws.rs.DELETE;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.xnap.commons.i18n.I18nFactory;


public class DataSourceCrawlDataService  extends BaseService {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(DataSourceCrawlDataService.class);
	private String collection;
	private String dataSourceId;
	private ConnectorManager ccm;
	private volatile boolean existing = true;
	private DataSource ds;
	
	public DataSourceCrawlDataService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName, String dataSourceId,
			ConnectorManager ccm) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.dataSourceId = dataSourceId;
		this.ccm = ccm;
	}


	public void setExisting(boolean exists) {
		this.existing = exists;
	}
	

	public boolean isExisting() {
		return this.existing;
	}
	
	
	@DELETE
	@Produces("text/plain")
	public Response delete(@Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
		DataSourceId dsId = new DataSourceId(dataSourceId);
		DataSource ds = null;
		try {
			ds = ccm.getDataSource(dsId);
		} catch (Exception e) {
		}
		if (ds == null)
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		if ((collection != null) && (!collection.equals(ds.getCollection()))) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("Data source " + dataSourceId + " not found in collection "
					+ collection));
		}
		
		remove();
		
		Response response = buildResponse(Response.Status.NO_CONTENT);
		
		return response;
	}
	

	public void remove() throws Exception {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		if (ds != null) {
			deleteCrawlInfo();

			AuditLogger.log("removed datasource crawldata");
		}
	}
	

	protected void deleteCrawlInfo() throws Exception {
		ccm.reset(ds.getCrawlerType(), ds.getCollection(), ds.getDataSourceId());
	}

}
