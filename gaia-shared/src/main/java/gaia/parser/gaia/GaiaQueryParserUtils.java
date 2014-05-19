package gaia.parser.gaia;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SimpleTimeZone;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.BoostedQuery;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PriorityQueue;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.TrieDateField;
import org.apache.solr.schema.TrieField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.slf4j.Logger;

import gaia.feedback.FeedbackHelper;
import gaia.search.query.SpanFuzzyQuery;

public class GaiaQueryParserUtils {
	public static String hexDigits = "0123456789abcdef";

	public static String collapseWords(String text) {
		StringBuffer word = new StringBuffer();

		int n = text.length();
		for (int i = 0; i < n; i++) {
			char ch = text.charAt(i);
			if (Character.isLetterOrDigit(ch)) {
				word.append(ch);
			}
		}
		return word.toString();
	}

	public static void dumpQuery(GaiaQueryParser parser, Logger log, Query query) {
		dumpQuery(parser, log, 0, query);
	}

	public static void dumpQuery(GaiaQueryParser parser, Logger log, int level, Query query) {
		Class queryClass = null;
		String className = "null";
		if (query != null) {
			queryClass = query.getClass();
			className = queryClass.getSimpleName();
		}

		if (level == 0) {
			if (query == null)
				log.info("Query is null");
			else
				log.info(new StringBuilder().append("Query dumpQueryToString: <<").append(dumpQueryToString(parser, query))
						.append(">>").toString());
		}
		String dots = "";
		for (int i = 0; i <= level; i++)
			dots = new StringBuilder().append(dots).append("..").toString();
		String dotsPlus = new StringBuilder().append(dots).append("..").toString();

		if (query == null) {
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": null").toString());
		} else if ((query instanceof BooleanQuery)) {
			BooleanQuery boolQuery = (BooleanQuery) query;
			BooleanClause[] clauses = boolQuery.getClauses();
			Float boost = Float.valueOf(boolQuery.getBoost());
			int minMatch = boolQuery.getMinimumNumberShouldMatch();
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": boolean - ")
					.append(clauses.length).append(" clauses")
					.append(minMatch > 0 ? new StringBuilder().append(" minMatch: ").append(minMatch).toString() : "")
					.append(" boost: ").append(boost).toString());
			for (int i = 0; i < clauses.length; i++) {
				BooleanClause clause = clauses[i];
				log.info(new StringBuilder().append(dotsPlus).append(" clause[").append(i).append("] ")
						.append(clause.isRequired() ? "IS" : "is NOT").append(" required and ")
						.append(clause.isProhibited() ? "IS" : "is NOT").append(" prohibited").toString());
				dumpQuery(parser, log, level + 1, clause.getQuery());
			}
		} else if ((query instanceof DisjunctionMaxQuery)) {
			DisjunctionMaxQuery dq = (DisjunctionMaxQuery) query;
			Iterator<Query> it = dq.iterator();
			Float boost = Float.valueOf(dq.getBoost());
			String tos = dq.toString();
			int ns = tos.length();
			float tie = 0.0F;
			int is = 0;
			int is1 = 0;
			for (is = ns - 1; is >= 0; is--) {
				if (tos.charAt(is) == '~') {
					for (is1 = is + 1; is1 < ns; is1++) {
						char ch = tos.charAt(is1);
						if (!Character.isDigit(ch))
							if (ch != '.') {
								break;
							}
					}
				}
			}

			if (is > 0) {
				tie = Float.valueOf(tos.substring(is + 1, is1)).floatValue();
			}
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": DisjunctionMaxQuery - boost: ")
					.append(boost).append(" tie: ").append(tie).append("f").toString());
			while (it.hasNext()) {
				Query q = (Query) it.next();
				dumpQuery(parser, log, level + 1, q);
			}
		} else if ((query instanceof FuzzyQuery)) {
			FuzzyQuery fuzzyQuery = (FuzzyQuery) query;
			Term term = fuzzyQuery.getTerm();
			String fieldName = term.field();
			String wordText = escapedUnicodeString(term.text());
			Float boost = Float.valueOf(fuzzyQuery.getBoost());
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": fuzzy - ").append(fieldName)
					.append(":\"").append(escapedUnicodeString(wordText)).append("\" maxEdits: ")
					.append(fuzzyQuery.getMaxEdits()).append(" boost: ").append(boost).toString());
		} else if ((query instanceof SpanFuzzyQuery)) {
			SpanFuzzyQuery fuzzyQuery = (SpanFuzzyQuery) query;
			Term term = fuzzyQuery.getTerm();
			String fieldName = term.field();
			String wordText = escapedUnicodeString(term.text());
			Float boost = Float.valueOf(fuzzyQuery.getBoost());
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": span fuzzy - ").append(fieldName)
					.append(":\"").append(escapedUnicodeString(wordText)).append("\" minSimilarity: ")
					.append(fuzzyQuery.getMinSimilarity()).append(" boost: ").append(boost).toString());
		} else if ((query instanceof MatchAllDocsQuery)) {
			MatchAllDocsQuery allQuery = (MatchAllDocsQuery) query;
			Float boost = Float.valueOf(allQuery.getBoost());
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": MatchAllDocs - boost: ")
					.append(boost).toString());
		} else if ((query instanceof NumericRangeQuery)) {
			NumericRangeQuery q = (NumericRangeQuery) query;
			String fieldName = q.getField();
			Number min = q.getMin();
			Number max = q.getMax();
			String minString = min != null ? min.toString() : null;
			String maxString = max != null ? max.toString() : null;
			String minDateString = "";
			String maxDateString = "";
			boolean isDate = false;
			String typeText = "";
			if (parser != null) {
				ParserField pf = parser.getParserField(fieldName);
				GaiaSchemaFieldType type = pf.schemaField.type;
				isDate = type.isDate;
				if (isDate) {
					if (min != null)
						minDateString = dumpTerm(longDateToString(min.longValue()));
					if (max != null)
						maxDateString = dumpTerm(longDateToString(max.longValue()));
				}
				typeText = dumpTrieTypeText(type);
			}
			String lowerVal = dumpTerm(minString);
			String upperVal = dumpTerm(maxString);
			boolean includesLower = q.includesMin();
			boolean includesUpper = q.includesMax();
			Float boost = Float.valueOf(q.getBoost());
			log.info(new StringBuilder()
					.append(dots)
					.append(" L")
					.append(level)
					.append(": Numeric")
					.append(typeText)
					.append("Range - ")
					.append(fieldName)
					.append(":")
					.append(includesLower ? "[" : "{")
					.append(
							min != null ? new StringBuilder().append(lowerVal)
									.append(isDate ? new StringBuilder().append(" (").append(minDateString).append(")").toString() : "")
									.toString() : "*")
					.append(" TO ")
					.append(
							max != null ? new StringBuilder().append(upperVal)
									.append(isDate ? new StringBuilder().append(" (").append(maxDateString).append(")").toString() : "")
									.toString() : "*").append(includesUpper ? "]" : "}").append(" boost: ").append(boost).toString());
		} else if ((query instanceof PrefixQuery)) {
			PrefixQuery prefixQuery = (PrefixQuery) query;
			Term term = prefixQuery.getPrefix();
			String fieldName = term.field();
			String wordText = escapedUnicodeString(term.text());
			Float boost = Float.valueOf(prefixQuery.getBoost());
			String rmText = dumpMultiTermRewriteMethod(prefixQuery);
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": ").append(rmText)
					.append("Prefix - ").append(fieldName).append(":\"").append(escapedUnicodeString(wordText))
					.append("\" boost: ").append(boost).toString());
		} else if ((query instanceof PhraseQuery)) {
			PhraseQuery phraseQuery = (PhraseQuery) query;
			int proximitySlop = phraseQuery.getSlop();
			Term[] terms = phraseQuery.getTerms();
			Float boost = Float.valueOf(phraseQuery.getBoost());
			String termText = "";
			for (int i = 0; i < terms.length; i++)
				termText = new StringBuilder().append(termText).append(" [").append(i).append("]: ").append(terms[i].field())
						.append(":\"").append(escapedUnicodeString(terms[i].text())).append("\"").toString();
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": phrase - ").append(terms.length)
					.append(" words -").append(termText).append(" proximitySlop: ").append(proximitySlop).append(" boost: ")
					.append(boost).toString());
		} else if ((query instanceof SpanNearQuery)) {
			SpanNearQuery spanQuery = (SpanNearQuery) query;
			int proximitySlop = spanQuery.getSlop();
			boolean inOrder = spanQuery.isInOrder();
			SpanQuery[] clauses = spanQuery.getClauses();
			Float boost = Float.valueOf(spanQuery.getBoost());
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": SpanNear - ")
					.append(clauses.length).append(" clauses proximitySlop: ").append(proximitySlop).append(" InOrder: ")
					.append(inOrder).append(" boost: ").append(boost).toString());
			for (int i = 0; i < clauses.length; i++) {
				SpanQuery clause = clauses[i];
				log.info(new StringBuilder().append(dotsPlus).append(" clause[").append(i).append("] is ")
						.append(clause.getClass().getSimpleName()).toString());
				dumpQuery(parser, log, level + 1, clause);
			}
		} else if ((query instanceof SpanNotQuery)) {
			SpanNotQuery spanQuery = (SpanNotQuery) query;
			Float boost = Float.valueOf(spanQuery.getBoost());
			String fieldName = spanQuery.getField();
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": SpanNot - ").append(" field: ")
					.append(fieldName).append(" boost: ").append(boost).toString());
			SpanQuery include = spanQuery.getInclude();
			log.info(new StringBuilder().append(dotsPlus).append(" Include:").toString());
			dumpQuery(parser, log, level + 1, include);
			SpanQuery exclude = spanQuery.getExclude();
			log.info(new StringBuilder().append(dotsPlus).append(" Exclude:").toString());
			dumpQuery(parser, log, level + 1, exclude);
		} else if ((query instanceof SpanOrQuery)) {
			SpanOrQuery spanQuery = (SpanOrQuery) query;
			SpanQuery[] clauses = spanQuery.getClauses();
			Float boost = Float.valueOf(spanQuery.getBoost());
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": SpanOr - ").append(clauses.length)
					.append(" boost: ").append(boost).toString());
			for (int i = 0; i < clauses.length; i++) {
				SpanQuery clause = clauses[i];
				log.info(new StringBuilder().append(dotsPlus).append(" clause[").append(i).append("] is ")
						.append(clause.getClass().getSimpleName()).toString());
				dumpQuery(parser, log, level + 1, clause);
			}
		} else if ((query instanceof SpanTermQuery)) {
			SpanTermQuery termQuery = (SpanTermQuery) query;
			Term term = termQuery.getTerm();
			String fieldName = term.field();
			String wordText = escapedUnicodeString(term.text());
			Float boost = Float.valueOf(termQuery.getBoost());
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": SpanTerm - ").append(fieldName)
					.append(":\"").append(escapedUnicodeString(wordText)).append("\" boost: ").append(boost).toString());
		} else if ((query instanceof TermRangeQuery)) {
			TermRangeQuery q = (TermRangeQuery) query;
			String fieldName = q.getField();
			BytesRef lowerTerm = q.getLowerTerm();
			String lowerVal = "*";
			if (lowerTerm != null)
				lowerVal = escapedUnicodeString(lowerTerm.utf8ToString());
			BytesRef upperTerm = q.getUpperTerm();
			String upperVal = "*";
			if (upperTerm != null)
				upperVal = escapedUnicodeString(upperTerm.utf8ToString());
			boolean includesLower = q.includesLower();
			boolean includesUpper = q.includesUpper();
			Float boost = Float.valueOf(q.getBoost());
			String rmText = dumpMultiTermRewriteMethod(q);
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": ").append(rmText)
					.append("TermRange - ").append(fieldName).append(":").append(includesLower ? "[" : "{")
					.append(lowerVal != null ? new StringBuilder().append("\"").append(lowerVal).append("\"").toString() : "*")
					.append(" TO ")
					.append(upperVal != null ? new StringBuilder().append("\"").append(upperVal).append("\"").toString() : "*")
					.append(includesUpper ? "]" : "}").append(" boost: ").append(boost).toString());
		} else if ((query instanceof TermQuery)) {
			TermQuery termQuery = (TermQuery) query;
			Term term = termQuery.getTerm();
			String fieldName = term.field();
			String wordText = escapedUnicodeString(term.text());
			Float boost = Float.valueOf(termQuery.getBoost());
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": term - ").append(fieldName)
					.append(":\"").append(escapedUnicodeString(wordText)).append("\" boost: ").append(boost).toString());
		} else if ((query instanceof WildcardQuery)) {
			WildcardQuery wildQuery = (WildcardQuery) query;
			Term term = wildQuery.getTerm();
			String fieldName = term.field();
			String wordText = term.text();
			Float boost = Float.valueOf(wildQuery.getBoost());
			String rmText = dumpMultiTermRewriteMethod(wildQuery);
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": ").append(rmText)
					.append("Wild - ").append(fieldName).append(":\"").append(escapedUnicodeString(wordText))
					.append("\" boost: ").append(boost).toString());
		} else if ((query instanceof BoostedQuery)) {
			BoostedQuery boostedQuery = (BoostedQuery) query;
			Query subQuery = boostedQuery.getQuery();
			ValueSource valueSource = boostedQuery.getValueSource();
			String vsStr = valueSource.toString();
			Float boost = Float.valueOf(boostedQuery.getBoost());
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": boosted - valueSource: \"")
					.append(vsStr).append("\" boost: ").append(boost).toString());
			dumpQuery(parser, log, level + 1, subQuery);
		} else if ((query instanceof FunctionQuery)) {
			FunctionQuery fq = (FunctionQuery) query;
			ValueSource valueSource = fq.getValueSource();
			String vsStr = valueSource.toString();
			Float boost = Float.valueOf(fq.getBoost());
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": function - valueSource: \"")
					.append(vsStr).append("\" boost: ").append(boost).toString());
		} else {
			log.info(new StringBuilder().append(dots).append(" L").append(level).append(": unknown class name: ")
					.append(className).toString());
		}
	}

	public static String escapedChar(char ch) {
		String s = new StringBuilder().append("\\u").append(hexDigits.charAt(ch / '\020' / 16 / 16))
				.append(hexDigits.charAt(ch / '\020' / 16 % 16)).append(hexDigits.charAt(ch / '\020' % 16))
				.append(hexDigits.charAt(ch % '\020')).toString();

		return s;
	}

	public static String escapedUnicodeString(String s) {
		StringBuffer sb = new StringBuffer();

		if (s == null) {
			return null;
		}
		int n = s.length();

		for (int i = 0; i < n; i++) {
			char ch = s.charAt(i);
			int k = ch;
			if ((k < 32) || (k >= 127))
				sb.append(escapedChar(ch));
			else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	public static String dumpUnicodeChar(char ch) {
		String s = new StringBuilder().append("\\u").append(hexDigits.charAt(ch / '\020' / 16 / 16))
				.append(hexDigits.charAt(ch / '\020' / 16 % 16)).append(hexDigits.charAt(ch / '\020' % 16))
				.append(hexDigits.charAt(ch % '\020')).toString();

		return s;
	}

	public static String dumpTerm(Term term) {
		String fieldName = term.field();
		String termText = dumpTerm(term.text());

		return new StringBuilder().append(fieldName).append(":").append(termText).toString();
	}

	public static String dumpTerm(String s) {
		StringBuffer s2 = new StringBuffer();

		if (s == null) {
			return null;
		}
		int n = s.length();

		for (int i = 0; i < n; i++) {
			char ch = s.charAt(i);
			int k = ch;
			if ((k < 32) || (k >= 127)) {
				s2.append(dumpUnicodeChar(ch));
			} else if ((Character.isLetterOrDigit(ch)) || (ch == '-') || (ch == '*') || (ch == '?') || (ch == '.')
					|| (ch == '+') || (ch == '#') || (ch == '/')) {
				s2.append(ch);
			} else if ((ch == ':') && (i > 0) && (Character.isDigit(s.charAt(i - 1))) && (i < n - 2)
					&& (Character.isDigit(s.charAt(i + 1)))) {
				s2.append(ch);
			} else {
				s2.append('\\');
				s2.append(ch);
			}
		}

		return s2.toString();
	}

	public static String dumpBoost(float boost) {
		if (boost != 1.0F) {
			return new StringBuilder().append("^").append(Float.toString(boost)).toString();
		}
		return "";
	}

	public static String dumpQueryToString(GaiaQueryParser parser, Query query) {
		if (query == null) {
			return "";
		}
		if ((query instanceof BooleanQuery)) {
			BooleanQuery q = (BooleanQuery) query;

			StringBuffer s = new StringBuffer();
			s.append("bool([");

			BooleanClause[] clauses = q.getClauses();
			Float boost = Float.valueOf(q.getBoost());

			int mm = q.getMinimumNumberShouldMatch();
			for (int i = 0; i < clauses.length; i++) {
				BooleanClause clause = clauses[i];

				String s2 = dumpQueryToString(parser, clause.getQuery());

				if (i > 0) {
					s.append(' ');
				}
				if (clause.isRequired())
					s.append('+');
				else if (clause.isProhibited()) {
					s.append('-');
				}
				s.append(s2);
			}

			s.append("])");
			if (mm > 0) {
				s.append('~');
				s.append(mm);
			}
			s.append(dumpBoost(boost.floatValue()));

			return s.toString();
		}
		if ((query instanceof BoostedQuery)) {
			BoostedQuery q = (BoostedQuery) query;
			Query subQuery = q.getQuery();
			ValueSource valueSource = q.getValueSource();
			String vsStr = valueSource.toString();
			Float boost = Float.valueOf(q.getBoost());
			String s = dumpQueryToString(parser, subQuery);
			return new StringBuilder().append("boost(").append(s).append(",").append(vsStr).append(")")
					.append(dumpBoost(boost.floatValue())).toString();
		}
		if ((query instanceof DisjunctionMaxQuery)) {
			DisjunctionMaxQuery q = (DisjunctionMaxQuery) query;

			Iterator<Query> it = q.iterator();
			Float boost = Float.valueOf(q.getBoost());

			String tos = q.toString();
			int ns = tos.length();
			float tie = 0.0F;
			int is = 0;
			int is1 = 0;
			for (is = ns - 1; is >= 0; is--) {
				if (tos.charAt(is) == '~') {
					for (is1 = is + 1; is1 < ns; is1++) {
						char ch = tos.charAt(is1);
						if (!Character.isDigit(ch))
							if (ch != '.') {
								break;
							}
					}
				}
			}

			if (is > 0) {
				tie = Float.valueOf(tos.substring(is + 1, is1)).floatValue();
			}

			String s = "";
			while (it.hasNext()) {
				Query q2 = (Query) it.next();
				String s2 = dumpQueryToString(parser, q2);
				if (s.length() > 0)
					s = new StringBuilder().append(s).append(" ").append(s2).toString();
				else {
					s = s2;
				}
			}
			return new StringBuilder().append("dismax([").append(s).append("])")
					.append(tie != 0.0F ? new StringBuilder().append("~").append(tie).toString() : "")
					.append(dumpBoost(boost.floatValue())).toString();
		}
		if ((query instanceof FunctionQuery)) {
			FunctionQuery fq = (FunctionQuery) query;
			ValueSource valueSource = fq.getValueSource();
			String vsStr = valueSource.toString();
			Float boost = Float.valueOf(fq.getBoost());
			return new StringBuilder().append("func(").append(vsStr).append(")").append(dumpBoost(boost.floatValue()))
					.toString();
		}
		if ((query instanceof FuzzyQuery)) {
			FuzzyQuery q = (FuzzyQuery) query;
			Term term = q.getTerm();
			int maxEdits = q.getMaxEdits();
			String termText = dumpTerm(term);
			Float boost = Float.valueOf(q.getBoost());
			return new StringBuilder().append("fuzzy(").append(termText).append(")~").append(maxEdits)
					.append(dumpBoost(boost.floatValue())).toString();
		}
		if ((query instanceof MatchAllDocsQuery)) {
			MatchAllDocsQuery q = (MatchAllDocsQuery) query;
			Float boost = Float.valueOf(q.getBoost());
			return new StringBuilder().append("matchAllDocs()").append(dumpBoost(boost.floatValue())).toString();
		}
		if ((query instanceof NumericRangeQuery)) {
			NumericRangeQuery q = (NumericRangeQuery) query;
			String fieldName = q.getField();
			Number min = q.getMin();
			Number max = q.getMax();
			String minString = min != null ? min.toString() : null;
			String maxString = max != null ? max.toString() : null;
			String typeText = "";
			if (parser != null) {
				ParserField pf = parser.getParserField(fieldName);
				GaiaSchemaFieldType type = pf.schemaField.type;
				if (type.isDate) {
					if (min != null)
						minString = longDateToString(min.longValue());
					if (max != null) {
						maxString = longDateToString(max.longValue());
					}
				}
				typeText = dumpTrieTypeText(type);
			}
			String lowerVal = dumpTerm(minString);
			String upperVal = dumpTerm(maxString);
			boolean includesLower = q.includesMin();
			boolean includesUpper = q.includesMax();
			Float boost = Float.valueOf(q.getBoost());
			return new StringBuilder().append("num").append(typeText).append("Range(").append(fieldName).append(":")
					.append(includesLower ? "[" : "{").append(lowerVal != null ? lowerVal : "*").append(" TO ")
					.append(upperVal != null ? upperVal : "*").append(includesUpper ? "]" : "}").append(")")
					.append(dumpBoost(boost.floatValue())).toString();
		}
		if ((query instanceof PhraseQuery)) {
			PhraseQuery q = (PhraseQuery) query;
			int proximitySlop = q.getSlop();
			Term[] terms = q.getTerms();
			int[] pos = q.getPositions();
			Float boost = Float.valueOf(q.getBoost());

			String fieldName = "";
			int n = terms.length;
			if (n > 0) {
				fieldName = terms[0].field();
			}
			String s = "";
			int prevPos = 0;
			for (int i = 0; i < terms.length; i++) {
				String termText = dumpTerm(terms[i].text());

				int newPos = pos[i];
				for (int j = prevPos + 1; j < newPos; j++) {
					if (s.length() > 0)
						s = new StringBuilder().append(s).append(" ").toString();
					s = new StringBuilder().append(s).append("*").toString();
				}
				prevPos = newPos;

				if (s.length() > 0)
					s = new StringBuilder().append(s).append(" ").append(termText).toString();
				else {
					s = termText;
				}
			}
			return new StringBuilder().append(fieldName).append(":\"").append(s).append("\"")
					.append(proximitySlop == 0 ? "" : new StringBuilder().append("~").append(proximitySlop).toString())
					.append(dumpBoost(boost.floatValue())).toString();
		}
		if ((query instanceof PrefixQuery)) {
			PrefixQuery q = (PrefixQuery) query;
			Term t = q.getPrefix();
			String rmText = dumpMultiTermRewriteMethod(q);
			String prefixDump = dumpTerm(t);
			Float boost = Float.valueOf(q.getBoost());
			return new StringBuilder().append(rmText).append("Prefix(").append(prefixDump).append(")")
					.append(dumpBoost(boost.floatValue())).toString();
		}
		if ((query instanceof SpanFuzzyQuery)) {
			SpanFuzzyQuery q = (SpanFuzzyQuery) query;
			Term term = q.getTerm();
			Float minSim = Float.valueOf(q.getMinSimilarity());
			String termText = dumpTerm(term);
			Float boost = Float.valueOf(q.getBoost());
			return new StringBuilder().append("spanFuzzy(").append(termText).append(")~").append(minSim)
					.append(dumpBoost(boost.floatValue())).toString();
		}
		if ((query instanceof SpanNearQuery)) {
			SpanNearQuery spanQuery = (SpanNearQuery) query;
			int proximitySlop = spanQuery.getSlop();
			boolean inOrder = spanQuery.isInOrder();
			SpanQuery[] clauses = spanQuery.getClauses();
			Float boost = Float.valueOf(spanQuery.getBoost());

			String s = "";
			for (int i = 0; i < clauses.length; i++) {
				SpanQuery clause = clauses[i];
				String s2 = dumpQueryToString(parser, clause);
				if (i > 0)
					s = new StringBuilder().append(s).append(" ").append(s2).toString();
				else {
					s = s2;
				}
			}
			return new StringBuilder().append("spanNear([").append(s).append("], ").append(proximitySlop).append(", ")
					.append(inOrder).append(")").append(dumpBoost(boost.floatValue())).toString();
		}
		if ((query instanceof SpanNotQuery)) {
			SpanNotQuery spanQuery = (SpanNotQuery) query;
			Float boost = Float.valueOf(spanQuery.getBoost());
			String sInclude = dumpQueryToString(parser, spanQuery.getInclude());
			String sExclude = dumpQueryToString(parser, spanQuery.getExclude());
			return new StringBuilder().append("spanNot(").append(sInclude).append(", ").append(sExclude).append(")")
					.append(dumpBoost(boost.floatValue())).toString();
		}
		if ((query instanceof SpanOrQuery)) {
			SpanOrQuery spanQuery = (SpanOrQuery) query;
			SpanQuery[] clauses = spanQuery.getClauses();
			Float boost = Float.valueOf(spanQuery.getBoost());

			String s = "";
			for (int i = 0; i < clauses.length; i++) {
				SpanQuery clause = clauses[i];
				String s2 = dumpQueryToString(parser, clause);
				if (i > 0)
					s = new StringBuilder().append(s).append(" ").append(s2).toString();
				else {
					s = s2;
				}
			}
			return new StringBuilder().append("spanOr([").append(s).append("])").append(dumpBoost(boost.floatValue()))
					.toString();
		}
		if ((query instanceof SpanTermQuery)) {
			SpanTermQuery termQuery = (SpanTermQuery) query;
			Term term = termQuery.getTerm();
			String termText = dumpTerm(term);
			Float boost = Float.valueOf(termQuery.getBoost());
			return new StringBuilder().append("spanTerm(").append(termText).append(")").append(dumpBoost(boost.floatValue()))
					.toString();
		}
		if ((query instanceof TermRangeQuery)) {
			TermRangeQuery q = (TermRangeQuery) query;
			String fieldName = q.getField();
			String lowerVal = dumpTerm(q.getLowerTerm() == null ? null : q.getLowerTerm().utf8ToString());
			String upperVal = dumpTerm(q.getUpperTerm() == null ? null : q.getUpperTerm().utf8ToString());
			boolean includesLower = q.includesLower();
			boolean includesUpper = q.includesUpper();
			Float boost = Float.valueOf(q.getBoost());
			String rmText = dumpMultiTermRewriteMethod(q);
			return new StringBuilder().append(rmText).append("TermRange(").append(fieldName).append(":")
					.append(includesLower ? "[" : "{").append(lowerVal != null ? lowerVal : "*").append(" TO ")
					.append(upperVal != null ? upperVal : "*").append(includesUpper ? "]" : "}").append(")")
					.append(dumpBoost(boost.floatValue())).toString();
		}
		if ((query instanceof TermQuery)) {
			TermQuery q = (TermQuery) query;
			Term term = q.getTerm();
			String termText = dumpTerm(term);
			Float boost = Float.valueOf(q.getBoost());
			return new StringBuilder().append(termText).append(dumpBoost(boost.floatValue())).toString();
		}
		if ((query instanceof WildcardQuery)) {
			WildcardQuery q = (WildcardQuery) query;
			Term term = q.getTerm();
			String termText = dumpTerm(term);
			Float boost = Float.valueOf(q.getBoost());
			String rmText = dumpMultiTermRewriteMethod(q);
			return new StringBuilder().append(rmText).append("Wild(").append(termText).append(")")
					.append(dumpBoost(boost.floatValue())).toString();
		}
		return new StringBuilder().append("??-").append(query.getClass().getName()).append("-??").toString();
	}

	public static String wildcardReversalRangeLowerBoundAdjust(GaiaSchemaFieldType schemaFieldType, String lowerBound) {
		if ((lowerBound == null) && (schemaFieldType.reversedWildcardsIndexed)) {
			return Character.toString((char) (schemaFieldType.reversedWildcardFilterFactory.getMarkerChar() + '\001'));
		}
		return lowerBound;
	}

	public static String longDateToString(long longDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		SimpleTimeZone zone = new SimpleTimeZone(0, "GMT");
		sdf.setTimeZone(zone);

		Date dateDate = new Date(longDate);
		String stringDate = sdf.format(dateDate);
		return stringDate;
	}

	public static String dumpTrieTypeText(GaiaSchemaFieldType type) {
		String typeText = "";
		FieldType ft = type.fieldType;
		if (type.isTrie) {
			if ((ft instanceof TrieDateField)) {
				typeText = "Date";
			} else if ((ft instanceof TrieField)) {
				TrieField tf = (TrieField) ft;
				TrieField.TrieTypes tt = tf.getType();
				switch (tt) {
				case INTEGER:
					typeText = "Int";
					break;
				case LONG:
					typeText = "Long";
					break;
				case FLOAT:
					typeText = "Float";
					break;
				case DOUBLE:
					typeText = "Double";
					break;
				case DATE:
					typeText = "Date";
					break;
				default:
					typeText = tt.toString();
				}
			}
		}

		return typeText;
	}

	public static String dumpMultiTermRewriteMethod(MultiTermQuery q) {
		MultiTermQuery.RewriteMethod rm = q.getRewriteMethod();
		String rmText = "";
		if (rm == MultiTermQuery.CONSTANT_SCORE_AUTO_REWRITE_DEFAULT)
			rmText = "csAuto";
		else if (rm == MultiTermQuery.CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE)
			rmText = "csBool";
		else if (rm == MultiTermQuery.CONSTANT_SCORE_FILTER_REWRITE)
			rmText = "csFilter";
		else if (rm == MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE)
			rmText = "scBool";
		else
			rmText = "?Rewrite?";
		return rmText;
	}

	public static Query collapseSingleBooleanQuery(Query q) {
		if ((q instanceof BooleanQuery)) {
			BooleanQuery bq = (BooleanQuery) q;

			List<BooleanClause> bcs = bq.clauses();
			if ((bcs != null) && (bcs.size() == 1)) {
				return ((BooleanClause) bcs.get(0)).getQuery();
			}
		}
		return q;
	}

	public static Map<String, Float> extractDocumentTerms(SolrCore core, String docId, LikeDocParams likeDocParams) {
		Map<String, Float> termsAndWeights = new HashMap<String, Float>();

		RefCounted<SolrIndexSearcher> rcsis = null;
		SolrIndexSearcher searcher = null;
		rcsis = core.getSearcher();
		int docNum;
		FeedbackHelper helper;
		try {
			searcher = rcsis.get();

			Term term = new Term("id", docId);
			Query query = new TermQuery(term);
			TopDocs topDocs = null;
			try {
				topDocs = searcher.search(query, 1);
			} catch (Exception e) {
				return termsAndWeights;
			}
			ScoreDoc[] scoreDocs = topDocs.scoreDocs;
			int numDocs = scoreDocs.length;
			if (numDocs < 1)
				return termsAndWeights;
			ScoreDoc doc = scoreDocs[0];
			docNum = doc.doc;

			String[] fieldNames = likeDocParams.fl;
			if ((fieldNames != null) && (fieldNames.length > 0))
				helper = new FeedbackHelper(searcher.getIndexReader(), fieldNames);
			else
				helper = new FeedbackHelper(searcher.getIndexReader());
		} finally {
			rcsis.decref();
		}
		helper.setMaxQueryTerms(likeDocParams.maxQueryTermsPerDocument);
		helper.setMinTermFreq(likeDocParams.minTermFreq);
		helper.setMinDocFreq(likeDocParams.minDocFreq);

		helper.setMinWordLen(likeDocParams.minWordLength);
		helper.setMaxWordLen(0);

		helper.setAnalyzer(core.getLatestSchema().getAnalyzer());
		boolean useStopWords = likeDocParams.useNegatives;
		Set<Object> defaultStopwords = new CharArraySet(core.getSolrConfig().luceneMatchVersion,
				StopAnalyzer.ENGLISH_STOP_WORDS_SET, true);
		Set<Object> stopwords = defaultStopwords;
		if ((useStopWords == true) && (stopwords != null)) {
			helper.setStopWords(stopwords);
		}

		try {
			PriorityQueue<Object> pq = helper.retrieveTerms(docNum);

			int lim = helper.getMaxQueryTerms();
			Object cur;
			while (((cur = pq.pop()) != null) && (lim > 0)) {
				Object[] ar = (Object[]) cur;
				String termText = ar[0].toString();

				if ((termText == null) || (termText.length() <= 0) || (termText.charAt(0) != '\001')) {
					float factor = likeDocParams.beta;
					float tf = ((Integer) ar[5]).floatValue();
					float idf = ((Float) ar[3]).floatValue() - 1.0F;

					float adjScore = factor * (tf * Math.max(idf, 1.0E-06F));
					termsAndWeights.put(termText, Float.valueOf(likeDocParams.alpha + adjScore));
					lim--;
				}
			}
		} catch (Exception e) {
			return termsAndWeights;
		}

		return termsAndWeights;
	}

	public static char mapUnicodePunctToAscii(char ch) {
		if ((ch >= '‐') && (ch <= '―'))
			return '-';
		if ((ch >= '“') && (ch <= '‟'))
			return '"';
		if ((ch >= '‘') && (ch <= '‛')) {
			return '\'';
		}
		return ch;
	}

	public static class InsensitiveStringMap<V> extends HashMap<String, V> {
		private static final long serialVersionUID = 0L;

		public V get(String key) {
			return super.get(key);
		}

		public V put(String key, V v) {
			return super.put(key, v);
		}
	}
}
