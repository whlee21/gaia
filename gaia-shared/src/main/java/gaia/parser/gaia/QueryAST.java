package gaia.parser.gaia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.solr.schema.FieldType;

abstract class QueryAST {
	public ParserTermModifiers modifiers = new ParserTermModifiers();

	public GaiaQueryParser parser = null;

	public String clauseOp = null;

	public String op = null;

	public boolean haveBoost = false;
	public float boost = 0.0F;

	private ArrayList<QueryAST> clauses = new ArrayList<QueryAST>();

	public static String[] MonthAbbrevs = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov",
			"Dec" };

	public QueryAST(GaiaQueryParser p, ParserTermModifiers modifiers) {
		parser = p;
		modifiers = modifiers.clone();
	}

	public BooleanQuery generateBooleanQuery() {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateBooleanQuery();
	}

	public DisjunctionMaxQuery generateDisjunctionMaxQuery(float tieBreakerMultiplier) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateDisjunctionMaxQuery(tieBreakerMultiplier);
	}

	public Query generateFuzzyQuery(String fieldName, String termText, float minimumSimilarity) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateFuzzyQuery(fieldName, termText, minimumSimilarity);
	}

	public Query generateMatchAllDocsQuery() {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateMatchAllDocsQuery();
	}

	public PhraseQuery generatePhraseQuery() {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generatePhraseQuery();
	}

	public Query generatePrefixQuery(String fieldName, String prefixText, int cutoff) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generatePrefixQuery(fieldName, prefixText, cutoff);
	}

	public Query generateRangeQuery(GaiaSchemaField field, String lowerVal, String upperVal, boolean includeLower,
			boolean includeUpper) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateRangeQuery(field, lowerVal, upperVal, includeLower, includeUpper);
	}

	public SpanQuery generateSpanFuzzyQuery(String fieldName, String termText, float minimumSimilarity) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateSpanFuzzyQuery(fieldName, termText, minimumSimilarity);
	}

	public SpanNearQuery generateSpanNearQuery(List<SpanQuery> spanQueries) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateSpanNearQuery(spanQueries);
	}

	public SpanNearQuery generateSpanNearQuery(List<SpanQuery> spanQueries, int slop, boolean inOrder) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateSpanNearQuery(spanQueries, slop, inOrder);
	}

	public SpanNotQuery generateSpanNotQuery(SpanQuery include, SpanQuery exclude) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateSpanNotQuery(include, exclude);
	}

	public SpanOrQuery generateSpanOrQuery(List<SpanQuery> spanQueries) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateSpanOrQuery(spanQueries);
	}

	public SpanTermQuery generateSpanTermQuery(GaiaSchemaField field, String term) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateSpanTermQuery(field, term);
	}

	public Term generateTerm(String fieldName, String termText) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateTerm(fieldName, termText);
	}

	public Query generateTermQuery(Term term) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateTermQuery(term);
	}

	public Query generateTermQuery(String fieldName, String termText) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateTermQuery(fieldName, termText);
	}

	public Query generateWildcardQuery(GaiaSchemaField field, String termText, int cutoff) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		return parser.getQueryGenerator().generateWildcardQuery(field, termText, cutoff);
	}

	public String getClauseOp() {
		if (clauseOp == null) {
			return parser.defaultOp();
		}
		return clauseOp;
	}

	public String getOp() {
		if (op == null) {
			return modifiers.defaultOperator.name();
		}
		return op;
	}

	public void addBooleanQueryClause(BooleanQuery bq, Query query, BooleanClause.Occur occur) {
		if ((bq != null) && (query != null))
			try {
				bq.add(query, occur);
			} catch (BooleanQuery.TooManyClauses e) {
				GaiaQueryParser.LOG.warn(new StringBuilder().append(getClass().getSimpleName())
						.append(" unable to add clause <<").append(query.toString()).append(">>: ").append(e.getMessage())
						.toString());
			}
	}

	public void addClause(QueryAST ast) {
		if (ast != null) {
			clauses.add(ast);

			if (ast.modifiers.hyphenated)
				modifiers.hyphenated = true;
		}
	}

	public String checkLeadWild(String text, ParserTermModifiers modifiers) {
		if ((parser.leadWild) || (text == null) || ((modifiers.allFields) && (modifiers.isAllStarWild))) {
			return text;
		}
		while (text.length() > 0) {
			char ch = text.charAt(0);
			if ((ch != '*') && (ch != '?'))
				break;
			text = text.substring(1);
		}
		return text;
	}

	public QueryAST getClause(int i) {
		return (QueryAST) clauses.get(i);
	}

	public String getCleanDateVal() {
		String text = getTermVal();

		int n = text.length();

		String wildSuffix = "";
		while ((n > 1) && ((text.endsWith("*")) || (text.endsWith("?")))) {
			wildSuffix = new StringBuilder().append(text.substring(n - 1, n)).append(wildSuffix).toString();
			text = text.substring(0, --n);
		}

		if ((!text.contains("*")) && (!text.contains("?"))) {
			if (((n == 0 ? 1 : 0) & (wildSuffix.length() > 0 ? 1 : 0)) == 0)
				;
		} else {
			return new StringBuilder().append(text).append(wildSuffix).toString();
		}

		if ((n >= 3) && (text.substring(0, 3).equalsIgnoreCase("now"))) {
			return new StringBuilder().append("NOW").append(text.substring(3)).toString();
		}
		if (text.contains("z")) {
			int i = text.indexOf(122);
			text = new StringBuilder().append(text.substring(0, i)).append("Z").append(text.substring(2)).toString();
		} else if (!text.contains("Z")) {
			if (n == 23) {
				text = new StringBuilder().append(text).append("Z").toString();
				n++;
			} else if (n == 19) {
				text = new StringBuilder().append(text).append("Z").toString();
				n++;
			}

		}

		boolean removedZ = false;
		if ((text.endsWith("Z")) || (text.endsWith("z"))) {
			text = text.substring(0, --n);
			removedZ = true;
		}

		int k = text.indexOf(47);
		if ((k > 0) && (k < 5) && (n > 0) && (Character.isDigit(text.charAt(0)))) {
			String leadDigits = "";
			String middleDigits = "";
			String endDigits = "";
			boolean firstSlash = false;
			boolean secondSlash = false;

			n = text.length();
			int i = 0;
			for (i = 0; i < n; i++) {
				char ch = text.charAt(i);
				if (!Character.isDigit(ch))
					break;
				leadDigits = new StringBuilder().append(leadDigits).append(ch).toString();
			}

			if (i < n) {
				char ch = text.charAt(i);
				firstSlash = ch == '/';
			}
			if (leadDigits.length() == 1)
				leadDigits = new StringBuilder().append("0").append(leadDigits).toString();
			if (firstSlash) {
				for (i++; i < n; i++) {
					char ch = text.charAt(i);
					if (!Character.isDigit(ch))
						break;
					middleDigits = new StringBuilder().append(middleDigits).append(ch).toString();
				}

				if (middleDigits.length() == 1)
					middleDigits = new StringBuilder().append("0").append(middleDigits).toString();
				if (i < n) {
					char ch = text.charAt(i);
					secondSlash = ch == '/';
				}
				if (secondSlash) {
					for (i++; i < n; i++) {
						char ch = text.charAt(i);
						if (!Character.isDigit(ch))
							break;
						endDigits = new StringBuilder().append(endDigits).append(ch).toString();
					}

				}

				if (endDigits.length() == 1)
					endDigits = new StringBuilder().append("0").append(endDigits).toString();
				String suffix = "";
				if (i < n)
					suffix = text.substring(i);
				if (secondSlash) {
					if (endDigits.length() == 2) {
						if (endDigits.charAt(0) >= '5')
							endDigits = new StringBuilder().append("19").append(endDigits).toString();
						else
							endDigits = new StringBuilder().append("20").append(endDigits).toString();
					}
					text = new StringBuilder().append(endDigits).append("-").append(leadDigits).append("-").append(middleDigits)
							.append(suffix).toString();
				} else if (firstSlash) {
					if (middleDigits.length() == 2) {
						if (middleDigits.charAt(0) >= '5')
							middleDigits = new StringBuilder().append("19").append(middleDigits).toString();
						else
							middleDigits = new StringBuilder().append("20").append(middleDigits).toString();
					}
					text = new StringBuilder().append(middleDigits).append("-").append(leadDigits).append(suffix).toString();
				}
				n = text.length();
			}

		}

		k = text.indexOf(45);
		if (((k == 1) || (k == 2)) && (n > k + 1) && (Character.isDigit(text.charAt(0)))
				&& (Character.isDigit(text.charAt(k + 1)))) {
			String leadDigits = "";
			String middleDigits = "";
			String endDigits = "";
			boolean firstDash = false;
			boolean secondDash = false;

			n = text.length();
			int i = 0;
			for (i = 0; i < n; i++) {
				char ch = text.charAt(i);
				if (!Character.isDigit(ch))
					break;
				leadDigits = new StringBuilder().append(leadDigits).append(ch).toString();
			}

			if (i < n) {
				char ch = text.charAt(i);
				firstDash = ch == '-';
			}
			if (leadDigits.length() == 1)
				leadDigits = new StringBuilder().append("0").append(leadDigits).toString();
			if (firstDash) {
				for (i++; i < n; i++) {
					char ch = text.charAt(i);
					if (!Character.isDigit(ch))
						break;
					middleDigits = new StringBuilder().append(middleDigits).append(ch).toString();
				}

				if (middleDigits.length() == 1)
					middleDigits = new StringBuilder().append("0").append(middleDigits).toString();
				if (i < n) {
					char ch = text.charAt(i);
					secondDash = ch == '-';
				}
				if (secondDash) {
					for (i++; i < n; i++) {
						char ch = text.charAt(i);
						if (!Character.isDigit(ch))
							break;
						endDigits = new StringBuilder().append(endDigits).append(ch).toString();
					}

				}

				if (endDigits.length() == 1)
					endDigits = new StringBuilder().append("0").append(endDigits).toString();
				String suffix = "";
				if (i < n)
					suffix = text.substring(i);
				if (secondDash) {
					if (endDigits.length() == 2) {
						if (endDigits.charAt(0) >= '5')
							endDigits = new StringBuilder().append("19").append(endDigits).toString();
						else
							endDigits = new StringBuilder().append("20").append(endDigits).toString();
					}
					text = new StringBuilder().append(endDigits).append("-").append(leadDigits).append("-").append(middleDigits)
							.append(suffix).toString();
				} else if (firstDash) {
					if (middleDigits.length() == 2) {
						if (middleDigits.charAt(0) >= '5')
							middleDigits = new StringBuilder().append("19").append(middleDigits).toString();
						else
							middleDigits = new StringBuilder().append("20").append(middleDigits).toString();
					}
					text = new StringBuilder().append(middleDigits).append("-").append(leadDigits).append(suffix).toString();
				}
				n = text.length();
			}

		}

		boolean haveYear = (n >= 4) && (Character.isDigit(text.charAt(0))) && (Character.isDigit(text.charAt(1)))
				&& (Character.isDigit(text.charAt(2))) && (Character.isDigit(text.charAt(3)));

		k = text.indexOf("-");
		if (((k == 1) || (k == 2)) && (n > k + 3) && (Character.isDigit(text.charAt(0)))
				&& (!Character.isDigit(text.charAt(k + 1)))) {
			String day = "";
			if (k == 1)
				day = new StringBuilder().append("0").append(text.substring(0, 1)).toString();
			else
				day = text.substring(0, 2);
			if (text.charAt(k + 4) == '-') {
				String month = text.substring(k + 1, k + 4);
				int m = 0;
				while ((m < 12) && (!MonthAbbrevs[m].equalsIgnoreCase(month))) {
					m++;
				}

				if (m < 12) {
					String year = "";
					int i;
					for (i = k + 5; i < n; i++) {
						char ch = text.charAt(i);
						if (!Character.isDigit(ch))
							break;
						year = new StringBuilder().append(year).append(ch).toString();
					}

					String suffix = text.substring(i);
					int ny = year.length();
					if (ny == 1)
						year = new StringBuilder().append("200").append(year).toString();
					else if (ny == 2) {
						if (year.charAt(0) >= '5')
							year = new StringBuilder().append("19").append(year).toString();
						else
							year = new StringBuilder().append("20").append(year).toString();
					} else if (ny == 3)
						year = new StringBuilder().append("2").append(year).toString();
					else if (ny > 4)
						year = year.substring(ny - 4);
					text = new StringBuilder()
							.append(year)
							.append("-")
							.append(
									m < 9 ? new StringBuilder().append("0").append((char) (49 + m)).toString() : new StringBuilder()
											.append("1").append((char) (48 + m % 9)).toString()).append("-").append(day).append(suffix)
							.toString();

					n = text.length();
				}
			}
		} else if ((k == 3) && (n > k + 1)) {
			String month = text.substring(0, 3);
			int m = 0;
			while ((m < 12) && (!MonthAbbrevs[m].equalsIgnoreCase(month))) {
				m++;
			}

			if (m < 12) {
				String year = "";
				int i;
				for (i = k + 1; i < n; i++) {
					char ch = text.charAt(i);
					if (!Character.isDigit(ch))
						break;
					year = new StringBuilder().append(year).append(ch).toString();
				}

				String suffix = text.substring(i);
				int ny = year.length();
				if (ny == 1)
					year = new StringBuilder().append("200").append(year).toString();
				else if (ny == 2) {
					if (year.charAt(0) >= '5')
						year = new StringBuilder().append("19").append(year).toString();
					else
						year = new StringBuilder().append("20").append(year).toString();
				} else if (ny == 3)
					year = new StringBuilder().append("2").append(year).toString();
				else if (ny > 4)
					year = year.substring(ny - 4);
				text = new StringBuilder()
						.append(year)
						.append("-")
						.append(
								m < 9 ? new StringBuilder().append("0").append((char) (49 + m)).toString() : new StringBuilder()
										.append("1").append((char) (48 + m % 9)).toString()).append(suffix).toString();

				n = text.length();
			}

		}

		if ((n == 8) && (!text.contains("-")) && (haveYear)) {
			text = new StringBuilder().append(text.substring(0, 4)).append("-").append(text.substring(4, 6)).append("-")
					.append(text.substring(6, 8)).toString();

			n = text.length();
		}

		if ((n == 6) && (!text.contains("-")) && (haveYear)) {
			text = new StringBuilder().append(text.substring(0, 4)).append("-").append(text.substring(4, 6)).toString();
			n = text.length();
		}

		if (((n == 6) && (text.charAt(4) == '-'))
				|| ((n > 6) && (text.charAt(4) == '-') && (!Character.isDigit(text.charAt(6))) && (haveYear))) {
			text = new StringBuilder().append(text.substring(0, 5)).append("0").append(text.substring(5)).toString();
			n = text.length();
		}

		if (((n == 9) && (text.charAt(7) == '-'))
				|| ((n > 9) && (text.charAt(7) == '-') && (!Character.isDigit(text.charAt(9))) && (haveYear))) {
			text = new StringBuilder().append(text.substring(0, 8)).append("0").append(text.substring(8)).toString();
			n = text.length();
		}

		if ((n > 10) && (text.charAt(10) == 't')) {
			text = new StringBuilder().append(text.substring(0, 10)).append("T").append(text.substring(11, n)).toString();
		}

		if (removedZ) {
			text = new StringBuilder().append(text).append("Z").toString();
			n = text.length();
		}

		if ((n > 19) && (text.charAt(10) == 'T') && (text.charAt(19) == ',')) {
			text = new StringBuilder().append(text.substring(0, 19)).append(".").append(text.substring(20, n)).toString();
		}
		return text;
	}

	public ParserField getField() {
		return null;
	}

	public String getFieldName() {
		return null;
	}

	public String getMinTermVal() {
		return getMinTermVal(getField());
	}

	public String getMinTermVal(ParserField field) {
		if ((field == null) || (!field.isDate())) {
			return getTermVal();
		}
		String text = getCleanDateVal();
		if (text == null) {
			return null;
		}
		int n = text.length();

		if ((n >= 3) && (text.substring(0, 3).equalsIgnoreCase("now"))) {
			return new StringBuilder().append("NOW").append(text.substring(3)).toString();
		}
		if (n >= 19) {
			if (text.endsWith("Z")) {
				return text;
			}
			return new StringBuilder().append(text).append("Z").toString();
		}

		String fullMinDate = "0000-01-01T00:00:00.000Z";

		if (text.endsWith("Z")) {
			text = text.substring(0, text.length() - 1);
		}

		n = text.length();
		String lowerVal = text;
		if ((n >= 1) && (Character.isDigit(text.charAt(0)))) {
			lowerVal = new StringBuilder().append(text).append(fullMinDate.substring(n)).toString();
		}
		return lowerVal;
	}

	public String getMaxTermVal() {
		return getMaxTermVal(getField());
	}

	public String getMaxTermVal(ParserField field) {
		if ((field == null) || (!field.isDate())) {
			return getTermVal();
		}
		String text = getCleanDateVal();
		if (text == null) {
			return null;
		}
		int n = text.length();

		if ((n >= 3) && (text.substring(0, 3).equalsIgnoreCase("now"))) {
			return new StringBuilder().append("NOW").append(text.substring(3)).toString();
		}
		if (n >= 19) {
			if (text.endsWith("Z")) {
				return text;
			}
			return new StringBuilder().append(text).append("Z").toString();
		}

		if (text.endsWith("Z")) {
			text = text.substring(0, text.length() - 1);
		}
		String fullMaxDate = "9999-12-31T23:59:59.999Z";
		String fullMaxDate30 = "9999-12-30T23:59:59.999Z";
		String fullMaxDate29 = "9999-12-29T23:59:59.999Z";
		String fullMaxDate28 = "9999-12-28T23:59:59.999Z";

		n = text.length();
		String upperVal = text;
		if ((n >= 1) && (Character.isDigit(text.charAt(0)))) {
			if ((n >= 7) && (n <= 10)) {
				String mm = text.substring(5, 7);
				String maxDate = fullMaxDate;
				if ((mm.equals("04")) || (mm.equals("06")) || (mm.equals("09")) || (mm.equals("11"))) {
					maxDate = fullMaxDate30;
				} else if (mm.equals("02")) {
					int year = Integer.parseInt(text.substring(0, 4));
					if ((year % 400 == 0) || ((year % 100 != 0) && (year % 4 == 0)))
						maxDate = fullMaxDate29;
					else
						maxDate = fullMaxDate28;
				}
				upperVal = new StringBuilder().append(text).append(maxDate.substring(n)).toString();
			} else {
				upperVal = new StringBuilder().append(text).append(fullMaxDate.substring(n)).toString();
			}
		}
		return upperVal;
	}

	public QueryAST getParserTerm() {
		if ((size() > 0) && (!isNegativeOp())) {
			return getClause(0).getParserTerm();
		}
		return null;
	}

	public QueryAST getLastParserTerm() {
		int n = size();
		if (n > 0) {
			return getClause(n - 1).getLastParserTerm();
		}
		return null;
	}

	public ParserTerm getTermParserTerm() {
		if (size() > 0) {
			return getClause(0).getTermParserTerm();
		}
		return null;
	}

	public Term getTerm() throws IOException {
		return null;
	}

	public TermAST getTermClause(int i) {
		QueryAST q = (QueryAST) clauses.get(i);

		if ((q instanceof TermAST)) {
			return (TermAST) q;
		}
		return null;
	}

	public String getTermVal() {
		if (size() > 0) {
			return ((QueryAST) clauses.get(0)).getTermVal();
		}
		return null;
	}

	public String getFieldTypeName(String fieldName) {
		FieldType fieldType = parser.getSchema().getFieldTypeNoEx(fieldName);
		if (fieldType == null) {
			return null;
		}
		return fieldType.getTypeName();
	}

	public boolean isDateField(String fieldName) {
		String type = getFieldTypeName(fieldName);
		if (type == null) {
			return false;
		}
		return type.equals("date");
	}

	public boolean isRelOp(String op) {
		return (op != null)
				&& ((op.equals("==")) || (op.equals("!=")) || (op.equals("<")) || (op.equals("<=")) || (op.equals(">")) || (op
						.equals(">=")));
	}

	public int size() {
		return clauses.size();
	}

	public boolean isNegativeOp() {
		if ((op != null)
				&& (("-".equals(op)) || (("!".equals(op)) && (size() <= 1)) || ("&&!".equals(op)) || ("||!".equals(op))
						|| ("!=".equals(op)) || ("<".equals(op)) || (">".equals(op)))) {
			return true;
		}
		if ((clauseOp != null)
				&& (("-".equals(clauseOp)) || ("!".equals(clauseOp)) || ("&&!".equals(clauseOp)) || ("||!".equals(clauseOp))
						|| ("!=".equals(clauseOp)) || ("<".equals(clauseOp)) || (">".equals(clauseOp)))) {
			return true;
		}
		if ((op == null) && (clauseOp == null) && (clauses.size() > 0)) {
			return getClause(0).isNegativeOp();
		}
		return false;
	}

	public void dump() {
		dump(0);
	}

	public void dump(int level) {
		Class queryClass = null;
		String className = "null";
		queryClass = getClass();
		className = queryClass.getSimpleName();

		String dots = "";
		for (int i = 0; i <= level; i++) {
			dots = new StringBuilder().append(dots).append("..").toString();
		}
		String extra = "";
		if ((className.equals("PhraseAST")) || (className.equals("NearAST"))) {
			PhraseAST pa = (PhraseAST) this;
			extra = new StringBuilder().append(" proximitySlop: ").append(pa.proximitySlop).toString();
		} else if (className.equals("RelOpAST")) {
			extra = new StringBuilder().append(" relational op: ").append(op).toString();
		}
		if (!modifiers.expandSynonyms)
			extra = new StringBuilder().append(extra).append(" nosyn:").toString();
		if (!modifiers.stemWords) {
			extra = new StringBuilder().append(extra).append(" nostem:").toString();
		}
		if ((className.equals("QueryAST")) || (className.equals("TermListAST"))) {
			if (modifiers.minMatch > 0)
				extra = new StringBuilder().append(extra).append(" minMatch:").append(modifiers.minMatch).toString();
			else if (modifiers.minMatchPercent > 0) {
				extra = new StringBuilder().append(extra).append(" minMatch:").append(modifiers.minMatchPercent).append("%")
						.toString();
			}
		}
		String prefix = new StringBuilder().append(dots).append(" AST L").append(level).append(": ").append(className)
				.append(" ").append(size()).append(" clauses clauseOp: ").append(clauseOp).append(" op: ").append(op)
				.append(haveBoost ? new StringBuilder().append(" boost: ").append(boost).toString() : "").append(extra)
				.toString();

		String suffix = "";
		if (className.equals("TermAST")) {
			TermAST ta = (TermAST) this;
			suffix = new StringBuilder().append(" term: <<").append(ta.toString()).append(">>").toString();
		} else if (className.equals("StringAST")) {
			TermAST ta = (TermAST) this;
			suffix = new StringBuilder().append(" term: <<").append(ta.toString()).append(">>").toString();
		} else if (className.equals("QuotedStringAST")) {
			TermAST ta = (TermAST) this;
			suffix = new StringBuilder().append(" term: <<").append(ta.toString()).append(">>").toString();
		} else if (className.equals("FuzzyAST")) {
			FuzzyAST ta = (FuzzyAST) this;
			suffix = new StringBuilder().append(" fuzzy: <<").append(ta.toString()).append(">> minimumSimilarity: ")
					.append(ta.minimumSimilarity).toString();
		} else if (className.equals("FuzzyStringAST")) {
			FuzzyStringAST ta = (FuzzyStringAST) this;
			suffix = new StringBuilder().append(" fuzzy: <<").append(ta.toString()).append(">> minimumSimilarity: ")
					.append(ta.minimumSimilarity).toString();
		} else if (className.equals("RelOpAST")) {
			RelOpAST ta = (RelOpAST) this;
			suffix = new StringBuilder().append(" relop term: <<").append(ta.toString()).append(">> relational operator: ")
					.append(op).toString();
		}

		GaiaQueryParser.LOG.info(new StringBuilder().append(prefix).append(suffix).toString());

		for (int i = 0; i < size(); i++)
			getClause(i).dump(level + 1);
	}

	public String collapseWords(String text) {
		String word = "";

		int n = text.length();
		for (int i = 0; i < n; i++) {
			char ch = text.charAt(i);
			if (Character.isLetterOrDigit(ch)) {
				word = new StringBuilder().append(word).append(ch).toString();
			}
		}
		return word;
	}

	public List<String> getWords() throws IOException {
		return null;
	}

	public List<String> getActualWords() throws IOException {
		return null;
	}

	public boolean isStopWord() throws IOException {
		return false;
	}

	public boolean isWild() {
		return false;
	}

	public Query generateQuery(boolean allowStopWords) throws IOException {
		return generateQuery();
	}

	public Query generateQuery() throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		int n = size();
		Query query = null;

		int numOptionalClauses = 0;

		if ((n == 1) && (getClause(0).op == null)) {
			query = getClause(0).generateQuery();
		} else if (n > 0) {
			BooleanQuery bq = generateBooleanQuery();
			int numClauses = 0;
			boolean allNegative = true;

			for (int i = 0; i < n; i++) {
				QueryAST cl = getClause(i);

				String op = cl.getOp();
				BooleanClause.Occur occur = null;
				if ((op.equals("OR")) || (op.equals("||"))) {
					occur = BooleanClause.Occur.SHOULD;
					allNegative = false;
				} else if ((op.equals("+")) || (op.equals("AND")) || (op.equals("&&"))) {
					occur = BooleanClause.Occur.MUST;
					allNegative = false;
				} else if ((op.equals("-")) || (op.equals("&&!")) || (op.equals("!"))) {
					if ((cl.size() == 1) || (i > 0))
						occur = BooleanClause.Occur.MUST_NOT;
					else
						occur = BooleanClause.Occur.MUST;
				} else if (op.equals("||!")) {
					occur = BooleanClause.Occur.SHOULD;
					allNegative = false;
				}

				Query q = cl.generateQuery();

				numClauses++;

				if (q != null) {
					if (numClauses > parser.maxGenTerms) {
						GaiaQueryParser.LOG.warn(new StringBuilder().append("Ignoring term <<").append(cl.getTermVal())
								.append(">> since more than ").append(parser.maxGenTerms).append(" terms in query").toString());
					} else {
						addBooleanQueryClause(bq, q, occur);

						if (occur == BooleanClause.Occur.SHOULD) {
							numOptionalClauses++;
						}
					}
				}
			}
			if (allNegative) {
				addBooleanQueryClause(bq, generateMatchAllDocsQuery(), BooleanClause.Occur.MUST);
			}

			if ((bq != null) && (haveBoost)) {
				bq.setBoost(boost);
			}

			int minMatchPercent = modifiers.minMatchPercent;
			int minMatch = modifiers.minMatch;
			if (bq != null) {
				if (minMatch == 0) {
					minMatch = numOptionalClauses * minMatchPercent / 100;

					if ((minMatch == 0) && (minMatchPercent > 0))
						minMatch = 1;
				} else if (minMatch > numOptionalClauses) {
					minMatch = numOptionalClauses;
				}
				bq.setMinimumNumberShouldMatch(minMatch);
			}

			query = bq;
		}

		return query;
	}

	public SpanQuery generateSpanQuery(ParserField field) throws IOException {
		ArrayList<SpanQuery> spanTerms = new ArrayList<SpanQuery>();

		int n = size();
		for (int i = 0; i < n; i++) {
			QueryAST clauseAST = getClause(i);
			if (!clauseAST.isNegativeOp()) {
				SpanQuery sq = clauseAST.generateSpanQuery(field);
				if (sq != null) {
					spanTerms.add(sq);
				}
			}
		}

		int numSpanTerms = spanTerms.size();
		if (numSpanTerms == 0) {
			return null;
		}

		if (numSpanTerms == 1) {
			return (SpanQuery) spanTerms.get(0);
		}
		SpanOrQuery sor = generateSpanOrQuery(spanTerms);

		return sor;
	}

	public Query generateTermQuery(ParserTerm term, String text) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		ParserTermModifiers modifiers = term.modifiers;
		ParserField field = modifiers.field;

		Boolean needDismax = Boolean.valueOf((field.isDefault) || (field.isAll));
		if ((field.isDefault) && (parser.queryFields.size() == 1)) {
			needDismax = Boolean.valueOf(false);
			field = (ParserField) parser.queryFields.get(0);
		}
		if ((field.isAll) && (parser.allFields.size() == 1)) {
			needDismax = Boolean.valueOf(false);
			field = (ParserField) parser.allFields.get(0);
		}

		if (needDismax.booleanValue()) {
			Query firstQ = null;
			DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

			ArrayList qf = field.isDefault ? parser.queryFields : parser.allFields;

			int n = qf.size();
			int k = 0;
			for (int i = 0; i < n; i++) {
				ParserField field1 = (ParserField) qf.get(i);

				if ((!modifiers.synonymExpansion) || (field1.expandSynonyms)) {
					if (parser.countGenTerms()) {
						break;
					}
					Query q = null;
					if (field1.isDate()) {
						String dateText = getCleanDateVal();
						boolean isWild = (dateText.contains("*")) || (dateText.contains("?"));

						boolean doExactDateMatch = false;
						if (dateText.length() >= 19)
							doExactDateMatch = true;
						else if ((dateText.length() >= 3) && (dateText.substring(0, 3).equalsIgnoreCase("now"))) {
							doExactDateMatch = true;
						}
						boolean allowWildcardInDate = true;

						dateText = checkLeadWild(dateText, modifiers);

						if ((allowWildcardInDate) && (isWild)) {
							String prefix = dateText.substring(0, dateText.length() - 1);

							boolean useMatchAll = false;

							if (term.modifiers.isAllStarWild) {
								if ((useMatchAll) || (modifiers.allFields))
									q = generateMatchAllDocsQuery();
								else
									q = generatePrefixQuery(field1.fieldName(), "", term.modifiers.termCountHardCutoff);
							} else if (term.modifiers.hasAllStarWildSuffix)
								q = generatePrefixQuery(field1.fieldName(), prefix, term.modifiers.termCountHardCutoff);
							else if (dateText.length() > 0)
								q = generateWildcardQuery(field1.schemaField, dateText, term.modifiers.termCountHardCutoff);
						} else if (doExactDateMatch) {
							String originalText = dateText;
							ParserTermModifiers newModifiers = modifiers.clone();
							newModifiers.field = field1;
							ParserTerm newTerm = new ParserTerm(parser, newModifiers, dateText);

							newTerm = newTerm.filterTerm(true, true, false, false);
							if (newTerm == null)
								return null;
							dateText = newTerm.text;

							if ((dateText.length() == 19) && (originalText.length() >= 3)
									&& (originalText.substring(0, 3).equalsIgnoreCase("NOW"))) {
								dateText = new StringBuilder().append(dateText).append(".000").toString();
							} else if (dateText.length() == 21)
								dateText = new StringBuilder().append(dateText).append("00").toString();
							else if (dateText.length() == 22)
								dateText = new StringBuilder().append(dateText).append("0").toString();
							q = generateTermQuery(field1.fieldName(), dateText);
						} else {
							String lowerVal = getMinTermVal(field1);
							String upperVal = getMaxTermVal(field1);
							boolean includeLower = true;
							boolean includeUpper = true;

							String originalLowerVal = lowerVal;
							ParserTermModifiers newModifiers = modifiers.clone();
							newModifiers.field = field1;
							ParserTerm newTerm = new ParserTerm(parser, newModifiers, lowerVal);

							newTerm = newTerm.filterTerm(true, true, false, false);
							if (newTerm == null)
								continue;
							lowerVal = newTerm.text;

							if ((originalLowerVal.length() >= 3) && (originalLowerVal.substring(0, 3).equalsIgnoreCase("NOW"))) {
								if (lowerVal.length() == 19)
									lowerVal = new StringBuilder().append(lowerVal).append(".000").toString();
								else if (lowerVal.length() == 21)
									lowerVal = new StringBuilder().append(lowerVal).append("00").toString();
								else if (lowerVal.length() == 22) {
									lowerVal = new StringBuilder().append(lowerVal).append("0").toString();
								}
							}
							String originalUpperVal = upperVal;
							newTerm = new ParserTerm(parser, newModifiers, upperVal);
							newTerm = newTerm.filterTerm(true, true, false, false);
							if (newTerm == null)
								continue;
							upperVal = newTerm.text;

							if ((originalUpperVal.length() >= 3) && (originalUpperVal.substring(0, 3).equalsIgnoreCase("NOW"))) {
								if (upperVal.length() == 19)
									upperVal = new StringBuilder().append(upperVal).append(".000").toString();
								else if (upperVal.length() == 21)
									upperVal = new StringBuilder().append(upperVal).append("00").toString();
								else if (upperVal.length() == 22) {
									upperVal = new StringBuilder().append(upperVal).append("0").toString();
								}
							}
							q = generateRangeQuery(field1.schemaField, lowerVal, upperVal, includeLower, includeUpper);
						}
					} else {
						q = generateTermQuery(field1.fieldName(), text);
					}
					q.setBoost(field.isDefault ? field1.queryBoost : field1.allQueryBoost);

					bq.add(q);
					if (k == 0)
						firstQ = q;
					k++;
				}
			}
			if (k == 0)
				return null;
			if (k == 1) {
				return firstQ;
			}
			return bq;
		}
		if ((!modifiers.synonymExpansion) || (field.expandSynonyms)) {
			if (parser.countGenTerms())
				return null;
			return generateTermQuery(field.fieldName(), text);
		}
		return null;
	}

	public Query generatePrefixQuery(ParserField field, String prefix, int cutoff) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		if ((field.isDefault) || (field.isAll)) {
			Query firstQ = null;
			DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

			ArrayList qf = field.isDefault ? parser.queryFields : parser.allFields;

			int n = qf.size();
			int k = 0;
			for (int i = 0; i < n; i++) {
				ParserField field1 = (ParserField) qf.get(i);

				if ((!modifiers.synonymExpansion) || (field1.expandSynonyms)) {
					if (parser.countGenTerms()) {
						break;
					}
					Query tq = generatePrefixQuery(field1.fieldName(), prefix, cutoff);
					tq.setBoost(field.isDefault ? field1.queryBoost : field1.allQueryBoost);

					bq.add(tq);
					if (k == 0)
						firstQ = tq;
					k++;
				}
			}
			if (k == 0)
				return null;
			if (k == 1) {
				return firstQ;
			}
			return bq;
		}
		if ((!modifiers.synonymExpansion) || (field.expandSynonyms)) {
			if (parser.countGenTerms())
				return null;
			Query q = generatePrefixQuery(field.fieldName(), prefix, cutoff);
			return q;
		}
		return null;
	}

	public Query generateRangeQuery(ParserField field, ParserTermModifiers modifiers, String lowerBound,
			String upperBound, boolean quotedLowerBound, boolean quotedUpperBound, boolean includeLower, boolean includeUpper) {
		return parser.getQueryGenerator().generateRangeQuery(field.schemaField, modifiers, lowerBound, upperBound,
				quotedLowerBound, quotedUpperBound, includeLower, includeUpper);
	}

	public Query generateWildcardQuery(ParserField field, String text, int cutoff) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		if ((field.isDefault) || (field.isAll)) {
			Query firstQ = null;
			DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

			ArrayList qf = field.isDefault ? parser.queryFields : parser.allFields;

			int n = qf.size();
			int k = 0;
			for (int i = 0; i < n; i++) {
				ParserField field1 = (ParserField) qf.get(i);

				if ((!modifiers.synonymExpansion) || (field1.expandSynonyms)) {
					Query tq = generateWildcardQuery(field1.schemaField, text, cutoff);
					tq.setBoost(field.isDefault ? field1.queryBoost : field1.allQueryBoost);

					bq.add(tq);
					if (k == 0)
						firstQ = tq;
					k++;
				}
			}
			if (k == 0)
				return null;
			if (k == 1) {
				return firstQ;
			}
			return bq;
		}
		if ((!modifiers.synonymExpansion) || (field.expandSynonyms)) {
			if (parser.countGenTerms()) {
				return null;
			}
			Query q = generateWildcardQuery(field.schemaField, text, cutoff);
			return q;
		}
		return null;
	}

	public Query generateFuzzyQuery(ParserField field, String text, float minSim) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		if ((field.isDefault) || (field.isAll)) {
			Query firstQ = null;
			DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

			ArrayList qf = field.isDefault ? parser.queryFields : parser.allFields;

			int n = qf.size();
			int k = 0;
			for (int i = 0; i < n; i++) {
				ParserField field1 = (ParserField) qf.get(i);

				if ((!modifiers.synonymExpansion) || (field1.expandSynonyms)) {
					Query fq = generateFuzzyQuery(field1.fieldName(), text, minSim);
					fq.setBoost(field.isDefault ? field1.queryBoost : field1.allQueryBoost);

					bq.add(fq);
					if (k == 0)
						firstQ = fq;
					k++;
				}
			}
			if (k == 0)
				return null;
			if (k == 1) {
				return firstQ;
			}
			return bq;
		}
		if ((!modifiers.synonymExpansion) || (field.expandSynonyms)) {
			if (parser.countGenTerms()) {
				return null;
			}
			return generateFuzzyQuery(field.fieldName(), text, minSim);
		}
		return null;
	}

	public BooleanQuery generateSynonym(BooleanQuery bq, List<QueryAST> terms, List<String> synonymTerms,
			ParserField field, boolean stem) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		if (bq == null) {
			bq = generateBooleanQuery();
		}

		BooleanQuery bq2 = generateBooleanQuery();
		bq.add(bq2, BooleanClause.Occur.MUST);

		if (terms != null) {
			BooleanQuery bq1 = generateBooleanQuery();
			int n1 = terms.size();
			for (int i1 = 0; i1 < n1; i1++) {
				QueryAST term1 = (QueryAST) terms.get(i1);

				boolean expand = term1.modifiers.expandSynonyms;
				term1.modifiers.expandSynonyms = false;
				Query tq1 = term1.generateQuery();
				term1.modifiers.expandSynonyms = expand;

				bq1.add(tq1, BooleanClause.Occur.MUST);
			}
			bq2.add(bq1, BooleanClause.Occur.SHOULD);
		}

		int n = synonymTerms.size();
		for (int i = 0; (i < n) && (!parser.countGenTerms()); i++) {
			ParserTermModifiers newModifiers = modifiers.clone();
			newModifiers.expandSynonyms = false;
			newModifiers.synonymExpansion = true;
			newModifiers.stemWords = stem;
			String termText = (String) synonymTerms.get(i);
			TermAST nt = parser.getASTGenerator().newTermAST(parser, newModifiers);
			nt.term = new ParserTerm(parser, newModifiers, termText);
			nt.setTermSlop(parser.queryPhraseSlop);
			Query tq = nt.generateQuery();
			bq2.add(tq, BooleanClause.Occur.SHOULD);
		}

		return bq;
	}

	public SpanOrQuery generateSynonymSpanQuery(List<QueryAST> terms, List<String> synonymTerms, ParserField field,
			boolean stem) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}

		ArrayList<SpanQuery> nears = new ArrayList<SpanQuery>();
		int n = synonymTerms.size();
		for (int i = 0; (i < n) && (!parser.countGenTerms()); i++) {
			ParserTermModifiers newModifiers = modifiers.clone();
			newModifiers.expandSynonyms = false;
			newModifiers.synonymExpansion = true;
			newModifiers.stemWords = stem;
			String termText = (String) synonymTerms.get(i);
			TermAST nt = parser.getASTGenerator().newTermAST(parser, newModifiers);
			nt.term = new ParserTerm(parser, newModifiers, termText);
			nt.setTermSlop(parser.queryPhraseSlop);

			SpanQuery tq = nt.generateSpanQuery(field);
			nears.add(tq);
		}

		SpanOrQuery sor = generateSpanOrQuery(nears);

		return sor;
	}

	public Query addBigramRelevance(Query query, QueryAST term1, QueryAST term2) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return query;
		}
		ParserField field1 = null;
		ParserField field2 = null;
		List<String> words1 = null;
		List<String> words2 = null;

		if ((term1 != null) && (!term1.isNegativeOp()) && (!term1.isWild())) {
			field1 = term1.getField();
			words1 = term1.getWords();
		}
		if ((term2 != null) && (!term2.isNegativeOp()) && (!term2.isWild())) {
			field2 = term2.getField();
			words2 = term2.getWords();
		}

		boolean mustBeText = true;
		if ((field1 == null) || (field1.schemaField == null) || (field1 != field2) || (words1 == null) || (words2 == null)
				|| ((mustBeText) && ((!field1.isText()) || (!field2.isText())))) {
			return query;
		}
		int n1 = words1.size();
		int n2 = words2.size();
		int n = n1 + n2;

		if (n >= 2) {
			Query rq = null;

			ArrayList qf = parser.bigramRelevancyFields;
			int nq = 0;
			if ((field1.isDefault) || (field1.isAll)) {
				nq = qf.size();
				if (nq == 1) {
					field1 = (ParserField) qf.get(0);
				}
			}
			if (nq > 1) {
				PhraseQuery firstQ = null;
				DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

				int k = 0;
				for (int i = 0; i < nq; i++) {
					ParserField field = (ParserField) qf.get(i);
					String fieldName = field.fieldName();
					if (fieldName != null) {
						if (parser.countGenTerms()) {
							break;
						}
						PhraseQuery pq = generatePhraseQuery();
						pq.setSlop(parser.relevancyPhraseSlop);

						for (int ip = 0; ip < n1; ip++) {
							String word = (String) words1.get(ip);

							if ((field.schemaField.type.stopwordsIndexed) || (!field.schemaField.type.isStopWord(word))) {
								Term t = generateTerm(fieldName, word);
								pq.add(t);
							}
						}
						for (int ip = 0; ip < n2; ip++) {
							String word = (String) words2.get(ip);

							if ((field.schemaField.type.stopwordsIndexed) || (!field.schemaField.type.isStopWord(word))) {
								Term t = generateTerm(fieldName, word);
								pq.add(t);
							}
						}
						pq.setBoost(field.bigramRelevancyBoost);

						if (pq.getTerms().length >= 2) {
							bq.add(pq);

							if (k == 0)
								firstQ = pq;
							k++;
						}
					}
				}
				if (k == 0)
					rq = null;
				else if (k == 1)
					rq = firstQ;
				else
					rq = bq;
			} else {
				PhraseQuery pq = generatePhraseQuery();
				pq.setSlop(parser.relevancyPhraseSlop);

				for (int i = 0; (i < n1) && (!parser.countGenTerms()); i++) {
					String word = (String) words1.get(i);

					if ((field1.schemaField.type.stopwordsIndexed) || (!field1.schemaField.type.isStopWord(word))) {
						Term t = generateTerm(field1.fieldName(), word);
						pq.add(t);
					}
				}
				for (int i = 0; (i < n2) && (!parser.countGenTerms()); i++) {
					String word = (String) words2.get(i);

					if ((field1.schemaField.type.stopwordsIndexed) || (!field1.schemaField.type.isStopWord(word))) {
						Term t = generateTerm(field1.fieldName(), word);
						pq.add(t);
					}
				}

				if (pq.getTerms().length >= 2) {
					rq = pq;

					float boost = 1.0F;

					if (term1.haveBoost)
						boost = term1.boost;
					if ((term2.haveBoost) && (term2.boost > boost))
						boost = term2.boost;
					rq.setBoost(boost * field1.bigramRelevancyBoost);
				} else {
					rq = null;
				}
			}

			if (rq != null) {
				if (query == null) {
					query = generateBooleanQuery();
				} else if (!(query instanceof BooleanQuery)) {
					BooleanQuery bq1 = generateBooleanQuery();
					bq1.add(query, BooleanClause.Occur.MUST);
					query = bq1;
				}

				BooleanQuery bq = (BooleanQuery) query;
				addBooleanQueryClause(bq, rq, BooleanClause.Occur.SHOULD);
			}
		}
		return query;
	}

	public Query addTrigramRelevance(Query query, QueryAST term1, QueryAST term2, QueryAST term3) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return query;
		}
		ParserField field1 = null;
		ParserField field2 = null;
		ParserField field3 = null;
		List<String> words1 = null;
		List<String> words2 = null;
		List<String> words3 = null;
		boolean trigramBoostSet = false;

		if ((term1 != null) && (!term1.isNegativeOp()) && (!term1.isWild())) {
			field1 = term1.getField();
			words1 = term1.getWords();
		}
		if ((term2 != null) && (!term2.isNegativeOp()) && (!term2.isWild())) {
			field2 = term2.getField();
			words2 = term2.getWords();
		}
		if ((term3 != null) && (!term3.isNegativeOp()) && (!term3.isWild())) {
			field3 = term3.getField();
			words3 = term3.getWords();
		}

		boolean mustBeText = true;
		if ((field1 == null) || (field2 == null) || (field3 == null) || (words1 == null) || (words2 == null)
				|| (words3 == null) || (field1.schemaField == null) || (field1 != field2) || (field1 != field3)
				|| ((mustBeText) && ((!field1.isText()) || (!field2.isText()) || (!field3.isText())))) {
			return query;
		}

		boolean term1Stopword = false;
		boolean term2Stopword = false;
		boolean term3Stopword = false;
		if (!field1.schemaField.type.stopwordsIndexed) {
			term1Stopword = term1.isStopWord();
			term2Stopword = term2.isStopWord();
			term3Stopword = term3.isStopWord();
			if ((term1Stopword) || (term3Stopword)) {
				return query;
			}
		}
		int n1 = words1.size();
		int n2 = words2.size();
		int n3 = words3.size();
		int n = n1 + n2 + n3;

		if (n >= 3) {
			Query rq = null;

			ArrayList qf = parser.trigramRelevancyFields;
			int nq = 0;
			if ((field1.isDefault) || (field1.isAll)) {
				nq = qf.size();
				if (nq == 1) {
					field1 = (ParserField) qf.get(0);
				}
			}
			if (nq > 1) {
				PhraseQuery firstQ = null;
				DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

				int k = 0;
				for (int i = 0; i < nq; i++) {
					ParserField field = (ParserField) qf.get(i);
					String fieldName = field.fieldName();
					if (fieldName != null) {
						PhraseQuery pq = addTrigramRelevanceOnePhrase(field, words1, words2, words3, term2Stopword);

						bq.add(pq);
						if (k == 0)
							firstQ = pq;
						k++;
					}
				}
				if (k == 0) {
					rq = null;
				} else if (k == 1) {
					rq = firstQ;
					trigramBoostSet = true;
				} else {
					rq = bq;
				}
			} else {
				PhraseQuery pq = addTrigramRelevanceOnePhrase(field1, words1, words2, words3, term2Stopword);

				rq = pq;
			}

			float boost = 1.0F;

			if (term1.haveBoost)
				boost = term1.boost;
			if ((term2.haveBoost) && (term2.boost > boost))
				boost = term2.boost;
			if ((term3.haveBoost) && (term3.boost > boost))
				boost = term3.boost;
			if ((!trigramBoostSet) && (rq != null) && (field1 != null)) {
				rq.setBoost(boost * field1.trigramRelevancyBoost);
			}
			if (query == null) {
				query = generateBooleanQuery();
			} else if (!query.getClass().getSimpleName().equals("BooleanQuery")) {
				BooleanQuery bq1 = generateBooleanQuery();
				bq1.add(query, BooleanClause.Occur.MUST);
				query = bq1;
			}

			BooleanQuery bq = (BooleanQuery) query;
			addBooleanQueryClause(bq, rq, BooleanClause.Occur.SHOULD);
		}
		return query;
	}

	public PhraseQuery addTrigramRelevanceOnePhrase(ParserField field, List<String> words1, List<String> words2,
			List<String> words3, boolean term2Stopword) {
		PhraseQuery pq = generatePhraseQuery();
		pq.setSlop(parser.relevancyPhraseSlop);

		String fieldName = field.fieldName();

		int n1 = words1.size();
		for (int ip = 0; ip < n1; ip++) {
			Term t = generateTerm(fieldName, (String) words1.get(ip));
			pq.add(t);
		}

		int n2 = words2.size();
		if (!term2Stopword) {
			for (int ip = 0; ip < n2; ip++) {
				Term t = generateTerm(fieldName, (String) words2.get(ip));
				pq.add(t);
			}
		}

		int n3 = words3.size();
		for (int ip = 0; ip < n3; ip++) {
			Term t = generateTerm(fieldName, (String) words3.get(ip));
			if ((term2Stopword) && (field.schemaField.type.stopwordPositionsIncremented) && (ip == 0))
				pq.add(t, n2 + 1);
			else {
				pq.add(t);
			}
		}
		pq.setBoost(field.trigramRelevancyBoost);

		return pq;
	}

	public Query addRelevance() throws IOException {
		return addRelevance(null);
	}

	public Query addRelevance(Query query) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return query;
		}
		int n = size();

		if (isNegativeOp()) {
			n--;
		}
		if ((n == 1) && (getClause(0).getOp() == null)) {
			query = getClause(0).addRelevance(query);
		} else if (n > 0) {
			for (int i = 0; i < n; i++) {
				if (parser.maxGenTermsExceeded) {
					return query;
				}
				QueryAST cl = getClause(i);

				if (!cl.isNegativeOp()) {
					query = cl.addRelevance(query);
				}
			}
			if ((parser.opGram) && (n >= 2))
				for (int i = 0; (i < n - 1) && (!parser.maxGenTermsExceeded); i++) {
					QueryAST cl1 = getClause(i);
					QueryAST cl2 = getClause(i + 1);

					if ((!cl1.getOp().equals("!")) && (!cl2.getOp().equals("!"))) {
						if ((this instanceof ClauseListAST)) {
							QueryAST term1 = cl1.getLastParserTerm();
							QueryAST term2 = cl2.getParserTerm();

							if ((term1 != null) || (term2 != null)) {
								QueryAST term = term1 != null ? term1 : term2;

								String op1 = cl2.getClauseOp();

								String opText = "and";
								if ((op1.equals("AND")) || (op1.equals("&&")) || (op1.equals("&&!"))) {
									opText = "and";
								} else {
									if ((!op1.equals("OR")) && (!op1.equals("||")) && (!op1.equals("||!")))
										continue;
									opText = "or";
								}

								ParserField field1 = term.getField();
								TermAST ta = null;
								if (field1.isText())
									ta = parser.getASTGenerator().newTermAST(parser, modifiers);
								else
									ta = parser.getASTGenerator().newStringAST(parser, modifiers);
								ParserTermModifiers newModifiers = modifiers.clone();
								newModifiers.field = term.getField();
								ta.term = new ParserTerm(parser, newModifiers, opText);

								boolean term1OpNeg = term1 != null ? term1.isNegativeOp() : false;

								boolean term2OpNeg = term2 != null ? term2.isNegativeOp() : false;

								if ((term1 != null) && (!term1OpNeg))
									query = addBigramRelevance(query, term1, ta);
								if ((term2 != null) && (!term2OpNeg)) {
									query = addBigramRelevance(query, ta, term2);
								}
								if ((term1 != null) && (term2 != null) && (!term1OpNeg) && (!term2OpNeg)) {
									query = addTrigramRelevance(query, term1, ta, term2);
								}
							}
						}
					}
				}
		}
		return query;
	}

	public String combineWords(List<String> words) {
		int n = words.size();
		String word = "";
		for (int i = 0; i < n; i++)
			word = new StringBuilder().append(word).append((String) words.get(i)).toString();
		return word;
	}

	public List<TermAST> getNegativeTerms() {
		return getNegativeTerms(null);
	}

	public List<TermAST> getNegativeTerms(List<TermAST> negativeTerms) {
		int n = size();
		for (int i = 0; i < n; i++)
			negativeTerms = getClause(i).getNegativeTerms(negativeTerms);
		return negativeTerms;
	}
}
