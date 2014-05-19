package gaia.parser.gaia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.synonym.SynonymFilterFactory;
import org.apache.lucene.queries.function.BoostedQuery;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.ProductFloatFunction;
import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.core.SolrCore;
import org.apache.solr.parser.QueryParser;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.FunctionQParserPlugin;
import org.apache.solr.search.QParser;
import org.apache.solr.util.SolrPluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GaiaQueryParser {
	public static transient Logger LOG = LoggerFactory.getLogger(GaiaQueryParser.class);

	public boolean logOutput = false;

	public boolean timeParse = false;

	public IndexSchema schema = null;

	public SolrCore core = null;

	public ParserField allParserField = null;

	public ParserField defaultParserField = null;

	public ParserField valParserField = null;

	public QueryParser.Operator defaultOperator = QueryParser.Operator.AND;

	public String textPrefix = "text";

	public boolean processStopwords = true;

	public List<String> stopwordsFields = null;

	public boolean expandSynonyms = true;

	public boolean defaultSynonyms = true;

	public List<String> synonymsFields = null;

	public boolean removeAccents = true;

	public QParser qParser = null;

	public ParserTermModifiers modifiers = new ParserTermModifiers();

	public boolean opUp = false;

	public boolean notUp = true;

	public boolean natUp = false;

	public boolean opGram = true;

	public GaiaSchemaFields schemaFields = null;

	private boolean sortFieldsByName = true;

	public ParserFieldList queryFields = new ParserFieldList(sortFieldsByName);

	public ParserFieldList allFields = new ParserFieldList(sortFieldsByName);

	public ParserFieldList bigramRelevancyFields = new ParserFieldList(sortFieldsByName);

	public ParserFieldList trigramRelevancyFields = new ParserFieldList(sortFieldsByName);

	public String[] multBoosts = null;

	public String[] boostFuncs = null;

	public String[] boostParams = null;

	public boolean boostUnigrams = false;

	public boolean boostBigrams = true;

	public boolean boostTrigrams = true;

	public boolean leftToRightPrec = true;

	public boolean spanFuzzy = true;

	public int minStripTrailQMark = 3;

	public boolean implicitNiceToHave = true;

	public Map<String, ParserField> parserFields = new HashMap<String, ParserField>();

	public float tieBreakerMultiplier = 0.01F;

	public SynonymFilterFactory synonymFilterFactory = null;

	public int queryPhraseSlop = 0;

	public int relevancyPhraseSlop = 50;

	public int nearSlop = 15;

	public int moreLikeThreshold = 12;

	public boolean leadWild = true;

	public boolean strictColon = false;

	public boolean stickyModifiers = true;

	public float defaultMinimumSimilarity = 2.0F;

	private GaiaTokenizer tokenizer = new GaiaTokenizer(this);

	private int parenLevel = 0;

	public int maxTerms = 20000;

	public int maxGenTerms = 100000;

	public int genTermCount = 0;

	public boolean maxGenTermsExceeded = false;

	public int maxQuery = 65536;

	public int maxBooleanClauses = 100000;

	public int minMatch = 0;

	private GaiaASTGenerator astGenerator = new GaiaASTGenerator();
	private GaiaQueryGenerator queryGenerator;
	protected LikeDocParams likeDocParams = new LikeDocParams();

	public void setLogOutput(boolean logOutput) {
		if (logOutput)
			LOG.info("setLogOutput(true)");
		this.logOutput = logOutput;
	}

	public void setSchema(IndexSchema s) {
		schema = s;
	}

	public IndexSchema getSchema() {
		return schema;
	}

	public void setCore(SolrCore c) {
		core = c;
	}

	public SolrCore getCore() {
		return core;
	}

	public QueryParser.Operator getDefaultOperator() {
		return defaultOperator;
	}

	public void setTextPrefix(String prefix) {
		schemaFields.setTextPrefix(prefix);

		if (parserFields != null) {
			defaultParserField.defaultIsText = false;
			defaultParserField.schemaField = null;

			allParserField.defaultIsText = false;
			allParserField.schemaField = null;

			GaiaSchemaField field1 = null;

			for (ParserField pf : parserFields.values()) {
				pf.determineFieldType();
				if ((field1 == null) && (pf.isQueryField) && (pf.schemaField != null))
					field1 = pf.schemaField;
				if ((pf.isText()) && (pf.isQueryField)) {
					defaultParserField.defaultIsText = true;
					if (defaultParserField.schemaField == null)
						defaultParserField.schemaField = pf.schemaField;
				}
				if ((pf.isText()) && (pf.isAllField)) {
					allParserField.defaultIsText = true;
					if (allParserField.schemaField == null) {
						allParserField.schemaField = pf.schemaField;
					}
				}
			}

			if (defaultParserField.schemaField == null)
				defaultParserField.schemaField = field1;
			if (allParserField.schemaField == null)
				allParserField.schemaField = field1;
		}
	}

	public void setProcessStopwords(boolean b) {
		processStopwords = b;
	}

	public void setStopwordsFields(List<String> fields) {
		stopwordsFields = fields;

		for (ParserField f : parserFields.values()) {
			f.processStopwords = false;
		}

		int n = fields.size();
		for (int i = 0; i < n; i++) {
			String fieldName = (String) fields.get(i);
			if ((fieldName != null) && (fieldName.length() > 0)) {
				ParserField f = getParserField(fieldName);
				if (f != null) {
					f.processStopwords = true;

					if ((f.isQueryField) && (defaultParserField != null))
						defaultParserField.processStopwords = true;
					if ((f.isAllField) && (allParserField != null))
						allParserField.processStopwords = true;
				}
			}
		}
	}

	public void setExpandSynonyms(boolean b) {
		expandSynonyms = b;
	}

	public void setDefaultSynonyms(boolean b) {
		defaultSynonyms = b;
	}

	public void setSynonymsFields(List<String> fields) {
		synonymsFields = fields;

		for (ParserField f : parserFields.values()) {
			f.expandSynonyms = false;
		}

		int n = fields.size();
		for (int i = 0; i < n; i++) {
			String fieldName = (String) fields.get(i);
			if ((fieldName != null) && (fieldName.length() > 0)) {
				ParserField f = getParserField(fieldName);
				if (f != null) {
					f.expandSynonyms = true;

					if ((f.isQueryField) && (defaultParserField != null))
						defaultParserField.expandSynonyms = true;
					if ((f.isAllField) && (allParserField != null))
						allParserField.expandSynonyms = true;
				}
			}
		}
	}

	public void setRemoveAccents(boolean remove) {
		removeAccents = remove;
	}

	public QParser getQParser() {
		return qParser;
	}

	public void setQParser(QParser parser) {
		qParser = parser;
	}

	public void setOpUp(boolean b) {
		opUp = b;
		tokenizer.setOpUp(b);
	}

	public void setNotUp(boolean b) {
		notUp = b;
		tokenizer.setNotUp(b);
	}

	public void setNatUp(boolean b) {
		natUp = b;
		tokenizer.setNatUp(b);
	}

	public void setOpGram(boolean b) {
		opGram = b;
	}

	public void setBoostUnigrams(boolean b) {
		boostUnigrams = b;
	}

	public void setBoostBigrams(boolean b) {
		boostBigrams = b;
	}

	public void setBoostTrigrams(boolean b) {
		boostTrigrams = b;
	}

	public void setLeftToRightPrec(boolean b) {
		leftToRightPrec = b;
	}

	public void setSpanFuzzy(boolean b) {
		spanFuzzy = b;
	}

	public void setMinStripTrailQMark(int n) {
		minStripTrailQMark = n;
	}

	public void setImplicitNiceToHave(boolean b) {
		implicitNiceToHave = b;
	}

	public ParserField getParserField(String fieldName) {
		if (parserFields == null) {
			return null;
		}
		if (fieldName.equals("ALL")) {
			if (allParserField == null) {
				allParserField = new ParserField(this, "ALL");
				parserFields.put(allParserField.fieldName(), allParserField);
			}
			return allParserField;
		}
		if (fieldName.equals("DEFAULT")) {
			if (defaultParserField == null) {
				defaultParserField = new ParserField(this, "DEFAULT");
				parserFields.put(defaultParserField.fieldName(), defaultParserField);
			}
			return defaultParserField;
		}
		if (fieldName.equals("_val_")) {
			if (valParserField == null) {
				valParserField = new ParserField(this, "_val_");
				parserFields.put(valParserField.fieldName(), valParserField);
			}
			return valParserField;
		}
		ParserField pf = (ParserField) parserFields.get(fieldName);
		if (pf == null) {
			SchemaField field = schemaFields.getSchemaField(fieldName);
			if (field != null) {
				pf = new ParserField(this, fieldName);
				parserFields.put(fieldName, pf);

				int n = queryFields.size();
				for (int i = 0; i < n; i++) {
					ParserField pf1 = (ParserField) queryFields.get(i);
					if (pf1.fieldName().equals(fieldName)) {
						pf.setQueryBoost(pf1.queryBoost);

						pf.isQueryField = true;
						break;
					}

				}

				n = bigramRelevancyFields.size();
				for (int i = 0; i < n; i++) {
					ParserField pf1 = (ParserField) bigramRelevancyFields.get(i);
					String pf1Name = pf1.fieldName();
					if ((pf1Name != null) && (pf1Name.equals(fieldName))) {
						pf.setBigramRelevancyBoost(pf1.queryBoost);
						break;
					}

				}

				n = trigramRelevancyFields.size();
				for (int i = 0; i < n; i++) {
					ParserField pf1 = (ParserField) trigramRelevancyFields.get(i);
					String pf1Name = pf1.fieldName();
					if ((pf1Name != null) && (pf1Name.equals(fieldName))) {
						pf.setTrigramRelevancyBoost(pf1.queryBoost);
						break;
					}
				}
			}
		}
		return pf;
	}

	public void setTieBreakerMultiplier(float tie) {
		tieBreakerMultiplier = tie;
	}

	public void setAllFields(Map<String, Float> fields) {
		for (Map.Entry<String, ParserField> entry : parserFields.entrySet()) {
			Object value = entry.getValue();
			ParserField pf = (ParserField) value;
			pf.setAllQueryBoost(1.0F);
			pf.isAllField = false;
		}

		allFields = new ParserFieldList(sortFieldsByName);

		allParserField.defaultIsText = false;
		allParserField.schemaField = null;

		GaiaSchemaField field1 = null;

		for (Map.Entry<String, Float> entry : fields.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (key.equals("*")) {
				List<String> fieldNames = schemaFields.getNames();
				for (int i = 0; i < fieldNames.size(); i++) {
					key = (String) fieldNames.get(i);
					ParserField pf = getParserField(key);
					if (pf == null) {
						LOG.warn("Ignoring undefined field name <<" + key + ">> in ALL fields list");
					} else {
						pf.isAllField = true;

						if (pf.isText()) {
							allParserField.defaultIsText = true;
							if (allParserField.schemaField == null) {
								allParserField.schemaField = pf.schemaField;
							}
						}

						if ((field1 == null) && (pf.schemaField != null)) {
							field1 = pf.schemaField;
						}
						if (value != null)
							pf.setAllQueryBoost(((Float) value).floatValue());
						allFields.add(pf);
					}
				}
			} else {
				ParserField pf = getParserField(key);
				if (pf == null) {
					LOG.warn("Ignoring undefined field name <<" + key + ">> in ALL fields list");
				} else {
					pf.isAllField = true;

					if (pf.isText()) {
						allParserField.defaultIsText = true;
						if (allParserField.schemaField == null) {
							allParserField.schemaField = pf.schemaField;
						}
					}

					if ((field1 == null) && (pf.schemaField != null)) {
						field1 = pf.schemaField;
					}
					if (value != null)
						pf.setAllQueryBoost(((Float) value).floatValue());
					allFields.add(pf);
				}
			}
		}

		if (allParserField.schemaField == null)
			allParserField.schemaField = field1;
	}

	public void setQueryFields(Map<String, Float> fields) {
		for (Map.Entry<String, ParserField> entry : parserFields.entrySet()) {
			Object value = entry.getValue();
			ParserField pf = (ParserField) value;
			pf.setQueryBoost(1.0F);
			pf.isQueryField = false;
		}

		queryFields = new ParserFieldList(sortFieldsByName);

		defaultParserField.defaultIsText = false;
		defaultParserField.schemaField = null;

		GaiaSchemaField field1 = null;

		for (Map.Entry<String, Float> entry : fields.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (key.equals("*")) {
				List<String> fieldNames = schemaFields.getNames();
				for (int i = 0; i < fieldNames.size(); i++) {
					key = (String) fieldNames.get(i);
					ParserField pf = getParserField(key);
					if (pf == null) {
						LOG.warn("Ignoring undefined field name <<" + key + ">> in query fields list");
					} else {
						pf.isQueryField = true;

						if (pf.isText()) {
							defaultParserField.defaultIsText = true;
							if (defaultParserField.schemaField == null) {
								defaultParserField.schemaField = pf.schemaField;
							}
						}

						if ((field1 == null) && (pf.schemaField != null)) {
							field1 = pf.schemaField;
						}
						if (value != null)
							pf.setQueryBoost(((Float) value).floatValue());
						queryFields.add(pf);
					}
				}
			} else {
				ParserField pf = getParserField(key);
				if (pf == null) {
					LOG.warn("Ignoring undefined field name <<" + key + ">> in query fields list");
				} else {
					pf.isQueryField = true;

					if (pf.isText()) {
						defaultParserField.defaultIsText = true;
						if (defaultParserField.schemaField == null) {
							defaultParserField.schemaField = pf.schemaField;
						}
					}

					if ((field1 == null) && (pf.schemaField != null)) {
						field1 = pf.schemaField;
					}
					if (value != null)
						pf.setQueryBoost(((Float) value).floatValue());
					queryFields.add(pf);
				}
			}
		}

		if (defaultParserField.schemaField == null)
			defaultParserField.schemaField = field1;
	}

	public void setBigramRelevancyFields(Map<String, Float> fields) {
		bigramRelevancyFields = new ParserFieldList(sortFieldsByName);

		int n = fields.size();
		Iterator<Map.Entry<String, Float>> it = fields.entrySet().iterator();
		for (int i = 0; i < n; i++) {
			Map.Entry<String, Float> entry = it.next();
			String key = entry.getKey();
			Object value = entry.getValue();

			if (key.equals("*")) {
				List<String> fieldNames = schemaFields.getNames();
				for (int j = 0; j < fieldNames.size(); j++) {
					key = (String) fieldNames.get(j);
					ParserField pf = new ParserField(this, key);
					if (pf.schemaField == null) {
						LOG.warn("Ignoring undefined field name <<" + key + ">> in bigram relevancy boost fields list");
					} else {
						if (value != null)
							pf.setBigramRelevancyBoost(((Float) value).floatValue());
						bigramRelevancyFields.add(pf);
					}
				}
			} else {
				ParserField pf = new ParserField(this, key);
				if (pf.schemaField == null) {
					LOG.warn("Ignoring undefined field name <<" + key + ">> in bigram relevancy boost fields list");
				} else {
					if (value != null)
						pf.setBigramRelevancyBoost(((Float) value).floatValue());
					bigramRelevancyFields.add(pf);
				}
			}
		}
	}

	public void setTrigramRelevancyFields(Map<String, Float> fields) {
		trigramRelevancyFields = new ParserFieldList(sortFieldsByName);

		int n = fields.size();
		Iterator<Map.Entry<String, Float>> it = fields.entrySet().iterator();
		for (int i = 0; i < n; i++) {
			Map.Entry<String, Float> entry = it.next();
			String key = (String) entry.getKey();
			Object value = entry.getValue();

			if (key.equals("*")) {
				List<String> fieldNames = schemaFields.getNames();
				for (int j = 0; j < fieldNames.size(); j++) {
					key = (String) fieldNames.get(j);
					ParserField pf = new ParserField(this, key);
					if (pf.schemaField == null) {
						LOG.warn("Ignoring undefined field name <<" + key + ">> in trigram relevancy boost fields list");
					} else {
						if (value != null)
							pf.setTrigramRelevancyBoost(((Float) value).floatValue());
						trigramRelevancyFields.add(pf);
					}
				}
			} else {
				ParserField pf = new ParserField(this, key);
				if (pf.schemaField == null) {
					LOG.warn("Ignoring undefined field name <<" + key + ">> in trigram relevancy boost fields list");
				} else {
					if (value != null)
						pf.setTrigramRelevancyBoost(((Float) value).floatValue());
					trigramRelevancyFields.add(pf);
				}
			}
		}
	}

	public void setQueryPhraseSlop(int n) {
		queryPhraseSlop = n;
	}

	public void setRelevancyPhraseSlop(int n) {
		relevancyPhraseSlop = n;
	}

	public void setNearSlop(int n) {
		nearSlop = n;
	}

	public void setMoreLikeThreshold(int n) {
		moreLikeThreshold = n;
	}

	public void setLeadWild(boolean b) {
		leadWild = b;
	}

	public void setStrictColon(boolean b) {
		strictColon = b;
	}

	public void setStickyModifiers(boolean b) {
		stickyModifiers = b;
	}

	public void setDefaultMinimumSimilarity(float f) {
		defaultMinimumSimilarity = f;
	}

	public void setMaxTerms(int m) {
		if (m < 10)
			maxTerms = 10;
		else if (m > 100000)
			maxTerms = 100000;
		else
			maxTerms = m;
	}

	public void setMaxGenTerms(int m) {
		if (m < 10)
			maxGenTerms = 10;
		else if (m > 500000)
			maxGenTerms = 500000;
		else
			maxGenTerms = m;
	}

	public boolean countGenTerms() {
		genTermCount += 1;

		if ((genTermCount > maxGenTerms) && (!maxGenTermsExceeded)) {
			maxGenTermsExceeded = true;
			LOG.warn("Exceeded generated term limit of " + maxGenTerms + " terms");
		}

		return maxGenTermsExceeded;
	}

	public void setMaxQuery(int m) {
		if (m < 10)
			maxQuery = 10;
		else if (m > 100000)
			maxQuery = 100000;
		else
			maxQuery = m;
	}

	public void setMaxBooleanClauses(int m) {
		if ((m > 0) && (m < 10))
			maxBooleanClauses = 10;
		else if (m > 1000000)
			maxBooleanClauses = 1000000;
		else {
			maxBooleanClauses = m;
		}
		if (maxBooleanClauses > 0)
			BooleanQuery.setMaxClauseCount(maxBooleanClauses);
	}

	public void setMinMatch(int n) {
		if (n < 0)
			minMatch = 0;
		else if (n > 100)
			minMatch = 100;
		else
			minMatch = n;
	}

	public void setDefaultOperator(QueryParser.Operator op) {
		modifiers.defaultOperator = op;
	}

	public GaiaASTGenerator getASTGenerator() {
		return astGenerator;
	}

	public void setASTGenerator(GaiaASTGenerator astGenerator) {
		if (astGenerator == null)
			astGenerator = new GaiaASTGenerator();
		else
			this.astGenerator = astGenerator;
	}

	public GaiaQueryGenerator getQueryGenerator() {
		return queryGenerator;
	}

	public void setQueryGenerator(GaiaQueryGenerator queryGenerator) {
		if (queryGenerator == null)
			queryGenerator = new GaiaQueryGenerator(core.getSolrConfig().luceneMatchVersion);
		else
			this.queryGenerator = queryGenerator;
	}

	public void setLikeDocParams(LikeDocParams likeDocParams) {
		this.likeDocParams = likeDocParams;
	}

	public GaiaQueryParser(SolrCore core) {
		setSchema(core == null ? null : core.getLatestSchema());
		setCore(core);
		schemaFields = new GaiaSchemaFields(this);
		modifiers.field = getParserField("DEFAULT");
		getParserField("ALL");
		setDefaultOperator(QueryParser.Operator.AND);
		modifiers.defaultOperator = getDefaultOperator();
		queryGenerator = new GaiaQueryGenerator(core.getSolrConfig().luceneMatchVersion);
	}

	public String defaultOp() {
		if (modifiers.defaultOperator.equals(QueryParser.Operator.OR)) {
			return "OR";
		}
		return "AND";
	}

	public String getFieldTypeName(String fieldName) {
		FieldType fieldType = schema.getFieldTypeNoEx(fieldName);
		if (fieldType == null) {
			return null;
		}
		return fieldType.getTypeName();
	}

	public boolean isQueryStrict() {
		if (tokenizer.tokenCount < 2) {
			return false;
		}
		int parenLevel = 0;
		boolean lastTokenWasTerm = false;

		GaiaToken token = null;
		while (!(token = tokenizer.getToken()).isEnd) {
			String type = token.opType;
			if ((token.isWord) && (type.equals("word"))) {
				if (lastTokenWasTerm) {
					tokenizer.restart(null);
					return false;
				}
				lastTokenWasTerm = true;
			} else if (type.equals("\"")) {
				if (lastTokenWasTerm) {
					tokenizer.restart(null);
					return false;
				}
				while ((!(token = tokenizer.getToken()).isEnd) && (!token.type.equals("\"")))
					;
				lastTokenWasTerm = true;
			} else {
				if (type.equals("(")) {
					parenLevel++;
					lastTokenWasTerm = false;
					while (!(token = tokenizer.getToken()).isEnd) {
						type = token.type;
						if (type.equals("(")) {
							parenLevel++;
						} else if (type.equals(")")) {
							parenLevel--;
							if (parenLevel == 0)
								break;
						} else if (type.equals("\"")) {
							tokenizer.getToken();
							while ((!(token = tokenizer.getToken()).isEnd) && (!token.type.equals("\"")))
								;
						}
					}
				}
				lastTokenWasTerm = false;
			}
		}
		tokenizer.restart(null);

		return true;
	}

	public QueryAST parseFullQuery(String query) {
		QueryAST ast = null;

		modifiers.field = defaultParserField;

		modifiers.minMatchPercent = minMatch;
		modifiers.minMatch = 0;

		parenLevel = 0;

		tokenizer.setup(query);

		if (isQueryStrict()) {
			if (logOutput)
				LOG.info("Strict query: <<" + query + ">>");
			tokenizer.restart(null);
			ast = parseQuery();
		} else {
			int wordCount = tokenizer.wordCount;
			if (wordCount > moreLikeThreshold) {
				if (logOutput) {
					String queryCopy = query;
					int lenLimit = 1024;
					if (query.length() > lenLimit)
						queryCopy = query.substring(0, lenLimit) + "... (truncated at " + lenLimit + "chars)";
					LOG.info("More-like query: <<" + queryCopy + ">> with " + wordCount + " terms (threshold is "
							+ moreLikeThreshold + ")");
				}
				ast = parseMoreLike();
			} else {
				if (logOutput)
					LOG.info("Loose query: <<" + query + ">>");
				ast = parseQuery();
			}
		}

		if (logOutput) {
			if (ast == null)
				LOG.info("parseFullQuery generated null AST for query: <<" + query + ">>");
			else
				LOG.info("parseFullQuery generated AST with " + ast.size() + " clauses for query: <<" + query + ">>");
		}
		return ast;
	}

	public QueryAST parseMoreLike() {
		TermListAST tla = astGenerator.newTermListAST(this, modifiers);
		while (true) {
			GaiaToken t = tokenizer.getToken();
			if (t.isEnd)
				break;
			if (t.isWord) {
				TermAST ta = astGenerator.newTermAST(this, modifiers);
				ParserTerm term = new ParserTerm(this, modifiers, t.text);
				ta.term = term;
				ta.op = "OR";
				tla.addClause(ta);
			}
		}

		return tla;
	}

	public QueryAST parseQuery() {
		QueryAST ast = null;

		boolean gotNot = false;
		GaiaToken prevToken = tokenizer.prevToken;
		GaiaToken token = tokenizer.peekToken();
		String type = token.opType;
		if (((!opUp) && (!notUp) && (token.text.equalsIgnoreCase("not")))
				|| (((opUp) || (notUp)) && (token.text.equals("NOT")))) {
			GaiaToken notToken = token;
			tokenizer.skipToken();
			GaiaToken nextToken = tokenizer.peekToken();
			type = nextToken.opType;
			String text = nextToken.text;
			if ((type.equals("end")) || (type.equals(")")) || (type.equals("~")) || (type.equals("^")) || (type.equals("]"))
					|| (type.equals("}")) || (type.equals("&&")) || (type.equals("||")) || (type.equals("!"))
					|| (text.equalsIgnoreCase("to")) || (text.equalsIgnoreCase("near")) || (text.equalsIgnoreCase("and"))
					|| (text.equalsIgnoreCase("or"))) {
				if (prevToken != null)
					tokenizer.restartSkip(prevToken);
				else
					tokenizer.restart(notToken);
				type = tokenizer.peekTokenTypeOp();
			} else {
				gotNot = true;
			}
		} else if (type.equals("!")) {
			GaiaToken notToken = tokenizer.getToken();
			GaiaToken nextToken = tokenizer.peekToken();
			type = nextToken.opType;
			String text = nextToken.text;
			if ((type.equals("end")) || (type.equals(")")) || (type.equals("~")) || (type.equals("^")) || (type.equals("]"))
					|| (type.equals("}")) || (type.equals("&&")) || (type.equals("||")) || (type.equals("!"))
					|| (text.equalsIgnoreCase("to")) || (text.equalsIgnoreCase("near")) || (text.equalsIgnoreCase("and"))
					|| (text.equalsIgnoreCase("or"))) {
				if (prevToken != null)
					tokenizer.restartSkip(prevToken);
				else
					tokenizer.restart(notToken);
				type = tokenizer.peekTokenTypeOp();
			} else {
				gotNot = true;
			}
		}
		if (gotNot) {
			QueryAST q = parseClause();
			if (q != null) {
				if ((q.op == null) || (q.op.equals("+")) || (q.op.equals("&&")))
					q.clauseOp = "!";
				else if (q.op.equals("-"))
					q.clauseOp = "+";
				ast = q;
			} else {
				ast = null;
			}
		} else {
			ast = parseClause();
		}

		ClauseListAST ast1 = astGenerator.newClauseListAST(this, modifiers);
		if (ast != null)
			ast1.addClause(ast);
		ast = ast1;
		while (true) {
			type = tokenizer.peekTokenTypeOp();

			if ((!type.equals("&&")) && (!type.equals("||")) && (!type.equals("!")) && (!type.equals("("))) {
				break;
			}
			tokenizer.getToken();

			String copyType = type;
			if ((type.equals("&&")) && (tokenizer.peekTokenTypeOp().equals("!"))) {
				tokenizer.skipToken();
				type = "&&!";
				copyType = "&&";
			} else if ((type.equals("||")) && (tokenizer.peekTokenTypeOp().equals("!"))) {
				tokenizer.skipToken();
				type = "||!";
				copyType = "||";
			} else if (type.equals("!")) {
				copyType = null;
			}
			QueryAST nextAst = null;
			if ((type.equals("(")) || (type.equals("(!"))) {
				parenLevel += 1;
				nextAst = parseQuery();
				if (tokenizer.peekTokenTypeOp().equals(")"))
					tokenizer.skipToken();
				parenLevel -= 1;

				copyType = null;

				parseTermBoost(nextAst);
			} else {
				nextAst = parseClause();
			}

			if (nextAst != null) {
				if ((ast.size() == 1) && (ast.getClause(0).clauseOp == null)) {
					ast.getClause(0).clauseOp = copyType;
				}
				nextAst.clauseOp = type;

				ast.addClause(nextAst);
			}
		}

		int n = ast.size();
		if (n == 0)
			return null;
		if (n == 1) {
			QueryAST cl1 = ast.getClause(0);
			String op = cl1.clauseOp;
			if (op == null)
				return cl1;
			if ((ast.op == null) && ((op.equals("!")) || (op.equals("-")))) {
				return ast;
			}
			return cl1;
		}
		return ast;
	}

	public QueryAST parseClause() {
		boolean gotNot = false;
		GaiaToken prevToken = tokenizer.prevToken;
		GaiaToken token = tokenizer.peekToken();
		String type = token.opType;
		if (((!opUp) && (!notUp) && (token.text.equalsIgnoreCase("not")))
				|| (((opUp) || (notUp)) && (token.text.equals("NOT")))) {
			GaiaToken notToken = token;
			tokenizer.skipToken();
			GaiaToken nextToken = tokenizer.peekToken();
			type = nextToken.opType;
			String text = nextToken.text;
			if ((type.equals("end")) || (type.equals(")")) || (type.equals("~")) || (type.equals("^")) || (type.equals("]"))
					|| (type.equals("}")) || (type.equals("&&")) || (type.equals("||")) || (type.equals("!"))
					|| (text.equalsIgnoreCase("to")) || (text.equalsIgnoreCase("near")) || (text.equalsIgnoreCase("and"))
					|| (text.equalsIgnoreCase("or"))) {
				if (prevToken != null)
					tokenizer.restartSkip(prevToken);
				else
					tokenizer.restart(notToken);
			} else
				gotNot = true;
		} else if (type.equals("!")) {
			GaiaToken notToken = tokenizer.getToken();
			GaiaToken nextToken = tokenizer.peekToken();
			type = nextToken.opType;
			String text = nextToken.text;
			if ((type.equals("end")) || (type.equals(")")) || (type.equals("~")) || (type.equals("^")) || (type.equals("]"))
					|| (type.equals("}")) || (type.equals("&&")) || (type.equals("||")) || (type.equals("!"))
					|| (text.equalsIgnoreCase("to")) || (text.equalsIgnoreCase("near")) || (text.equalsIgnoreCase("and"))
					|| (text.equalsIgnoreCase("or"))) {
				if (prevToken != null)
					tokenizer.restartSkip(prevToken);
				else
					tokenizer.restart(notToken);
			} else
				gotNot = true;
		}

		if (gotNot) {
			QueryAST q = parseClause();
			if (q != null) {
				QueryAST q1 = astGenerator.newNotAST(this, modifiers);
				if ((q.op == null) || (q.op.equals("+")) || (q.op.equals("&&")))
					q1.op = "!";
				else if (q.op.equals("-"))
					q1.op = "+";
				q1.addClause(q);
				return q1;
			}
			return null;
		}
		return parseProximityList();
	}

	public QueryAST parseProximityList() {
		ProximityAST proximityAst = null;
		QueryAST termListAst = null;

		termListAst = parseTermList();
		while (true) {
			GaiaToken token = tokenizer.peekToken();
			String type = token.opType;
			ProximityAST newProximityAst = null;
			if (type.equals("near")) {
				newProximityAst = astGenerator.newNearAST(this, modifiers);
			} else if (type.equals("before")) {
				newProximityAst = astGenerator.newBeforeAST(this, modifiers);
			} else {
				if (!type.equals("after"))
					break;
				newProximityAst = astGenerator.newAfterAST(this, modifiers);
			}

			tokenizer.skipToken();

			int proximitySlop = 0;
			if ((tokenizer.peekTokenType().equals(":"))
					&& ((!strictColon) || ((!tokenizer.isSpaceBefore()) && (!tokenizer.isSpaceAfter())))) {
				tokenizer.skipToken();

				proximitySlop = (int) parseNumber();
				newProximityAst.proximitySlop(proximitySlop);
			}

			if (proximityAst == null)
				newProximityAst.addClause(termListAst);
			else
				newProximityAst.addClause(proximityAst);
			proximityAst = newProximityAst;

			termListAst = parseTermList();
			proximityAst.addClause(termListAst);

			if (((proximityAst != null) && (proximityAst.modifiers != null) && (proximityAst.modifiers.hyphenated))
					|| ((termListAst != null) && (termListAst.modifiers != null) && (termListAst.modifiers.hyphenated))) {
				newProximityAst.modifiers.hyphenated = true;
			}
			proximityAst = newProximityAst;
		}

		if (proximityAst == null) {
			return termListAst;
		}
		return proximityAst;
	}

	public QueryAST parseTermList() {
		TermListAST termListAst = null;
		QueryAST termAst = null;

		termAst = parseTerm();

		termListAst = astGenerator.newTermListAST(this, modifiers);
		termListAst.setMoreLikeThreshold(moreLikeThreshold);
		if (termAst != null) {
			termListAst.addClause(termAst);
		}
		while (true) {
			GaiaToken prevToken = tokenizer.prevToken;
			GaiaToken token = tokenizer.peekToken();
			String type = token.opType;
			if ((type.equals("&&")) || (type.equals("||"))) {
				GaiaToken opToken = tokenizer.getToken();
				String nextType = tokenizer.peekTokenTypeOp();
				if (nextType.equals("!")) {
					tokenizer.getToken();
					String nextType2 = tokenizer.peekTokenTypeOp();
					tokenizer.restartSkip(prevToken);
					type = tokenizer.peekTokenTypeOp();
					if ((!nextType2.equals("end")) && (!nextType2.equals(")")) && (!nextType2.equals("~"))
							&& (!nextType2.equals("^")) && (!nextType2.equals("]")) && (!nextType2.equals("}"))) {
						break;
					}

				} else {
					if (prevToken != null)
						tokenizer.restartSkip(prevToken);
					else
						tokenizer.restart(opToken);
					type = tokenizer.peekTokenTypeOp();
					if ((!nextType.equals("end")) && (!nextType.equals(")")) && (!nextType.equals("~"))
							&& (!nextType.equals("^")) && (!nextType.equals("]")) && (!nextType.equals("}"))) {
						break;
					}

				}

			} else if (type.equals("!")) {
				GaiaToken opToken = tokenizer.getToken();
				String nextType = tokenizer.peekTokenTypeOp();
				if (prevToken != null)
					tokenizer.restartSkip(prevToken);
				else
					tokenizer.restart(opToken);
				type = tokenizer.peekTokenTypeOp();
				if ((!nextType.equals("end")) && (!nextType.equals(")")) && (!nextType.equals("~")) && (!nextType.equals("^"))
						&& (!nextType.equals("]")) && (!nextType.equals("}"))) {
					break;
				}

			} else {
				if ((type.equals(")")) || (type.equals("end")))
					break;
				if ((type.equals("near")) || (type.equals("after")) || (type.equals("before")))
					break;
				if ((!token.isWord) && (!type.equals("to")) && (!type.equals("\"")) && (!type.equals(":"))
						&& (!type.equals("+")) && (!type.equals("-")) && (!type.equals("(")) && (!type.equals("{"))
						&& (!type.equals("[")) && (!token.isRelOp)) {
					tokenizer.skipToken();
					continue;
				}
			}
			termAst = parseTerm();

			if (termAst != null) {
				termListAst.addClause(termAst);
			}
		}
		int n = termListAst.size();
		if (n == 0)
			return null;
		if (n == 1) {
			QueryAST cl1 = termListAst.getClause(0);
			String op = cl1.op;
			if ((op == null) || ((!op.equals("!")) && (!op.equals("-")) && (!op.equals("!=")))) {
				return cl1;
			}
			return termListAst;
		}
		return termListAst;
	}

	public QueryAST parseTerm() {
		ParserTermModifiers modifiers = this.modifiers.clone();
		QueryAST ast = null;
		TermAST ta = null;
		String op = null;
		String type = null;
		boolean skipColon = false;
		boolean doLikeDoc = false;

		if (modifiers.defaultOperator == QueryParser.Operator.OR) {
			op = "|";
		}

		boolean relop = false;
		while (true) {
			GaiaToken prevToken = tokenizer.prevToken;
			GaiaToken curToken = tokenizer.peekToken();
			type = curToken.opType;
			if (curToken.isWord) {
				GaiaToken word1 = tokenizer.getToken();

				String newFieldName = word1.text;
				if ((tokenizer.peekTokenType().equals(":"))
						&& ((!strictColon) || ((!tokenizer.isSpaceBefore()) && (!tokenizer.isSpaceAfter())))) {
					boolean sticky = (!strictColon) && (stickyModifiers) && (tokenizer.isSpaceAfter());

					if (newFieldName.equalsIgnoreCase("syn")) {
						modifiers.expandSynonyms = true;
						if (sticky)
							modifiers.expandSynonyms = true;
						tokenizer.skipToken();
					} else if (newFieldName.equalsIgnoreCase("nosyn")) {
						modifiers.expandSynonyms = false;
						if (sticky)
							modifiers.expandSynonyms = false;
						tokenizer.skipToken();
					} else if (newFieldName.equalsIgnoreCase("stem")) {
						modifiers.stemWords = true;
						if (sticky)
							modifiers.stemWords = true;
						tokenizer.skipToken();
					} else if (newFieldName.equalsIgnoreCase("nostem")) {
						modifiers.stemWords = false;
						if (sticky)
							modifiers.stemWords = false;
						tokenizer.skipToken();
					} else if (newFieldName.equalsIgnoreCase("like")) {
						modifiers.defaultOperator = QueryParser.Operator.OR;
						op = "|";
						relop = false;
						if (sticky)
							modifiers.defaultOperator = QueryParser.Operator.OR;
						tokenizer.skipToken();

						GaiaToken termToken = tokenizer.peekToken();
						String termType = termToken.type;
						doLikeDoc = (!termType.equals("(")) && (!tokenizer.isSpaceBefore())
								&& ((termType.equals("\"")) || ((termToken.isWord) && (!termToken.isAlpha())));
					} else if ((newFieldName.equalsIgnoreCase(GaiaQueryParserParams.MINMATCH))
							|| (newFieldName.equalsIgnoreCase("atLeast"))) {
						tokenizer.skipToken();

						if (tokenizer.peekTokenType().equals("word")) {
							GaiaToken numWord = tokenizer.peekToken();
							String numText = numWord.text;
							int numDigits = 0;
							int numDots = 0;
							int numPercents = 0;
							int n = numText.length();
							for (int i = 0; i < n; i++) {
								char ch = numText.charAt(i);
								if (Character.isDigit(ch)) {
									numDigits++;
								} else if (ch == '.') {
									numDots++;
								} else {
									if (ch != '%')
										break;
									if (i != n - 1)
										break;
									numPercents++;
								}

							}

							if (numDigits == n - (numDots + numPercents)) {
								tokenizer.skipToken();

								if (numPercents > 0) {
									numText = numText.substring(0, n - 1);
								}

								float minMatchRaw = Float.valueOf(numText).floatValue();

								int minMatchCount = 0;
								int minMatchPercent = 0;
								if (numPercents > 0)
									minMatchPercent = (int) minMatchRaw;
								else if ((numDots > 0) && (minMatchRaw <= 1.0F))
									minMatchPercent = (int) (minMatchRaw * 100.0F);
								else if (minMatchRaw <= 19.0F)
									minMatchCount = (int) minMatchRaw;
								else {
									minMatchPercent = (int) minMatchRaw;
								}

								if (minMatchPercent > 100) {
									minMatchPercent = 100;
								}

								boolean nonSticky = false;
								if ((tokenizer.peekTokenType().equals(":"))
										&& ((!strictColon) || ((!tokenizer.isSpaceBefore()) && (!tokenizer.isSpaceAfter())))) {
									nonSticky = (strictColon) || (stickyModifiers) || (!tokenizer.isSpaceAfter());

									if (nonSticky) {
										tokenizer.skipToken();
									}
								} else if (tokenizer.peekTokenType().equals("(")) {
									nonSticky = true;
								}

								if (!nonSticky) {
									modifiers.minMatch = minMatchCount;
									modifiers.minMatchPercent = minMatchPercent;
								}

								modifiers.minMatch = minMatchCount;
								modifiers.minMatchPercent = minMatchPercent;
							}
						}
					} else if (newFieldName.equals("*")) {
						modifiers.field = allParserField;
						modifiers.allFields = true;
						if (sticky) {
							modifiers.field = allParserField;
							modifiers.allFields = true;
						}
						tokenizer.skipToken();
					} else if (newFieldName.equals("ALL")) {
						modifiers.field = allParserField;
						modifiers.allFields = true;
						if (sticky) {
							modifiers.field = allParserField;
							modifiers.allFields = true;
						}
						tokenizer.skipToken();
					} else if (newFieldName.equals("DEFAULT")) {
						modifiers.field = defaultParserField;
						modifiers.allFields = false;
						if (sticky) {
							modifiers.field = defaultParserField;
							modifiers.allFields = false;
						}
						tokenizer.skipToken();
					} else if (newFieldName.equals("_val_")) {
						if (valParserField == null)
							valParserField = getParserField(newFieldName);
						modifiers.field = valParserField;
						modifiers.allFields = false;
						if (sticky) {
							modifiers.field = valParserField;
							modifiers.allFields = false;
						}
						tokenizer.skipToken();
					} else if (schemaFields.isValidFieldName(newFieldName)) {
						modifiers.field = getParserField(newFieldName);
						modifiers.allFields = false;
						if (sticky) {
							modifiers.field = modifiers.field;
							modifiers.allFields = false;
						}
						tokenizer.skipToken();
					} else if (newFieldName.equalsIgnoreCase("all")) {
						modifiers.field = allParserField;
						modifiers.allFields = true;
						if (sticky) {
							modifiers.field = allParserField;
							modifiers.allFields = true;
						}
						tokenizer.skipToken();
					} else if (newFieldName.equalsIgnoreCase("default")) {
						modifiers.field = defaultParserField;
						modifiers.allFields = false;
						if (sticky) {
							modifiers.field = defaultParserField;
							modifiers.allFields = false;
						}
						tokenizer.skipToken();
					} else if (newFieldName.equalsIgnoreCase("debugLog")) {
						setLogOutput(true);
						tokenizer.skipToken();
					} else {
						if (prevToken != null)
							tokenizer.restartSkip(prevToken);
						else
							tokenizer.restart(curToken);
						skipColon = true;
						break;
					}
				} else {
					if (prevToken != null) {
						tokenizer.restartSkip(prevToken);
						break;
					}
					tokenizer.restart(curToken);
					break;
				}
			} else if (type.equals("+")) {
				if (!tokenizer.isSpaceAfter()) {
					op = type;
					relop = false;
				} else {
					op = null;
				}
				tokenizer.skipToken();
			} else if (type.equals("-")) {
				if (!tokenizer.isSpaceAfter()) {
					tokenizer.skipToken();
					if (tokenizer.peekTokenType().equals("-")) {
						while ((tokenizer.peekTokenType().equals("-")) && (!tokenizer.isSpaceBefore()))
							tokenizer.skipToken();
					}
					op = type;
					relop = false;
				} else {
					tokenizer.skipToken();
					op = null;
				}
			} else if (type.equals(":")) {
				modifiers.field = allParserField;
				modifiers.allFields = true;
				if ((!strictColon) && (stickyModifiers) && (tokenizer.isSpaceAfter())) {
					modifiers.field = allParserField;
					modifiers.allFields = true;
				}
				tokenizer.skipToken();
			} else if (curToken.isRelOp) {
				op = type;
				relop = true;
				tokenizer.skipToken();
			} else {
				if ((!type.equals(")")) || (parenLevel != 0))
					break;
				tokenizer.skipToken();
			}

		}

		boolean isText = modifiers.field.isText();

		GaiaToken word1 = tokenizer.peekToken();
		type = word1.type;
		String termText = "";
		boolean isWord = true;
		if (word1.isWord)
			termText = word1.text;
		else if (type.equals("&&"))
			termText = "and";
		else if (type.equals("||"))
			termText = "or";
		else if (type.equals("!"))
			termText = "not";
		else {
			isWord = false;
		}

		if (doLikeDoc) {
			String docId = null;
			if (type.equals("\"")) {
				GaiaToken string = tokenizer.parseQuotedStringToken();
				docId = string.text;
			} else {
				GaiaToken docIdToken = tokenizer.getToken();
				docId = docIdToken.text;
			}

			Map<String, Float> termsAndWeights = GaiaQueryParserUtils.extractDocumentTerms(core, docId, likeDocParams);

			int numTerms = termsAndWeights.size();
			if (numTerms > 0) {
				ast = astGenerator.newTermListAST(this, modifiers);

				for (String likeTermText : termsAndWeights.keySet()) {
					TermAST likeAst = astGenerator.newTermAST(this, modifiers);
					ParserTerm term = new ParserTerm(this, modifiers, likeTermText);
					likeAst.term = term;
					likeAst.op = "OR";
					likeAst.boost = ((Float) termsAndWeights.get(likeTermText)).floatValue();
					likeAst.haveBoost = true;

					ast.addClause(likeAst);
				}

				parseMinMatch(ast, modifiers);
			}
		} else if (isWord) {
			GaiaToken curToken = tokenizer.getToken();

			if (skipColon) {
				tokenizer.skipToken();
			}
			ParserTermModifiers termModifiers = modifiers.clone();
			termModifiers.isWild = curToken.isWild;
			termModifiers.isAllStarWild = curToken.isAllStarWild;
			termModifiers.hasAllStarWildSuffix = curToken.hasWildStarSuffix;

			if (isText)
				ta = astGenerator.newTermAST(this, termModifiers);
			else {
				ta = astGenerator.newStringAST(this, termModifiers);
			}
			parseTermBoost(ta);
			boolean haveBoost = ta.haveBoost;
			float boost = ta.boost;

			if ((termModifiers.isWild) && (tokenizer.peekTokenType().equals("~"))) {
				tokenizer.skipToken();
				if (tokenizer.peekTokenType().equals("word")) {
					GaiaToken numWord = tokenizer.peekToken();
					String numText = numWord.text;
					int numDigits = 0;
					int numDots = 0;
					for (int i = 0; i < numText.length(); i++) {
						char ch = numText.charAt(i);
						if (Character.isDigit(ch)) {
							numDigits++;
						} else {
							if (ch != '.')
								break;
							numDots++;
						}
					}

					if ((numDigits == numText.length()) || ((numDigits == numText.length() - 1) && (numDots == 1))) {
						tokenizer.skipToken();
					}

				}

			}

			if ((tokenizer.peekTokenType().equals("~")) && (!termModifiers.isWild)) {
				tokenizer.skipToken();

				FuzzyAST fa = null;
				if (isText)
					fa = astGenerator.newFuzzyAST(this, termModifiers);
				else
					fa = astGenerator.newFuzzyStringAST(this, termModifiers);
				ta = fa;

				if (tokenizer.peekTokenType().equals("word")) {
					GaiaToken numWord = tokenizer.peekToken();
					String numText = numWord.text;
					int numDigits = 0;
					int numDots = 0;
					for (int i = 0; i < numText.length(); i++) {
						char ch = numText.charAt(i);
						if (Character.isDigit(ch)) {
							numDigits++;
						} else {
							if (ch != '.')
								break;
							numDots++;
						}
					}

					if ((numDigits == numText.length()) || ((numDigits == numText.length() - 1) && (numDots == 1))) {
						tokenizer.skipToken();

						float minimumSimilarity = Float.valueOf(numText).floatValue();

						fa.minimumSimilarity = minimumSimilarity;
					}

				}

				if (relop)
					if (isText)
						ta = astGenerator.newRelOpAST(this, termModifiers);
					else
						ta = astGenerator.newStringRelOpAST(this, termModifiers);
			} else if (relop) {
				if (isText)
					ta = astGenerator.newRelOpAST(this, termModifiers);
				else
					ta = astGenerator.newStringRelOpAST(this, termModifiers);
			} else if (isText) {
				ta = astGenerator.newTermAST(this, termModifiers);
			} else {
				ta = astGenerator.newStringAST(this, termModifiers);
			}

			ta.haveBoost = haveBoost;
			ta.boost = boost;

			ParserTerm term = new ParserTerm(this, termModifiers, termText);

			ta.setTerm(term);
			ast = ta;
		} else if (type.equals("\"")) {
			ParserTermModifiers phraseModifiers = modifiers.clone();
			phraseModifiers.quoted = true;

			if (isText) {
				tokenizer.skipToken();

				PhraseAST pa = new PhraseAST(this, phraseModifiers);

				int proximitySlop = -1;

				while ((!tokenizer.peekTokenType().equals("\"")) && (!tokenizer.peekTokenType().equals("end"))) {
					GaiaToken t = tokenizer.getToken();
					if (t.type.equals("word")) {
						ParserTermModifiers termModifiers = phraseModifiers.clone();
						termModifiers.isWild = t.isWild;
						termModifiers.isAllStarWild = t.isAllStarWild;
						termModifiers.hasAllStarWildSuffix = t.hasWildStarSuffix;
						ParserTerm term = new ParserTerm(this, termModifiers, t.text);
						ta = astGenerator.newTermAST(this, termModifiers);
						ta.term = term;
						ta.op = "\"";
						pa.addClause(ta);

						if (term.modifiers.hyphenated)
							pa.modifiers.hyphenated = true;
					}
				}
				if (tokenizer.peekTokenType().equals("\"")) {
					tokenizer.skipToken();
				}
				parseTermBoost(pa);

				if (tokenizer.peekTokenType().equals("~")) {
					tokenizer.skipToken();
					if (tokenizer.peekTokenType().equals("word")) {
						GaiaToken numWord = tokenizer.peekToken();
						String numText = numWord.text;
						int numDigits = 0;
						for (int i = 0; i < numText.length(); i++) {
							char ch = numText.charAt(i);
							if (!Character.isDigit(ch))
								break;
							numDigits++;
						}

						if (numDigits == numText.length()) {
							tokenizer.skipToken();

							proximitySlop = Integer.valueOf(numText).intValue();
						}
					}

				}

				if (pa.size() > 0) {
					ast = pa;
					if (proximitySlop == -1)
						pa.proximitySlop(queryPhraseSlop);
					else {
						pa.proximitySlop(proximitySlop);
					}
				}

				if ((relop) && (ast != null)) {
					RelOpAST ra = astGenerator.newRelOpAST(this, modifiers);

					ra.haveBoost = ast.haveBoost;
					ra.boost = ast.boost;

					ra.term = ast.getParserTerm().getTermParserTerm();
					ast = ra;
				}
			} else {
				QuotedStringAST sa = astGenerator.newQuotedStringAST(this, phraseModifiers);
				GaiaToken string = tokenizer.parseQuotedStringToken();
				tokenizer.skipToken();

				sa.term = new ParserTerm(this, modifiers, string.text);

				parseTermBoost(sa);

				if (tokenizer.peekTokenType().equals("~")) {
					tokenizer.skipToken();
					FuzzyAST fa = astGenerator.newFuzzyStringAST(this, modifiers);
					fa.term = sa.term;
					if (sa.haveBoost) {
						fa.haveBoost = true;
						fa.boost = sa.boost;
					}

					if (tokenizer.peekTokenType().equals("word")) {
						GaiaToken numWord = tokenizer.peekToken();
						String numText = numWord.text;
						int numDigits = 0;
						int numDots = 0;
						for (int i = 0; i < numText.length(); i++) {
							char ch = numText.charAt(i);
							if (Character.isDigit(ch)) {
								numDigits++;
							} else {
								if (ch != '.')
									break;
								numDots++;
							}
						}

						if ((numDigits == numText.length()) || ((numDigits == numText.length() - 1) && (numDots == 1))) {
							tokenizer.skipToken();

							float minimumSimilarity = Float.valueOf(numText).floatValue();

							fa.minimumSimilarity = minimumSimilarity;
						}
					}
					ast = fa;
				} else {
					ast = sa;
				}
			}
		} else if (type.equals("(")) {
			ParserTermModifiers savedModifiers = modifiers.clone();
			modifiers = modifiers.clone();
			parenLevel += 1;
			tokenizer.skipToken();

			ast = parseQuery();

			if (tokenizer.peekTokenType().equals(")"))
				tokenizer.skipToken();
			parenLevel -= 1;
			modifiers = savedModifiers;

			if (tokenizer.peekTokenType().equals("~")) {
				tokenizer.skipToken();
				GaiaToken numWord = tokenizer.peekToken();
				String numText = numWord.text;
				int numDigits = 0;
				int numDots = 0;
				int numPercents = 0;
				int n = numText.length();
				for (int i = 0; i < n; i++) {
					char ch = numText.charAt(i);
					if (Character.isDigit(ch)) {
						numDigits++;
					} else if (ch == '.') {
						numDots++;
					} else {
						if (ch != '%')
							break;
						if (i != n - 1)
							break;
						numPercents++;
					}

				}

				if (numDigits == n - (numDots + numPercents)) {
					tokenizer.skipToken();

					if (numPercents > 0) {
						numText = numText.substring(0, n - 1);
					}

					float minMatchRaw = Float.valueOf(numText).floatValue();

					int minMatchCount = 0;
					int minMatchPercent = 0;
					if (numPercents > 0)
						minMatchPercent = (int) minMatchRaw;
					else if ((numDots > 0) && (minMatchRaw <= 1.0F))
						minMatchPercent = (int) (minMatchRaw * 100.0F);
					else if (minMatchRaw <= 19.0F)
						minMatchCount = (int) minMatchRaw;
					else {
						minMatchPercent = (int) minMatchRaw;
					}

					if (minMatchPercent > 100) {
						minMatchPercent = 100;
					}

					modifiers.minMatch = minMatchCount;
					modifiers.minMatchPercent = minMatchPercent;
					ast.modifiers = modifiers;
				}
			}

			parseTermBoost(ast);

			if ((relop) && (ast != null)) {
				RelOpAST ra = astGenerator.newRelOpAST(this, modifiers);

				ra.haveBoost = ast.haveBoost;
				ra.boost = ast.boost;

				ra.term = ast.getParserTerm().getTermParserTerm();
				ast = ra;
			}
		} else if ((type.equals("{")) || (type.equals("["))) {
			tokenizer.skipToken();

			RangeAST ra = astGenerator.newRangeAST(this, modifiers);
			ra.lowerVal = null;
			ra.upperVal = null;
			ra.includeLower = type.equals("[");
			ra.includeUpper = ra.includeLower;

			ParserTermModifiers savedModifiers = modifiers.clone();
			modifiers = modifiers.clone();

			QueryAST lower = null;
			if (!tokenizer.peekTokenText().equalsIgnoreCase("to")) {
				lower = parseTerm();
			}
			if (tokenizer.peekTokenText().equalsIgnoreCase("to")) {
				tokenizer.skipToken();
			}
			QueryAST upper = parseTerm();

			modifiers = savedModifiers;

			String closing = tokenizer.peekTokenText();
			if (closing.equals("]")) {
				ra.includeUpper = true;
				tokenizer.skipToken();
			} else if (closing.equals("}")) {
				ra.includeUpper = false;
				tokenizer.skipToken();
			}

			if (lower != null) {
				QueryAST lowerTerm = lower.getParserTerm();
				if (lowerTerm != null) {
					ra.quotedLowerVal = ((lowerTerm.modifiers.quoted) || ((lowerTerm.getTermVal().equals("*")) && (!lowerTerm.modifiers.isAllStarWild)));
					if (ra.includeLower)
						ra.lowerVal = lower.getMinTermVal();
					else {
						ra.lowerVal = lower.getMaxTermVal();
					}
				}
			}
			if (upper != null) {
				QueryAST upperTerm = upper.getParserTerm();
				if (upperTerm != null) {
					ra.quotedUpperVal = ((upperTerm.modifiers.quoted) || ((upperTerm.getTermVal().equals("*")) && (!upperTerm.modifiers.isAllStarWild)));
					if (ra.includeUpper)
						ra.upperVal = upper.getMaxTermVal();
					else {
						ra.upperVal = upper.getMinTermVal();
					}
				}
			}
			parseTermBoost(ra);

			ast = ra;
		} else if (type.equals(")")) {
			if (parenLevel == 0)
				tokenizer.skipToken();
		} else {
			if ((type.equals("&&")) || (type.equals("||")) || (type.equals("!"))) {
				return ast;
			}
			tokenizer.skipToken();
		}
		parseTermBoost(ast);

		if ((ast != null) && (ast.op == null)) {
			ast.op = op;
		}
		return ast;
	}

	public QueryAST parseTermBoost(QueryAST ast) {
		if (tokenizer.peekTokenType().equals("^")) {
			tokenizer.skipToken();
			if (tokenizer.peekTokenType().equals("word")) {
				GaiaToken numWord = tokenizer.peekToken();
				String numText = numWord.text;
				int numDigits = 0;
				int numDots = 0;
				for (int i = 0; i < numText.length(); i++) {
					char ch = numText.charAt(i);
					if (Character.isDigit(ch)) {
						numDigits++;
					} else {
						if (ch != '.')
							break;
						numDots++;
					}
				}

				if (numDigits + numDots == numText.length()) {
					tokenizer.skipToken();

					if (ast != null) {
						ast.boost = Float.valueOf(numText).floatValue();
						ast.haveBoost = true;
					}
				}
			}
		}

		return ast;
	}

	public float parseNumber() {
		float number = 0.0F;

		if (tokenizer.peekTokenType().equals("word")) {
			GaiaToken numWord = tokenizer.peekToken();
			String numText = numWord.text;
			int numDigits = 0;
			int numDots = 0;
			for (int i = 0; i < numText.length(); i++) {
				char ch = numText.charAt(i);
				if (Character.isDigit(ch)) {
					numDigits++;
				} else {
					if (ch != '.')
						break;
					numDots++;
				}
			}

			if (numDigits + numDots == numText.length()) {
				tokenizer.skipToken();

				number = Float.valueOf(numText).floatValue();
			}
		}

		return number;
	}

	protected boolean parseMinMatch(QueryAST ast, ParserTermModifiers modifiers) {
		if (tokenizer.peekTokenType().equals("~")) {
			tokenizer.skipToken();
			GaiaToken numWord = tokenizer.peekToken();
			String numText = numWord.text;
			int numDigits = 0;
			int numDots = 0;
			int numPercents = 0;
			int n = numText.length();
			for (int i = 0; i < n; i++) {
				char ch = numText.charAt(i);
				if (Character.isDigit(ch)) {
					numDigits++;
				} else if (ch == '.') {
					numDots++;
				} else {
					if (ch != '%')
						break;
					if (i != n - 1)
						break;
					numPercents++;
				}

			}

			if (numDigits == n - (numDots + numPercents)) {
				tokenizer.skipToken();

				if (numPercents > 0) {
					numText = numText.substring(0, n - 1);
				}

				float minMatchRaw = Float.valueOf(numText).floatValue();

				int minMatchCount = 0;
				int minMatchPercent = 0;
				if (numPercents > 0)
					minMatchPercent = (int) minMatchRaw;
				else if ((numDots > 0) && (minMatchRaw <= 1.0F))
					minMatchPercent = (int) (minMatchRaw * 100.0F);
				else if (minMatchRaw <= 19.0F)
					minMatchCount = (int) minMatchRaw;
				else {
					minMatchPercent = (int) minMatchRaw;
				}

				if (minMatchPercent > 100) {
					minMatchPercent = 100;
				}

				modifiers.minMatch = minMatchCount;
				modifiers.minMatchPercent = minMatchPercent;
				ast.modifiers = modifiers;
				return true;
			}
			return false;
		}
		return false;
	}

	Query genAST(Query genQuery, QueryAST ast) {
		return genQuery;
	}

	public Query generateQuery(String query, QueryAST ast) throws IOException {
		Query genQuery = null;

		if (ast != null) {
			genQuery = ast.generateQuery();
		}
		if (logOutput) {
			if (genQuery == null)
				LOG.info("generateQuery generated null Query for query: <<" + query + ">>");
			else
				LOG.info("generateQuery generated Query for query: <<" + query + ">>");
		}
		return genQuery;
	}

	public Query addBooleanClause(Query q, Object c) {
		if ((q == null) || (!(q instanceof BooleanQuery))) {
			BooleanQuery bq = queryGenerator.generateBooleanQuery();
			if (q != null)
				bq.add(q, BooleanClause.Occur.MUST);
			q = bq;
		}

		BooleanQuery bq = (BooleanQuery) q;
		BooleanClause clause = (BooleanClause) c;
		try {
			bq.add(clause);
		} catch (BooleanQuery.TooManyClauses e) {
			LOG.warn("Unable to add clause <<" + clause.getQuery().toString() + ">>: " + e.getMessage());
		}

		return q;
	}

	public Query addBooleanQuery(Query q, Query f, BooleanClause.Occur o) {
		if ((q == null) || (!(q instanceof BooleanQuery))) {
			BooleanQuery bq = queryGenerator.generateBooleanQuery();
			if (q != null)
				bq.add(q, BooleanClause.Occur.MUST);
			q = bq;
		}

		BooleanQuery bq = (BooleanQuery) q;
		try {
			bq.add(f, o);
		} catch (BooleanQuery.TooManyClauses e) {
			LOG.warn("Unable to add clause <<" + f.toString() + ">>: " + e.getMessage());
		}

		return q;
	}

	public Query parseAndGenerateQuery(String query) throws IOException {
		modifiers = new ParserTermModifiers();
		modifiers.expandSynonyms = ((defaultSynonyms) && (expandSynonyms));
		modifiers.field = defaultParserField;

		tokenizer.setup(query);

		QueryAST queryAST = parseQuery();
		if ((queryAST != null) && (logOutput)) {
			queryAST.dump();
		}
		Query genQuery = generateQuery(query, queryAST);

		return genQuery;
	}

	public Query parse(String query) {
		long start = System.currentTimeMillis();

		if (qParser == null) {
			setQParser(null);
		}
		modifiers.expandSynonyms = ((defaultSynonyms) && (expandSynonyms));

		if (query != null) {
			int n = query.length();
			if (n > maxQuery) {
				int i = 0;
				for (i = maxQuery; (i > 0) && (query.charAt(i) != ' '); i--)
					;
				if (i <= 0)
					i = maxQuery - 1;
				query = query.substring(0, i);
				int n1 = query.length();
				LOG.warn("Truncated query of length " + n + " to " + n1 + " chars");
			}

		}

		genTermCount = 0;
		maxGenTermsExceeded = false;

		QueryAST queryAST = parseFullQuery(query);
		if ((queryAST != null) && (logOutput)) {
			queryAST.dump();
		}
		long endParse = System.currentTimeMillis();

		Query genQuery = null;
		try {
			genQuery = generateQuery(query, queryAST);
		} catch (IOException ioe) {
			throw new RuntimeException("Error while generating lucene query: ", ioe);
		}

		long endGenerate = System.currentTimeMillis();

		if (genQuery == null) {
			genQuery = queryGenerator.generateBooleanQuery();
		}

		long endGenerateRelevance = 0L;
		if ((genQuery != null) && (queryAST != null) && ((boostUnigrams) || (boostBigrams) || (boostTrigrams))) {
			Boolean genDirectly = Boolean.valueOf(false);
			Boolean genRel = Boolean.valueOf(true);
			if ((genDirectly.booleanValue()) && (genRel.booleanValue())) {
				try {
					genQuery = queryAST.addRelevance(genQuery);
				} catch (IOException ioe) {
					throw new RuntimeException("error while adding adjacent n-gram boost", ioe);
				}
			} else if (genRel.booleanValue()) {
				Query relQuery = null;
				try {
					relQuery = queryAST.addRelevance(null);
				} catch (IOException ioe) {
					throw new RuntimeException("error while adding relevance boost", ioe);
				}

				endGenerateRelevance = System.currentTimeMillis();

				if (relQuery != null) {
					BooleanQuery combined = null;
					if ((genQuery instanceof BooleanQuery)) {
						BooleanQuery bq = (BooleanQuery) genQuery;
						BooleanClause[] bc = bq.getClauses();
						boolean noShould = true;
						int n = bc.length;
						for (int i = 0; i < n; i++) {
							if ((!bc[i].isRequired()) && (!bc[i].isProhibited())) {
								noShould = false;
								break;
							}
						}
						if (noShould) {
							combined = (BooleanQuery) genQuery;
						}

					}

					if (combined == null) {
						combined = queryAST.generateBooleanQuery();
						combined.add(genQuery, BooleanClause.Occur.MUST);
					}

					if ((relQuery instanceof BooleanQuery)) {
						BooleanQuery bq = (BooleanQuery) relQuery;
						BooleanClause[] bc = bq.getClauses();
						int n = bc.length;
						for (int i = 0; i < n; i++)
							combined.add(bc[i]);
					} else {
						combined.add(relQuery, BooleanClause.Occur.SHOULD);
					}
					genQuery = combined;
				}
			}
		}

		long endRelevance = System.currentTimeMillis();
		if (endGenerateRelevance == 0L) {
			endGenerateRelevance = endRelevance;
		}

		List<Query> boostQueries = null;
		if ((boostParams != null) && (boostParams.length > 0)) {
			boostQueries = new ArrayList<Query>();
			for (String qs : boostParams)
				if (qs.trim().length() != 0) {
					if (logOutput)
						LOG.info("Parse \"bq\" query: <<" + qs + ">>");
					Query q = null;
					try {
						q = parseAndGenerateQuery(qs);
					} catch (IOException ioe) {
						throw new RuntimeException("error while adding bq boost queries", ioe);
					}
					if (q != null)
						boostQueries.add(q);
				}
		}
		if (boostQueries != null) {
			if ((boostQueries.size() == 1) && (boostParams.length == 1)) {
				Query f = (Query) boostQueries.get(0);
				if ((f.getBoost() == 1.0F) && ((f instanceof BooleanQuery))) {
					for (Object c : ((BooleanQuery) f).clauses())
						genQuery = addBooleanClause(genQuery, c);
				} else
					genQuery = addBooleanQuery(genQuery, f, BooleanClause.Occur.SHOULD);
			} else {
				for (Query f : boostQueries) {
					genQuery = addBooleanQuery(genQuery, f, BooleanClause.Occur.SHOULD);
				}
			}

		}

		if ((boostFuncs != null) && (boostFuncs.length > 0)) {
			for (String boostFunc : boostFuncs) {
				if ((null != boostFunc) && (!"".equals(boostFunc))) {
					Map<String, Float> ff = SolrPluginUtils.parseFieldBoosts(boostFunc);
					for (Map.Entry<String, Float> entry : ff.entrySet()) {
						Query fq = null;
						try {
							fq = qParser.subQuery((String) entry.getKey(), FunctionQParserPlugin.NAME).getQuery();
						} catch (Exception e) {
							String msg = e.getMessage();
							String logMsg = "boostFuncs got exception for <<" + (String) entry.getKey() + ">>: " + msg;
							LOG.warn(logMsg, e);
						}
						Float b = (Float) entry.getValue();
						if ((fq != null) && (b != null)) {
							fq.setBoost(b.floatValue());
						}
						if (fq != null) {
							genQuery = addBooleanQuery(genQuery, fq, BooleanClause.Occur.SHOULD);
						}
					}
				}
			}
		}
		Query topQuery = genQuery;
		if ((genQuery != null) && (multBoosts != null) && (multBoosts.length > 0)) {
			List<ValueSource> boosts = new ArrayList<ValueSource>();
			for (String boostStr : multBoosts) {
				if ((boostStr != null) && (boostStr.length() != 0)) {
					Query boost = null;
					try {
						boost = qParser.subQuery(boostStr, FunctionQParserPlugin.NAME).getQuery();
					} catch (Exception e) {
						String msg = e.getMessage();
						String logMsg = "multBoosts got exception for <<" + boostStr + ">>: " + msg;
						LOG.warn(logMsg, e);
					}
					ValueSource vs;
					if ((boost instanceof FunctionQuery))
						vs = ((FunctionQuery) boost).getValueSource();
					else {
						vs = new QueryValueSource(boost, 1.0F);
					}
					boosts.add(vs);
				}
			}
			if (boosts.size() > 1) {
				ValueSource prod = new ProductFloatFunction((ValueSource[]) boosts.toArray(new ValueSource[boosts.size()]));
				topQuery = new BoostedQuery(genQuery, prod);
			} else if (boosts.size() == 1) {
				topQuery = new BoostedQuery(genQuery, (ValueSource) boosts.get(0));
			}

		}

		if (logOutput) {
			if (topQuery == null)
				LOG.info("Generated Query is null");
			else
				LOG.info("Generated Query dumpQueryToString: <<" + GaiaQueryParserUtils.dumpQueryToString(this, topQuery)
						+ ">>");
			GaiaQueryParserUtils.dumpQuery(this, LOG, topQuery);

			long end = System.currentTimeMillis();
			long delta = end - start;
			LOG.info("Parse took " + delta + " ms");
		}

		if ((timeParse) && (logOutput)) {
			long end = System.currentTimeMillis();
			long delta = end - start;
			LOG.info("Full parse took " + delta + " ms");

			long deltaParse = endParse - start;
			LOG.info("Parse alone took " + deltaParse + " ms");

			long deltaGenerate = endGenerate - endParse;
			LOG.info("Generate alone took " + deltaGenerate + " ms");

			long deltaGenerateRelevance = endGenerateRelevance - endGenerate;
			LOG.info("Relevance generation alone took " + deltaGenerateRelevance + " ms");

			long deltaCopyRelevance = endRelevance - endGenerateRelevance;
			LOG.info("Relevance copy alone took " + deltaCopyRelevance + " ms");

			long deltaOther = end - endRelevance;
			LOG.info("Other took " + deltaOther + " ms");
		}

		return topQuery;
	}
}
