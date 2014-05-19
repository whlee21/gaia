package gaia.crawl.connector.api.services;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.DataSourceFactoryException;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSource;
import gaia.utils.StringUtils;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class StartBatchJobHandler extends BaseHandler implements PutMethodHandler {

	public StartBatchJobHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers, String body) {
		try {
			RequestBody requestBody = bodyParser.parse(body);
			Map<String, Object> m = requestBody.getProperties();
			String crawler = (String) getParam("crawler", queryParams, m, true);
			String collection = (String) getParam("collection", queryParams, m, false);
			String id = (String) getParam("id", queryParams, m, true);
			boolean parse = StringUtils.getBoolean(getParam("parse", queryParams, m, false), true).booleanValue();
			boolean index = StringUtils.getBoolean(getParam("index", queryParams, m, false), true).booleanValue();

			Map<String, Object> dsMap = (Map) getParam("template", queryParams, m, false);
			DataSource template = null;
			if (dsMap != null) {
				template = DataSource.fromMap(dsMap);
				try {
					template = connManager.validateDataSource(template, true, false);
				} catch (DataSourceFactoryException dse) {
					List<Error> errors = dse.getErrors();
					if ((errors != null) && (!errors.isEmpty())) {
						throw ErrorUtils.statusExp(422, errors);
					}
					throw ErrorUtils.statusExp(dse);
				} catch (Exception e) {
					throw ErrorUtils.statusExp(e);
				}
			}
			CrawlId cid = null;

			cid = connManager.startBatchJob(crawler, collection, id, template, parse, index);

			return buildResponse(Status.OK, cid.toString());
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
