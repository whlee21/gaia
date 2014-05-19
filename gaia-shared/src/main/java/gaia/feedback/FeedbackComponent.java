package gaia.feedback;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.queries.function.BoostedQuery;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.QueryTermExtractor;
import org.apache.lucene.search.highlight.WeightedTerm;
import org.apache.lucene.util.Version;
import org.apache.solr.common.params.DefaultSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.response.ResultContext;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.utils.SortedStringList;

public class FeedbackComponent extends SearchComponent implements SolrCoreAware {
	private static transient Logger LOG = LoggerFactory.getLogger(FeedbackComponent.class);
	public static final String COMPONENT_NAME = "feedback";
	public static final String OR = "OR";
	public static final String AND = "AND";
	public static final int DEFAULT_TOP_DOCS_SIZE = 10;
	public static final String PREFIX = "fdbk.";
	public static final String TOP_X = "fdbk.topX";
	public static final String BOTTOM_Y = "fdbk.bottomY";
	public static final String USE_NEGATIVES = "fdbk.useNegatives";
	public static final String MAX_QUERY_TERMS = "fdbk.maxQueryTermsPerDocument";
	public static final String MAX_CLAUSES = "fdbk.maxClauses";
	public static final String MIN_DOC_FREQ = "fdbk.minDocFreq";
	public static final String MIN_TERM_FREQ = "fdbk.minTermFreq";
	public static final String FIELD_NAMES = "fdbk.fl";
	public static final String MIN_WORD_LENGTH = "fdbk.minWordLength";
	public static final String MAX_WORD_LENGTH = "fdbk.maxWordLength";
	public static final String USE_STOPWORDS = "fdbk.useStopwords";
	public static final String STOPWORDS_FILE = "fdbk.stopwords";
	private static Set<Object> DEFAULT_STOP_WORDS = new CharArraySet(Version.LUCENE_45,
			StopAnalyzer.ENGLISH_STOP_WORDS_SET, true);
	public static final String DEFAULT_OPERATOR = "fdbk.operator";
	private SolrParams defaults;
	private Set<Object> stopwords = DEFAULT_STOP_WORDS;
	private String stopwordsFilename;
	private boolean ignoreCase = true;
	private Version matchVersion;
	private static final String FEEDBACK_CLASS_NAME = "feedbackClassName";
	private static final String NUM_ITERATIONS = "fdbk.iterations";

	public void prepare(ResponseBuilder rb) throws IOException {
	}

	public void process(ResponseBuilder rb) throws IOException {
		SolrParams params = rb.req.getParams();
		if (LOG.isDebugEnabled())
			LOG.debug(new StringBuilder().append("FeedbackComponent.process entered params=").append(params.toString())
					.toString());
		if (params.getBool("feedback", false) == true) {
			long startTime = System.currentTimeMillis();
			if (LOG.isDebugEnabled())
				LOG.debug("FeedbackComponent.process component enabled");
			DefaultSolrParams sp = new DefaultSolrParams(params, defaults);

			Query originalQuery = rb.getQuery();
			Query query = originalQuery;

			Boolean wasBoostedQuery = Boolean.valueOf(originalQuery instanceof BoostedQuery);
			ValueSource prod = null;
			Float boost = Float.valueOf(1.0F);
			if (wasBoostedQuery.booleanValue()) {
				BoostedQuery bq = (BoostedQuery) query;
				query = bq.getQuery();
				prod = bq.getValueSource();
				boost = Float.valueOf(bq.getBoost());
			}

			DocList inputDocs = rb.getResults().docList;
			int topX = sp.getInt("fdbk.topX", 10);

			boolean useNegatives = sp.getBool("fdbk.useNegatives", false);
			String[] fieldNames = sp.getParams("fdbk.fl");
			FeedbackHelper helper;
			if ((fieldNames != null) && (fieldNames.length > 0))
				helper = new FeedbackHelper(rb.req.getSearcher().getIndexReader(), fieldNames);
			else {
				helper = new FeedbackHelper(rb.req.getSearcher().getIndexReader());
			}
			DocList positives = getPositives(inputDocs, topX);
			DocList negatives = null;
			int bottomY = sp.getInt("fdbk.bottomY", 10);
			if (useNegatives) {
				negatives = getNegatives(inputDocs, bottomY, topX);
			}
			int maxQueryTerms = sp.getInt("fdbk.maxQueryTermsPerDocument", 25);
			helper.setMaxQueryTerms(maxQueryTerms);
			int minTermFreq = sp.getInt("fdbk.minTermFreq", 2);
			helper.setMinTermFreq(minTermFreq);
			int minDocFreq = sp.getInt("fdbk.minDocFreq", 5);
			helper.setMinDocFreq(minDocFreq);

			helper.setMinWordLen(sp.getInt("fdbk.minWordLength", 0));
			helper.setMaxWordLen(sp.getInt("fdbk.maxWordLength", 0));

			helper.setAnalyzer(rb.req.getSchema().getAnalyzer());
			String operatorStr = sp.get("fdbk.operator", "OR");
			BooleanClause.Occur operator = BooleanClause.Occur.SHOULD;
			if (operatorStr.equalsIgnoreCase("AND") == true) {
				operator = BooleanClause.Occur.MUST;
			}
			int maxClauses = sp.getInt("fdbk.maxClauses", BooleanQuery.getMaxClauseCount());
			boolean useStopWords = sp.getBool("fdbk.useStopwords", true);
			if ((useStopWords == true) && (stopwords != null)) {
				helper.setStopWords(stopwords);
			}
			AbstractRelevanceFeedback feedback = getFeedback(rb.req.getCore().getResourceLoader(), sp);

			SortedStringList originalTerms = new SortedStringList();
			WeightedTerm[] originalWeightedTerms = QueryTermExtractor.getTerms(query);
			int numOriginalTerms = originalWeightedTerms.length;
			for (int i = 0; i < numOriginalTerms; i++) {
				originalTerms.add(originalWeightedTerms[i].getTerm());
			}

			SortedStringList feedbackTerms = new SortedStringList();

			int numIterations = sp.getInt(NUM_ITERATIONS, 1);
			if ((numIterations < 1) || (numIterations > 20)) {
				LOG.warn("Invalid number of iterations, reseting to 1");
				numIterations = 1;
			}
			Query newQuery = query;
			SolrIndexSearcher searcher = rb.req.getSearcher();
			int rows = sp.getInt("rows", 10);
			int start = sp.getInt("start", 0);
			DocListAndSet listAndSet = null;
			for (int i = 0; i < numIterations; i++) {
				LOG.debug(new StringBuilder().append("Feedback Iteration: ").append(i).toString());
				newQuery = feedback.generateQuery(newQuery, positives, negatives, helper, operator, sp, maxClauses,
						originalTerms, feedbackTerms);
				if ((newQuery != null) && (!newQuery.equals(query))) {
					if (LOG.isDebugEnabled()) {
						LOG.debug(new StringBuilder().append("Feedback Query: ").append(newQuery).toString());
					}

					Query newBoostedQuery = newQuery;
					if (wasBoostedQuery.booleanValue()) {
						newBoostedQuery = new BoostedQuery(newQuery, prod);
						newBoostedQuery.setBoost(boost.floatValue());
					}

					listAndSet = searcher.getDocListAndSet(newBoostedQuery, rb.getFilters(), rb.getSortSpec().getSort(), start,
							rows, 1);
					if ((listAndSet != null) && (listAndSet.docList.matches() > 0)) {
						positives = getPositives(listAndSet.docList, topX);
						if (useNegatives) {
							negatives = getNegatives(listAndSet.docList, bottomY, topX);
						}
					}
				}
			}
			if (LOG.isDebugEnabled())
				if (listAndSet == null)
					LOG.debug("listAndSet is null ");
				else
					LOG.debug(new StringBuilder().append("listAndSet: ").append(listAndSet.docList.toString())
							.append(" matches: ").append(listAndSet.docList.matches()).toString());
			if ((listAndSet != null) && (listAndSet.docList.matches() > 0)) {
				rb.setResults(listAndSet);

				String feedbackTermsString = feedbackTerms.toString();
				LOG.debug(new StringBuilder().append("feedbackTerms: ").append(feedbackTermsString).toString());
				rb.rsp.add("feedbackTerms", feedbackTermsString);

				String feedbackQuery = sp.get("q", sp.get("q.alt", "*:*"));

				feedbackQuery = feedbackQuery.trim();
				String emphasis = sp.get("feedback.emphasis", "relevancy");
				if ((emphasis.equalsIgnoreCase("relevancy")) && (feedbackQuery.length() > 0)) {
					if (feedbackQuery.indexOf(32) > 0)
						feedbackQuery = new StringBuilder().append("+(").append(feedbackQuery).append(")").toString();
					else
						feedbackQuery = new StringBuilder().append("+").append(feedbackQuery).toString();
				}
				feedbackQuery = new StringBuilder().append(feedbackQuery)
						.append(feedbackQuery.length() > 0 ? Character.valueOf(' ') : "").append("like:(")
						.append(feedbackTermsString).append(")").toString();
				LOG.debug(new StringBuilder().append("feedbackQuery: ").append(feedbackQuery).toString());
				rb.rsp.add("feedbackQuery", feedbackQuery);

				Query newBoostedQuery = newQuery;
				if (wasBoostedQuery.booleanValue()) {
					newBoostedQuery = new BoostedQuery(newQuery, prod);
					newBoostedQuery.setBoost(boost.floatValue());
				}
				rb.setQuery(newBoostedQuery);
				if (LOG.isDebugEnabled()) {
					LOG.debug(new StringBuilder().append("rb.setQuery: ").append(newBoostedQuery.toString()).toString());
				}

				rb.setHighlightQuery(newQuery);
				if (LOG.isDebugEnabled()) {
					LOG.debug(new StringBuilder().append("setHighlightQuery: ").append(newQuery.toString()).toString());
				}

				int idx = rb.rsp.getValues().indexOf("response", 0);
				if (idx != -1) {
					ResultContext rc = (ResultContext) rb.rsp.getValues().get("response");
					rc.docs = listAndSet.docList;
					rb.rsp.getValues().setVal(idx, rc);
				} else {
					ResultContext rc = new ResultContext();
					rc.docs = listAndSet.docList;
					rb.rsp.add("response", rc);
				}
			}
			long endTime = System.currentTimeMillis();
			LOG.debug(new StringBuilder().append("feedbackComponent.process took ").append(endTime - startTime).append("ms.")
					.toString());
		}
	}

	private DocList getNegatives(DocList inputDocs, int bottomY, int topX) {
		DocList result = null;
		if (inputDocs.size() < bottomY + topX) {
			bottomY = inputDocs.size() - topX;
		}
		int offset = Math.max(inputDocs.size() - bottomY, 0);
		result = inputDocs.subset(offset, bottomY);
		return result;
	}

	private DocList getPositives(DocList inputDocs, int topX) {
		return inputDocs.subset(0, Math.min(topX, inputDocs.size()));
	}

	private AbstractRelevanceFeedback getFeedback(SolrResourceLoader resourceLoader, DefaultSolrParams sp) {
		AbstractRelevanceFeedback feedback = null;
		String feedbackClassName = sp.get(FEEDBACK_CLASS_NAME);
		if (feedbackClassName != null)
			feedback = (AbstractRelevanceFeedback) resourceLoader.newInstance(feedbackClassName,
					AbstractRelevanceFeedback.class);
		else {
			feedback = new Rocchio();
		}
		return feedback;
	}

	public void inform(SolrCore core) {
		matchVersion = core.getSolrConfig().luceneMatchVersion;
		if (stopwordsFilename != null)
			try {
				List<String> wlist = core.getResourceLoader().getLines(stopwordsFilename);
				stopwords = StopFilter.makeStopSet(matchVersion, (String[]) wlist.toArray(new String[0]),
						ignoreCase);
			} catch (IOException e) {
				LOG.error("Exception, using default stop words", e);
			}
	}

	public void init(NamedList args) {
		super.init(args);
		defaults = SolrParams.toSolrParams((NamedList) args.get("defaults"));
		stopwordsFilename = ((String) args.get("fdbk.stopwords"));
		Object o = args.get("ignoreCase");
		if (o != null)
			ignoreCase = Boolean.parseBoolean(o.toString());
	}

	public String getDescription() {
		return "FeedbackComponent";
	}

	public String getVersion() {
		return "$Revision$";
	}

	public String getSource() {
		return "$URL$";
	}

	public URL[] getDocs() {
		return null;
	}
}
