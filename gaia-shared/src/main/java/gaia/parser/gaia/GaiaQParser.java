package gaia.parser.gaia;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queries.function.BoostedQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.DefaultSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.parser.QueryParser;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.util.SolrPluginUtils;

public class GaiaQParser extends QParser {
	Map<String, Float> allFields = null;

	Map<String, Float> queryFields = null;
	List<String> synonymsFields;
	List<String> stopwordsFields;
	public boolean logOutput = false;

	public GaiaQParserPlugin plugin = null;

	public GaiaQueryParserPluginStatics parserStatics = null;
	Query parsedUserQuery;
	private String[] boostParams;
	private List<Query> boostQueries;
	private String altUserQuery;

	public void setLogOutput(boolean log) {
		logOutput = log;
	}

	public void setGaiaQParserPlugin(GaiaQParserPlugin p) {
		plugin = p;
	}

	public GaiaQueryParserPluginStatics getParserStatics(IndexSchema schema) {
		if (plugin != null) {
			return plugin.getParserStatics(schema);
		}
		if (parserStatics == null)
			parserStatics = new GaiaQueryParserPluginStatics(schema);
		else if (parserStatics.schema != schema)
			parserStatics.reInit(schema);
		return parserStatics;
	}

	public void setParserRequestParams(GaiaQueryParser p, SolrParams solrParams) {
		allFields = U.parseFieldBoosts(solrParams.getParams(GaiaQueryParserParams.ALLFIELDS));
		queryFields = U.parseFieldBoosts(solrParams.getParams(GaiaQueryParserParams.QF));
		synonymsFields = Arrays.asList(solrParams.get(GaiaQueryParserParams.SYNONYMS_FIELDS, "").split(","));
		stopwordsFields = Arrays.asList(solrParams.get(GaiaQueryParserParams.STOPWORDS_FIELDS, "").split(","));
		Map<String, Float> bigramRelevancyFields = U.parseFieldBoosts(solrParams.getParams(GaiaQueryParserParams.PF));
		Map<String, Float> trigramRelevancyFields = U.parseFieldBoosts(solrParams.getParams(GaiaQueryParserParams.PF3));

		Map<String, Float> tempFields = null;
		if (((allFields == null) || (allFields.size() == 0)) && ((queryFields == null) || (queryFields.size() == 0))) {
			tempFields = new HashMap<String, Float>();
			tempFields.put("*", Float.valueOf(1.0F));
		}
		p.setAllFields((queryFields != null) && (queryFields.size() > 0) ? queryFields : (allFields != null)
				&& (allFields.size() > 0) ? allFields : tempFields);
		p.setQueryFields((allFields != null) && (allFields.size() > 0) ? allFields : (queryFields != null)
				&& (queryFields.size() > 0) ? queryFields : tempFields);

		p.setBigramRelevancyFields((allFields != null) && (allFields.size() > 0) ? allFields : (queryFields != null)
				&& (queryFields.size() > 0) ? queryFields : (bigramRelevancyFields != null)
				&& (bigramRelevancyFields.size() > 0) ? bigramRelevancyFields : tempFields);
		p.setTrigramRelevancyFields((allFields != null) && (allFields.size() > 0) ? allFields : (queryFields != null)
				&& (queryFields.size() > 0) ? queryFields : (bigramRelevancyFields != null)
				&& (bigramRelevancyFields.size() > 0) ? bigramRelevancyFields : (trigramRelevancyFields != null)
				&& (trigramRelevancyFields.size() > 0) ? trigramRelevancyFields : tempFields);
		p.setSynonymsFields(synonymsFields);
		p.setExpandSynonyms(solrParams.getBool(GaiaQueryParserParams.SYNONYMS_ENABLED,
				GaiaQueryParserParams.SYNONYMS_ENABLEDDefault));
		p.setDefaultSynonyms(solrParams.getBool(GaiaQueryParserParams.SYNONYMS_DEFAULT,
				GaiaQueryParserParams.SYNONYMS_DEFAULTDefault));
		p.setStopwordsFields(stopwordsFields);
		p.setProcessStopwords(solrParams.getBool(GaiaQueryParserParams.STOPWORDS_ENABLED,
				GaiaQueryParserParams.STOPWORDS_ENABLEDDefault));

		p.multBoosts = solrParams.getParams(GaiaQueryParserParams.MULTBOOST);

		p.boostFuncs = solrParams.getParams(GaiaQueryParserParams.BF);

		p.boostParams = solrParams.getParams(GaiaQueryParserParams.BQ);

		p.setTieBreakerMultiplier(solrParams.getFloat(GaiaQueryParserParams.TIE, GaiaQueryParserParams.TIEDefault));

		p.setRelevancyPhraseSlop(solrParams.getInt(GaiaQueryParserParams.PS, GaiaQueryParserParams.PSDefault));
		p.setQueryPhraseSlop(solrParams.getInt(GaiaQueryParserParams.QS, GaiaQueryParserParams.QSDefault));
		p.setNearSlop(solrParams.getInt(GaiaQueryParserParams.NEARSLOP, GaiaQueryParserParams.NEARSLOPDefault));

		boolean opUp = solrParams.getBool(GaiaQueryParserParams.OPUP, GaiaQueryParserParams.OPUPDefault);
		p.setOpUp(opUp);
		boolean notUp = solrParams.getBool(GaiaQueryParserParams.NOTUP, GaiaQueryParserParams.NOTUPDefault);
		p.setNotUp(notUp);
		boolean natUp = solrParams.getBool(GaiaQueryParserParams.NATUP, GaiaQueryParserParams.NATUPDefault);
		p.setNatUp(natUp);
		boolean opGram = solrParams.getBool(GaiaQueryParserParams.OPGRAM, GaiaQueryParserParams.OPGRAMDefault);
		p.setOpGram(opGram);
		boolean rmAcc = solrParams.getBool(GaiaQueryParserParams.RMACC, GaiaQueryParserParams.RMACCDefault);
		p.setRemoveAccents(rmAcc);
		int likeMin = solrParams.getInt(GaiaQueryParserParams.LIKEMIN, GaiaQueryParserParams.LIKEMINDefault);
		p.setMoreLikeThreshold(likeMin);

		boolean leadWild = solrParams.getBool(GaiaQueryParserParams.LEADWILD, GaiaQueryParserParams.LEADWILDDefault);
		p.setLeadWild(leadWild);

		boolean stickyModifiers = solrParams.getBool(GaiaQueryParserParams.STICKYMODIFIERS, true);
		p.setStickyModifiers(stickyModifiers);

		boolean strictColon = solrParams.getBool(GaiaQueryParserParams.STRICTCOLON, false);
		p.setStrictColon(strictColon);

		boolean boostUnigrams = solrParams.getBool(GaiaQueryParserParams.BOOSTUNIGRAMS, false);
		p.setBoostUnigrams(boostUnigrams);

		boolean boostBigrams = solrParams.getBool(GaiaQueryParserParams.BOOSTBIGRAMS, true);
		p.setBoostBigrams(boostBigrams);

		boolean boostTrigrams = solrParams.getBool(GaiaQueryParserParams.BOOSTTRIGRAMS, true);
		p.setBoostTrigrams(boostTrigrams);

		String defaultOperator = solrParams.get(GaiaQueryParserParams.DEFOP,
				GaiaQueryParserParams.DEFOPDefault == QueryParser.Operator.AND ? "AND" : "OR");
		p.setDefaultOperator(defaultOperator.equalsIgnoreCase("AND") ? QueryParser.Operator.AND : QueryParser.Operator.OR);

		String textPrefix = solrParams.get(GaiaQueryParserParams.TEXTPREFIX, "text");
		p.setTextPrefix(textPrefix);

		float minSim = solrParams.getFloat(GaiaQueryParserParams.MINSIM, 2.0F);
		p.setDefaultMinimumSimilarity(minSim);
		altUserQuery = solrParams.get(GaiaQueryParserParams.ALTQ, GaiaQueryParserParams.ALTQDefault);

		boolean leftToRightPrec = solrParams.getBool(GaiaQueryParserParams.LEFTTORIGHTPREC, true);
		p.setLeftToRightPrec(leftToRightPrec);

		boolean spanFuzzy = solrParams.getBool(GaiaQueryParserParams.SPANFUZZY, true);
		p.setSpanFuzzy(spanFuzzy);

		int maxTerms = solrParams.getInt(GaiaQueryParserParams.MAXTERMS, GaiaQueryParserParams.MAXTERMSDefault);
		p.setMaxTerms(maxTerms);

		int maxGenTerms = solrParams.getInt(GaiaQueryParserParams.MAXGENTERMS, GaiaQueryParserParams.MAXGENTERMSDefault);
		p.setMaxGenTerms(maxGenTerms);

		int maxQuery = solrParams.getInt(GaiaQueryParserParams.MAXQUERY, GaiaQueryParserParams.MAXQUERYDefault);
		p.setMaxQuery(maxQuery);

		int maxBooleanClauses = solrParams.getInt(GaiaQueryParserParams.MAXBOOLEANCLAUSES,
				GaiaQueryParserParams.MAXBOOLEANCLAUSESDefault);
		p.setMaxBooleanClauses(maxBooleanClauses);

		int minStripTrailQMark = solrParams.getInt(GaiaQueryParserParams.MINSTRIPTRAILQMARK,
				GaiaQueryParserParams.MINSTRIPTRAILQMARKDefault);
		p.setMinStripTrailQMark(minStripTrailQMark);

		boolean implicitNiceToHave = solrParams
				.getBool(GaiaQueryParserParams.IMPNICE, GaiaQueryParserParams.IMPNICEDefault);
		p.setImplicitNiceToHave(implicitNiceToHave);

		int minMatch = solrParams.getInt(GaiaQueryParserParams.MINMATCH, GaiaQueryParserParams.MINMATCHDefault);
		p.setMinMatch(minMatch);

		LikeDocParams likeDocParams = new LikeDocParams();
		String[] fl = null;
		String likeDocFl = solrParams.get(LikeDocParams.FL_PARAM);
		if (likeDocFl != null)
			fl = SolrPluginUtils.split(likeDocFl);
		if ((fl == null) || (fl.length < 1))
			fl = LikeDocParams.FL_DEFAULT;
		likeDocParams.fl = fl;
		likeDocParams.topX = solrParams.getInt(LikeDocParams.TOPX_PARAM, LikeDocParams.TOPX_DEFAULT);
		likeDocParams.bottomX = solrParams.getInt(LikeDocParams.BOTTOMX_PARAM, LikeDocParams.BOTTOMX_DEFAULT);
		likeDocParams.useNegatives = solrParams.getBool(LikeDocParams.USENEGATIVES_PARAM,
				LikeDocParams.USENEGATIVES_DEFAULT);
		likeDocParams.maxQueryTermsPerDocument = solrParams.getInt(LikeDocParams.MAXQUERYTERMSPERDOCUMENT_PARAM,
				LikeDocParams.MAXQUERYTERMSPERDOCUMENT_DEFAULT);
		likeDocParams.minTermFreq = solrParams.getInt(LikeDocParams.MINTERMFREQ_PARAM, LikeDocParams.MINTERMFREQ_DEFAULT);
		likeDocParams.minDocFreq = solrParams.getInt(LikeDocParams.MINDOCFREQ_PARAM, LikeDocParams.MINDOCFREQ_DEFAULT);
		likeDocParams.operator = solrParams.get(LikeDocParams.OPERATOR_PARAM, LikeDocParams.OPERATOR_DEFAULT);
		likeDocParams.maxClauses = solrParams.getInt(LikeDocParams.MAXCLAUSES_PARAM, LikeDocParams.MAXCLAUSES_DEFAULT);
		likeDocParams.useStopwords = Boolean.valueOf(solrParams.getBool(LikeDocParams.USESTOPWORDS_PARAM,
				LikeDocParams.USESTOPWORDS_DEFAULT.booleanValue()));
		likeDocParams.minWordLength = solrParams.getInt(LikeDocParams.MINWORDLENGTH_PARAM,
				LikeDocParams.MINWORDLENGTH_DEFAULT);
		likeDocParams.alpha = solrParams.getFloat(LikeDocParams.ALPHA_PARAM, LikeDocParams.ALPHA_DEFAULT);
		likeDocParams.beta = solrParams.getFloat(LikeDocParams.BETA_PARAM, LikeDocParams.BETA_DEFAULT);
		likeDocParams.gamma = solrParams.getFloat(LikeDocParams.GAMMA_PARAM, LikeDocParams.GAMMA_DEFAULT);
		likeDocParams.searchField = solrParams.get(LikeDocParams.SEARCHFIELD_PARAM, LikeDocParams.SEARCHFIELD_DEFAULT);
		p.setLikeDocParams(likeDocParams);
	}

	public GaiaQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
		super(qstr, localParams, params, req);
	}

	public Query parse() throws SyntaxError {
		SolrParams localParams = getLocalParams();
		SolrParams params = getParams();

		SolrParams solrParams = localParams == null ? params : new DefaultSolrParams(localParams, params);

		SolrCore core = getReq().getCore();

		GaiaQueryParser p = new GaiaQueryParser(core);
		p.setQParser(this);

		p.qParser = this;
		p.setLogOutput(logOutput);

		setParserRequestParams(p, solrParams);

		String userQuery = getString();
		String queryToParse = userQuery;

		if ((userQuery == null) || (userQuery.trim().length() < 1)) {
			if (altUserQuery != null)
				queryToParse = altUserQuery;
			else {
				queryToParse = "";
			}

		}

		Query generatedQuery = p.parse(queryToParse);
		parsedUserQuery = generatedQuery;
		if ((parsedUserQuery instanceof BoostedQuery)) {
			BoostedQuery bq = (BoostedQuery) generatedQuery;
			parsedUserQuery = bq.getQuery();
		}

		return generatedQuery;
	}

	public String[] getDefaultHighlightFields() {
		String[] highFields = (String[]) queryFields.keySet().toArray(new String[0]);
		return highFields;
	}

	public Query getHighlightQuery() {
		return parsedUserQuery;
	}

	public void addDebugInfo(NamedList<Object> debugInfo) {
		super.addDebugInfo(debugInfo);
		debugInfo.add("altquerystring", altUserQuery);
		if (null != boostQueries) {
			debugInfo.add("boost_queries", boostParams);
			debugInfo.add("parsed_boost_queries", QueryParsing.toString(boostQueries, getReq().getSchema()));
		}

		debugInfo.add("boostfuncs", getReq().getParams().getParams(GaiaQueryParserParams.BF));
	}

	private static class U extends SolrPluginUtils {
	}
}
