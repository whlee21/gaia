package gaia.parser.gaia;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.reverse.ReverseStringFilter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.apache.solr.analysis.ReversedWildcardFilterFactory;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.TrieDateField;
import org.apache.solr.schema.TrieDoubleField;
import org.apache.solr.schema.TrieField;
import org.apache.solr.schema.TrieFloatField;
import org.apache.solr.schema.TrieIntField;
import org.apache.solr.schema.TrieLongField;

import gaia.search.query.SpanFuzzyQuery;

class GaiaQueryGenerator {
	protected Version version;

	public GaiaQueryGenerator(Version version) {
		this.version = version;
	}

	public BooleanQuery generateBooleanQuery() {
		return new BooleanQuery();
	}

	public DisjunctionMaxQuery generateDisjunctionMaxQuery(float tieBreakerMultiplier) {
		return new DisjunctionMaxQuery(tieBreakerMultiplier);
	}

	public Query generateFuzzyQuery(String fieldName, String termText, float minimumSimilarity) {
		if ((fieldName == null) || (termText == null))
			return null;
		Term term = generateTerm(fieldName, termText);
		int numEdits;
		if (minimumSimilarity >= 1.0F) {
			numEdits = FuzzyQuery.floatToEdits(minimumSimilarity, termText.codePointCount(0, termText.length()));
		} else {
			float adjustedMin = Math.max(0.0F, Math.min(0.999F, minimumSimilarity));
			numEdits = FuzzyQuery.floatToEdits(adjustedMin, termText.codePointCount(0, termText.length()));
		}
		return new FuzzyQuery(term, numEdits);
	}

	public Query generateMatchAllDocsQuery() {
		return new MatchAllDocsQuery();
	}

	public PhraseQuery generatePhraseQuery() {
		return new PhraseQuery();
	}

	public Query generatePrefixQuery(String fieldName, String prefixText, int cutoff) {
		if ((fieldName == null) || (prefixText == null))
			return null;
		Term term = generateTerm(fieldName, prefixText);
		PrefixQuery pq = new PrefixQuery(term);

		boolean useAuto = true;
		if (!useAuto) {
			pq.setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_FILTER_REWRITE);
		}
		return pq;
	}

	public Query generateRangeQuery(GaiaSchemaField field, ParserTermModifiers modifiers, String lowerBound,
			String upperBound, boolean quotedLowerBound, boolean quotedUpperBound, boolean includeLower, boolean includeUpper) {
		GaiaSchemaFieldType type = field.type;
		FieldType fieldType = type.fieldType;

		String genLowerBound = lowerBound;
		String genUpperBound = upperBound;

		if ((lowerBound != null) && ((lowerBound.length() == 0) || (lowerBound.equals("*")))) {
			genLowerBound = null;
		}
		if ((upperBound != null) && ((upperBound.length() == 0) || (upperBound.equals("*")))) {
			genUpperBound = null;
		}

		if (type.isTrie) {
			TrieField.TrieTypes trieType = null;
			if ((fieldType instanceof TrieField)) {
				TrieField tf = (TrieField) fieldType;
				trieType = tf.getType();
			}

			if ((!(fieldType instanceof TrieDateField)) && (trieType != TrieField.TrieTypes.DATE)) {
				if (((fieldType instanceof TrieIntField)) || (trieType == TrieField.TrieTypes.INTEGER)) {
					if (genLowerBound != null) {
						try {
							genLowerBound = Integer.toString((int) Double.parseDouble(lowerBound));
						} catch (Exception e) {
							String msg = e.getMessage();
							String logMsg = "generateRangeQuery unable to convert lower bound of \"" + lowerBound
									+ "\" to integer; assuming * - " + msg;
							GaiaQueryParser.LOG.warn(logMsg, e);

							genLowerBound = null;
						}

					}

					if (genUpperBound != null)
						try {
							genUpperBound = Integer.toString((int) Double.parseDouble(upperBound));
						} catch (Exception e) {
							String msg = e.getMessage();
							String logMsg = "generateRangeQuery unable to convert upper bound of \"" + upperBound
									+ "\" to integer; assuming * - " + msg;

							GaiaQueryParser.LOG.warn(logMsg, e);

							genUpperBound = null;
						}
				} else if (((fieldType instanceof TrieLongField)) || (trieType == TrieField.TrieTypes.LONG)) {
					if (lowerBound != null) {
						try {
							genLowerBound = Long.toString((long) Double.parseDouble(lowerBound));
						} catch (Exception e) {
							String msg = e.getMessage();
							String logMsg = "generateRangeQuery unable to convert lower bound of \"" + lowerBound
									+ "\" to long; assuming * - " + msg;

							GaiaQueryParser.LOG.warn(logMsg, e);

							genLowerBound = null;
						}

					}

					if (upperBound != null)
						try {
							genUpperBound = Long.toString((long) Double.parseDouble(upperBound));
						} catch (Exception e) {
							String msg = e.getMessage();
							String logMsg = "generateRangeQuery unable to convert upper bound of \"" + upperBound
									+ "\" to long; assuming open upper bound: " + msg;

							GaiaQueryParser.LOG.warn(logMsg, e);

							genUpperBound = null;
						}
				} else if (((fieldType instanceof TrieFloatField)) || (trieType == TrieField.TrieTypes.FLOAT)) {
					if (lowerBound != null) {
						try {
							genLowerBound = Float.toString((float) Double.parseDouble(lowerBound));
						} catch (Exception e) {
							String msg = e.getMessage();
							String logMsg = "generateRangeQuery unable to convert lower bound of \"" + lowerBound
									+ "\" to float; assuming * - " + msg;

							GaiaQueryParser.LOG.warn(logMsg, e);

							genLowerBound = null;
						}

					}

					if (upperBound != null)
						try {
							genUpperBound = Float.toString((float) Double.parseDouble(upperBound));
						} catch (Exception e) {
							String msg = e.getMessage();
							String logMsg = "generateRangeQuery unable to convert upper bound of \"" + upperBound
									+ "\" to float; assuming open upper bound: " + msg;
							GaiaQueryParser.LOG.warn(logMsg, e);

							genUpperBound = null;
						}
				} else if (((fieldType instanceof TrieDoubleField)) || (trieType == TrieField.TrieTypes.DOUBLE)) {
					if (lowerBound != null) {
						try {
							genLowerBound = Double.toString(Double.parseDouble(lowerBound));
						} catch (Exception e) {
							String msg = e.getMessage();
							String logMsg = "generateRangeQuery unable to convert lower bound of \"" + lowerBound
									+ "\" to double; assuming * - " + msg;
							GaiaQueryParser.LOG.warn(logMsg, e);

							genLowerBound = null;
						}

					}

					if (upperBound != null) {
						try {
							genUpperBound = Double.toString(Double.parseDouble(upperBound));
						} catch (Exception e) {
							String msg = e.getMessage();
							String logMsg = "generateRangeQuery unable to convert upper bound of \"" + upperBound
									+ "\" to double; assuming open upper bound: " + msg;

							GaiaQueryParser.LOG.warn(logMsg, e);

							genUpperBound = null;
						}
					}
				}
			}

			Query nq = null;
			try {
				nq = fieldType.getRangeQuery(null, field.schemaField, genLowerBound, genUpperBound, includeLower, includeUpper);
			} catch (Exception e) {
				String msg = e.getMessage();
				String logMsg = "getRangeQuery unable to generate range query - " + msg;
				GaiaQueryParser.LOG.warn(logMsg, e);
			}

			return nq;
		}

		Query q = null;

		if ((lowerBound != null) && ((quotedLowerBound) || (!lowerBound.equals("*")))) {
			ParserTerm tempLowerTerm = new ParserTerm(type.parser, modifiers, lowerBound);
			ParserTerm newLowerTerm = null;
			try {
				newLowerTerm = tempLowerTerm.filterTerm(true, true, modifiers.stemWords, false);
			} catch (IOException ioe) {
				throw new RuntimeException("error while generating range query lower bound", ioe);
			}
			if (newLowerTerm != null) {
				String text = newLowerTerm.text;

				if (field.isText()) {
					text = GaiaQueryParserUtils.collapseWords(text);
				}

				if (text.length() == 0)
					genLowerBound = null;
				else {
					genLowerBound = text;
				}
			}
		}
		if ((upperBound != null) && ((quotedUpperBound) || (!upperBound.equals("*")))) {
			ParserTerm tempUpperTerm = new ParserTerm(type.parser, modifiers, upperBound);
			ParserTerm newUpperTerm = null;
			try {
				newUpperTerm = tempUpperTerm.filterTerm(true, true, modifiers.stemWords, false);
			} catch (IOException ioe) {
				throw new RuntimeException("error while generating range query upper bound", ioe);
			}
			if (newUpperTerm != null) {
				String text = newUpperTerm.text;

				if (field.isText()) {
					text = GaiaQueryParserUtils.collapseWords(text);
				}

				if (text.length() == 0)
					genUpperBound = null;
				else {
					genUpperBound = text;
				}
			}
		}

		q = generateTermRangeQuery(field, genLowerBound, genUpperBound, includeLower, includeUpper);
		return q;
	}

	public Query generateRangeQuery(GaiaSchemaField field, String lowerVal, String upperVal, boolean includeLower,
			boolean includeUpper) {
		if ((lowerVal != null) && (lowerVal.equals(upperVal)) && ((includeLower) || (includeUpper))) {
			return generateTermQuery(field.getName(), lowerVal);
		}

		lowerVal = GaiaQueryParserUtils.wildcardReversalRangeLowerBoundAdjust(field.type, lowerVal);

		return new TermRangeQuery(field.getName(), lowerVal == null ? null : new BytesRef(lowerVal),
				upperVal == null ? null : new BytesRef(upperVal), includeLower, includeUpper);
	}

	public SpanQuery generateSpanFuzzyQuery(String fieldName, String termText, float minimumSimilarity) {
		if ((fieldName == null) || (termText == null))
			return null;
		Term term = generateTerm(fieldName, termText);
		return new SpanFuzzyQuery(term, minimumSimilarity >= 1.0F ? (int) minimumSimilarity : Math.max(0.0F,
				Math.min(0.999F, minimumSimilarity)));
	}

	public SpanNearQuery generateSpanNearQuery(List<SpanQuery> spanQueries) {
		return generateSpanNearQuery(spanQueries, 0, true);
	}

	public SpanNearQuery generateSpanNearQuery(List<SpanQuery> spanQueries, int slop, boolean inOrder) {
		if (spanQueries == null)
			return null;
		int nc = spanQueries.size();
		SpanQuery[] spanVector = new SpanQuery[nc];
		for (int i = 0; i < nc; i++) {
			spanVector[i] = ((SpanQuery) spanQueries.get(i));
		}
		return new SpanNearQuery(spanVector, slop, inOrder);
	}

	public SpanNotQuery generateSpanNotQuery(SpanQuery include, SpanQuery exclude) {
		return new SpanNotQuery(include, exclude);
	}

	public SpanOrQuery generateSpanOrQuery(List<SpanQuery> spanQueries) {
		if (spanQueries == null)
			return null;
		int nc = spanQueries.size();
		SpanQuery[] spanVector = new SpanQuery[nc];
		for (int i = 0; i < nc; i++) {
			spanVector[i] = ((SpanQuery) spanQueries.get(i));
		}
		return new SpanOrQuery(spanVector);
	}

	public SpanTermQuery generateSpanTermQuery(GaiaSchemaField field, String term) {
		return new SpanTermQuery(generateTerm(field.getName(), term));
	}

	public Term generateTerm(String fieldName, String termText) {
		if ((fieldName == null) || (termText == null)) {
			return null;
		}
		return new Term(fieldName, termText);
	}

	public Query generateTermRangeQuery(GaiaSchemaField field, String lowerVal, String upperVal, boolean includeLower,
			boolean includeUpper) {
		lowerVal = GaiaQueryParserUtils.wildcardReversalRangeLowerBoundAdjust(field.type, lowerVal);

		TermRangeQuery tq = new TermRangeQuery(field.getName(), lowerVal == null ? null : new BytesRef(lowerVal),
				upperVal == null ? null : new BytesRef(upperVal), includeLower, includeUpper);

		boolean useAuto = true;
		if (!useAuto) {
			tq.setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_FILTER_REWRITE);
		}
		return tq;
	}

	public Query generateTermQuery(Term term) {
		if (term == null) {
			return null;
		}
		return new TermQuery(term);
	}

	public Query generateTermQuery(String fieldName, String termText) {
		return generateTermQuery(generateTerm(fieldName, termText));
	}

	public Query generateWildcardQuery(GaiaSchemaField field, String termText, int cutoff) {
		if ((field == null) || (termText == null)) {
			return null;
		}

		ReversedWildcardFilterFactory factory = field.getType().reversedWildcardFilterFactory;
		if ((factory != null) && (factory.shouldReverse(termText))) {
			termText = ReverseStringFilter.reverse(version, termText + factory.getMarkerChar());

			boolean trailStar = false;
			int n = termText.length();
			int i = 0;
			for (i = n - 1; (i > 0) && (termText.charAt(i) == '*'); i--) {
				trailStar = true;
			}

			if (trailStar) {
				String prefixText = termText.substring(0, i + 1);
				if ((!prefixText.contains("*")) && (!prefixText.contains("?"))) {
					return generatePrefixQuery(field.getName(), prefixText, cutoff);
				}
			}
		}

		Term term = generateTerm(field.getName(), termText);

		WildcardQuery wq = new WildcardQuery(term);

		boolean useAuto = true;
		if (!useAuto) {
			wq.setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_FILTER_REWRITE);
		}
		return wq;
	}
}
