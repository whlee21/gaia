package gaia.crawl.http.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import gaia.crawl.metadata.Metadata;
import gaia.crawl.metadata.SpellCheckedMetadata;

public class HttpResponse {
	private URL url;
	private Metadata metadata = new SpellCheckedMetadata();
	private byte[] data = new byte[0];
	private int code;

	public HttpResponse(HttpProtocol http, URL url, long lastModifiedTime, boolean followRedirects, int maxSize,
			HttpProtocol.Method m) throws IOException {
		this.url = url;
		HttpRequestBase method = null;
		switch (m) {
		case GET:
			method = new HttpGet(url.toString());
			break;
		case HEAD:
			method = new HttpHead(url.toString());
			break;
		case POST:
			method = new HttpPost(url.toString());
		}
		method.getParams().setParameter("http.protocol.handle-redirects", Boolean.valueOf(followRedirects));
		method.getParams().setParameter("http.protocol.handle-authentication", Boolean.valueOf(true));
		if (lastModifiedTime > 0L) {
			method.setHeader("If-Modified-Since", DateUtils.formatDate(new Date(lastModifiedTime)));
		}

		HttpParams params = method.getParams();
		if (http.cfg.isUseHttp11())
			params.setParameter("http.protocol.version", HttpVersion.HTTP_1_1);
		else {
			params.setParameter("http.protocol.version", HttpVersion.HTTP_1_0);
		}
		params.setParameter("http.protocol.content-charset", "UTF-8");
		params.setParameter("http.protocol.cookie-policy", "compatibility");
		params.setParameter("http.protocol.single-cookie-header", Boolean.valueOf(true));

		HttpContext localContext = new BasicHttpContext();
		org.apache.http.HttpResponse rsp = http.getClient().execute(method, localContext);

		code = rsp.getStatusLine().getStatusCode();
		Header[] heads = rsp.getAllHeaders();

		for (int i = 0; i < heads.length; i++) {
			metadata.add(heads[i].getName(), heads[i].getValue());
		}

		int contentLength = Integer.MAX_VALUE;
		String contentLengthString = metadata.get("Content-Length");
		if (contentLengthString != null) {
			try {
				contentLength = Integer.parseInt(contentLengthString.trim());
			} catch (NumberFormatException ex) {
				throw new IOException(new StringBuilder().append("bad content length: ").append(contentLengthString).toString());
			}
		}

		if (m != HttpProtocol.Method.HEAD) {
			if ((maxSize >= 0) && (contentLength > maxSize)) {
				contentLength = maxSize;
			}

			HttpEntity entity = rsp.getEntity();
			InputStream in = null;
			if (entity != null) {
				in = entity.getContent();
			}
			long totalRead = 0L;
			IOException ex = null;
			if (in != null) {
				try {
					byte[] buffer = new byte[8192];
					int bufferFilled = 0;
					ByteArrayOutputStream out = new ByteArrayOutputStream();

					while (((bufferFilled = in.read(buffer, 0, buffer.length)) != -1)
							&& (totalRead + bufferFilled <= contentLength)) {
						totalRead += bufferFilled;
						out.write(buffer, 0, bufferFilled);
					}
					data = out.toByteArray();

					if (in != null) {
						in.close();
					}
					if ((ex != null) || (totalRead < entity.getContentLength()))
						method.abort();
				} catch (Exception e) {
					if (code == 200) {
						ex = new IOException(e.toString());
						throw ex;
					}

					if (in != null) {
						in.close();
					}
					if ((ex != null) || (totalRead < entity.getContentLength()))
						method.abort();
				} finally {
					if (in != null) {
						in.close();
					}
					if ((ex != null) || (totalRead < entity.getContentLength())) {
						method.abort();
					}
				}
			}
		}
		StringBuilder fetchTrace = null;
		if (HttpProtocol.LOG.isTraceEnabled()) {
			fetchTrace = new StringBuilder(new StringBuilder().append("url: ").append(url).append("; status code: ")
					.append(code).append("; bytes received: ").append(data.length).toString());

			if (getMeta("Content-Length") != null) {
				fetchTrace
						.append(new StringBuilder().append("; Content-Length: ").append(getMeta("Content-Length")).toString());
			}
			if (getMeta("Location") != null) {
				fetchTrace.append(new StringBuilder().append("; Location: ").append(getMeta("Location")).toString());
			}
		}
		if ((data != null) && (data.length > 0)) {
			String contentEncoding = metadata.get("Content-Encoding");
			if ((contentEncoding != null) && (HttpProtocol.LOG.isTraceEnabled()))
				fetchTrace.append(new StringBuilder().append("; Content-Encoding: ").append(contentEncoding).toString());
			if (("gzip".equals(contentEncoding)) || ("x-gzip".equals(contentEncoding))) {
				data = HttpContentUtils.processGzipEncoded(data, url, http.cfg.getMaxSize());
				if (data != null) {
					metadata.add("X-Content-Length-Uncompressed", String.valueOf(data.length));
				}
				if (HttpProtocol.LOG.isTraceEnabled())
					fetchTrace.append(new StringBuilder().append("; extracted to ").append(data.length).append(" bytes")
							.toString());
			} else if ("deflate".equals(contentEncoding)) {
				data = HttpContentUtils.processDeflateEncoded(data, url, http.cfg.getMaxSize());
				if (data != null) {
					metadata.add("X-Content-Length-Uncompressed", String.valueOf(data.length));
				}
				if (HttpProtocol.LOG.isTraceEnabled()) {
					fetchTrace.append(new StringBuilder().append("; extracted to ").append(data.length).append(" bytes")
							.toString());
				}
			}
		}

		if (HttpProtocol.LOG.isTraceEnabled())
			HttpProtocol.LOG.trace(fetchTrace.toString());
	}

	public URL getUrl() {
		return url;
	}

	public int getCode() {
		return code;
	}

	public String getMeta(String name) {
		return metadata.get(name);
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public byte[] getData() {
		return data;
	}
}
