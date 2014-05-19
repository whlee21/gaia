package gaia.crawl.gcm.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.eclipse.jetty.http.HttpParser;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.server.LocalConnector;

public class LocalJettyGCMServer extends RemoteGCMServer {
	LocalConnector connector;

	public LocalJettyGCMServer(String baseUrl, LocalConnector connector) {
		super(baseUrl);
		this.connector = connector;
	}

	protected InputStream doGet(String url) throws IOException {
		try {
			URI u = new URI(url);
			ByteArrayBuffer buffer = new ByteArrayBuffer(new StringBuilder().append("GET ").append(u.getPath())
					.append(u.getQuery() != null ? new StringBuilder().append("?").append(u.getQuery()).toString() : "")
					.append(" HTTP/1.0\n\n").toString());

			ByteArrayBuffer response = this.connector.getResponses(buffer, false);

			if (response == null) {
				throw new IOException(new StringBuilder().append("Unable to retrieve GET response for url: ").append(url)
						.toString());
			}
			final ContenBuffer content = new ContenBuffer();

			HttpParser parser = new HttpParser(response, new HttpParser.EventHandler() {
				public void startResponse(Buffer arg0, int arg1, Buffer arg2) throws IOException {
				}

				public void startRequest(Buffer arg0, Buffer arg1, Buffer arg2) throws IOException {
				}

				public void content(Buffer arg0) throws IOException {
					content.content = arg0.asArray();
				}
			});
			parser.parse();

			return new ByteArrayInputStream(content.content);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}

	protected InputStream doPost(String url, byte[] postData) throws IOException {
		try {
			URI u = new URI(url);
			ByteArrayBuffer buffer = new ByteArrayBuffer(new StringBuilder().append("POST ").append(u.getPath())
					.append(" HTTP/1.0\nContent-Length: ").append(postData.length).append("\n\n").append(new String(postData))
					.toString());

			ByteArrayBuffer response = this.connector.getResponses(buffer, false);

			if (response == null) {
				throw new IOException(new StringBuilder().append("Unable to retrieve POST response for url: ").append(url)
						.toString());
			}
			final ContenBuffer content = new ContenBuffer();

			HttpParser parser = new HttpParser(response, new HttpParser.EventHandler() {
				public void startResponse(Buffer arg0, int arg1, Buffer arg2) throws IOException {
				}

				public void startRequest(Buffer arg0, Buffer arg1, Buffer arg2) throws IOException {
				}

				public void content(Buffer arg0) throws IOException {
					content.content = arg0.asArray();
				}
			});
			parser.parse();

			return new ByteArrayInputStream(content.content);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}

	class ContenBuffer {
		byte[] content;

		ContenBuffer() {
		}
	}
}
