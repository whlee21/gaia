package gaia.crawl.dih;

import gaia.crawl.datasource.DataSource;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.util.IOUtils;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DIHImporter {
	private static transient Logger LOG = LoggerFactory.getLogger(DIHImporter.class);
	private File dir;
	private DIHConfigurationManager confManager;
	public EmbeddedSolrServer server;

	public DIHImporter(String dataDir) {
		dir = new File(dataDir);
	}

	public void init() throws IOException {
		confManager = new DIHConfigurationManager(dir);
		confManager.initConfigs();
	}

	public void generateDihConfigs(DataSource ds) throws IOException {
		confManager.generateDihConfigs(ds);
	}

	public void copyJdbcJars(String collection) throws IOException {
		confManager.copyJdbcJars(new File(DIHCrawlerController.JARS_DIR, collection));
	}

	public void startEmbeddedSolrServer() throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

		String zkHost = System.clearProperty("zkHost");
		String zkRun = System.clearProperty("zkRun");
		try {
			File solrXmlFile = new File(dir, "solr.xml");
			CoreContainer container = CoreContainer.createAndLoad(dir.getAbsolutePath(), solrXmlFile);
			server = new EmbeddedSolrServer(container, "collection1");
		} catch (RuntimeException e) {
			throw new RuntimeException("Could not start embedded solr server", e);
		} finally {
			Thread.currentThread().setContextClassLoader(classLoader);
			if (zkHost != null)
				System.setProperty("zkHost", zkHost);
			if (zkRun != null)
				System.setProperty("zkRun", zkRun);
		}
	}

	public void shutdown() {
		SolrCore core = server.getCoreContainer().getCore("collection1");
		ClassLoader classLoader = null;
		try {
			classLoader = core.getResourceLoader().getClassLoader();
		} finally {
			core.close();
		}

		try {
			Class clz = classLoader.loadClass("com.lucid.jdbc.JdbcLeakPrevention");
			Object obj = clz.newInstance();

			List<String> driverNames = (List) obj.getClass().getMethod("clearJdbcDriverRegistrations", new Class[0])
					.invoke(obj, new Object[0]);

			for (String name : driverNames)
				LOG.info(new StringBuilder().append("Unregistering ").append(name).append(" driver to release permgen space")
						.toString());
		} catch (Exception e) {
			LOG.warn("Could not unregister jdbc drivers", e);
		}

		if (server != null) {
			server.shutdown();
			server = null;
		}

		if ((classLoader instanceof Closeable)) {
			IOUtils.closeWhileHandlingException(new Closeable[] { (Closeable) classLoader });
		}
	}

	public DIHStatus runImport(DataSource ds, SolrParams customParams) throws SolrServerException, IOException {
		ModifiableSolrParams params = getSolrParams(ds);
		if (customParams != null) {
			params.add(customParams);
		}
		SolrRequest request = new QueryRequest(params);
		NamedList nl = server.request(request);
		DIHStatus info = parseDihResponse(nl);
		return info;
	}

	public DIHStatus stopImport(DataSource ds) throws SolrServerException, IOException {
		if (server == null) {
			return null;
		}
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.add("qt", new String[] { JDBCUtils.requestHandlerName(ds) });
		params.add("command", new String[] { "abort" });
		SolrRequest request = new QueryRequest(params);
		NamedList nl = server.request(request);
		DIHStatus info = parseDihResponse(nl);
		return info;
	}

	private DIHStatus parseDihResponse(NamedList<Object> nl) {
		DIHStatus status = new DIHStatus();

		if (nl.get("responseHeader") != null) {
			NamedList responseHeader = (NamedList) nl.get("responseHeader");
			if (responseHeader.get("status") != null) {
				Integer statusNumber = (Integer) responseHeader.get("status");
				if (statusNumber.intValue() != 0) {
					LOG.error(new StringBuilder().append("DIH response status is ").append(statusNumber).toString());
					status.setStatus(DIHStatus.Status.FAILED);
				}
			} else {
				LOG.error("Could not find status value in DIH response");
				status.setStatus(DIHStatus.Status.FAILED);
			}
		}

		if (nl.get("status") != null) {
			if ("idle".equals(nl.get("status")))
				status.setStatus(DIHStatus.Status.IDLE);
			else if ("busy".equals(nl.get("status")))
				status.setStatus(DIHStatus.Status.BUSY);
			else {
				LOG.error(new StringBuilder().append("Unknown status response from DIH handler - ").append(status).toString());
			}
		}

		if (nl.get("statusMessages") != null) {
			Map statusMessage = (Map) nl.get("statusMessages");

			if (statusMessage.get("") != null) {
				String freetextStatus = (String) statusMessage.get("");
				Pattern pattern = Pattern.compile("\\QIndexing failed.\\E");
				Matcher matcher = pattern.matcher(freetextStatus);
				if (matcher.find()) {
					LOG.error(new StringBuilder().append("DIH response status is '").append(freetextStatus).append("'")
							.toString());
					status.setStatus(DIHStatus.Status.FAILED);
				}
			}

		}

		NamedList docElement = null;
		try {
			docElement = (NamedList) nl.get("verbose-output");
		} catch (Exception e) {
		}
		if (docElement != null) {
			List<String> exceptions = findExceptionsRecursively(docElement);

			List<String> rootCauses = new ArrayList<String>();
			for (String str : exceptions) {
				StringBuilder sb = new StringBuilder();

				if (str.indexOf("\n") != -1) {
					sb.append(str.substring(0, str.indexOf("\n")));
					sb.append("\n");
				}

				String marker = "Caused by:";
				int start = str.indexOf(marker);
				int end = str.indexOf(marker, start + marker.length());
				if (start != -1) {
					if (end != -1)
						sb.append(str.substring(start + marker.length(), end));
					else {
						sb.append(str.substring(start + marker.length()));
					}
				}
				if (sb.length() == 0)
					rootCauses.add(str);
				else {
					rootCauses.add(sb.toString());
				}
			}
			if (!rootCauses.isEmpty()) {
				status.setError(new StringBuilder().append("Could not import data from database: ")
						.append(rootCauses.toString()).toString());
			}
		}

		return status;
	}

	private List<String> findExceptionsRecursively(NamedList nl) {
		List<String> res = new ArrayList<String>();
		Iterator iterator = nl.iterator();
		while (iterator.hasNext()) {
			Map.Entry item = (Map.Entry) iterator.next();
			if ("EXCEPTION".equals(item.getKey())) {
				res.add((String) item.getValue());
			}
			if ((item.getValue() instanceof NamedList)) {
				res.addAll(findExceptionsRecursively((NamedList) item.getValue()));
			}
		}
		return res;
	}

	private static ModifiableSolrParams getSolrParams(DataSource ds) {
		ModifiableSolrParams result = new ModifiableSolrParams();
		result.add("qt", new String[] { JDBCUtils.requestHandlerName(ds) });
		result.add("ds", new String[] { ds.getDataSourceId().toString() });

		File dir = new File(DIHCrawlerController.DS_DIR, ds.getDataSourceId().getId());
		boolean dihPropsFileExists = new DIHConfigurationManager(dir).isDihPropFileExists(ds.getDataSourceId());
		if ((JDBCUtils.isIncremental(ds)) && (dihPropsFileExists)) {
			result.add("command", new String[] { "delta-import" });
			result.add("clean", new String[] { "false" });
		} else {
			result.add("command", new String[] { "full-import" });
			result.add("clean", new String[] { new Boolean(ds.getBoolean("clean_in_full_import_mode", true)).toString() });
		}
		result.add("optimize", new String[] { "false" });
		result.add("synchronous", new String[] { "true" });
		return result;
	}
}
