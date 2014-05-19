package gaia.crawl.gcm;

import gaia.crawl.CrawlId;
import gaia.crawl.CrawlerController;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.io.Content;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GCMParser {
	private static final String ENCODING_BASE64BINARY = "base64binary";
	private static final String ATTR_ACTION = "action";
	private static final String ATTR_META_NAME = "name";
	private static final String ATTR_META_CONTENT = "content";
	private static final String ATTR_ENCODING = "encoding";
	private static final String ATTR_LAST_MODIFIED = "last-modified";
	private static final String ATTR_MIMETYPE = "mimetype";
	private static final String ATTR_DISPLAYURL = "displayurl";
	private static final String ATTR_URL = "url";
	private static final String ACTION_ADD = "add";
	private static final Object ACTION_DELETE = "delete";
	private static final String ACTION_FORBID = "forbid";
	private static final String RECORD = "record";
	private static final String GROUP = "group";
	private static final String DATASOURCE = "datasource";
	private static final String FEEDTYPE = "feedtype";
	private static final String META = "meta";
	private static final Object CONTENT = "content";

	private static Set<String> attributeNames = new HashSet<String>(Arrays.asList(new String[] { ATTR_DISPLAYURL,
			ATTR_MIMETYPE, ATTR_LAST_MODIFIED }));

	private static final Logger LOG = LoggerFactory.getLogger(GCMParser.class);
	private String datasource;
	private CrawlerController controller;
	private GCMCrawlState state;
	private String feedtype;
	private List<Content> additions = new ArrayList<Content>();
	private List<String> deletes = new ArrayList<String>();
	private List<String> forbidden = new ArrayList<String>();
	private InputStream input;
	private String currentAction = ACTION_ADD;

	public String getDatasource() {
		return datasource;
	}

	public String getFeedtype() {
		return feedtype;
	}

	public List<Content> getAdditions() {
		return additions;
	}

	public List<String> getDeletes() {
		return deletes;
	}

	public List<String> getForbidden() {
		return forbidden;
	}

	public InputStream getInput() {
		return input;
	}

	public GCMParser(InputStream input, CrawlerController controller) {
		this.input = input;
		this.controller = controller;
	}

	public void parse() {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader parser = null;

		factory.setProperty("javax.xml.stream.isValidating", Boolean.valueOf(false));
		factory.setProperty("javax.xml.stream.supportDTD", Boolean.valueOf(false));
		Content current = null;
		try {
			parser = factory.createXMLStreamReader(input);
			while (parser.hasNext()) {
				int event = parser.next();
				if (event == 1) {
					if (GROUP.equals(parser.getLocalName())) {
						String action = parser.getAttributeValue(null, ATTR_ACTION);
						if (action != null)
							currentAction = action;
					} else if (RECORD.equals(parser.getLocalName())) {
						current = processRecord(parser);
					} else if (DATASOURCE.equals(parser.getLocalName())) {
						datasource = parser.getElementText();
						if (controller != null)
							try {
								state = ((GCMCrawlState) controller.getCrawlState(new CrawlId(datasource)));
							} catch (Exception e) {
							}
					} else if (FEEDTYPE.equals(parser.getLocalName())) {
						feedtype = parser.getElementText();
					} else if (META.equals(parser.getLocalName())) {
						mapMetadata(parser, current);
					} else if (CONTENT.equals(parser.getLocalName())) {
						String encoding = parser.getAttributeValue(null, ATTR_ENCODING);
						if (ENCODING_BASE64BINARY.equals(encoding)) {
							String text = parser.getElementText().trim();
							try {
								current.setData(Base64.decodeBase64(text.getBytes()));
							} catch (Throwable t) {
								String collection = null;
								if (state != null) {
									collection = state.getDataSource().getCollection();
								}
								LOG.warn(
										CrawlerUtils.msgDocFailed(collection, state.getDataSource(),
												current != null ? current.getKey() : "unknown", "Base64 decoding failed: " + t.getMessage()), t);
							}
						}
					}
				}
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		} finally {
			if (parser != null)
				try {
					parser.close();
				} catch (Throwable t) {
				}
		}
	}

	private void mapMetadata(XMLStreamReader parser, Content current) {
		String from = parser.getAttributeValue(null, ATTR_META_NAME);
		String value = parser.getAttributeValue(null, ATTR_META_CONTENT);
		if (value != null)
			current.addMetadata("GCM_" + from, value);
	}

	private Content processRecord(XMLStreamReader parser) throws XMLStreamException {
		String action = parser.getAttributeValue(null, ATTR_ACTION);
		if (action == null)
			action = currentAction;
		if (ACTION_ADD.equals(action)) {
			Content content = new Content();
			content.setKey(parser.getAttributeValue(null, ATTR_URL));
			additions.add(content);

			for (int i = 0; i < parser.getAttributeCount(); i++) {
				String attrName = parser.getAttributeLocalName(i);
				if (attributeNames.contains(attrName)) {
					content.addMetadata("GCM_" + attrName, parser.getAttributeValue(i));
				}
			}
			return content;
		}
		if (ACTION_DELETE.equals(action)) {
			deletes.add(parser.getAttributeValue(null, ATTR_URL));
			return null;
		}
		if (ACTION_FORBID.equals(action)) {
			forbidden.add(parser.getAttributeValue(null, ATTR_URL));
			return null;
		}

		return null;
	}
}
