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

class TermAST extends QueryAST {
	public ParserTerm term = null;

	public int termSlop = 0;

	public void setTermSlop(int n) {
		termSlop = n;
	}

	public TermAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		super(parser, modifiers);
	}

	public ParserField getField() {
		if (term != null) {
			return term.field;
		}
		return null;
	}

	public String getFieldName() {
		if (term != null) {
			return term.fieldName();
		}
		return null;
	}

	public QueryAST getParserTerm() {
		if (!isNegativeOp()) {
			return this;
		}
		return null;
	}

	public QueryAST getLastParserTerm() {
		if (!isNegativeOp()) {
			return this;
		}
		return null;
	}

	public ParserTerm getTermParserTerm() {
		return term;
	}

	public String getTermVal() {
		if (term != null) {
			return term.text;
		}
		return null;
	}

	public boolean isStopWord() throws IOException {
		if ((term == null) || (!parser.processStopwords) || (!term.field.processStopwords)) {
			return false;
		}
		return term.field.isStopWord(term.text);
	}

	public boolean isWild() {
		if (term == null) {
			return false;
		}
		return term.modifiers.isWild;
	}

	public Query generateQuery() throws IOException {
		return generateQuery(true);
	}

	public boolean isAll(String s, char ch) {
		if (s == null) {
			return false;
		}
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) != ch)
				return false;
		}
		return true;
	}

	public Query generateQuery(boolean allowStopWords) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		Query q = null;

		if (term != null) {
			boolean isTextField = term.isText();

			int synonymIndex = -1;
			boolean synonymIsReplacement = false;
			List<String> syns = null;
			if ((modifiers.expandSynonyms) && (parser.expandSynonyms) && (term.field.expandSynonyms)) {
				ParserTerm newTerm = term.filterTerm(true, true, false, true);
				if ((newTerm != null) && (term.field.expandSynonyms)) {
					synonymIndex = term.field.getSynonymIndex(newTerm.text);
					synonymIsReplacement = term.field.getSynonymReplacementType(synonymIndex);

					syns = term.field.getSynonymTargetTerms(synonymIndex);
				}
			}
			int ns = syns == null ? 0 : syns.size();
			if (!synonymIsReplacement) {
				ParserTerm newTerm = term.filterTerm(allowStopWords, true, false, false);

				if (newTerm == null)
					return null;
				ParserField field = newTerm.field;
				String text = newTerm.text;

				if ((!field.schemaField.type.stopwordsIndexed) && (term.field.schemaField.type.isStopWord(text))) {
					return null;
				}
				List<String> words = null;
				if (isTextField) {
					words = newTerm.splitWords(term.field, text, modifiers.stemWords);
				} else {
					words = new ArrayList<String>();
					words.add(text);
				}
				int n = words.size();
				if (n == 1) {
					String word = (String) words.get(0);

					word = checkLeadWild(word, term.modifiers);

					if (term.modifiers.isWild) {
						String prefix = word.substring(0, word.length() - 1);

						boolean useMatchAll = false;

						if (term.modifiers.isAllStarWild) {
							if ((useMatchAll) || (modifiers.allFields))
								q = generateMatchAllDocsQuery();
							else
								q = generatePrefixQuery(field, "", term.modifiers.termCountHardCutoff);
						} else if (term.modifiers.hasAllStarWildSuffix)
							q = generatePrefixQuery(field, prefix, term.modifiers.termCountHardCutoff);
						else if (word.length() > 0)
							q = generateWildcardQuery(field, word, term.modifiers.termCountHardCutoff);
					} else if (word.length() > 0) {
						q = generateTermQuery(term, word);
					}
				} else if ((field.isDefault) || (field.isAll)) {
					Query firstQ = null;
					DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

					ArrayList qf = field.isDefault ? parser.queryFields : parser.allFields;

					int nq = qf.size();
					int k = 0;
					for (int i = 0; (i < nq) && (!parser.maxGenTermsExceeded); i++) {
						ParserField field1 = (ParserField) qf.get(i);

						if ((!modifiers.synonymExpansion) || (field1.expandSynonyms)) {
							if (parser.countGenTerms()) {
								break;
							}
							PhraseQuery pq = generatePhraseQuery();
							pq.setSlop(termSlop);

							int numNonEmpty = 0;
							for (int ip = 0; ip < n; ip++) {
								String word = (String) words.get(ip);

								if (isTextField) {
									word = term.stripWildcards(word);
								}
								if (word.length() > 0) {
									Term genTerm = generateTerm(field1.fieldName(), word);

									pq.add(genTerm);
									numNonEmpty++;
								}
							}

							float extraBoost = 1.0F;
							if (field.isDefault)
								extraBoost = field1.queryBoost;
							else if (field.isAll)
								extraBoost = field1.allQueryBoost;
							if (haveBoost)
								pq.setBoost(boost * extraBoost);
							else {
								pq.setBoost(extraBoost);
							}

							if (modifiers.hyphenated) {
								String singleTerm = combineWords(words);
								singleTerm = field1.stemWord(singleTerm);
								Query tq = generateTermQuery(field1.fieldName(), singleTerm);

								extraBoost = 1.0F;
								if (field.isDefault)
									extraBoost = field1.queryBoost;
								else if (field.isAll)
									extraBoost = field1.allQueryBoost;
								if (haveBoost)
									tq.setBoost(boost * extraBoost);
								else {
									tq.setBoost(extraBoost);
								}

								BooleanQuery bq1 = generateBooleanQuery();
								bq1.add(pq, BooleanClause.Occur.SHOULD);
								bq1.add(tq, BooleanClause.Occur.SHOULD);

								bq.add(bq1);
								if (k == 0) {
									firstQ = bq1;
								}
							} else if (numNonEmpty > 0) {
								bq.add(pq);
								if (k == 0) {
									firstQ = pq;
								}
							}

							if (numNonEmpty > 0)
								k++;
						}
					}
					if (k == 0)
						q = null;
					else if (k == 1)
						q = firstQ;
					else
						q = bq;
				} else {
					if (parser.countGenTerms()) {
						return null;
					}
					PhraseQuery pq = generatePhraseQuery();
					pq.setSlop(termSlop);

					for (int i = 0; i < n; i++) {
						String word = (String) words.get(i);

						if (isTextField) {
							word = term.stripWildcards(word);
						}
						if (word.length() > 0) {
							Term genTerm = generateTerm(field.fieldName(), word);

							pq.add(genTerm);
						}

					}

					if (modifiers.hyphenated) {
						String singleTerm = combineWords(words);
						singleTerm = field.stemWord(singleTerm);
						Query tq = generateTermQuery(field.fieldName(), singleTerm);

						BooleanQuery bq1 = generateBooleanQuery();
						bq1.add(pq, BooleanClause.Occur.SHOULD);
						bq1.add(tq, BooleanClause.Occur.SHOULD);

						q = bq1;
					} else {
						q = pq;
					}
				}

			}

			if (ns >= 1) {
				q = generateSynonym(null, null, syns, term.field, modifiers.stemWords);
			}

		}

		if (q != null) {
			String op = getClauseOp();
			if ("||!".equals(op)) {
				BooleanQuery bq = generateBooleanQuery();
				bq.add(q, BooleanClause.Occur.MUST_NOT);
				bq.add(generateMatchAllDocsQuery(), BooleanClause.Occur.MUST);
				q = bq;
			}

			ParserField field = term.field;
			float fieldQueryBoost = field.queryBoost;
			if (((field.isAll) || (field.isDefault)) && (!(q instanceof DisjunctionMaxQuery)))
				fieldQueryBoost = q.getBoost();
			if (haveBoost)
				q.setBoost(boost * fieldQueryBoost);
			else {
				q.setBoost(fieldQueryBoost);
			}
		}
		return q;
	}

	public SpanQuery generateSpanQuery(ParserField field) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		boolean allowStopWords = true;

		SpanQuery q = null;

		if (term != null) {
			boolean isTextField = term.isText();

			List<QueryAST> terms = new ArrayList<QueryAST>();
			int synonymIndex = -1;
			boolean isSynonym = false;
			List<String> syns = null;
			if ((modifiers.expandSynonyms) && (parser.expandSynonyms) && (term.field.expandSynonyms)) {
				ParserTerm newTerm = term.filterTerm(true, true, false, true);
				terms.add(this);
				if ((newTerm != null) && (term.field.expandSynonyms)) {
					synonymIndex = term.field.getSynonymIndex(newTerm.text);
					isSynonym = synonymIndex >= 0;
					syns = term.field.getSynonymTargetTerms(synonymIndex);
				}
			}
			int ns = syns == null ? 0 : syns.size();
			if (!isSynonym) {
				ParserTerm newTerm = term.filterTerm(allowStopWords, true, false, false);

				if (newTerm == null)
					return null;
				String text = newTerm.text;

				if ((term.isStopWord(term.field, text)) && (!term.field.schemaField.type.stopwordsIndexed)) {
					return null;
				}

				List<String> words = null;
				if (isTextField) {
					words = newTerm.splitWords(term.field, text, modifiers.stemWords);
				} else {
					words = new ArrayList<String>();
					words.add(text);
				}
				int n = words.size();
				if (n == 1) {
					String word = (String) words.get(0);

					if (term.modifiers.isWild) {
						word = term.stripWildcards(word);
					}
					if (word.length() > 0)
						q = generateSpanTermQuery(field.schemaField, word);
				} else {
					if (parser.countGenTerms()) {
						return null;
					}
					ArrayList<SpanQuery> spanTerms = new ArrayList<SpanQuery>();

					for (int i = 0; i < n; i++) {
						String word = (String) words.get(i);

						if (isTextField) {
							word = term.stripWildcards(word);
						}
						if (word.length() > 0) {
							SpanTermQuery tq = generateSpanTermQuery(field.schemaField, word);
							spanTerms.add(tq);
						}
					}
					SpanNearQuery pq = generateSpanNearQuery(spanTerms, termSlop, true);

					if (modifiers.hyphenated) {
						String singleTerm = combineWords(words);
						singleTerm = field.stemWord(singleTerm);
						SpanQuery tq = generateSpanTermQuery(field.schemaField, singleTerm);

						ArrayList<SpanQuery> sc = new ArrayList<SpanQuery>();

						sc.add(pq);
						sc.add(tq);

						SpanOrQuery oq = generateSpanOrQuery(sc);

						q = oq;
					} else {
						q = pq;
					}
				}
			}

			if (ns >= 1) {
				q = generateSynonymSpanQuery(terms, syns, field, modifiers.stemWords);
			}
		}
		if (q != null) {
			String op = getClauseOp();
			if ("||!".equals(op)) {
				SpanNotQuery nq = generateSpanNotQuery(null, q);
				q = nq;
			}

			float fieldQueryBoost = field.queryBoost;
			if (haveBoost)
				q.setBoost(boost * fieldQueryBoost);
			else {
				q.setBoost(fieldQueryBoost);
			}
		}
		return q;
	}

	public Term getTerm() throws IOException {
		if (term == null) {
			return null;
		}
		ParserTerm newTerm = term.filterTerm(true, true, false, true);

		ParserField field = newTerm.field;
		String text = newTerm.text;

		List<String> words = term.splitWords(term.field, text, (modifiers.stemWords) && (field.isText()));

		int n = words.size();
		if (n == 1) {
			String word = (String) words.get(0);

			Term genTerm = generateTerm(field.fieldName(), word);

			return genTerm;
		}
		return null;
	}

	public List<String> getWords() throws IOException {
		if (term != null) {
			ParserTerm newTerm = term.filterTerm(true, true, false, false);
			if (newTerm == null) {
				return null;
			}
			return term.splitWords(term.field, newTerm.text, modifiers.stemWords);
		}

		return null;
	}

	public List<String> getActualWords() throws IOException {
		if (term != null) {
			ParserTerm newTerm = term.filterTerm(true, true, false, true);
			if (newTerm == null) {
				return null;
			}
			List<String> words = new ArrayList<String>();
			words.add(newTerm.text);
			return words;
		}

		return null;
	}

	public List<TermAST> getNegativeTerms(List<TermAST> negativeTerms) {
		if (isNegativeOp()) {
			if (negativeTerms == null)
				negativeTerms = new ArrayList<TermAST>();
			negativeTerms.add(this);
		}
		return negativeTerms;
	}

	public void setTerm(ParserTerm t) {
		term = t;

		modifiers.hyphenated = t.modifiers.hyphenated;
	}

	public String toString() {
		if (term == null) {
			return "(null)";
		}
		return term.fieldName() + ":" + term.text;
	}
}
