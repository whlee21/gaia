package gaia.crawl.gcm.api;

import gaia.utils.HttpClientSSLUtil;
import gaia.utils.RealmUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;

public class RemoteGCMServer implements RemoteManagerAPI {
	private static UnsupportedOperationException UNSUPPORTED = new UnsupportedOperationException(
			"Not supported by the remote api");
	private final String baseUrl;
	private final AbstractHttpClient client;
	private final XMLInputFactory inputFactory;
	private final XMLOutputFactory outputFactory;

	public RemoteGCMServer(String baseUrl) {
		this(baseUrl, 3000);
	}

	public RemoteGCMServer(String baseUrl, int timeout) {
		this.baseUrl = baseUrl;
		ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager();
		this.client = new DefaultHttpClient(mgr);
		HttpConnectionParams.setConnectionTimeout(this.client.getParams(), timeout);
		HttpConnectionParams.setSoTimeout(this.client.getParams(), timeout);

		RealmUtil.prepareClient(this.client);
		HttpClientSSLUtil.prepareClient(this.client);

		this.inputFactory = XMLInputFactory.newInstance();
		this.inputFactory.setProperty("javax.xml.stream.isValidating", Boolean.valueOf(false));
		this.inputFactory.setProperty("javax.xml.stream.supportDTD", Boolean.valueOf(false));
		this.outputFactory = XMLOutputFactory.newInstance();
	}

	public void close() {
	}

	public AuthenticationResponse authenticate(final String connectorName, final String username, final String password,
			final String domain) throws IOException {
		byte[] body = write(new Writer(connectorName, username) {
			void write(XMLStreamWriter xmlw) throws Exception {
				xmlw.writeStartElement("AuthnRequest");
				xmlw.writeStartElement("Connectors");
				writeElementWithValue("ConnectorName", connectorName, xmlw);
				xmlw.writeEndElement();
				xmlw.writeStartElement("Credentials");
				writeElementWithValue("Username", username, xmlw);
				if (password != null)
					writeElementWithValue("Password", password, xmlw);
				if (domain != null)
					writeElementWithValue("Domain", domain, xmlw);
				xmlw.writeEndElement();
				xmlw.writeEndElement();
			}
		});
		XMLStreamReader parser = getReader(doPost(this.baseUrl + "authenticate", body));
		boolean status = false;
		List groups = new ArrayList();
		try {
			while (parser.hasNext()) {
				int event = parser.next();
				if (event == 1) {
					if ("Success".equals(parser.getLocalName())) {
						status = true;
					}
					if ("Group".equals(parser.getLocalName())) {
						String group = parser.getElementText().trim();
						if ((group != null) && (StringUtils.isNotBlank(group)))
							groups.add(group);
					}
				}
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		} finally {
			close(parser);
		}
		return new AuthenticationResponse(status, groups);
	}

	public Set<String> authorizeDocids(final String connectorName, final List<String> docidList, final String user)
			throws IOException {
		byte[] body = write(new Writer(user, docidList) {
			void write(XMLStreamWriter xmlw) throws Exception {
				xmlw.writeStartElement("AuthorizationQuery");
				xmlw.writeStartElement("ConnectorQuery");
				xmlw.writeStartElement("Identity");
				xmlw.writeAttribute("source", "connector");
				xmlw.writeCharacters(user);
				xmlw.writeEndElement();
				for (String docId : docidList) {
					xmlw.writeStartElement("Resource");
					xmlw.writeAttribute("connectorname", connectorName);
					xmlw.writeCharacters(docId);
					xmlw.writeEndElement();
				}
				xmlw.writeEndElement();
				xmlw.writeEndElement();
			}
		});
		XMLStreamReader parser = getReader(doPost(this.baseUrl + "authorization", body));
		TreeSet authorizedDocs = new TreeSet();
		try {
			while (parser.hasNext()) {
				int event = parser.next();
				if ((event == 1) && ("Answer".equals(parser.getLocalName()))) {
					String path = getElementText("Resource", parser);
					String decision = getElementText("Decision", parser);
					if ("PERMIT".equals(decision)) {
						authorizedDocs.add(path);
					}
				}
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		} finally {
			close(parser);
		}
		return authorizedDocs;
	}

	private String getElementText(String elementName, XMLStreamReader parser) throws XMLStreamException {
		while (parser.hasNext()) {
			int event = parser.next();
			if ((event == 1) && (elementName.equals(parser.getLocalName()))) {
				return parser.getElementText().trim();
			}
		}

		throw new RuntimeException("Expected tag not found:" + elementName);
	}

	public ConfigureResponse getConfigForm(String connectorTypeName, String language) throws IOException {
		XMLStreamReader parser = getReader(doGet(this.baseUrl + "getConfigForm?ConnectorType=" + connectorTypeName
				+ "&Lang=" + language));

		ConfigureResponse response = parseConfigureResponse(parser);
		close(parser);
		return response;
	}

	public ConfigureResponse getConfigFormForConnector(String connectorName, String language) throws IOException {
		XMLStreamReader parser = getReader(doGet(this.baseUrl + "getConnectorConfigToEdit?ConnectorName=" + connectorName
				+ "&Lang=" + language));

		ConfigureResponse response = parseConfigureResponse(parser);
		close(parser);
		return response;
	}

	public ConnectorStatus getConnectorStatus(String connectorName) throws IOException {
		XMLStreamReader parser = getReader(doGet(this.baseUrl + "getConnectorStatus?ConnectorName=" + connectorName));

		ConnectorStatus status = new ConnectorStatus();
		try {
			while (parser.hasNext()) {
				int event = parser.next();
				if (event == 1) {
					if ("StatusId".equals(parser.getLocalName())) {
						status.setStatusId(Integer.parseInt(parser.getElementText()));
					}
					if ("ConnectorStatus".equals(parser.getLocalName()))
						parseConnectorStatus(parser, status);
				}
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		} finally {
			close(parser);
		}
		return status;
	}

	public List<ConnectorStatus> getConnectorStatuses() throws IOException {
		XMLStreamReader parser = getReader(doGet(this.baseUrl + "getConnectorInstanceList"));

		List statuses = new ArrayList();
		try {
			while (parser.hasNext()) {
				int event = parser.next();
				if (event == 1) {
					if ("StatusId".equalsIgnoreCase(parser.getLocalName())) {
						int status = Integer.parseInt(parser.getElementText());
						if (status == 5215) {
							break;
						}
					}
					if ("ConnectorInstance".equals(parser.getLocalName())) {
						ConnectorStatus status = new ConnectorStatus();
						parseConnectorStatus(parser, status);
						statuses.add(status);
					}
				}
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		} finally {
			close(parser);
		}
		return statuses;
	}

	public ConnectorType getConnectorType(String typeName) throws IOException {
		throw UNSUPPORTED;
	}

	public Set<String> getConnectorTypeNames() throws IOException {
		XMLStreamReader parser = getReader(doGet(this.baseUrl + "getConnectorList"));
		TreeSet names = new TreeSet();
		try {
			while (parser.hasNext()) {
				int event = parser.next();
				if ((event == 1) && ("ConnectorType".equals(parser.getLocalName()))) {
					names.add(parser.getElementText());
				}
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		} finally {
			close(parser);
		}
		return names;
	}

	public CMResponse removeConnector(String connectorName) throws IOException {
		XMLStreamReader parser = getReader(doGet(this.baseUrl + "removeConnector?ConnectorName=" + connectorName));

		CMResponse response = new CMResponse();
		try {
			while (parser.hasNext()) {
				int event = parser.next();
				if ((event == 1) && ("StatusId".equals(parser.getLocalName()))) {
					response.setStatusId(Integer.parseInt(parser.getElementText()));
				}
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		} finally {
			close(parser);
		}
		return response;
	}

	public CMResponse restartConnectorTraversal(String connectorName) throws IOException {
		XMLStreamReader parser = getReader(doGet(this.baseUrl + "restartConnectorTraversal?ConnectorName=" + connectorName));

		ConfigureResponse response = parseConfigureResponse(parser);
		close(parser);
		return response;
	}

	public ConfigureResponse setConnectorConfig(final String connectorName, final String connectorTypeName,
			final Map<String, String> configData, String language, final boolean update) throws IOException {
		byte[] body = write(new Writer(connectorName, connectorTypeName) {
			void write(XMLStreamWriter xmlw) throws Exception {
				xmlw.writeStartElement("ConnectorConfig");
				writeElementWithValue("ConnectorName", connectorName, xmlw);
				writeElementWithValue("ConnectorType", connectorTypeName, xmlw);
				writeElementWithValue("Update", Boolean.valueOf(update), xmlw);
				xmlw.writeStartElement("config");
				for (Map.Entry entry : configData.entrySet()) {
					xmlw.writeStartElement("Param");
					xmlw.writeAttribute("name", (String) entry.getKey());
					xmlw.writeAttribute("value", (String) entry.getValue());
					xmlw.writeEndElement();
				}
				xmlw.writeEndElement();
			}
		});
		XMLStreamReader parser = getReader(doPost(this.baseUrl + "setConnectorConfig?ConnectorName=" + connectorName
				+ "&ConnectorType=" + connectorTypeName + "&Lang=" + language, body));

		ConfigureResponse response = parseConfigureResponse(parser);
		close(parser);
		return response;
	}

	private ConfigureResponse parseConfigureResponse(XMLStreamReader parser) {
		ConfigureResponse response = new ConfigureResponse();
		try {
			while (parser.hasNext()) {
				int event = parser.next();
				if (event == 1) {
					if ("StatusId".equals(parser.getLocalName())) {
						response.setStatusId(Integer.parseInt(parser.getElementText()));
					}
					if ("message".equals(parser.getLocalName())) {
						response.setMessage(parser.getElementText());
					}
					if ("FormSnippet".equals(parser.getLocalName()))
						response.setFormSnippet(parser.getElementText());
				}
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
		return response;
	}

	public CMResponse setConnectorManagerConfig(final String feederGateHost, final int feederGatePort,
			final String protocol, final int securePort) throws IOException {
		byte[] body = write(new Writer(feederGateHost, feederGatePort) {
			void write(XMLStreamWriter xmlw) throws Exception {
				xmlw.writeStartElement("ManagerConfig");
				xmlw.writeStartElement("FeederGate");
				xmlw.writeAttribute("host", feederGateHost);
				xmlw.writeAttribute("port", Integer.toString(feederGatePort));
				xmlw.writeAttribute("protocol", protocol);
				xmlw.writeAttribute("securePort", Integer.toString(securePort));
				xmlw.writeEndElement();
				xmlw.writeEndElement();
			}
		});
		XMLStreamReader parser = getReader(doPost(this.baseUrl + "setManagerConfig", body));

		CMResponse response = parseStatusResponse(parser, new CMResponse());
		close(parser);
		return response;
	}

	public CMResponse stopTraversal(String connectorName) throws IOException {
		XMLStreamReader parser = getReader(doGet(this.baseUrl + "stopTraversal?ConnectorName=" + connectorName));

		ConfigureResponse response = parseConfigureResponse(parser);
		close(parser);
		return response;
	}

	public CMResponse setSchedule(final String connectorName, final Schedule schedule) throws IOException {
		byte[] body = write(new Writer(connectorName, schedule) {
			void write(XMLStreamWriter xmlw) throws Exception {
				xmlw.writeStartElement("ConnectorSchedules");
				writeElementWithValue("ConnectorName", connectorName, xmlw);
				writeElementWithValue("load", schedule.getLoad(), xmlw);
				writeElementWithValue("TimeIntervals", schedule.getTimeIntervals(), xmlw);

				writeElementWithValue("RetryDelayMillis", schedule.getRetryDelay(), xmlw);

				xmlw.writeEndElement();
			}
		});
		XMLStreamReader parser = getReader(doPost(this.baseUrl + "setSchedule", body));
		CMResponse response = parseStatusResponse(parser, new CMResponse());
		close(parser);
		return response;
	}

	public CMResponse testConnectivity() throws IOException {
		XMLStreamReader parser = getReader(doGet(this.baseUrl + "testConnectivity"));
		ConfigureResponse response = parseConfigureResponse(parser);
		close(parser);
		return response;
	}

	private CMResponse parseStatusResponse(XMLStreamReader parser, CMResponse cmResponse) {
		try {
			while (parser.hasNext()) {
				int event = parser.next();
				if ((event == 1) && ("StatusId".equals(parser.getLocalName())))
					cmResponse.setStatusId(Integer.parseInt(parser.getElementText()));
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
		return cmResponse;
	}

	protected InputStream doGet(String url) throws IOException {
		HttpGet get = new HttpGet(url);
		return getStream(get);
	}

	protected InputStream doPost(String url, byte[] content) throws IOException {
		HttpPost post = new HttpPost(url);
		post.setEntity(new ByteArrayEntity(content));
		return getStream(post);
	}

	private InputStream getStream(HttpUriRequest request) throws IOException {
		InputStream is = null;
		try {
			HttpResponse response = this.client.execute(request);
			int responseCode = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			is = entity.getContent();
			byte[] buffer = IOUtils.toByteArray(is);
			if (responseCode == 200) {
				if (buffer.length > 0) {
					return new ByteArrayInputStream(buffer);
				}
				throw new IOException("Request retured empty response body.");
			}
			throw new IOException("Request returned non OK reply: " + new String(buffer));
		} finally {
			if (is != null)
				is.close();
		}
	}

	private XMLStreamReader getReader(InputStream input) {
		try {
			return this.inputFactory.createXMLStreamReader(input);
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] write(Writer writer) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLStreamWriter w = getStreamWriter(out);
		try {
			w.writeStartDocument();
			writer.write(w);
			w.writeEndDocument();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		return out.toByteArray();
	}

	private void writeElementWithValue(String elementName, Object value, XMLStreamWriter xmlw) throws XMLStreamException {
		xmlw.writeStartElement(elementName);
		xmlw.writeCharacters(value.toString());
		xmlw.writeEndElement();
	}

	private XMLStreamWriter getStreamWriter(OutputStream output) {
		try {
			return this.outputFactory.createXMLStreamWriter(output);
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	private void parseConnectorStatus(XMLStreamReader parser, ConnectorStatus status) throws XMLStreamException {
		while (parser.hasNext()) {
			int event = parser.next();
			if ((event == 2) && ("ConnectorInstance".equals(parser.getLocalName()))) {
				return;
			}
			if (event == 1) {
				if ("ConnectorName".equals(parser.getLocalName())) {
					status.setName(parser.getElementText());
				}
				if ("ConnectorType".equals(parser.getLocalName())) {
					status.setType(parser.getElementText());
				}
				if ("Status".equals(parser.getLocalName())) {
					status.setStatus(Integer.parseInt(parser.getElementText()));
				}
				if ("ConnectorSchedules".equals(parser.getLocalName())) {
					String txt = parser.getElementText();
					if (txt.length() > 0) {
						Schedule schedule = new Schedule(txt);
						status.setSchedule(schedule);
					}
				}
			}
		}
	}

	private void close(XMLStreamReader parser) {
		if (parser == null)
			return;
		try {
			parser.close();
		} catch (Throwable t) {
		}
	}

	private abstract class Writer {
		private Writer() {
		}

		abstract void write(XMLStreamWriter paramXMLStreamWriter) throws Exception;
	}
}
