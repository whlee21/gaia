package gaia.crawl.gcm;

import gaia.Defaults;
import gaia.commons.server.Error;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.gcm.api.CMResponse;
import gaia.crawl.gcm.api.ConfigureResponse;
import gaia.crawl.gcm.api.RemoteGCMServer;
import gaia.crawl.gcm.feeder.GaiaFeedConsumer;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;
import gaia.utils.ExceptionUtil;
import gaia.utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GCMSpec extends DataSourceSpec {
	private static transient Logger log = LoggerFactory.getLogger(GCMSpec.class);
	public static final String ACL_FIELD = "acl";
	public static final String ACL_PREFIX = "GCM";
	private LWEGCMAdaptor adaptor;
	Random r = new Random();
	public static final String CONNECTOR_TYPE = "connector_type";

	public GCMSpec(LWEGCMAdaptor adaptor) {
		super(DataSourceSpec.Category.GCM.toString());
		this.adaptor = adaptor;
	}

	public FieldMapping getDefaultFieldMapping() {
		return new FieldMapping();
	}

	protected void addCrawlerSupportedProperties() {
		addBatchProcessingProperties();
		addFieldMappingProperties();
		addCommitProperties();
		addVerifyAccessProperties();
		addSpecProperty(new SpecProperty("add_failed_docs", "datasource.add_failed_docs", Boolean.class, Boolean.FALSE,
				Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("max_docs", "datasource.max_docs", Integer.class, Integer.valueOf(this.defaults
				.getInt(Defaults.Group.datasource, "max_docs")), Validator.INT_STRING_VALIDATOR, false));
	}

	private static String getStringFromInputStream(InputStream is) {
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		try {
			br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null)
				sb.append(line);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();
	}

	private void loadJSONFile(String dsName, String filepath, List<Error> errors) {
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(new File(filepath));
			JSONObject mappings = new JSONObject(getStringFromInputStream(fileInputStream));
			HashMap inclusions = new HashMap();
			HashMap exclusions = new HashMap();

			Iterator iterator = mappings.keys();
			while (iterator.hasNext()) {
				Object key = iterator.next();
				String mimetype = key.toString();
				Set inc = new HashSet();
				Set exc = new HashSet();
				JSONObject mimetypeObject = (JSONObject) mappings.get(mimetype);

				if (mimetypeObject.has("include_tags")) {
					JSONArray tags = (JSONArray) mimetypeObject.get("include_tags");

					if (tags != null) {
						for (int index = 0; index < tags.length(); index++) {
							inc.add(String.valueOf(tags.get(index)));
						}
					}
				}

				if (mimetypeObject.has("exclude_tags")) {
					JSONArray tags = (JSONArray) mimetypeObject.get("exclude_tags");
					if (tags != null) {
						for (int index = 0; index < tags.length(); index++) {
							exc.add(String.valueOf(tags.get(index)));
						}
					}
				}

				if ((!inc.isEmpty()) && (!exc.isEmpty())) {
					errors.add(new Error(null, Error.E_INVALID_VALUE, new StringBuilder()
							.append("JSON file cannot specify both, inclusions and exclusions, for mimetype = ").append(mimetype)
							.toString()));
					return;
				}
				log.info(new StringBuilder().append("Includes: ").append(inc).toString());
				log.info(new StringBuilder().append("Excludes: ").append(exc).toString());
				if (!inc.isEmpty())
					inclusions.put(mimetype, inc);
				if (!exc.isEmpty())
					exclusions.put(mimetype, exc);
			}

			GaiaFeedConsumer.setInclusions(dsName, inclusions);
			GaiaFeedConsumer.setExclusions(dsName, exclusions);
		} catch (Exception ex) {
			log.warn(new StringBuilder().append("Error while loading json file: ").append(filepath).toString(), ex);
			errors.add(new Error(null, Error.E_INVALID_VALUE, new StringBuilder().append("Could not load JSON file: ")
					.append(ExceptionUtil.getRootCause(ex).getMessage()).toString()));
		} finally {
			if (fileInputStream != null)
				try {
					fileInputStream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	}

	public List<Error> validate(Map<String, Object> map) {
		List errors = super.validate(map);

		String jsonFile = (String) map.get("tags_file");
		if ((jsonFile != null) && (!jsonFile.trim().isEmpty())) {
			Object idObject = map.get("id");
			log.info(new StringBuilder().append("idObject = ").append(idObject).append(", class = ")
					.append(idObject.getClass().getName()).toString());
			String dsName = String.valueOf(idObject);

			int idx = dsName.indexOf("id=");
			if (idx != -1) {
				dsName = dsName.substring(idx + 3, dsName.indexOf(",", idx));
			}

			if ((idObject instanceof Map)) {
				dsName = ((Map) idObject).get("id").toString();
			}

			log.info(new StringBuilder().append("dsName = ").append(dsName).toString());
			loadJSONFile(dsName, jsonFile, errors);
		}

		if (errors.isEmpty()) {
			reachabilityCheck(map, errors);
		}

		return errors;
	}

	private void reachabilityCheck(final Map<String, Object> map, List<Error> errors) {
		if ((map.get("verify_access") != null) && (!StringUtils.getBoolean(map.get("verify_access")).booleanValue())) {
			return;
		}

		Callable task = new Callable() {
			List<Error> errors = new ArrayList();

			public List<Error> call() {
				RemoteGCMServer gcm = GCMController.client;
				try {
					String tmpName = "tmp-" + r.nextInt();
					Map gcmProps = adaptor.getGCMProperties(map);
					String connectorType = (String) gcmProps.get("connectorType");
					try {
						ConfigureResponse response = gcm.setConnectorConfig(tmpName, connectorType, gcmProps, "en_EN", false);

						if (response.getStatusId() != 0) {
							this.errors.add(new Error(null, Error.E_INVALID_VALUE, "Data not reachable: " + response.getMessage()));

							log.warn("Could not create datasource: " + response.getMessage());
						}
					} catch (Throwable e) {
						CMResponse response;
						this.errors.add(new Error(null, Error.E_INVALID_VALUE, "Data not reachable: "
								+ ExceptionUtil.getRootCause(e).getMessage()));

						log.warn("Could not create datasource: " + e.getClass(), e);
					} finally {
						CMResponse response;
						try {
							CMResponse response;
							response = gcm.removeConnector(tmpName);
						} catch (Throwable t) {
						}
					}
				} finally {
					gcm.close();
				}
				return this.errors;
			}
		};
		ExecutorService es = Executors.newSingleThreadScheduledExecutor();
		CompletionService ecs = new ExecutorCompletionService(es);

		Future future = ecs.submit(task);
		try {
			errors.addAll((Collection) future.get(3600L, TimeUnit.SECONDS));
		} catch (TimeoutException t) {
			errors.add(new Error(null, Error.E_INVALID_VALUE, "Data not reachable: timeout"));
		} catch (InterruptedException e) {
			errors.add(new Error(null, Error.E_INVALID_VALUE, "Data not reachable: interrupted"));
		} catch (ExecutionException e) {
			errors.add(new Error(null, Error.E_INVALID_VALUE, new StringBuilder().append("Data not reachable: unknown")
					.append(ExceptionUtil.getRootCause(e).getMessage()).toString()));
		} finally {
			es.shutdown();
		}
	}
}
