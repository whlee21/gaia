package gaia.crawl.aperture;

import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.http.protocol.HttpProtocol;
import gaia.crawl.http.protocol.HttpProtocolConfig;
import gaia.crawl.http.protocol.ProtocolOutput;
import gaia.crawl.http.protocol.ProtocolStatus;
import gaia.crawl.http.robots.RobotsCache;
import gaia.utils.UrlUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.apache.solr.common.util.DateUtil;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.semanticdesktop.aperture.accessor.AccessData;
import org.semanticdesktop.aperture.accessor.DataAccessor;
import org.semanticdesktop.aperture.accessor.DataObject;
import org.semanticdesktop.aperture.accessor.RDFContainerFactory;
import org.semanticdesktop.aperture.accessor.UrlNotFoundException;
import org.semanticdesktop.aperture.accessor.base.FileDataObjectBase;
import org.semanticdesktop.aperture.accessor.http.ContentType;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.vocabulary.NFO;
import org.semanticdesktop.aperture.vocabulary.NIE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GaiaHttpAccessor implements DataAccessor {
	private static final Logger LOG = LoggerFactory.getLogger(GaiaHttpAccessor.class);
	private static final String ACCESSED_KEY = "accessed";
	private HttpProtocol protocol;
	private gaia.crawl.datasource.DataSource ds;
	private CrawlState crawlState;

	public GaiaHttpAccessor(gaia.crawl.datasource.DataSource ds, CrawlState crawlState, RobotsCache robots,
			HttpProtocolConfig cfg) {
		protocol = new HttpProtocol(cfg, robots);
		this.ds = ds;
		this.crawlState = crawlState;
	}

	public DataObject getDataObject(String url, org.semanticdesktop.aperture.datasource.DataSource source, Map params,
			RDFContainerFactory containerFactory) throws UrlNotFoundException, IOException {
		return get(url, source, null, params, containerFactory);
	}

	public DataObject getDataObjectIfModified(String url, org.semanticdesktop.aperture.datasource.DataSource source,
			AccessData accessData, Map params, RDFContainerFactory containerFactory) throws UrlNotFoundException, IOException {
		return get(url, source, accessData, params, containerFactory);
	}

	private DataObject get(String urlString, org.semanticdesktop.aperture.datasource.DataSource source,
			AccessData accessData, Map params, RDFContainerFactory containerFactory) throws UrlNotFoundException, IOException {
		String originalUrlString = urlString;

		URI uri = null;
		URL url = null;
		try {
			url = UrlUtils.normalizeURL(urlString);
			urlString = url.toExternalForm();
			uri = new URIImpl(urlString);
		} catch (Exception e) {
			crawlState.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
			LOG.warn(CrawlerUtils.msgDocFailed(ds.getCollection(), ds, urlString, "Invalid URL: " + e.toString()));

			throw new UrlNotFoundException(e.getMessage());
		}

		ProtocolOutput output = null;

		Date ifModifiedSince = accessData == null ? null : getIfModifiedSince(urlString, accessData);
		long modifiedTime = ifModifiedSince != null ? ifModifiedSince.getTime() : 0L;

		long fetchTime = System.currentTimeMillis();
		output = protocol.getProtocolOutput(url, modifiedTime);
		if (output != null) {
			ProtocolStatus.Code sCode = output.getStatus().code;

			if (isRedirected(sCode)) {
				return recordErrorObject(uri, source, output, accessData, fetchTime, containerFactory, null,
						"too many redirections, max = " + protocol.getConfig().getMaxRedirects() + ", url = " + originalUrlString
								+ ", lastUrl=" + urlString);
			}

			String[] redirects = output.getMetadata().getValues("X-Proto-Redirects-URL");
			if ((redirects != null) && (redirects.length > 0)) {
				String lastUrl = urlString;
				try {
					URIImpl utmp = new URIImpl(lastUrl);
				} catch (Exception e) {
					return recordErrorObject(uri, source, output, accessData, fetchTime, containerFactory, e,
							"invalid redirection, original url = " + originalUrlString + ", lastUrl=" + lastUrl);
				}

				for (String redirect : redirects) {
					if (accessData != null) {
						accessData.remove(lastUrl, "date");
						accessData.remove(lastUrl, ACCESSED_KEY);
						try {
							URL urlTmp = UrlUtils.normalizeURL(redirect);
							redirect = urlTmp.toExternalForm();
							URIImpl utmp = new URIImpl(redirect);
						} catch (Exception e) {
							LOG.warn("Invalid redirect URL: " + e.toString());
							continue;
						}
						accessData.put(lastUrl, "redirectsTo", redirect);
					}
					lastUrl = redirect;
				}
				try {
					uri = new URIImpl(lastUrl);
				} catch (Exception e) {
					crawlState.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
					LOG.warn(CrawlerUtils.msgDocFailed(ds.getCollection(), ds, lastUrl, "Invalid URL when redirecting from "
							+ urlString + ": " + e.toString()));

					return recordErrorObject(uri, source, output, accessData, fetchTime, containerFactory, e,
							"invalid URL when redirecting from " + urlString + ", lastUrl=" + lastUrl);
				}

				if (urlString.equals(lastUrl)) {
					crawlState.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
					LOG.warn(CrawlerUtils.msgDocFailed(ds.getCollection(), ds, urlString, "URL redirects to itself"));

					return recordErrorObject(uri, source, output, accessData, fetchTime, containerFactory, null,
							"url redirects to itself: " + urlString);
				}

				urlString = lastUrl;
			}
			String msg = urlString;
			if ((redirects != null) && (redirects.length > 0)) {
				msg = msg + ", originUrl = " + originalUrlString + ", redirects = " + Arrays.toString(redirects);
			}

			if (sCode == ProtocolStatus.Code.NOT_FOUND) {
				crawlState.getStatus().incrementCounter(CrawlStatus.Counter.Not_Found);
				LOG.warn(CrawlerUtils.msgDocFailed(ds.getCollection(), ds, msg, "URL not found:, code = " + sCode));

				throw new UrlNotFoundException(urlString);
			}
			if (sCode == ProtocolStatus.Code.NOT_MODIFIED) {
				return null;
			}
			if (sCode == ProtocolStatus.Code.ACCESS_DENIED) {
				crawlState.getStatus().incrementCounter(CrawlStatus.Counter.Access_Denied);
				LOG.warn(CrawlerUtils.msgDocFailed(ds.getCollection(), ds, msg, "Access denied:, code = " + sCode));

				return recordErrorObject(uri, source, output, accessData, fetchTime, containerFactory, null, msg
						+ ", Access denied: code = " + sCode);
			}
			if (sCode == ProtocolStatus.Code.ROBOTS_DENIED) {
				crawlState.getStatus().incrementCounter(CrawlStatus.Counter.Robots_Denied);
				LOG.warn(CrawlerUtils.msgDocFailed(ds.getCollection(), ds, msg, "Robots denied:, code = " + sCode));

				return recordErrorObject(uri, source, output, accessData, fetchTime, containerFactory, null, msg
						+ ", Robots denied: code = " + sCode);
			}
			if (sCode != ProtocolStatus.Code.OK) {
				String message = output.getStatus().message;
				if ((sCode == ProtocolStatus.Code.EXCEPTION)
						&& (message != null)
						&& ((message.startsWith("java.net.ConnectException"))
								|| (message.startsWith("java.net.UnknownHostException")) || (message
									.startsWith("java.net.SocketException")))) {
					crawlState.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
					LOG.warn(CrawlerUtils.msgDocFailed(ds.getCollection(), ds, msg, "Failed accessing URL:" + message
							+ ", code = " + sCode));

					return recordErrorObject(uri, source, output, accessData, fetchTime, containerFactory, null, msg
							+ ", Failed accessing URL:" + message + ", code = " + sCode);
				}

				crawlState.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
				LOG.warn(CrawlerUtils.msgDocFailed(ds.getCollection(), ds, msg, "Http connection error:"
						+ output.getStatus().message + ", code = " + sCode));

				return recordErrorObject(uri, source, output, accessData, fetchTime, containerFactory, null, msg
						+ ", Http connection error:" + output.getStatus().message + ", code = " + sCode);
			}

		}

		DataObject result = createDataObject(uri, source, output, containerFactory);

		updateAccessData(accessData, urlString, fetchTime);

		return result;
	}

	private Date getIfModifiedSince(String urlString, AccessData accessData) {
		if (accessData == null) {
			return null;
		}

		String value = accessData.get(urlString, "date");
		if (value == null) {
			return null;
		}
		try {
			long l = Long.parseLong(value);
			return new Date(l);
		} catch (NumberFormatException e) {
			LOG.error("invalid long: " + value, e);
		}
		return null;
	}

	private boolean isRedirected(ProtocolStatus.Code code) {
		return (code == ProtocolStatus.Code.REDIRECT_PERM) || (code == ProtocolStatus.Code.REDIRECT_TEMP)
				|| (code == ProtocolStatus.Code.REDIRECT_PROXY);
	}

	private DataObject recordErrorObject(URI uri, org.semanticdesktop.aperture.datasource.DataSource source,
			ProtocolOutput output, AccessData accessData, long fetchTime, RDFContainerFactory containerFactory, Exception e,
			String message) throws IOException {
		DataObject res = createDataObject(uri, source, output, containerFactory);
		RDFContainer metadata = res.getMetadata();
		if (e != null) {
			metadata.add(NIE.comment, e.toString());
		}
		if (message != null) {
			metadata.add(NIE.comment, message);
		}
		if ((e == null) && (message == null)) {
			metadata.add(NIE.comment, "unspecified error");
		}
		updateAccessData(accessData, uri.toString(), fetchTime);
		return res;
	}

	private DataObject createDataObject(URI uri, org.semanticdesktop.aperture.datasource.DataSource source,
			ProtocolOutput output, RDFContainerFactory containerFactory) throws IOException {
		RDFContainer metadata = containerFactory.getRDFContainer(uri);
		if (output == null) {
			output = new ProtocolOutput(new URL(uri.toString()), null, null, new ProtocolStatus(
					ProtocolStatus.Code.EXCEPTION, "null output", -1));
		}

		ByteArrayInputStream bis = new ByteArrayInputStream(output.getData());

		DataObject object = new FileDataObjectBase(uri, source, metadata, bis);

		String characterSet = null;
		String mimeType = null;

		String contentType = output.getMetadata().get("Content-Type");
		if (contentType != null) {
			ContentType parsedType = new ContentType(contentType);
			characterSet = parsedType.getCharset();
			mimeType = parsedType.getMimeType();
		}

		if (characterSet == null) {
			characterSet = "ISO-8859-1";
		}
		metadata.put(NIE.description, output.getStatus().toString());

		metadata.add(RDF.type, NFO.RemoteDataObject);
		metadata.add(RDF.type, NIE.InformationElement);
		metadata.add(NIE.characterSet, characterSet);

		if (mimeType != null) {
			metadata.add(NIE.mimeType, mimeType);
		}

		String lengthString = output.getMetadata().get("Content-Length");
		long contentLength = 0L;
		if ((lengthString != null) && (lengthString.length() > 0)) {
			try {
				contentLength = Long.parseLong(lengthString);
			} catch (Exception e) {
				LOG.warn("error parsing content length header", e);
			}
		}
		if (contentLength >= 0L) {
			metadata.add(NIE.byteSize, contentLength);
		}

		String lastModString = output.getMetadata().get("Last-Modified");
		long lastModified = 0L;
		if ((lastModString != null) && (lastModString.length() > 0)) {
			try {
				Date d = DateUtil.parseDate(lastModString);
				lastModified = d.getTime();
			} catch (Exception e) {
				LOG.warn("error parsing last modified header", e);
			}
		}
		if (lastModified != 0L) {
			metadata.add(NIE.contentLastModified, new Date(lastModified));
		}

		return object;
	}

	private void updateAccessData(AccessData accessData, String urlString, long date) {
		if (accessData != null) {
			accessData.remove(urlString, ACCESSED_KEY);
			accessData.remove(urlString, "date");
			accessData.remove(urlString, "redirectsTo");

			if (date == 0L) {
				accessData.remove(urlString, "date");
				accessData.remove(urlString, "redirectsTo");

				accessData.put(urlString, ACCESSED_KEY, "");
			} else {
				accessData.remove(urlString, ACCESSED_KEY);
				accessData.remove(urlString, "redirectsTo");

				accessData.put(urlString, "date", String.valueOf(date));
			}
		}
	}
}
