package gaia.search.server.api.services;

import gaia.admin.collection.CollectionManager;
import gaia.api.AuditLogger;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class DataSourceIndexService  extends BaseService {
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(DataSourceIndexService.class);
	private static final Logger LOG = LoggerFactory.getLogger(DataSourceIndexService.class);

	private String collection;
	private String dataSourceId;
	private ConnectorManager crawlerManager;
	private CollectionManager cm;
	protected DataSource ds;
	private volatile boolean existing = true;

	public DataSourceIndexService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName,
			String dataSourceId, ConnectorManager crawlerManager, CollectionManager cm) {
		super(serializer, bodyParser);
		this.collection= collectionName;
		this.dataSourceId = dataSourceId;
		this.crawlerManager = crawlerManager;
		this.cm = cm;
		
		String idStr = dataSourceId;
		if (idStr == null) {
			LOG.info("no dsId");
			setExisting(false);
			return;
		}
		DataSourceId dsId = new DataSourceId(idStr);
		try {
			try {
				ds = crawlerManager.getDataSource(dsId);
			} catch (Exception e) {
				LOG.warn(i18n.tr("Exception getting data source: " + e.toString()), e);
			}
		} finally {
			if (collection != null)
				setExisting((ds != null) && (collection.equals(ds.getCollection())));
			else
				setExisting(ds != null);
		}
	}
	

	public void setExisting(boolean exists) {
		this.existing = exists;
	}
	
	public boolean isExisting() {
		return this.existing;
	}

	@DELETE
	@Produces("text/plain")
	public Response deleteDataSourceIndex(@Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
		LOG.debug("whlee21 deleteDataSourceIndex (collectionName, dataSourceId) = (" + collection+ ", " + dataSourceId
				+ ")");
		if (this.ds != null) {
			deleteCrawlInfo();

			deleteIndex();

			AuditLogger.log("Removed index content for datasource: " + this.ds.getDisplayName());
		}
		return buildResponse(Response.Status.NO_CONTENT);
	}


	protected void deleteCrawlInfo() throws Exception {
		crawlerManager.reset(ds.getCrawlerType(), ds.getCollection(), ds.getDataSourceId());
	}
	

	protected void deleteIndex() throws Exception {
		crawlerManager.deleteOutputData(ds.getDataSourceId());
	}
	
}
