package gaia.crawl.aperture;

import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.UpdateController;
import gaia.crawl.datasource.DataSourceUtils;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.impl.TikaParserController;
import gaia.crawl.io.Content;
import gaia.utils.IdGenerator;
import gaia.utils.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.ModelFactory;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.semanticdesktop.aperture.accessor.AccessData;
import org.semanticdesktop.aperture.accessor.DataObject;
import org.semanticdesktop.aperture.accessor.FileDataObject;
import org.semanticdesktop.aperture.accessor.RDFContainerFactory;
import org.semanticdesktop.aperture.crawler.Crawler;
import org.semanticdesktop.aperture.crawler.CrawlerHandler;
import org.semanticdesktop.aperture.crawler.ExitCode;
import org.semanticdesktop.aperture.crawler.mail.MessageDataObject;
import org.semanticdesktop.aperture.datasource.config.DomainBoundaries;
import org.semanticdesktop.aperture.extractor.Extractor;
import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.semanticdesktop.aperture.extractor.ExtractorFactory;
import org.semanticdesktop.aperture.extractor.ExtractorRegistry;
import org.semanticdesktop.aperture.extractor.FileExtractor;
import org.semanticdesktop.aperture.extractor.FileExtractorFactory;
import org.semanticdesktop.aperture.extractor.xmp.XMPExtractorFactory;
import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.rdf.impl.RDFContainerImpl;
import org.semanticdesktop.aperture.subcrawler.SubCrawler;
import org.semanticdesktop.aperture.subcrawler.SubCrawlerException;
import org.semanticdesktop.aperture.subcrawler.SubCrawlerFactory;
import org.semanticdesktop.aperture.subcrawler.SubCrawlerHandler;
import org.semanticdesktop.aperture.subcrawler.SubCrawlerRegistry;
import org.semanticdesktop.aperture.util.IOUtil;
import org.semanticdesktop.aperture.vocabulary.NFO;
import org.semanticdesktop.aperture.vocabulary.NIE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrApertureCallbackHandler extends AbstractApertureCallbackHandler implements CrawlerHandler {
	public UpdateController updateController;
	private static final String PROCESSED_BY_SUBCRAWLER = "processedBySubCrawler";
	private static final String FILES_IN_ARCHIVE = "filesInArchive";
	private static final String PROCESSED = "processed";
	private static final String MAIN_PROCESSED = "mainProcessed";
	private static transient Logger LOG = LoggerFactory.getLogger(SolrApertureCallbackHandler.class);
	protected MimeTypeIdentifier mimeTypeIdentifier;
	protected ExtractorRegistry extractorRegistry;
	protected SubCrawlerRegistry subCrawlerRegistry;
	protected XMPExtractorFactory xmpFactory;
	protected DomainBoundaries domainBoundaries;
	final FieldMapping fieldMapping;
	String errHeader = "ApertureDocumentLoader:";
	protected String defaultField;
	protected IdGenerator<?> idGenerator;
	protected ModelFactory factory = RDF2Go.getModelFactory();
	protected String uuidStr;
	protected String batchIdName;
	protected boolean logExtraDetail;
	protected boolean failUnsupportedFileTypes;
	protected boolean warnUnknownMimeTypes;
	protected boolean addFailedDocs;
	protected boolean addOriginal;
	private AtomicInteger filesCount = new AtomicInteger(0);
	private CrawlStatus status;
	private TikaParserController tika;
	private String uniqueKey;
	private gaia.crawl.datasource.DataSource ds;
	private static long loggedCrawledDocs = 0L;

	private static long maxLoggedCrawledDocs = -1L;
	private static long loggingInterval = 1000L;
	private int maxSize;
	private int maxRetries;
	protected ApertureCrawlState state;
	protected boolean subHandledMainDoc = false;
	protected String handlerName = ".";

	public SolrApertureCallbackHandler(TikaParserController tika, UpdateController updateController, String defaultField,
			gaia.crawl.datasource.DataSource ds, ApertureCrawlState crawlState, String batchId, String uuidStr,
			boolean logExtraDetail, boolean failUnsupportedFileTypes, boolean warnUnknownMimeTypes, boolean addFailedDocs,
			int maxRetries) {
		this.tika = tika;
		this.state = crawlState;
		this.updateController = updateController;
		this.mimeTypeIdentifier = state.mimeTypeIdentifier;
		this.extractorRegistry = state.extractorRegistry;
		this.subCrawlerRegistry = state.subCrawlerRegistry;
		this.xmpFactory = state.xmpExtractorFactory;
		this.defaultField = defaultField;
		this.idGenerator = state.getIdGenerator();
		this.ds = ds;
		this.fieldMapping = ds.getFieldMapping();
		this.uuidStr = uuidStr;
		this.status = state.getStatus();
		this.batchIdName = batchId;
		this.logExtraDetail = logExtraDetail;
		this.failUnsupportedFileTypes = failUnsupportedFileTypes;
		this.warnUnknownMimeTypes = warnUnknownMimeTypes;
		this.addFailedDocs = addFailedDocs;
		if (this.maxRetries < 0) {
			this.maxRetries = Integer.MAX_VALUE;
		}
		this.maxRetries = maxRetries;
		if (this.fieldMapping != null) {
			this.uniqueKey = fieldMapping.getUniqueKey();
			this.addOriginal = fieldMapping.isAddOriginalContent();
		} else {
			this.uniqueKey = "id";
			this.addOriginal = false;
		}

		this.maxSize = ((int) DataSourceUtils.getMaxBytes(ds));
		if (this.maxSize < 0) {
			this.maxSize = 104857600;
			LOG.info(new StringBuilder().append("Maximum content size unlimited (max_bytes==-1), limiting to ")
					.append(maxSize).toString());
			DataSourceUtils.setMaxBytes(ds, maxSize);
		}
	}

	public void accessingObject(Crawler crawler, String url) {
		if ((logExtraDetail)
				&& ((maxLoggedCrawledDocs < 0L) || (loggedCrawledDocs++ < maxLoggedCrawledDocs) || (loggedCrawledDocs
						% loggingInterval == 0L)))
			LOG.info("Accessing: {} crawler: {}", url, crawler.toString());
		if ((maxLoggedCrawledDocs > 0L) && (loggedCrawledDocs == maxLoggedCrawledDocs))
			LOG.info(new StringBuilder().append("Logging of individual crawled documents suspended since threshold of ")
					.append(maxLoggedCrawledDocs).append(" reached; will log once every ").append(loggingInterval)
					.append(" documents").toString());
	}

	private boolean processErrorObject(Crawler crawler, DataObject object) {
		if (object == null) {
			return false;
		}
		String uri = object.getID().toString();
		RDFContainer metadata = object.getMetadata();

		Collection comments = metadata.getAll(NIE.comment);
		String st = metadata.getString(NIE.description);

		if (st != null) {
			metadata.remove(NIE.description);
		}
		if ((comments == null) || (comments.isEmpty())) {
			return false;
		}
		metadata.remove(NIE.comment);
		String errorString = crawler.getAccessData().get(uri, "errorCount");
		int count = 0;
		if (errorString != null) {
			try {
				count = Integer.parseInt(errorString);
			} catch (Exception e) {
				LOG.warn(new StringBuilder().append("Invalid error count in accessData, ignoring: ").append(errorString)
						.toString());
			}
		}
		count++;
		String msg = comments.toString();
		boolean keep = false;
		if (count == maxRetries) {
			addFailedFetching(object, msg, st);
			crawler.getAccessData().put(uri, "errorCount", String.valueOf(count));
			keep = true;
		} else if (count > maxRetries) {
			status.incrementCounter(CrawlStatus.Counter.Failed, -1L);
			objectRemoved(crawler, uri);
			keep = false;
		} else {
			crawler.getAccessData().put(uri, "errorCount", String.valueOf(count));
			keep = true;
		}

		if (uri.equals(DataSourceUtils.getSourceUri(ds))) {
			AccessData accessData = crawler.getAccessData();
			ClosableIterator iter = null;
			try {
				iter = accessData.getUntouchedIDsIterator();
				while (iter.hasNext())
					if (keep) {
						accessData.touch(iter.next().toString());
						status.incrementCounter(CrawlStatus.Counter.Failed);
					} else {
						String id = iter.next().toString();
						accessData.remove(id);
						objectRemoved(crawler, id);
					}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (iter != null) {
					iter.close();
				}
			}
		}
		object.dispose();
		return true;
	}

	public void objectNotModified(Crawler crawler, String url) {
		if ((logExtraDetail)
				&& ((maxLoggedCrawledDocs < 0L) || (loggedCrawledDocs < maxLoggedCrawledDocs) || (loggedCrawledDocs
						% loggingInterval == 0L))) {
			LOG.info("Unchanged: {} handler={}", url, handlerName);
		}
		String rootUrl = ApertureDataSourceUtil.getDomainPattern(ds);

		if ((!domainBoundaries.inDomain(url)) && (!rootUrl.equals(url))) {
			LOG.info("NotModified: removing excluded {}", url);

			objectRemoved(crawler, url);
			return;
		}

		if ("true".equals(crawler.getAccessData().get(url, PROCESSED))) {
			status.incrementCounter(CrawlStatus.Counter.Unchanged);
		}

		Integer filesInArchive = Integer.valueOf(0);
		try {
			filesInArchive = Integer.valueOf(Integer.parseInt(crawler.getAccessData().get(url, FILES_IN_ARCHIVE)));
		} catch (NumberFormatException e) {
		}
		if (filesInArchive.intValue() != 0) {
			status.incrementCounter(CrawlStatus.Counter.Unchanged, filesInArchive.intValue());
		}

		checkForInterrupt(crawler);
	}

	public void objectChanged(Crawler crawler, DataObject object) {
		String url = object.getID().toString();
		String rootUrl = ApertureDataSourceUtil.getDomainPattern(ds);
		if ((!domainBoundaries.inDomain(url)) && (!rootUrl.equals(url))) {
			LOG.debug("Updating: skipping excluded {}", url);
			object.dispose();
			return;
		}
		if (processErrorObject(crawler, object)) {
			return;
		}

		if ((maxLoggedCrawledDocs < 0L) || (loggedCrawledDocs < maxLoggedCrawledDocs)
				|| (loggedCrawledDocs % loggingInterval == 0L))
			LOG.info("Updating: {} handler={}", object.getID(), handlerName);
		int numAdded = processObject(crawler, object, CrawlStatus.Counter.Updated);
	}

	public void objectRemoved(Crawler crawler, String url) {
		if ((maxLoggedCrawledDocs < 0L) || (loggedCrawledDocs < maxLoggedCrawledDocs)
				|| (loggedCrawledDocs % loggingInterval == 0L))
			LOG.info("Deleting: {} handler={}", url, handlerName);
		status.incrementCounter(CrawlStatus.Counter.Deleted);
		try {
			updateController.delete(url);

			String processedBySubCrawler = crawler.getAccessData().get(url, PROCESSED_BY_SUBCRAWLER);
			if (processedBySubCrawler != null) {
				String query = new StringBuilder().append("{!prefix f=").append(uniqueKey != null ? uniqueKey : "id")
						.append("}").append(processedBySubCrawler).append(":").append(url).toString();
				LOG.info(new StringBuilder()
						.append("Deleting archive content docs by using Solr deleteByQuery call with prefix query parser '")
						.append(query).append("'").toString());
				updateController.deleteByQuery(query);
			}

			Integer filesInArchive = Integer.valueOf(0);
			try {
				filesInArchive = Integer.valueOf(Integer.parseInt(crawler.getAccessData().get(url, FILES_IN_ARCHIVE)));
			} catch (NumberFormatException e) {
			}
			if (filesInArchive.intValue() != 0)
				status.incrementCounter(CrawlStatus.Counter.Deleted, filesInArchive.intValue());
		} catch (Exception e) {
			LOG.error("Exception", e);
		}
		checkForInterrupt(crawler);
	}

	public void objectNew(Crawler crawler, DataObject object) {
		String url = object.getID().toString();
		String rootUrl = ApertureDataSourceUtil.getDomainPattern(ds);

		if ((!domainBoundaries.inDomain(url)) && (!rootUrl.equals(url))) {
			LOG.info("New: skipping excluded {}", url);

			filesCount.incrementAndGet();
			object.dispose();
			return;
		}
		if (processErrorObject(crawler, object)) {
			return;
		}

		if ((maxLoggedCrawledDocs < 0L) || (loggedCrawledDocs < maxLoggedCrawledDocs)
				|| (loggedCrawledDocs % loggingInterval == 0L))
			LOG.info("New: {} handler={}", object.getID(), handlerName);
		int numAdded = processObject(crawler, object, CrawlStatus.Counter.New);
		if (numAdded > 0) {
			filesCount.incrementAndGet();

			String processedBySubCrawler = crawler.getAccessData().get(object.getID().toString(), PROCESSED_BY_SUBCRAWLER);
			if (processedBySubCrawler != null) {
				filesCount.addAndGet(numAdded);
				crawler.getAccessData().put(object.getID().toString(), FILES_IN_ARCHIVE, Integer.toString(numAdded));
			}
		}
	}

	protected int processObject(Crawler crawler, DataObject object, CrawlStatus.Counter c) {
		int numDocs = 0;
		org.ontoware.rdf2go.model.node.URI id = object.getID();
		String idStr = id.asJavaURI().toString();
		try {
			RDFContainer container = object.getMetadata();
			if ((object instanceof FileDataObject)) {
				FileDataObject fdo = (FileDataObject) object;
				FieldValues perDocVals = new FieldValues();
				int numProcessed = extract(fdo.getContent(), id, fdo, crawler, perDocVals);

				boolean mainProcessed = Boolean.parseBoolean(crawler.getAccessData().get(object.getID().toString(),
						MAIN_PROCESSED));
				if ((numProcessed != 0) && (!mainProcessed)) {
					addDoc(idStr, container, Collections.<String> emptySet(), perDocVals, c);
				}
				numDocs = numProcessed;

				if (status.getCounter(CrawlStatus.Counter.Total) >= maxDocs) {
					LOG.info(new StringBuilder().append("Stopping crawl because maxDocs=").append(maxDocs)
							.append(" limit was reached or exceeded.").toString());
					crawler.stop();
				}
			} else if ((object instanceof MessageDataObject)) {
				MimeMessage mimeMessage = ((MessageDataObject) object).getMimeMessage();
				FieldValues fieldValues = new FieldValues();
				if (addOriginal) {
					try {
						InputStream content = mimeMessage.getRawInputStream();
						byte[] data = IOUtil.readBytes(content, maxSize);
						fieldValues.add(new FieldValue("", "original_content", data, 1.0F));
					} catch (Exception e) {
						LOG.warn(new StringBuilder().append("Can't cache original content of message ")
								.append(mimeMessage.getMessageID()).append(": ").append(e.getMessage()).toString());
					}
				}
				Address[] to;
				try {
					to = mimeMessage.getRecipients(MimeMessage.RecipientType.TO);
				} catch (Exception e) {
					to = new Address[] { new InternetAddress(e.getClass().getSimpleName()) };
				}
				Address[] cc;
				try {
					cc = mimeMessage.getRecipients(MimeMessage.RecipientType.CC);
				} catch (Exception e) {
					cc = new Address[] { new InternetAddress(e.getClass().getSimpleName()) };
				}
				Address[] bcc;
				try {
					bcc = mimeMessage.getRecipients(MimeMessage.RecipientType.BCC);
				} catch (Exception e) {
					bcc = new Address[] { new InternetAddress(e.getClass().getSimpleName()) };
				}
				Address[] from;
				try {
					from = mimeMessage.getFrom();
				} catch (Exception e) {
					from = new Address[] { new InternetAddress(e.getClass().getSimpleName()) };
				}
				addAddresses(id, fieldValues, to, "to", 1.0F);
				addAddresses(id, fieldValues, cc, "cc", 1.0F);
				addAddresses(id, fieldValues, bcc, "bcc", 1.0F);
				addAddresses(id, fieldValues, from, "from", 1.0F);
				addDoc(idStr, container, Collections.<String> emptySet(), fieldValues, c);
				numDocs = 1;
			}

		} catch (Exception e) {
			LOG.warn(new StringBuilder().append("Exception while crawling ").append(object).append(": ").append(e.toString())
					.toString(), e);
			addFailedParsingDoc(idStr, object, e, null, CrawlStatus.Counter.Failed);
			numDocs = -1;
		} catch (OutOfMemoryError oome) {
			LOG.warn(new StringBuilder().append("File caused an Out of Memory Exception, skipping: ").append(object)
					.toString(), oome);

			addFailedParsingDoc(idStr, object, oome, null, CrawlStatus.Counter.Failed);
			numDocs = -1;
		} catch (Error e) {
			LOG.warn(new StringBuilder().append("Error while crawling ").append(object).append(": ").append(e.toString())
					.toString(), e);
			addFailedParsingDoc(idStr, object, e, null, CrawlStatus.Counter.Failed);
			numDocs = -1;
		} finally {
			object.dispose();
		}
		checkForInterrupt(crawler);

		if (numDocs != 0) {
			crawler.getAccessData().put(object.getID().toString(), PROCESSED, "true");
		}

		return numDocs;
	}

	private void checkForInterrupt(Crawler crawler) {
	}

	private void addAddresses(org.ontoware.rdf2go.model.node.URI id, FieldValues values, Address[] recips,
			String fldName, float boost) {
		if ((recips != null) && (id != null) && (fldName != null))
			for (int i = 0; i < recips.length; i++) {
				Address recip = recips[i];
				values.add(new FieldValue(id.asJavaURI().toString(), fldName, recip.toString(), boost));
			}
	}

	protected Set<String> processLinkAuthorityLinks(String id, AccessData accessData) {
		if (accessData == null) {
			return Collections.emptySet();
		}
		Set<String> linkIds = accessData.getReferredIDs(id);
		Set<String> links;
		if (linkIds != null) {
			links = new HashSet<String>();
			for (String link : linkIds) {
				boolean addLink = false;
				if ((domainBoundaries == null) && (link.startsWith("http://")))
					addLink = true;
				else if ((domainBoundaries != null) && (domainBoundaries.inDomain(link))) {
					addLink = true;
				}
				if (addLink)
					links.add(link);
			}
		} else {
			return Collections.<String> emptySet();
		}
		return links;
	}

	protected void addDoc(String id, RDFContainer container, Set<String> hyperlinks, FieldValues perDocVals,
			CrawlStatus.Counter c) throws IOException {
		SolrInputDocument doc = new SolrInputDocument();
		boolean seenUnique = FieldsHelper.addFieldsToDocument(doc, defaultField, container, fieldMapping);

		if ((perDocVals != null) && (!perDocVals.isEmpty())) {
			seenUnique = (FieldsHelper.addFieldValues(perDocVals, doc, uniqueKey, id)) || (seenUnique);
		}

		StringBuilder sb = new StringBuilder();
		Date date = new Date();
		try {
			DateUtil.formatDate(date, null, sb);
		} catch (IOException ioe) {
			LOG.warn("Cannot format date", ioe);
			sb.setLength(0);
			sb.append(date.toString());
		}
		doc.addField("fetch_time", sb.toString());
		if (!seenUnique) {
			doc.addField(uniqueKey, id, 1.0F);
		}

		doc.addField("crawl_uri", id);
		if (batchIdName != null) {
			doc.addField("batch_id", uuidStr);
		}

		for (String link : hyperlinks) {
			doc.addField("field_authority_links", link);
		}

		finishAddDoc(doc, id.toString(), c);
	}

	protected void finishAddDoc(SolrInputDocument doc, String id, CrawlStatus.Counter c) throws IOException {
		if (c != null) {
			status.incrementCounter(c);
		}
		updateController.add(doc, id);
	}

	public void crawlStarted(Crawler crawler) {
		LOG.info(new StringBuilder().append("Crawl started for data source: ")
				.append(crawler.getDataSource().getID().toString()).toString());

		loggedCrawledDocs = 0L;
	}

	public void crawlStopped(Crawler crawler, ExitCode exitCode) {
		LOG.info(new StringBuilder().append("Crawl stopped for data source: ")
				.append(crawler.getDataSource().getID().toString()).append(" exitCode: ").append(exitCode.toString())
				.toString());
	}

	void extract(InputStream inputStream, org.ontoware.rdf2go.model.node.URI uri, FileDataObject fdo,
			FieldValues perDocVals) throws IOException {
		extract(inputStream, uri, fdo, null, perDocVals);
	}

	public static org.ontoware.rdf2go.model.node.URI getURIFromName(String name) {
		if (name.indexOf("://") == -1) {
			name = new StringBuilder().append("data:,name=").append(name).toString();
		}
		return new URIImpl(name);
	}

	protected int extract(InputStream inputStream, org.ontoware.rdf2go.model.node.URI uri, FileDataObject object,
			Crawler crawler, FieldValues perDocVals) {
		int numProcessed = 0;

		String uriStr = uri.asJavaURI().toString();
		try {
			byte[] data = IOUtil.readBytes(inputStream, maxSize);

			int bufferSize = Math.min(Math.max(mimeTypeIdentifier.getMinArrayLength(), 8192), data.length);

			byte[] bytes = new byte[bufferSize];
			System.arraycopy(data, 0, bytes, 0, bufferSize);

			RDFContainer metadata = object.getMetadata();
			String mimeType = mimeTypeIdentifier.identify(bytes, metadata.getString(NFO.fileName), uri);

			if ((mimeType != null) && (mimeType.equals("text/xml"))) {
				mimeType = "text/html";
			}

			if ((mimeType == null) || (mimeType.equals("text/plain"))) {
				if (StringUtils.isFullText(bytes)) {
					if (warnUnknownMimeTypes)
						LOG.warn(new StringBuilder().append("Trying plain text for ").append(uri).toString());
					mimeType = "text/plain";
				} else {
					if (warnUnknownMimeTypes)
						LOG.warn(new StringBuilder().append("Trying application/octet-stream for non-text file: ").append(uri)
								.toString());
					mimeType = "application/octet-stream";
				}
			}

			mimeType = MimeTypeVerificator.check(uriStr, mimeType, state.getDataSource());

			metadata.remove(NIE.mimeType);
			metadata.add(NIE.mimeType, mimeType);

			Exception logException = null;
			try {
				boolean status = applyExtractor(uri, data, mimeType, metadata, perDocVals);
				if (status)
					numProcessed = 1;
			} catch (Exception e) {
				logException = e;
			}
			if (numProcessed == 0) {
				boolean noExtractor = false;
				try {
					boolean status = applyFileExtractor(object, uri, mimeType, metadata, perDocVals);
					if (status)
						numProcessed = 1;
				} catch (Exception e) {
					logException = e;
				}
				if ((numProcessed == 0) && (crawler != null)) {
					try {
						numProcessed = applySubCrawler(data, mimeType, object, crawler);

						if (numProcessed == -1) {
							noExtractor = true;
							numProcessed = 0;
						}
					} catch (Exception e) {
						logException = e;
					}
				}
				if (numProcessed == 0) {
					String message = null;

					LOG.info(new StringBuilder().append("Using Tika to parse ").append(object.getID().toString()).toString());
					Content c = makeContent(uri, object, data);
					List docs = null;
					try {
						docs = tika.parse(c);
					} catch (Exception e) {
						if (e.getCause() != null)
							message = e.getCause().getMessage();
						else {
							message = e.getMessage();
						}
					}
					if ((docs == null) || (docs.size() == 0)) {
						if (message == null)
							message = new StringBuilder().append("No extractor for ").append(mimeType).append("; Skipping: ")
									.append(uri).toString();
						else
							message = new StringBuilder().append(message).append("; Skipping: ").append(uri).toString();
					} else {
						numProcessed = docs.size();

						crawler.getAccessData().put(object.getID().toString(), MAIN_PROCESSED, Boolean.TRUE.toString());
						for (int k = 0; k < docs.size(); k++) {
							SolrInputDocument d = (SolrInputDocument) docs.get(k);
							String id = (String) d.getFieldValue("id");

							if (id == null) {
								id = new StringBuilder().append(object.getID().toString()).append("!").append(k).toString();
							}
							if (d.getFieldValue("batch_id") == null) {
								d.addField("batch_id", uuidStr);
							}
							if (d.getFieldValue("crawl_uri") == null) {
								d.addField("crawl_uri", id);
							}
							d.addField("parsing", "ok (Tika)");
							finishAddDoc(d, id, CrawlStatus.Counter.New);
						}
					}
					if (numProcessed == 0)
						if (failUnsupportedFileTypes)
							addFailedParsingDoc(uriStr, object, null, message, CrawlStatus.Counter.Failed);
						else
							LOG.warn(message);
				}
			}
		} catch (IOException e) {
			LOG.warn(new StringBuilder().append("IO Exception processing ").append(uri).append(": ").append(e.toString())
					.toString());
			addFailedParsingDoc(uriStr, object, e, null, CrawlStatus.Counter.Failed);
		}

		return numProcessed;
	}

	private Content makeContent(org.ontoware.rdf2go.model.node.URI uri, FileDataObject object, byte[] data)
			throws IOException {
		Content c = new Content();
		c.setKey(uri.toString());
		c.setData(data);
		String path = uri.asJavaURI().getPath();
		if (path != null) {
			String[] els = path.split("/");
			if (els.length > 0) {
				c.addMetadata("resourceName", els[(els.length - 1)]);
			}

		}

		return c;
	}

	private boolean applyExtractor(org.ontoware.rdf2go.model.node.URI id, byte[] data, String mimeType,
			RDFContainer metadata, FieldValues perDocVals) throws ExtractorException, IOException {
		InputStream contentStream = new ByteArrayInputStream(data);
		Set extractors = extractorRegistry.getExtractorFactories(mimeType);
		boolean supportedByXmp = xmpFactory.getSupportedMimeTypes().contains(mimeType);

		boolean result = false;
		if (!extractors.isEmpty()) {
			ExtractorFactory factory = (ExtractorFactory) extractors.iterator().next();

			Extractor extractor = factory.get();

			extractor.extract(id, contentStream, null, mimeType, metadata);
			result = true;
		}
		if ((!result) && (supportedByXmp)) {
			Extractor extractor = xmpFactory.get();

			extractor.extract(id, contentStream, null, mimeType, metadata);
			result = true;
		}

		if ((result) && (addOriginal)) {
			perDocVals.add(new FieldValue("", "original_content", data, 1.0F));
		}
		return result;
	}

	private boolean applyFileExtractor(FileDataObject object, org.ontoware.rdf2go.model.node.URI id, String mimeType,
			RDFContainer metadata, FieldValues perDocVals) throws ExtractorException, IOException {
		Set fileExt = extractorRegistry.getFileExtractorFactories(mimeType);
		if (!fileExt.isEmpty()) {
			if ((maxLoggedCrawledDocs < 0L) || (loggedCrawledDocs++ < maxLoggedCrawledDocs)) {
				LOG.info("Trying file extractor for id: {} with mime type: {}", id, mimeType);
			}
			FileExtractorFactory factory = (FileExtractorFactory) fileExt.iterator().next();

			FileExtractor extractor = factory.get();
			File originalFile = object.getFile();
			if (originalFile != null) {
				if (addOriginal) {
					try {
						byte[] data = IOUtil.readBytes(new FileInputStream(originalFile), maxSize);
						if ((data != null) && (data.length > 0))
							perDocVals.add(new FieldValue("", "original_content", data, 1.0F));
					} catch (Exception e) {
						LOG.warn(new StringBuilder().append("Can't cache data from ").append(originalFile).append(": ")
								.append(e.getMessage()).toString());
					}
				}
				extractor.extract(id, originalFile, null, mimeType, metadata);
				return true;
			}
			File tempFile = object.downloadContent();
			if (addOriginal)
				try {
					byte[] data = IOUtil.readBytes(new FileInputStream(tempFile), maxSize);
					if ((data != null) && (data.length > 0))
						perDocVals.add(new FieldValue("", "original_content", data, 1.0F));
				} catch (Exception e) {
					LOG.warn(new StringBuilder().append("Can't cache data from ").append(tempFile).append(": ")
							.append(e.getMessage()).toString());
				}
			try {
				extractor.extract(id, tempFile, null, mimeType, metadata);
				return true;
			} finally {
				if (tempFile != null) {
					if (!tempFile.delete()) {
						tempFile.deleteOnExit();
					}
				}
			}
		}

		return false;
	}

	private int applySubCrawler(byte[] data, String mimeType, final DataObject object, final Crawler crawler)
			throws SubCrawlerException {
		Set subCrawlers = subCrawlerRegistry.get(mimeType);
		if (!subCrawlers.isEmpty()) {
			if ((maxLoggedCrawledDocs < 0L) || (loggedCrawledDocs++ < maxLoggedCrawledDocs))
				LOG.info("Sub crawling for mime type: {}", mimeType);
			SubCrawlerFactory factory = (SubCrawlerFactory) subCrawlers.iterator().next();

			SubCrawler subCrawler = factory.get();

			final SolrApertureCallbackHandler tmp = selfFactory(object);
			tmp.handlerName = new StringBuilder().append(handlerName).append("/")
					.append(subCrawler.getClass().getSimpleName()).toString();

			SubCrawlerHandler handler = new SubCrawlerHandler() {
				public RDFContainerFactory getRDFContainerFactory(String url) {
					return tmp.getRDFContainerFactory(crawler, url);
				}

				public void objectNew(DataObject subObject) {
					if (subObject.getID().equals(object.getID())) {
						tmp.subHandledMainDoc = true;
					}
					tmp.objectNew(crawler, subObject);
				}

				public void objectChanged(DataObject subObject) {
					tmp.objectChanged(crawler, subObject);
				}

				public void objectNotModified(String url) {
					tmp.objectNotModified(crawler, url);
				}
			};
			org.semanticdesktop.aperture.datasource.DataSource dataSource = object.getDataSource();
			InputStream contentStream = new ByteArrayInputStream(data);
			subCrawler.subCrawl(object.getID(), contentStream, handler, dataSource, null, null, mimeType,
					object.getMetadata());

			crawler.getAccessData().put(object.getID().toString(), PROCESSED_BY_SUBCRAWLER, factory.getUriPrefix());
			crawler.getAccessData().put(object.getID().toString(), MAIN_PROCESSED, Boolean.toString(tmp.subHandledMainDoc));
			return tmp.filesCount.intValue();
		}
		return -1;
	}

	protected SolrApertureCallbackHandler selfFactory(DataObject object) {
		SolrApertureCallbackHandler res = new SolrApertureCallbackHandler(tika, updateController,
				defaultField, ds, state, batchIdName, uuidStr, logExtraDetail,
				failUnsupportedFileTypes, warnUnknownMimeTypes, addFailedDocs, maxRetries);

		res.setDomainBoundaries(domainBoundaries);
		return res;
	}

	public void addFailedParsingDoc(String uri, DataObject object, Throwable t, String message, CrawlStatus.Counter c) {
		String exceptionMsg = null;
		if (t != null) {
			while (t.getCause() != null) {
				t = t.getCause();
			}
			String msg = t.getMessage();
			if (msg != null)
				exceptionMsg = msg;
			else {
				exceptionMsg = t.getClass().getName();
			}

		}

		LOG.warn(CrawlerUtils.msgDocFailed(ds.getCollection(), ds, uri, exceptionMsg));

		if ((object != null) && (addFailedDocs)) {
			FieldValues fv = new FieldValues();
			fv.add(new FieldValue("", "parsing", new StringBuilder().append("failed: (").append(c.name()).append(") ")
					.append(exceptionMsg).toString(), 1.0F));
			try {
				addDoc(uri, object.getMetadata(), Collections.<String> emptySet(), fv, CrawlStatus.Counter.Failed);
			} catch (IOException e) {
				LOG.warn(new StringBuilder().append("Exception adding failed doc: ").append(e.getMessage()).toString());
			}
		} else {
			status.incrementCounter(CrawlStatus.Counter.Failed);
		}
	}

	public void addFailedFetching(DataObject object, String msg, String status) {
		if ((object != null) && (addFailedDocs)) {
			FieldValues fv = new FieldValues();
			String message = "failed:";
			if (msg != null) {
				message = new StringBuilder().append(message).append(" (").append(msg).append(")").toString();
			}
			if (status != null) {
				message = new StringBuilder().append(message).append(" (").append(status).append(")").toString();
			}
			if ((object != null) && (addFailedDocs)) {
				fv.add(new FieldValue("", "fetching", message, 1.0F));
				try {
					addDoc(object.getID().toString(), object.getMetadata(), Collections.<String> emptySet(), fv, null);
				} catch (IOException e) {
					LOG.warn(new StringBuilder().append("Exception adding failed doc: ").append(e.getMessage()).toString());
				}
			}
		}
	}

	public RDFContainer getRDFContainer(org.ontoware.rdf2go.model.node.URI uri) {
		Model m = RDF2Go.getModelFactory().createModel();
		m.open();
		return new RDFContainerImpl(m, uri);
	}

	public DomainBoundaries getDomainBoundaries() {
		return domainBoundaries;
	}

	public void setDomainBoundaries(DomainBoundaries domainBoundaries) {
		this.domainBoundaries = domainBoundaries;
	}

	protected RDFContainerImpl newInstance(String uri) {
		try {
			Model newModel = factory.createModel();
			newModel.open();
			return new RDFContainerImpl(newModel, uri);
		} catch (ModelRuntimeException me) {
			throw new RuntimeException(me);
		}
	}
}
