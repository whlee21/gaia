package gaia.crawl.dih;

import gaia.api.Error;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.datasource.DataSourceSpec;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDBCDataSourceValidator {
	private static transient Logger LOG = LoggerFactory.getLogger(JDBCDataSourceValidator.class);

	public static List<Error> validate(Map<String, Object> m) {
		List<Error> errors = new ArrayList<Error>();

		if (StringUtils.isEmpty((String) m.get("username"))) {
			errors.add(new Error("username", Error.E_EMPTY_VALUE));
		}
		if (StringUtils.isEmpty((String) m.get("url"))) {
			errors.add(new Error("url", Error.E_EMPTY_VALUE));
		}
		if (StringUtils.isEmpty((String) m.get("driver"))) {
			errors.add(new Error("driver", Error.E_EMPTY_VALUE));
		}
		String select = (String) m.get("sql_select_statement");
		if (select != null) {
			if (!select.toLowerCase().startsWith("select")) {
				errors.add(new Error("sql_select_statement", Error.E_INVALID_VALUE, "must start with select clause"));
			}
		}
		String deltaQuery = (String) m.get("delta_sql_query");
		if (!StringUtils.isEmpty(deltaQuery)) {
			if (deltaQuery.indexOf("$") == -1) {
				errors.add(new Error("delta_sql_query", Error.E_INVALID_VALUE,
						"must contain parametric value ( see '$' sign in docs )"));
			}
		}

		if (errors.isEmpty()) {
			errors.addAll(testImport(m));
		}
		return errors;
	}

	private static List<Error> testImport(Map<String, Object> m) {
		List<Error> errors = new ArrayList<Error>();

		String rand = UUID.randomUUID().toString();

		DataSource ds = new DataSource(DataSourceSpec.Type.jdbc.toString(), "gaia.jdbc", (String) m.get("collection"));
		ds.getProperties().putAll(m);
		ds.setCollection((String) m.get("collection"));
		ds.setDataSourceId(new DataSourceId(rand));

		String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
		File dir = new File(tmpDir, "dih-validate-" + new Date().getTime());
		dir.mkdirs();

		DIHImporter importer = null;
		try {
			importer = new DIHImporter(dir.getAbsolutePath());
			importer.init();
			importer.copyJdbcJars(ds.getCollection());
			importer.generateDihConfigs(ds);
			importer.startEmbeddedSolrServer();

			String url = JDBCUtils.getUrl(ds);
			String username = JDBCUtils.getUsername(ds);
			String password = JDBCUtils.getPassword(ds);
			if (password == null) {
				password = "";
			}
			String driver = JDBCUtils.getDriver(ds);

			SolrCore core = importer.server.getCoreContainer().getCore("collection1");
			ClassLoader classLoader = null;
			try {
				classLoader = core.getResourceLoader().getClassLoader();
			} finally {
				core.close();
			}

			Driver d = null;
			try {
				d = (Driver) Class.forName(driver, true, classLoader).newInstance();
			} catch (ClassNotFoundException e) {
				errors.add(new Error("driver", Error.E_INVALID_VALUE, "unknown JDBC driver class name"));
				return errors;
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			}
			Properties props = new Properties();
			props.setProperty("user", username);
			props.setProperty("password", password);
			Connection conn = null;
			try {
				conn = d.connect(url, props);
			} catch (SQLException ex) {
				errors.add(new Error("url", Error.E_EXCEPTION,
						"Could not create jdbc connection. Please recheck driver, URL, username and password. Database exception: "
								+ ex.getMessage()));
			}

			if (conn != null)
				try {
					conn.close();
				} catch (SQLException ex) {
				}
			else if (errors.isEmpty()) {
				errors.add(new Error("url", Error.E_EXCEPTION,
						"Could not create jdbc connection. Please recheck driver, URL, username and password"));
			}

			if (!errors.isEmpty()) {
				return errors;
			}

			ModifiableSolrParams params = new ModifiableSolrParams();
			params.set("debug", new String[] { "on" });
			params.set("rows", new String[] { "1" });
			params.set("verbose", new String[] { "on" });
			DIHStatus status = importer.runImport(ds, params);
			if (status.getStatus() == DIHStatus.Status.FAILED)
				errors.add(new Error("url", Error.E_EXCEPTION, "Could not import data from database: " + status.getError()));
		} catch (IOException e) {
			throw new RuntimeException("Exception during database data source validation", e);
		} catch (SolrServerException e) {
			throw new RuntimeException("Exception during database data source validation", e);
		} finally {
			if (importer != null) {
				importer.shutdown();
			}
			FileUtils.deleteQuietly(dir);
		}
		return errors;
	}
}
