package gaia.crawl.gcm.feeder;

import gaia.crawl.CrawlId;
import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerController;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.gcm.GCMController;
import gaia.crawl.gcm.GCMCrawlState;
import gaia.crawl.gcm.GCMParser;
import gaia.crawl.gcm.LWEGCMAdaptor;
import gaia.crawl.gcm.filter.XmlTagFilter;
import gaia.crawl.io.Content;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

public class GaiaFeedConsumer {
	private static final String FAILURE = "Failure";
	private static final String SUCCESS = "Success";
	private static final String DOCUMENT_LIMIT_REACHED = "Document Limit reached";
	private final GCMController controller;
	private static final Logger LOG = LoggerFactory.getLogger(GaiaFeedConsumer.class);

	private static Map<String, Map<String, Set<String>>> dsToInclusions = new HashMap<String, Map<String, Set<String>>>();
	private static Map<String, Map<String, Set<String>>> dsToExclusions = new HashMap<String, Map<String, Set<String>>>();

	public GaiaFeedConsumer(GCMController controller) {
		this.controller = controller;
	}

	public static void setInclusions(String ds, Map<String, Set<String>> tags) {
		LOG.info("Setting inclusions of ds = " + ds + " to " + tags);
		dsToInclusions.put(ds, tags);
	}

	public static void setExclusions(String ds, Map<String, Set<String>> tags) {
		LOG.info("Setting exclusions of ds = " + ds + " to " + tags);
		dsToExclusions.put(ds, tags);
	}

	public String process(InputStream is) {
		boolean limitReached = false;
		try {
			GCMParser parser = new GCMParser(is, this.controller);
			parser.parse();
			is.close();
			GCMCrawlState state = (GCMCrawlState) this.controller.getCrawlState(new CrawlId(parser.getDatasource()));

			if (state == null) {
				throw new IllegalStateException("No job found for id:" + parser.getDatasource());
			}

			LWEGCMAdaptor adaptor = LWEGCMAdaptor.getAdaptor(state.getDataSource().getType());

			for (int i = 0; (i < parser.getForbidden().size()) && (!limitReached); i++) {
				String key = (String) parser.getForbidden().get(i);
				LOG.info("Processing forbidden entry with key = [ " + key + " ]");
				try {
					limitReached = enforceLimits(this.controller, state);
					if (limitReached) {
						LOG.warn("LimitReached while processing Sharepoint FORBIDDEN.");
						break;
					}

					state.getProcessor().getUpdateController().delete(key);
					state.getStatus().incrementCounter(CrawlStatus.Counter.Access_Denied);
				} catch (Exception e) {
					LOG.warn(
							CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(), key,
									e.getMessage()), e);

					state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
				}
			}

			for (int i = 0; (i < parser.getDeletes().size()) && (!limitReached); i++) {
				String key = (String) parser.getDeletes().get(i);
				LOG.info("Processing delete with key = [ " + key + " ]");
				try {
					limitReached = enforceLimits(this.controller, state);
					if (limitReached) {
						LOG.warn("LimitReached while processing Sharepoint DELETES.");
						break;
					}
					state.getProcessor().delete(key);
				} catch (Exception e) {
					LOG.warn(
							CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(), key,
									e.getMessage()), e);

					state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
				}
			}

			for (int i = 0; (i < parser.getAdditions().size()) && (!limitReached); i++) {
				Content content = (Content) parser.getAdditions().get(i);
				LOG.info("Processing addition with key = [ " + content.getKey() + " ]");
				if (content.getData() == null)
					content.setData(new byte[0]);
				try {
					limitReached = enforceLimits(this.controller, state);
					if (limitReached) {
						LOG.warn("LimitReached while processing Sharepoint ADDITIONS.");
						break;
					}

					if (isContentEncrypted(content)) {
						String key = content.getKey();
						LOG.warn("Deleting password-protected file [ " + key + " ]");
						state.getProcessor().getUpdateController().delete(key);
						state.getStatus().incrementCounter(CrawlStatus.Counter.Access_Denied);
					} else {
						gaia.crawl.metadata.Metadata meta = content.getMetadata();
						String mimetype = meta.get("GCM_mimetype");
						if ((mimetype != null) && (mimetype.startsWith("text/html")))
							mimetype = "text/html";

						byte[] data = content.getData();
						String dsName = parser.getDatasource();

						Set inc = null;
						Set exc = null;
						Map tags = (Map) dsToInclusions.get(dsName);
						if (tags != null)
							inc = (Set) tags.get(mimetype);
						tags = (Map) dsToExclusions.get(dsName);
						if (tags != null)
							exc = (Set) tags.get(mimetype);

						if ((inc != null) || (exc != null)) {
							try {
								LOG.info("Applying tag-filter to: " + content.getKey());
								byte[] filtered = XmlTagFilter.filterXmlStream(data, inc, exc);
								content.setData(filtered);
							} catch (Exception ex) {
								LOG.warn("Error while applying tag-filter to: " + content.getKey(), ex);
							}
						}
						adaptor.postProcess(content);
						state.getProcessor().process(content);
					}
				} catch (Throwable t) {
					LOG.warn(CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(),
							content.getKey() != null ? content.getKey() : "unknown", "parsing error: " + t.getMessage()), t);

					state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
				}
			}
			if (limitReached) {
				return DOCUMENT_LIMIT_REACHED;
			}

			return SUCCESS;
		} catch (Throwable t) {
		}
		return FAILURE;
	}

	private boolean isContentEncrypted(Content content) {
		LOG.debug("Performing password-protected check for docid [ " + content.getKey() + " ]");

		byte[] raw = content.getData();
		if ((raw == null) || (raw.length <= 1)) {
			LOG.debug("Returning false since there's no content.");
			return false;
		}

		Parser parser = new AutoDetectParser();
		ContentHandler handler = new BodyContentHandler();
		org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();
		ParseContext context = new ParseContext();
		InputStream input = new ByteArrayInputStream(raw);

		boolean encrypted = false;
		try {
			parser.parse(input, handler, metadata, context);
		} catch (EncryptedDocumentException ex) {
			LOG.debug("EncryptedDocumentException thrown (this must be a ms-office doc).");
			encrypted = true;
		} catch (TikaException ex) {
			Throwable cause = ex.getCause();
			if ((cause != null) && (cause.getMessage().indexOf("Error decrypting") != -1))
				encrypted = true;
		} catch (Exception ex) {
			LOG.debug("Unexpected exception thrown", ex);
		}
		LOG.debug("End of password-protected check.");

		return encrypted;
	}

	private boolean enforceLimits(CrawlerController controller, CrawlState state) {
		int maxDocs = state.getDataSource().getInt("max_docs", Integer.MAX_VALUE);

		if (maxDocs < 1) {
			maxDocs = Integer.MAX_VALUE;
		}
		if (state.getStatus().getCounter(CrawlStatus.Counter.Total) >= maxDocs) {
			LOG.warn(CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(), null,
					"Document count limit reached, stopping crawl."));
			try {
				controller.stopJob(state.getId());
			} catch (Exception e) {
				LOG.error("Could not stop crawl: " + e.getMessage(), e);
			}
			return true;
		}

		return false;
	}
}
