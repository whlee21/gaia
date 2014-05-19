package gaia.handler;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenGroup;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.handler.component.HighlightComponent;
import org.apache.solr.highlight.SolrHighlighter;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DocumentHighlighterRequestHandler extends RequestHandlerBase implements DocumentHighlighterParams,
		SolrCoreAware {
	private static final Logger LOG = LoggerFactory.getLogger(DocumentHighlighterRequestHandler.class);
	private static final String DEFAULT_SOLRCELL_PATH = "/update/extract";
	private static final String DEFAULT_BEGIN_MARKER = "<b>";
	private static final String DEFAULT_END_MARKER = "</b>";
	private static Field startOffsetField;
	private static Field endOffsetField;
	private String solrCellPath;
	private String beginMarker;
	private String endMarker;
	private SolrHighlighter highlighter;
	private SolrRequestHandler extractor;
	private String uniqueKeyField;
	private DocumentBuilderFactory factory;
	private XPathFactory xfactory;
	private Map<String, FieldType> ftypes;

	public DocumentHighlighterRequestHandler() {
		solrCellPath = DEFAULT_SOLRCELL_PATH;

		beginMarker = DEFAULT_BEGIN_MARKER;
		endMarker = DEFAULT_END_MARKER;

		factory = DocumentBuilderFactory.newInstance();
		xfactory = XPathFactory.newInstance();
		ftypes = new HashMap<String, FieldType>();
	}

	public void init(NamedList args) {
		super.init(args);
	}

	public String getDescription() {
		return "Highlights supplied content";
	}

	public String getSource() {
		return "$URL$";
	}

	public String getVersion() {
		return "$Revision$";
	}

	public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
		SolrParams params = req.getParams();

		String qstr = params.get("dochl.q");
		if (qstr == null) {
			qstr = params.get("q");
			if (qstr == null) {
				rsp.add("error", "Missing required parameter: dochl.q");
				return;
			}
		}
		QParser parser = QParser.getParser(qstr, params.get("qt"), req);
		Query q = parser.getQuery();
		String sourcestr = params.get("dochl.source");
		if (sourcestr == null) {
			rsp.add("error", "Missing required parameter: dochl.source");
			return;
		}
		DocumentHighlighterParams.Source source = DocumentHighlighterParams.Source.valueOf(sourcestr.toUpperCase());
		if (source == null) {
			rsp.add(
					"error",
					new StringBuilder().append("Parameter dochl.source: invalid value '").append(sourcestr)
							.append("' not one of ").append(Arrays.toString(DocumentHighlighterParams.Source.values())).toString());

			return;
		}
		String xpath = params.get("dochl.xpath");
		Map<String, String[]> sourceText = null;
		try {
			// SOLRCELL, STORED, XML, TEXT;
			switch (source) {
			case SOLRCELL:
				if (extractor == null) {
					rsp.add("error", new StringBuilder().append("dochl.source=")
							.append(DocumentHighlighterParams.Source.SOLRCELL).append(" but SolrCell not present.").toString());
					return;
				}
				if (xpath == null)
					;
				sourceText = getTextFromExtractor(xpath, req);
				break;
			case STORED:
				if (uniqueKeyField == null) {
					rsp.add("error", new StringBuilder().append("dochl.source=").append(DocumentHighlighterParams.Source.STORED)
							.append(" but uniqueKey not present in schema.").toString());
					return;
				}
				String docid = params.get("dochl.docId");
				if (docid == null) {
					rsp.add("error", new StringBuilder().append("dochl.source=").append(DocumentHighlighterParams.Source.STORED)
							.append(" but ").append("dochl.docId").append(" not present.").toString());
					return;
				}
				sourceText = getTextFromDoc(docid, req);
				break;
			case XML:
				if (xpath == null)
					;
				sourceText = getTextFromXML(xpath, req);
				break;
			case TEXT:
				sourceText = getTextFromText(req);
			}
		} catch (Exception e) {
			LOG.error("", e);
			rsp.add("error", e);
			return;
		}

		if (sourceText == null) {
			rsp.add("error", "Could not obtain the source text.");
			return;
		}
		DocumentHighlighterParams.Mode mode = null;
		String modestr = params.get("dochl.mode");
		if (modestr != null) {
			mode = DocumentHighlighterParams.Mode.valueOf(modestr.toUpperCase());
		}
		if (mode == null) {
			LOG.info(new StringBuilder().append("dochl.mode unspecified or invalid, assuming dochl.mode=")
					.append(DocumentHighlighterParams.Mode.HIGHLIGHT).toString());
			mode = DocumentHighlighterParams.Mode.HIGHLIGHT;
		}
		parseFieldTypes(req);
		doHighlighting(q, mode, sourceText, req, rsp);
	}

	private void parseFieldTypes(SolrQueryRequest req) {
		ftypes.clear();
		SolrParams params = req.getParams();
		IndexSchema schema = req.getSchema();
		Map<String, FieldType> types = schema.getFieldTypes();

		String defType = params.get("dochl.ft");
		if (defType != null) {
			FieldType ft = (FieldType) types.get(defType);
			if (ft != null)
				ftypes.put("*", ft);
			else {
				LOG.warn(new StringBuilder().append("Requested default field type '").append(defType)
						.append("' not defined in schema!").toString());
			}
		}

		Iterator<String> it = params.getParameterNamesIterator();
		while (it.hasNext()) {
			String key = it.next();
			if ((key.startsWith("dochl.ft.")) && (key.length() > "dochl.ft.".length())) {
				String field = key.substring("dochl.ft.".length());
				String name = params.get(key);
				FieldType ft = (FieldType) types.get(name);
				if (ft != null)
					ftypes.put(field, ft);
				else
					LOG.warn(new StringBuilder().append("Requested field type ").append(key).append("=").append(name)
							.append(" not defined in schema!").toString());
			}
		}
	}

	private void doHighlighting(Query q, DocumentHighlighterParams.Mode mode, Map<String, String[]> sourceText,
			SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
		QueryScorer scorer = new QueryScorer(q, null);

		String curBeginMarker = req.getParams().get("dochl.beginMarker");
		String curEndMarker = req.getParams().get("dochl.endMarker");
		if (curBeginMarker == null) {
			curBeginMarker = beginMarker;
		}
		if (curEndMarker == null) {
			curEndMarker = endMarker;
		}
		boolean withOrigText = req.getParams().getBool("dochl.includeOrigText",
				mode == DocumentHighlighterParams.Mode.OFFSETS);

		SimpleOrderedMap<Object> res = new SimpleOrderedMap<Object>();
		rsp.add("results", res);
		SimpleOrderedMap<String> dbg = null;
		if (req.getParams().getBool("debugQuery", false)) {
			dbg = new SimpleOrderedMap<String>();
			dbg.add("query", q.toString());
		}
		for (Map.Entry<String, String[]> e : sourceText.entrySet()) {
			Analyzer a = null;

			FieldType ft = (FieldType) ftypes.get(e.getKey());
			if (ft != null) {
				a = ft.getAnalyzer();
			} else if (ftypes.get("*") != null) {
				a = ((FieldType) ftypes.get("*")).getAnalyzer();
			} else {
				IndexSchema schema = req.getSchema();
				SchemaField sf = schema.getFieldOrNull((String) e.getKey());
				if (sf != null) {
					a = sf.getType().getAnalyzer();
				} else {
					LOG.warn(new StringBuilder().append("No analyzer for field '").append((String) e.getKey())
							.append("', using StandardAnalyzer.").toString());

					a = new StandardAnalyzer(req.getCore().getSolrConfig().luceneMatchVersion);
				}
			}
			if (dbg != null) {
				dbg.add(new StringBuilder().append((String) e.getKey()).append("_analyzer").toString(), a.toString());
			}
			SimpleOrderedMap<Object> hlit = new SimpleOrderedMap<Object>();
			res.add((String) e.getKey(), hlit);
			for (String s : (String[]) e.getValue()) {
				SimpleOrderedMap<Object> hl = null;
				if (mode != DocumentHighlighterParams.Mode.HIGHLIGHT) {
					hl = new SimpleOrderedMap<Object>();
					hlit.add("offsets", hl);
				}
				RecordingFormatter f = new RecordingFormatter(hl, mode, curBeginMarker, curEndMarker, s);
				Highlighter highlighter = new Highlighter(f, scorer);

				TokenStream ts = a.tokenStream((String) e.getKey(), new StringReader(s));
				highlighter.getBestTextFragments(ts, s, true, 1);
				if (mode != DocumentHighlighterParams.Mode.OFFSETS) {
					hlit.add("highlighted", f.getFullText());
				}
				if (withOrigText) {
					hlit.add("text", s);
				}
			}
		}
		if (dbg != null)
			res.add("debug", dbg);
	}

	private Map<String, String[]> getTextFromExtractor(String xpath, SolrQueryRequest req) throws Exception {
		ModifiableSolrParams params = new ModifiableSolrParams(req.getParams());

		params.set("extractOnly", true);
		params.set("extractFormat", new String[] { "text" });

		if (xpath != null) {
			params.set("xpath", new String[] { xpath });
		}
		String filename = req.getParams().get("dochl.filename");
		if (filename != null) {
			params.set("resource.name", new String[] { filename });
		}
		LocalSolrQueryRequest extReq = new LocalSolrQueryRequest(req.getCore(), params);
		List<ContentStream> list = new ArrayList<ContentStream>();
		for (ContentStream stream : req.getContentStreams()) {
			list.add(stream);
		}
		extReq.setContentStreams(list);
		SolrQueryResponse rsp = new SolrQueryResponse();
		extractor.handleRequest(extReq, rsp);
		extReq.close();

		Map<String, String[]> res = new HashMap<String, String[]>();
		if (rsp.getException() != null) {
			throw new Exception(rsp.getException());
		}
		NamedList values = rsp.getValues();

		// int num = 0;
		Iterator it = values.iterator();
		boolean odd = true;

		while (it.hasNext()) {
			Map.Entry e = (Map.Entry) it.next();
			if (odd) {
				res.put("body", new String[] { e.getValue().toString() });
				odd = false;
			} else {
				odd = true;
			}
		}

		return res;
	}

	private Map<String, String[]> getTextFromDoc(String docid, SolrQueryRequest req) throws Exception {
		Query q = new TermQuery(new Term(uniqueKeyField, docid));
		SolrIndexSearcher searcher = req.getSearcher();
		DocSet docs = searcher.getDocSet(q);
		if (docs.size() > 1) {
			throw new Exception(new StringBuilder().append("Multiple docs with this unique key: ").append(docid).toString());
		}
		if (docs.size() == 0) {
			return null;
		}
		String[] fields = null;
		SolrParams params = req.getParams();
		String fieldList = params.get("dochl.fl");
		if (fieldList == null) {
			fieldList = params.get("fl");
			if (fieldList == null) {
				throw new Exception("Missing required parameter dochl.fl");
			}
		}
		fields = fieldList.split(",");
		DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor(fields);
		searcher.doc(docs.iterator().nextDoc(), visitor);
		org.apache.lucene.document.Document doc = visitor.getDocument();
		Map<String, String[]> res = new HashMap<String, String[]>();
		for (String f : fields) {
			IndexableField[] fieldVals = doc.getFields(f);
			String[] val = new String[fieldVals.length];
			for (int i = 0; i < fieldVals.length; i++) {
				val[i] = fieldVals[i].stringValue();
			}
			if (val.length > 0) {
				res.put(f, val);
			}
		}
		if (res.size() > 0) {
			return res;
		}
		return Collections.emptyMap();
	}

	private Map<String, String[]> getTextFromXML(String xpath, SolrQueryRequest req) throws Exception {
		Map<String, String[]> res = new HashMap<String, String[]>();
		XPath x = xfactory.newXPath();
		for (ContentStream cs : req.getContentStreams()) {
			InputStream is = cs.getStream();
			try {
				String name = cs.getName();
				if (name == null) {
					name = "body";
				}
				boolean ok = false;
				if ((cs.getContentType() == null) || (!cs.getContentType().equals("text/xml"))) {
					if ((cs.getName() != null) && (cs.getName().endsWith(".xml"))) {
						ok = true;
					} else {
						is = new PushbackInputStream(is, 10);
						byte[] buf = new byte[10];
						is.read(buf);
						String in = new String(buf);
						if (in.startsWith("<?xml ")) {
							ok = true;
							((PushbackInputStream) is).unread(buf);
						}
					}
				} else if (cs.getContentType().equals("text/xml")) {
					ok = true;
				}
				if (!ok) {
					LOG.warn(new StringBuilder().append("Skipping invalid stream ").append(name).append(" of type '")
							.append(cs.getContentType()).append("'").toString());

					if (is != null)
						is.close();
				} else {
					DocumentBuilder builder = factory.newDocumentBuilder();
					org.w3c.dom.Document doc = builder.parse(is);
					ArrayList<Node> nodes = new ArrayList<Node>();
					if (xpath == null) {
						nodes.add(doc.getDocumentElement());
					} else {
						NodeList nl = (NodeList) x.evaluate(xpath, doc, XPathConstants.NODESET);
						if (nl != null) {
							for (int i = 0; i < nl.getLength(); i++) {
								nodes.add(nl.item(i));
							}
						}
					}
					if (nodes.size() == 0) {
						if (is != null)
							is.close();
					} else {
						String[] vals = new String[nodes.size()];
						for (int i = 0; i < nodes.size(); i++) {
							vals[i] = ((Node) nodes.get(i)).getTextContent();
						}
						String[] oldVals = (String[]) res.get(name);
						if (oldVals != null) {
							String[] newVals = new String[oldVals.length + vals.length];
							System.arraycopy(oldVals, 0, newVals, 0, oldVals.length);
							System.arraycopy(vals, 0, newVals, oldVals.length, vals.length);
							res.put(name, newVals);
						} else {
							res.put(name, vals);
						}
					}
				}
			} finally {
				if (is != null) {
					is.close();
				}
			}
		}
		return res;
	}

	private Map<String, String[]> getTextFromText(SolrQueryRequest req) throws Exception {
		Map<String, String[]> res = new HashMap<String, String[]>();
		int num = 0;
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[512];
		for (ContentStream cs : req.getContentStreams()) {
			String name = cs.getName();
			if (name == null) {
				name = String.valueOf(num);
			}
			num++;
			sb.setLength(0);
			Reader r = cs.getReader();
			try {
				int cnt;
				while ((cnt = r.read(buf)) != -1) {
					sb.append(buf, 0, cnt);
				}
				res.put(name, new String[] { sb.toString() });
			} finally {
				if (r != null) {
					r.close();
				}
			}
		}
		return res;
	}

	public void inform(SolrCore core) {
		HighlightComponent hl = (HighlightComponent) core.getSearchComponent("highlight");
		if (hl == null) {
			throw new RuntimeException("Highlight component is required for this handler, but is not available.");
		}
		highlighter = hl.getHighlighter();
		SchemaField key = core.getLatestSchema().getUniqueKeyField();
		if (key == null)
			LOG.warn(new StringBuilder().append("Schema has no uniqueKey, dochl.source=")
					.append(DocumentHighlighterParams.Source.STORED).append(" is disabled.").toString());
		else {
			uniqueKeyField = key.getName();
		}
		extractor = core.getRequestHandler(solrCellPath);
		if (extractor == null)
			LOG.warn(new StringBuilder().append("SolrCell not present at ").append(solrCellPath).append(", ")
					.append("dochl.source").append("=").append(DocumentHighlighterParams.Source.SOLRCELL).append(" is disabled.")
					.toString());
	}

	static {
		try {
			startOffsetField = TextFragment.class.getDeclaredField("textStartPos");
			endOffsetField = TextFragment.class.getDeclaredField("textEndPos");
			startOffsetField.setAccessible(true);
			endOffsetField.setAccessible(true);
		} catch (Exception e) {
			LOG.warn("Highlight offset information won't be available", e);
		}
	}

	static class RecordingFormatter implements Formatter {
		NamedList res;
		String beginMarker;
		String endMarker;
		DocumentHighlighterParams.Mode mode;
		String originalText;
		StringBuilder fullText = new StringBuilder();
		int lastEnd = 0;

		public RecordingFormatter(NamedList res, DocumentHighlighterParams.Mode mode, String beginMarker, String endMarker,
				String originalText) {
			this.res = res;
			this.mode = mode;
			this.beginMarker = beginMarker;
			this.endMarker = endMarker;
			this.originalText = originalText;
		}

		public String highlightTerm(String original, TokenGroup tg) {
			if (tg.getTotalScore() <= 0.0F) {
				return original;
			}
			if ((mode != DocumentHighlighterParams.Mode.HIGHLIGHT) && (res != null)) {
				res.add(original, new StringBuilder().append(tg.getStartOffset()).append("-").append(tg.getEndOffset())
						.toString());
			}
			if (mode != DocumentHighlighterParams.Mode.OFFSETS) {
				StringBuilder returnBuffer = new StringBuilder(beginMarker.length() + original.length() + endMarker.length());
				returnBuffer.append(beginMarker);
				returnBuffer.append(original);
				returnBuffer.append(endMarker);

				if (tg.getStartOffset() > lastEnd) {
					fullText.append(originalText.substring(lastEnd, tg.getStartOffset()));
				}
				fullText.append(returnBuffer);
				lastEnd = tg.getEndOffset();
				return returnBuffer.toString();
			}
			return original;
		}

		public String getFullText() {
			if (lastEnd < originalText.length()) {
				fullText.append(originalText.substring(lastEnd));
				lastEnd = originalText.length();
			}
			return fullText.toString();
		}
	}
}
