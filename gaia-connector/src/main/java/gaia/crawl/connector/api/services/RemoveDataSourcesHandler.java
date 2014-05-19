package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSourceId;
import gaia.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class RemoveDataSourcesHandler extends BaseHandler implements DeleteMethodHandler {

	public RemoveDataSourcesHandler(ConnectorManager connManager, ObjectSerializer serializer,
			RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		try {
			String collection = (String) getParam("collection", queryParams, true);
			boolean force = StringUtils.getBoolean(getParam("force", queryParams, false), true).booleanValue();
			List<DataSourceId> lst = null;
			try {
				lst = connManager.removeDataSources(collection, force);
			} catch (Exception e) {
				throw ErrorUtils.statusExp(e);
			}
			List<String> res = new ArrayList<String>();
			for (DataSourceId dsId : lst) {
				res.add(dsId.toString());
			}
			return buildResponse(Status.OK, res);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
