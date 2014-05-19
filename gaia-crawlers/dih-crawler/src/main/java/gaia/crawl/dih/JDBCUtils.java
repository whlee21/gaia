package gaia.crawl.dih;

import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.utils.XMLUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class JDBCUtils {
	private static final Logger LOG = LoggerFactory.getLogger(JDBCUtils.class);

	public static DataSource create(String crawlerType, String collection, Element requestHandlerElement,
			Element dataConfigElement) {
		DataSource ds = new DataSource(DataSourceSpec.Type.jdbc.toString(), crawlerType, collection);
		try {
			Element dataSourceElement = (Element) XMLUtils.getNode("dataSource", dataConfigElement);
			if (dataSourceElement == null)
				throw new RuntimeException("Couldn't find datasource definition in DIH data-config");
			setDriver(ds, dataSourceElement.getAttribute("driver"));
			setUrl(ds, dataSourceElement.getAttribute("url"));
			setUsername(ds, dataSourceElement.getAttribute("user"));
			setPassword(ds, dataSourceElement.getAttribute("password"));
		} catch (XPathExpressionException ex) {
			throw new RuntimeException("Couldn't find datasource definition in DIH data-config", ex);
		}
		try {
			Element entityElement = (Element) XMLUtils.getNode("document/entity", dataConfigElement);
			if (entityElement == null)
				throw new RuntimeException("Couldn't find entity SQL query in DIH data-config");
			setSelectStatement(ds, entityElement.getAttribute("query"));
			setPrimaryKey(ds, entityElement.getAttribute("pk"));
			String deltaQuery = entityElement.getAttribute("deltaQuery");
			deltaQuery = deltaQuery.replace("'${dataimporter.last_index_time}'", "$");
			setDeltaQuery(ds, deltaQuery);

			NodeList nodes = entityElement.getChildNodes();
			List nestedQueriesList = new ArrayList();
			for (int i = 0; i < nodes.getLength(); i++) {
				if ((nodes.item(i) instanceof Element)) {
					Element nestedQueryElement = (Element) nodes.item(i);
					if (("entity".equalsIgnoreCase(nestedQueryElement.getTagName()))
							&& (nestedQueryElement.getAttribute("query") != null)
							&& (!"".equals(nestedQueryElement.getAttribute("query")))) {
						String q = nestedQueryElement.getAttribute("query");
						q = q.replace("'${root." + getPrimaryKey(ds) + "}'", "$");
						nestedQueriesList.add(q);
					}
				}
			}

			setNestedQueries(ds, nestedQueriesList);
		} catch (XPathExpressionException ex) {
			throw new RuntimeException("Couldn't find entity SQL query in DIH data-config", ex);
		}

		try {
			String configFile = getConfig(requestHandlerElement);
			Pattern pattern = Pattern.compile("dataconfig_([-\\w]+)\\.xml");
			Matcher matcher = pattern.matcher(configFile);
			if (!matcher.matches()) {
				throw new RuntimeException(
						"config element in DIH request handler has invalid pattern - it should be in form \"dataconfig_xxxx.xml\"");
			}
			String dataSourceId = matcher.group(1);

			String requestHandlerName = requestHandlerElement.getAttribute("name");
			pattern = Pattern.compile("\\/dataimport_([-\\w]+)");
			matcher = pattern.matcher(requestHandlerName);
			if (!matcher.matches()) {
				throw new RuntimeException(
						"request handler name has invalid pattern - it should be in form \"/dataimport_xxxx\"");
			}
			String handlerNameId = matcher.group(1);

			if (!dataSourceId.equals(handlerNameId)) {
				throw new RuntimeException("config filename should have the same id as request handler name");
			}
			ds.setDataSourceId(new DataSourceId(dataSourceId));
		} catch (NumberFormatException ex) {
			throw new RuntimeException(
					"config element in DIH request handler has invalid pattern - it should be in form \"dataconfig_xxxx.xml\"",
					ex);
		}
		return ds;
	}

	public static String getConfig(Element requestHandlerElement) {
		try {
			Text configTextNode = (Text) XMLUtils.getNode("lst[@name='defaults']/str[@name='config']/text()",
					requestHandlerElement);
			if (configTextNode == null)
				throw new RuntimeException("Couldn't find config file in DIH requestHandler definition");
			return configTextNode.getNodeValue();
		} catch (XPathExpressionException ex) {
			throw new RuntimeException("Couldn't find config file in DIH requestHandler definition", ex);
		}
	}

	public static Element requestHandlerXml(DataSource ds, Document doc, Element existingElement) {
		Element requestHandlerElement = null;
		if (existingElement == null)
			requestHandlerElement = doc.createElement("requestHandler");
		else {
			requestHandlerElement = existingElement;
		}
		requestHandlerElement.setAttribute("managedByGaia", "true");
		requestHandlerElement.setAttribute("class", "org.apache.solr.handler.dataimport.DataImportHandler");
		requestHandlerElement.setAttribute("name", requestHandlerName(ds));
		Text oldConfigTextNode = null;
		try {
			oldConfigTextNode = (Text) XMLUtils.getNode("lst[@name='defaults']/str[@name='config']/text()",
					requestHandlerElement);
		} catch (XPathExpressionException ex) {
			throw new RuntimeException("Couldn't find config file in DIH requestHandler definition", ex);
		}
		if (oldConfigTextNode == null) {
			Element listElement = doc.createElement("lst");
			listElement.setAttribute("name", "defaults");
			Element configFileElement = doc.createElement("str");
			configFileElement.setAttribute("name", "config");
			Text configFileTextNode = doc.createTextNode(dataConfigFilename(ds));
			configFileElement.appendChild(configFileTextNode);
			listElement.appendChild(configFileElement);

			requestHandlerElement.appendChild(listElement);
		} else {
			oldConfigTextNode.setData(dataConfigFilename(ds));
		}
		return requestHandlerElement;
	}

	public static String requestHandlerName(DataSource ds) {
		return "/dataimport_" + ds.getDataSourceId();
	}

	public static String dataConfigFilename(DataSource ds) {
		String filename = "dataconfig_" + ds.getDataSourceId() + ".xml";
		return filename;
	}

	public static String dataConfigPropertyFile(DataSource ds) {
		String filename = "dataimport_" + ds.getDataSourceId() + ".properties";
		return filename;
	}

	public static Element dataConfigXml(DataSource ds, Document doc) {
		Element dataConfigElement = doc.createElement("dataConfig");
		Element dataSourceElement = dataSourceXml(ds, doc, null);
		dataConfigElement.appendChild(dataSourceElement);
		Element documentElement = doc.createElement("document");
		documentElement.setAttribute("name", "items");
		Element entityElement = doc.createElement("entity");
		entityElement.setAttribute("name", "root");
		entityElement.setAttribute("query", getSelectStatement(ds));
		entityElement.setAttribute("transformer", "TemplateTransformer");

		if ((getPrimaryKey(ds) != null) && (!"".equals(getPrimaryKey(ds).trim()))) {
			entityElement.setAttribute("pk", getPrimaryKey(ds));
		}
		if ((getDeltaQuery(ds) != null) && (!"".equals(getDeltaQuery(ds).trim()))) {
			entityElement.setAttribute("deltaQuery", getDeltaQuery(ds).replace("$", "'${dataimporter.last_index_time}'"));
		}
		entityElement.setAttribute("preImportDeleteQuery", "data_source:" + ds.getDataSourceId().toString());
		int i;
		if ((getNestedQueries(ds) != null) && (getNestedQueries(ds).size() != 0)) {
			i = 1;
			for (String nestedQuery : getNestedQueries(ds)) {
				if ((nestedQuery != null) && (!"".equals(nestedQuery))) {
					Element nestedQueryElement = doc.createElement("entity");
					nestedQueryElement.setAttribute("name", "nested" + i);
					nestedQueryElement.setAttribute("query", nestedQuery.replace("$", "'${root." + getPrimaryKey(ds) + "}'"));
					entityElement.appendChild(nestedQueryElement);
					i++;
				}
			}
		}
		documentElement.appendChild(entityElement);
		dataConfigElement.appendChild(documentElement);
		return dataConfigElement;
	}

	public static Element dataConfigXml(DataSource ds, Document doc, Element oldDataConfigElement) {
		if (oldDataConfigElement == null) {
			return null;
		}
		Element dataConfigElement = (Element) oldDataConfigElement.cloneNode(true);
		try {
			Element oldDataSourceElement = (Element) XMLUtils.getNode("dataSource", dataConfigElement);
			if (oldDataSourceElement == null) {
				LOG.warn("Couldn't find required XML nodes in DIH data-config to update configuration in place - overwritting");
				return null;
			}
			Element dataSourceElement = dataSourceXml(ds, doc, oldDataSourceElement);
			dataConfigElement.replaceChild(dataSourceElement, oldDataSourceElement);

			Element oldEntityElement = (Element) XMLUtils.getNode("document/entity", dataConfigElement);
			if (oldEntityElement == null) {
				LOG.warn("Couldn't find required XML nodes in DIH data-config to update configuration in place - overwritting");
				return null;
			}

			Element entityElement = (Element) oldEntityElement.cloneNode(true);
			oldEntityElement.getParentNode().replaceChild(entityElement, oldEntityElement);
			entityElement.setAttribute("name", "root");
			entityElement.setAttribute("query", getSelectStatement(ds));
			String transformer = entityElement.getAttribute("transformer");
			if ((transformer == null) || ("".equals(transformer))) {
				entityElement.setAttribute("transformer", "TemplateTransformer");
			} else if (!transformer.contains("TemplateTransformer")) {
				entityElement.setAttribute("transformer", transformer + ",TemplateTransformer");
			}

			if ((getPrimaryKey(ds) != null) && (!"".equals(getPrimaryKey(ds).trim()))) {
				entityElement.setAttribute("pk", getPrimaryKey(ds));
			}
			if ((getDeltaQuery(ds) != null) && (!"".equals(getDeltaQuery(ds).trim())))
				entityElement.setAttribute("deltaQuery", getDeltaQuery(ds).replace("$", "'${dataimporter.last_index_time}'"));
			else {
				entityElement.removeAttribute("deltaQuery");
			}
			entityElement.setAttribute("preImportDeleteQuery", "data_source:" + ds.getDataSourceId().toString());
			int i;
			if (getNestedQueries(ds).size() != 0) {
				i = 1;
				for (String nestedQuery : getNestedQueries(ds)) {
					if ((nestedQuery != null) && (!"".equals(nestedQuery))) {
						Element nestedEntityElement = (Element) XMLUtils.getNode("entity[@name='nested" + i + "']", entityElement);
						if (nestedEntityElement == null) {
							nestedEntityElement = doc.createElement("entity");
							entityElement.appendChild(nestedEntityElement);
						}
						nestedEntityElement.setAttribute("name", "nested" + i);
						nestedEntityElement.setAttribute("query", nestedQuery.replace("$", "'${root." + getPrimaryKey(ds) + "}'"));
						i++;
					}
				}

			}

			NodeList nodes = entityElement.getChildNodes();
			for (int j = 0; j < nodes.getLength(); j++)
				if ((nodes.item(j) instanceof Element)) {
					Element e = (Element) nodes.item(j);
					if ((e.getTagName().equals("entity")) && (e.getAttribute("name").startsWith("nested"))) {
						String indexStr = e.getAttribute("name").substring(6);
						try {
							Integer index = Integer.valueOf(Integer.parseInt(indexStr));
							if (index.intValue() > getNestedQueries(ds).size())
								entityElement.removeChild(e);
						} catch (NumberFormatException ex) {
						}
					}
				}
		} catch (XPathExpressionException ex) {
			LOG.warn("Couldn't find required DOM nodes in DIH data-config to update configuration in place - overwritting",
					ex);
			return null;
		}

		return dataConfigElement;
	}

	private static Element dataSourceXml(DataSource ds, Document doc, Element oldDataSourceElement) {
		Element dataSourceElement;
		if (oldDataSourceElement == null) {
			dataSourceElement = doc.createElement("dataSource");

			dataSourceElement.setAttribute("autoCommit", "true");

			if (getUrl(ds).toLowerCase().contains("mysql"))
				dataSourceElement.setAttribute("batchSize", "-1");
			else {
				dataSourceElement.setAttribute("batchSize", "1000");
			}
			dataSourceElement.setAttribute("convertType", "true");
		} else {
			dataSourceElement = (Element) oldDataSourceElement.cloneNode(true);
		}
		dataSourceElement.setAttribute("driver", getDriver(ds));
		dataSourceElement.setAttribute("url", getUrl(ds));
		dataSourceElement.setAttribute("user", getUsername(ds));
		dataSourceElement.setAttribute("password", getPassword(ds));
		return dataSourceElement;
	}

	public static String getDeltaQuery(DataSource ds) {
		return (String) ds.getProperty("delta_sql_query");
	}

	public static String getDriver(DataSource ds) {
		return (String) ds.getProperty("driver");
	}

	public static List<String> getNestedQueries(DataSource ds) {
		List res = null;
		Object o = ds.getProperties().get("nested_queries");
		if (o == null) {
			return Collections.emptyList();
		}
		if ((o instanceof String)) {
			res = gaia.utils.StringUtils.getList(String.class, o);

			ds.setProperty("nested_queries", res);
		} else {
			res = (List) o;
		}
		return res;
	}

	public static String getPassword(DataSource ds) {
		return (String) ds.getProperty("password");
	}

	public static String getPrimaryKey(DataSource ds) {
		return (String) ds.getProperty("primary_key");
	}

	public static String getSelectStatement(DataSource ds) {
		return (String) ds.getProperty("sql_select_statement");
	}

	public static String getUrl(DataSource ds) {
		return (String) ds.getProperty("url");
	}

	public static String getUsername(DataSource ds) {
		return (String) ds.getProperty("username");
	}

	public static boolean isIncremental(DataSource ds) {
		return (getPrimaryKey(ds) != null) && (!"".equals(getPrimaryKey(ds).trim())) && (getDeltaQuery(ds) != null)
				&& (!"".equals(getDeltaQuery(ds).trim()));
	}

	public static void setDeltaQuery(DataSource ds, String deltaQuery) {
		ds.setProperty("delta_sql_query", deltaQuery);
	}

	public static void setDriver(DataSource ds, String driver) {
		ds.setProperty("driver", driver);
	}

	public static void setNestedQueries(DataSource ds, List<String> nestedQueries) {
		List filteredQueries = new ArrayList();
		if (nestedQueries != null) {
			for (String q : nestedQueries) {
				if (!org.apache.commons.lang.StringUtils.isEmpty(q)) {
					filteredQueries.add(q);
				}
			}
		}
		ds.setProperty("nested_queries", filteredQueries);
	}

	public static void setPassword(DataSource ds, String password) {
		ds.setProperty("password", password);
	}

	public static void setPrimaryKey(DataSource ds, String primaryKey) {
		ds.setProperty("primary_key", primaryKey);
	}

	public static void setSelectStatement(DataSource ds, String selectStatement) {
		ds.setProperty("sql_select_statement", selectStatement);
	}

	public static void setUrl(DataSource ds, String url) {
		ds.setProperty("url", url);
	}

	public static void setUsername(DataSource ds, String username) {
		ds.setProperty("username", username);
	}

	public static boolean customEquals(DataSource one, DataSource other) {
		if (other == null) {
			return false;
		}
		if (one.getClass() != other.getClass()) {
			return false;
		}
		if (one.getDataSourceId() == null ? other.getDataSourceId() != null : !one.getDataSourceId().equals(
				other.getDataSourceId())) {
			return false;
		}
		if (getDriver(one) == null ? getDriver(other) != null : !getDriver(one).equals(getDriver(other))) {
			return false;
		}
		if (getUsername(one) == null ? getUsername(other) != null : !getUsername(one).equals(getUsername(other))) {
			return false;
		}
		if (getPassword(one) == null ? getPassword(other) != null : !getPassword(one).equals(getPassword(other))) {
			return false;
		}
		if (getUrl(one) == null ? getUrl(other) != null : !getUrl(one).equals(getUrl(other))) {
			return false;
		}

		if (getSelectStatement(one) == null ? getSelectStatement(other) != null : !getSelectStatement(one).equals(
				getSelectStatement(other))) {
			return false;
		}
		if (getPrimaryKey(one) == null ? getPrimaryKey(other) != null : !getPrimaryKey(one).equals(getPrimaryKey(other))) {
			return false;
		}
		if (getDeltaQuery(one) == null ? getDeltaQuery(other) != null : !getDeltaQuery(one).equals(getDeltaQuery(other))) {
			return false;
		}
		if (getNestedQueries(one) == null ? getNestedQueries(other) != null : !getNestedQueries(one).equals(
				getNestedQueries(other))) {
			return false;
		}
		return true;
	}
}
