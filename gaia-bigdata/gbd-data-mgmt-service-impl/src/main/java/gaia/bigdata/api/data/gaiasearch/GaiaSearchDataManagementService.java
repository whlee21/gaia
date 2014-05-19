package gaia.bigdata.api.data.gaiasearch;

import gaia.api.CollectionResource;
import gaia.api.CollectionsResource;
import gaia.bigdata.api.State;
import gaia.bigdata.api.connector.ConnectorsResource;
import gaia.bigdata.api.data.BaseDataManagementService;
import gaia.bigdata.api.data.DataManagementService;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GaiaSearchDataManagementService extends BaseDataManagementService implements DataManagementService {
	private static transient Logger log = LoggerFactory.getLogger(GaiaSearchDataManagementService.class);

	@Inject
	public GaiaSearchDataManagementService(Configuration config, ServiceLocator locator) {
		super(config, locator);
	}

	public State createCollection(String collection) {
		State result = lookupCollection(collection);
		result.addProperty("service-impl", getClass().getSimpleName());
		if (result.getStatus().equals(gaia.bigdata.api.Status.EXISTS)) {
			result.setStatus(gaia.bigdata.api.Status.ALREADY_EXISTS);
			return result;
		}
		RestletContainer<CollectionsResource> collResourceRc = null;
		RestletContainer<ConnectorsResource> connResourceRc = null;
		RestletContainer<ConnectorsResource> connResourceCollocsRc = null;
		try {
			collResourceRc = RestletUtil.wrap(CollectionsResource.class, getServiceURI(ServiceType.GAIASEARCH.name()),
					"/collections");
			CollectionsResource collResource = (CollectionsResource) collResourceRc.getWrapped();
			if (collResource != null) {
				Map<String, Object> args = new HashMap<String, Object>();
				args.put("name", collection);
				result = new State();
				Map<String, Object> add = addCollection(collResource, args, collResourceRc);
				result.setId(add.get("name").toString());
				result.setCollection(result.getId());
				result.setProperties(add);

				args.put("name", collection + "_collocations");
				args.put("template", "collocations.zip");
				add = addCollection(collResource, args, collResourceRc);
				if (add == null) {
					result.setStatus(gaia.bigdata.api.Status.FAILED);
					result.setErrorMsg("Unable to create collection: " + collection);
				}
			} else {
				result.setStatus(gaia.bigdata.api.Status.FAILED);
				result.setErrorMsg("Unable to connect to GaiaSearch to create collection: " + collection);
				return result;
			}

			connResourceRc = RestletUtil.wrap(ConnectorsResource.class, getServiceURI(ServiceType.CONNECTOR.name()), "/"
					+ collection);
			ConnectorsResource connResource = (ConnectorsResource) connResourceRc.getWrapped();
			if (connResource != null) {
				createDataSource(connResource, collection, connResourceRc);
			} else {
				result.setStatus(gaia.bigdata.api.Status.FAILED);
				result.setErrorMsg("Unable to connect to GaiaSearch to create data source for : " + collection);
				return result;
			}
			connResourceCollocsRc = RestletUtil.wrap(ConnectorsResource.class, getServiceURI(ServiceType.CONNECTOR.name()),
					"/" + collection + "_collocations");
			connResource = (ConnectorsResource) connResourceCollocsRc.getWrapped();
			if (connResource != null) {
				createDataSource(connResource, collection + "_collocations", connResourceCollocsRc);
			} else {
				result.setStatus(gaia.bigdata.api.Status.FAILED);
				result.setErrorMsg("Unable to connect to GaiaSearch to create data source for collection: " + collection
						+ "_collocations");
				return result;
			}
			result.setStatus(gaia.bigdata.api.Status.CREATED);
		} catch (ResourceException e) {
			if ((e.getStatus() != null) && (e.getStatus().equals(org.restlet.data.Status.CLIENT_ERROR_CONFLICT))) {
				result.setStatus(gaia.bigdata.api.Status.ALREADY_EXISTS);
			} else {
				result.setStatus(gaia.bigdata.api.Status.FAILED);
				result.setThrowable(e);
			}
		} catch (Throwable e) {
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			result.setThrowable(e);
		}
		return result;
	}

	private Map<String, Object> addCollection(CollectionsResource collResource, Map<String, Object> args,
			RestletContainer<CollectionsResource> restletContainer) throws Exception {
		Map<String, Object> returnValue = null;
		try {
			returnValue = collResource.add(args);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(restletContainer);
		}
		return returnValue;
	}

	private void createDataSource(ConnectorsResource connResource, String collection,
			RestletContainer<ConnectorsResource> restletContainer) throws Exception {
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("name", collection + "_SDA_DS");
			params.put("source", "SDA");
			params.put("source_type", "sda_document_service");
			params.put("type", "external");
			params.put("crawler", "gaia.external");
			params.put("collection", collection);
			Map<String, Object> fmap = new HashMap<String, Object>();
			fmap.put("dynamic_field", null);
			params.put("mapping", fmap);
			State state = connResource.create(params);
			if (!state.getStatus().equals(gaia.bigdata.api.Status.CREATED))
				throw new Exception("Unable to create Document Service Data Source for " + collection + " collection");
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(restletContainer);
		}
	}

	public State deleteCollection(String collectionName) {
		State result = lookupCollection(collectionName);
		if (result.getStatus().equals(gaia.bigdata.api.Status.NON_EXISTENT)) {
			result.setStatus(gaia.bigdata.api.Status.NON_EXISTENT);
			return result;
		}
		result = new State(collectionName, collectionName);
		result.addProperty("service-impl", getClass().getSimpleName());
		gaiaDeleteCollection(collectionName, result);
		if (result.getStatus().equals(gaia.bigdata.api.Status.DELETED)) {
			gaiaDeleteCollection(collectionName + "_collocations", result);
		}
		return result;
	}

	private void gaiaDeleteCollection(String collectionName, State result) {
		RestletContainer<CollectionResource> collResourceRc = RestletUtil.wrap(CollectionResource.class,
				getServiceURI(ServiceType.GAIASEARCH.name()), "/collections/" + collectionName,
				Collections.singletonMap("force", "true"));

		CollectionResource collResource = (CollectionResource) collResourceRc.getWrapped();
		if (collResource != null) {
			try {
				Map<String, Object> args = new HashMap<String, Object>();
				collResource.remove(args);
				result.setStatus(gaia.bigdata.api.Status.DELETED);
			} catch (ResourceException e) {
				if ((e.getStatus() != null) && (e.getStatus().equals(org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND))) {
					result.setStatus(gaia.bigdata.api.Status.NON_EXISTENT);
				} else {
					result.setStatus(gaia.bigdata.api.Status.FAILED);
					result.setThrowable(e);
				}
			} catch (Throwable e) {
				result.setStatus(gaia.bigdata.api.Status.FAILED);
				result.setThrowable(e);
			} finally {
				RestletUtil.release(collResourceRc);
			}
		} else {
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			result.setErrorMsg("Unable to delete collection: " + collectionName);
		}
	}

	public State lookupCollection(String collectionName) {
		State result = new State(collectionName, collectionName);
		result.addProperty("service-impl", getClass().getSimpleName());
		RestletContainer<CollectionResource> collResourceRc = RestletUtil.wrap(CollectionResource.class,
				getServiceURI(ServiceType.GAIASEARCH.name()), "/collections/" + collectionName);
		CollectionResource collResource = (CollectionResource) collResourceRc.getWrapped();
		if (collResource != null) {
			try {
				Map<String, Object> retrieve = collResource.retrieve();
				if ((retrieve != null) && (!retrieve.isEmpty())) {
					String name = (String) retrieve.get("name");
					if (name.equals(collectionName))
						result.setStatus(gaia.bigdata.api.Status.EXISTS);
				} else {
					log.info("No collection available for name: " + collectionName);
					result.setStatus(gaia.bigdata.api.Status.NON_EXISTENT);
				}
			} catch (ResourceException e) {
				if (e.getStatus().equals(org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND)) {
					result.setStatus(gaia.bigdata.api.Status.NON_EXISTENT);
				} else {
					result.setStatus(gaia.bigdata.api.Status.FAILED);
					result.setThrowable(e);
				}
			} catch (Throwable e) {
				result.setStatus(gaia.bigdata.api.Status.FAILED);
				result.setThrowable(e);
			} finally {
				RestletUtil.release(collResourceRc);
			}
		} else {
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			result.setErrorMsg("Unable to connect to GaiaSearch for collection: " + collectionName);
		}
		return result;
	}

	public List<State> listCollections(Pattern namesToMatch) {
		List<State> result = null;
		RestletContainer<CollectionsResource> collResourceRc = RestletUtil.wrap(CollectionsResource.class,
				getServiceURI(ServiceType.GAIASEARCH.name()), "/collections");
		CollectionsResource collResource = (CollectionsResource) collResourceRc.getWrapped();
		if (collResource != null) {
			try {
				List<Map<String, Object>> retrieve = collResource.retrieve();
				result = new ArrayList<State>(retrieve.size());
				for (Map<String, Object> map : retrieve) {
					String name = map.get("name").toString();
					if (!name.endsWith("_collocations")) {
						Matcher matcher = namesToMatch.matcher(name);
						if (matcher.matches()) {
							State state = new State(name, name);
							state.setStatus(gaia.bigdata.api.Status.EXISTS);
							state.addProperty("service-impl", getClass().getSimpleName());
							result.add(state);
						}
					}
				}
			} catch (Throwable e) {
				return createFailedList(e);
			} finally {
				RestletUtil.release(collResourceRc);
			}
		} else {
			log.error("No CollectionsResource for");
			throw new RuntimeException("Unable to connect properly to GaiaSearch");
		}
		return result;
	}
}
