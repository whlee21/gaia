package gaia.bigdata.api.connector.gaiasearch;

import gaia.bigdata.api.State;
import gaia.bigdata.api.Status;
import gaia.bigdata.api.connector.ConnectorService;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseService;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;
import gaia.crawl.DataSourceFactoryException;
import gaia.crawl.api.DataSourceJobResource;
import gaia.crawl.api.DataSourceResource;
import gaia.crawl.api.DataSourcesResource;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GaiaConnectorService extends BaseService implements ConnectorService {
	private static transient Logger log = LoggerFactory.getLogger(GaiaConnectorService.class);

	@Inject
	protected GaiaConnectorService(Configuration config, ServiceLocator locator) {
		super(config, locator);
	}

	public String getType() {
		return ServiceType.CONNECTOR.name();
	}

	public State create(String collection, Map<String, Object> input) {
		State result = new State(collection, collection);

		RestletContainer<DataSourcesResource> dataSourcesResourceRc = RestletUtil.wrap(DataSourcesResource.class,
				getServiceURI(ServiceType.GAIASEARCH.name()), "/collections/" + collection + "/datasources");
		DataSourcesResource dataSourcesResource = (DataSourcesResource) dataSourcesResourceRc.getWrapped();
		if (dataSourcesResource != null) {
			Map<String, Object> dsArgs = new HashMap<String, Object>();
			try {
				for (Map.Entry<String, Object> entry : input.entrySet()) {
					String key = ((String) entry.getKey()).toString();
					Object val = entry.getValue();
					val = processValue(val);
					dsArgs.put(key, val);
				}
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			Map<String, Object> add = null;
			try {
				add = dataSourcesResource.add(dsArgs);

				String id = (String) add.get("id");
				result.setId(id);
				result.setStatus(Status.CREATED);
				result.setProperties(add);
			} catch (DataSourceFactoryException e) {
				log.error("Exception for " + collection, e);

				result.setStatus(Status.FAILED);
				result.setThrowable(e);
				result.setErrorMsg(e.getMessage());
			} catch (Throwable e) {
				log.error("Exception for " + collection, e);
				result.setStatus(Status.FAILED);
				result.setThrowable(e);
				result.setErrorMsg(e.getMessage());
			} finally {
				RestletUtil.release(dataSourcesResourceRc);
			}
		}
		return result;
	}

	public State update(String collection, String id, Map<String, Object> input) {
		State result = new State(collection, collection);

		RestletContainer<DataSourceResource> dsrRc = RestletUtil.wrap(DataSourceResource.class,
				getServiceURI(ServiceType.GAIASEARCH.name()), "/collections/" + collection + "/datasources/" + id);
		DataSourceResource dsr = (DataSourceResource) dsrRc.getWrapped();
		if (dsr != null) {
			try {
				dsr.update(input);
				result.setStatus(Status.SUCCEEDED);
			} catch (Throwable e) {
				log.error("Exception for " + collection, e);
				result.setStatus(Status.FAILED);
				result.setThrowable(e);
				result.setErrorMsg(e.getMessage());
			} finally {
				RestletUtil.release(dsrRc);
			}
		}
		return result;
	}

	private Object processValue(Object val) throws JSONException {
		if ((val instanceof JSONArray)) {
			List<Object> tmp = new ArrayList<Object>();
			JSONArray jsonArray = (JSONArray) val;
			for (int i = 0; i < jsonArray.length(); i++) {
				tmp.add(processValue(jsonArray.get(i)));
			}
			val = tmp;
		}
		return val;
	}

	public List<State> list(String collection) {
		List<State> result = new ArrayList<State>();
		RestletContainer<DataSourcesResource> dataSourcesResourceRc = RestletUtil.wrap(DataSourcesResource.class,
				getServiceURI(ServiceType.GAIASEARCH.name()), "/collections/" + collection + "/datasources");
		DataSourcesResource dataSourcesResource = (DataSourcesResource) dataSourcesResourceRc.getWrapped();

		if (dataSourcesResource != null) {
			try {
				List<Map<String, Object>> retrieve = dataSourcesResource.retrieve();
				if (retrieve != null)
					for (Iterator<Map<String, Object>> iter = retrieve.iterator(); iter.hasNext();) {
						Object o = iter.next();
						log.info("DS: " + o);

						Map<String, Object> map = (Map) o;
						String id = (String) map.get("id");
						State state = new State(id.toString(), null, null, collection, Status.EXISTS, new Date());
						state.setProperties(map);
						result.add(state);
					}
			} catch (Throwable e) {
				log.error("Exception for " + collection, e);
			} finally {
				RestletUtil.release(dataSourcesResourceRc);
			}
		}
		return result;
	}

	public State lookup(String collection, String id) {
		State result = new State(id, collection);

		RestletContainer<DataSourceResource> dsrRc = RestletUtil.wrap(DataSourceResource.class,
				getServiceURI(ServiceType.GAIASEARCH.name()), "/collections/" + collection + "/datasources/" + id);
		DataSourceResource dsr = (DataSourceResource) dsrRc.getWrapped();
		if (dsr != null) {
			try {
				Map<String, Object> retrieve = dsr.retrieve();
				if (retrieve != null) {
					result.setStatus(Status.EXISTS);
					result.setProperties(retrieve);
				} else {
					log.info("Couldn't find " + id + " in collection " + collection);
				}
			} catch (Throwable e) {
				log.error("Exception for " + collection, e);
			} finally {
				RestletUtil.release(dsrRc);
			}
		}
		return result;
	}

	public State execute(State state) {
		RestletContainer<DataSourceJobResource> dsjrRc = RestletUtil.wrap(DataSourceJobResource.class,
				getServiceURI(ServiceType.GAIASEARCH.name()),
				"/collections/" + state.getCollection() + "/datasources/" + state.getId() + "/job");

		DataSourceJobResource dsjr = (DataSourceJobResource) dsjrRc.getWrapped();
		if (dsjr != null) {
			try {
				dsjr.start(Collections.<String, Object> emptyMap());
				state.setStatus(Status.RUNNING);
			} catch (URISyntaxException e) {
				log.error("Exception", e);
				state.setStatus(Status.FAILED);
			} catch (Throwable e) {
				log.error("Exception", e);
				state.setStatus(Status.FAILED);
			} finally {
				RestletUtil.release(dsjrRc);
			}
		}
		return state;
	}

	public boolean remove(String collection, String id) {
		boolean result = false;
		RestletContainer<DataSourceResource> dsrRc = RestletUtil.wrap(DataSourceResource.class,
				getServiceURI(ServiceType.GAIASEARCH.name()), "/collections/" + collection + "/datasources/" + id);
		DataSourceResource dsr = (DataSourceResource) dsrRc.getWrapped();
		if (dsr != null) {
			try {
				dsr.remove();
				result = true;
			} catch (URISyntaxException e) {
				log.error("Exception", e);
			} catch (Throwable e) {
				log.error("Couldn't remove the data source", e);
			} finally {
				RestletUtil.release(dsrRc);
			}
		}
		return result;
	}
}
