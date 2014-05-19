package gaia.parser.gaia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

class PhraseAST extends QueryAST {
	public int proximitySlop = parser.queryPhraseSlop;

	public PhraseAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		super(parser, modifiers);
	}

	public ParserField getField() {
		if (size() >= 1) {
			return getTermClause(0).getField();
		}
		return null;
	}

	public QueryAST getLastParserTerm() {
		return this;
	}

	public QueryAST getParserTerm() {
		return this;
	}

	public int proximitySlop(int n) {
		proximitySlop = n;

		return n;
	}

	public Query generateQuery() throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		Query q = null;

		int n = size();

		ParserField field = null;
		if (n > 0) {
			ParserTerm term = getTermClause(0).term;
			field = term.field;
		}

		if ((n == 1) && (!modifiers.hyphenated)) {
			ParserTerm term = getTermClause(0).term;
			boolean textField = field.isText();
			ParserTerm newTerm = term.filterTerm(true, false, false, !textField);
			if (newTerm != null) {
				field = newTerm.field;
				String text = newTerm.text;

				ArrayList<String> words = null;
				if (textField) {
					words = newTerm.splitWordsOnWhiteSpace(text);
				} else {
					words = new ArrayList<String>();
					words.add(text);
				}
				int nw = words.size();
				if (nw == 1) {
					String word = (String) words.get(0);

					if ((modifiers.stemWords) && (field.isText())) {
						word = field.stemWord(word);
					}
					q = generateTermQuery(newTerm, word);
				} else if ((field.isDefault) || (field.isAll)) {
					PhraseQuery firstQ = null;
					DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

					ArrayList qf = field.isDefault ? parser.queryFields : parser.allFields;
					int nq = qf.size();
					int k = 0;
					for (int i = 0; (i < nq) && (!parser.countGenTerms()); i++) {
						ParserField field1 = (ParserField) qf.get(i);
						String fieldName = field1.fieldName();

						PhraseQuery pq = generatePhraseQuery();
						pq.setSlop(proximitySlop);
						for (int ip = 0; ip < nw; ip++) {
							String word = (String) words.get(ip);

							if ((modifiers.stemWords) && (field.isText())) {
								word = field1.stemWord(word);
							}
							Term genTerm = generateTerm(fieldName, word);
							pq.add(genTerm);
						}

						float extraBoost = 1.0F;
						if (field1.isAll)
							extraBoost = field1.allQueryBoost;
						else
							extraBoost = field1.queryBoost;
						if (haveBoost)
							pq.setBoost(boost * extraBoost);
						else {
							pq.setBoost(extraBoost);
						}
						bq.add(pq);
						if (k == 0)
							firstQ = pq;
						k++;
					}

					if (k == 0)
						q = null;
					else if (k == 1)
						q = firstQ;
					else
						q = bq;
				} else {
					if (parser.countGenTerms())
						return null;
					PhraseQuery pq = generatePhraseQuery();
					pq.setSlop(proximitySlop);
					for (int i = 0; i < nw; i++) {
						String word = (String) words.get(i);

						if ((modifiers.stemWords) && (field.isText())) {
							word = field.stemWord(word);
						}
						Term genTerm = generateTerm(field.fieldName(), word);
						pq.add(genTerm);
					}
					q = pq;

					ParserField f = getTermClause(0).term.field;
					float extraBoost = 1.0F;
					if (f.isAll)
						extraBoost = f.allQueryBoost;
					else
						extraBoost = f.queryBoost;
					if (haveBoost)
						q.setBoost(boost * extraBoost);
					else
						q.setBoost(extraBoost);
				}
			}
		} else if (n >= 1) {
			int k = 0;
			for (int i = 0; i < n; i++) {
				ParserTerm term = getTermClause(i).term;
				term = term.filterTerm(true, false, modifiers.stemWords, false);
				if (term != null) {
					k++;
				}
			}
			if (k >= 1) {
				if ((field.isDefault) || (field.isAll)) {
					Query firstQ = null;
					DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

					ArrayList qf = field.isDefault ? parser.queryFields : parser.allFields;
					int nq = qf.size();
					int kf = 0;
					for (int i = 0; (i < nq) && (!parser.countGenTerms()); i++) {
						ParserField field1 = (ParserField) qf.get(i);

						Query q1 = generateOneQuery(field1);

						if (q1 != null) {
							float extraBoost = 1.0F;
							if (field1.isAll)
								extraBoost = field1.allQueryBoost;
							else
								extraBoost = field1.queryBoost;
							if (haveBoost)
								q1.setBoost(boost * extraBoost);
							else {
								q1.setBoost(extraBoost);
							}
							bq.add(q1);
							if (kf == 0)
								firstQ = q1;
							kf++;
						}
					}
					if (kf == 0)
						q = null;
					else if (kf == 1)
						q = firstQ;
					else
						q = bq;
				} else {
					q = generateOneQuery(field);

					if (q != null) {
						if (haveBoost)
							q.setBoost(boost * field.queryBoost);
						else {
							q.setBoost(field.queryBoost);
						}
					}
				}
			}
		}

		return q;
	}

	public Query generateOneQuery(ParserField field) throws IOException {
		if (parser.countGenTerms()) {
			return null;
		}

		if (modifiers.hyphenated) {
			return generateSpanQuery(field);
		}
		Query q = null;

		PhraseQuery pq = generatePhraseQuery();
		pq.setSlop(proximitySlop);

		int posIncr = 0;
		boolean firstTerm = true;
		int initialSkipped = 0;

		int n = size();
		for (int i = 0; i < n; i++) {
			ParserTerm term = getTermClause(i).term;

			if (term.modifiers.isWild) {
				posIncr++;
			} else {
				term = term.filterTerm(true, false, false, false);
				if (term != null) {
					String text = term.text;

					ArrayList<String> words = null;
					if (field.isText()) {
						words = term.splitWordsOnWhiteSpace(text);
					} else {
						words = new ArrayList<String>();
						words.add(text);
					}
					int nw = words.size();
					if (nw == 1) {
						String word = (String) words.get(0);

						if ((modifiers.stemWords) && (field.isText())) {
							word = field.stemWord(word);
						}

						if ((field.schemaField.type.isStopWord(word)) && (!field.schemaField.type.stopwordsIndexed)) {
							if (field.schemaField.type.stopwordPositionsIncremented)
								posIncr++;
						} else {
							Term genTerm = generateTerm(field.fieldName(), word);

							if (posIncr > 0) {
								if (firstTerm) {
									pq.add(genTerm);
									initialSkipped = posIncr;
								} else {
									pq.add(genTerm, i - initialSkipped);
								}

								posIncr = 0;
							} else {
								pq.add(genTerm);
							}
							firstTerm = false;
						}
					} else if (nw > 1) {
						for (int j = 0; j < nw; j++) {
							String word = (String) words.get(j);

							if ((field.schemaField.type.isStopWord(word)) && (!field.schemaField.type.stopwordsIndexed)) {
								if (field.schemaField.type.stopwordPositionsIncremented)
									posIncr++;
							} else {
								if ((modifiers.stemWords) && (field.isText())) {
									word = field.stemWord(word);
								}
								Term genTerm = generateTerm(field.fieldName(), word);

								if (posIncr > 0) {
									if (firstTerm) {
										initialSkipped = posIncr;
									} else {
										pq.add(genTerm, i - initialSkipped);
									}

									posIncr = 0;
								} else {
									pq.add(genTerm);
								}
								firstTerm = false;
							}
						}
					}
				}
			}
		}
		if (pq != null) {
			Term[] terms = pq.getTerms();
			int nt = terms.length;
			if (nt == 0) {
				return null;
			}

			if (nt == 1) {
				q = generateTermQuery(field.fieldName(), terms[0].text());
			} else
				q = pq;

			float fieldQueryBoost = field.queryBoost;
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
		SpanQuery q = null;

		int n = size();

		if (n == 1) {
			ParserTerm term = getTermClause(0).term;
			boolean textField = field.isText();
			ParserTerm newTerm = term.filterTerm(true, false, false, !textField);
			if (newTerm != null) {
				String text = newTerm.text;

				if ((field.schemaField.type.isStopWord(text)) && (!field.schemaField.type.stopwordsIndexed)) {
					return null;
				}

				ArrayList<String> words = null;
				if (textField) {
					words = newTerm.splitWordsOnWhiteSpace(text);
				} else {
					words = new ArrayList<String>();
					words.add(text);
				}
				int nw = words.size();
				if (nw == 1) {
					String word = (String) words.get(0);

					if ((modifiers.stemWords) && (field.isText())) {
						word = field.stemWord(word);
					}
					q = generateSpanTermQuery(field.schemaField, word);
				} else {
					if (parser.countGenTerms()) {
						return null;
					}
					ArrayList<SpanQuery> terms = new ArrayList<SpanQuery>();

					for (int i = 0; i < nw; i++) {
						String word = (String) words.get(i);

						if ((modifiers.stemWords) && (field.isText())) {
							word = field.stemWord(word);
						}
						SpanTermQuery genTerm = generateSpanTermQuery(field.schemaField, word);
						terms.add(genTerm);
					}

					SpanNearQuery nq = generateSpanNearQuery(terms, proximitySlop, true);

					if (modifiers.hyphenated) {
						String singleTerm = combineWords(words);
						singleTerm = field.stemWord(singleTerm);
						SpanQuery tq = generateSpanTermQuery(field.schemaField, singleTerm);

						ArrayList<SpanQuery> sc = new ArrayList<SpanQuery>();

						sc.add(nq);
						sc.add(tq);

						SpanOrQuery oq = generateSpanOrQuery(sc);

						q = oq;
					} else {
						q = nq;
					}

					ParserField f = getTermClause(0).term.field;
					float extraBoost = 1.0F;
					if (f.isAll)
						extraBoost = f.allQueryBoost;
					else
						extraBoost = f.queryBoost;
					if (haveBoost)
						q.setBoost(boost * extraBoost);
					else
						q.setBoost(extraBoost);
				}
			}
		} else if (n >= 2) {
			int spanSlop = 0;

			int k = 0;
			for (int i = 0; i < n; i++) {
				ParserTerm term = getTermClause(i).term;
				term = term.filterTerm(true, false, modifiers.stemWords, false);
				if (term != null) {
					k++;
				}
			}
			if (k >= 1) {
				if (parser.countGenTerms()) {
					return null;
				}
				ArrayList<SpanQuery> terms = new ArrayList<SpanQuery>();

				for (int i = 0; i < n; i++) {
					ParserTerm sourceTerm = getTermClause(i).term;
					ParserTerm term = sourceTerm.filterTerm(true, false, false, false);
					if (term != null) {
						ParserField termField = term.field;
						String text = term.text;

						if ((!field.schemaField.type.isStopWord(text)) || (field.schemaField.type.stopwordsIndexed)) {
							ArrayList<String> words = null;
							if (termField.isText()) {
								words = term.splitWordsOnWhiteSpace(text);
							} else {
								words = new ArrayList<String>();
								words.add(text);
							}
							int nw = words.size();
							if (nw == 1) {
								String word = (String) words.get(0);

								if ((modifiers.stemWords) && (termField.isText())) {
									word = termField.stemWord(word);
								}
								SpanTermQuery genTerm = generateSpanTermQuery(field.schemaField, word);
								terms.add(genTerm);
							} else if (nw > 1) {
								ArrayList<SpanQuery> terms2 = new ArrayList<SpanQuery>();
								for (int j = 0; j < nw; j++) {
									String word = (String) words.get(j);

									if ((modifiers.stemWords) && (termField.isText())) {
										word = termField.stemWord(word);
									}
									SpanTermQuery genTerm = generateSpanTermQuery(field.schemaField, word);
									terms2.add(genTerm);
								}

								if (sourceTerm.modifiers.hyphenated) {
									SpanNearQuery nq = generateSpanNearQuery(terms2, proximitySlop, true);
									String singleTerm = combineWords(words);
									singleTerm = field.stemWord(singleTerm);
									SpanQuery tq = generateSpanTermQuery(field.schemaField, singleTerm);

									ArrayList<SpanQuery> sc = new ArrayList<SpanQuery>();

									sc.add(nq);
									sc.add(tq);

									SpanOrQuery oq = generateSpanOrQuery(sc);

									terms.add(oq);

									spanSlop++;
								} else {
									terms.addAll(terms2);
								}
							}
						}
					}
				}

				SpanNearQuery nq = null;
				if (terms.size() > 0)
					nq = generateSpanNearQuery(terms, proximitySlop + spanSlop, true);
				q = nq;

				if (q != null) {
					field = getTermClause(0).term.field;
					float fieldQueryBoost = field.queryBoost;
					if (haveBoost)
						q.setBoost(boost * fieldQueryBoost);
					else
						q.setBoost(fieldQueryBoost);
				}
			}
		}
		return q;
	}

	public boolean isWild() {
		int n = size();

		for (int i = 0; i < n; i++) {
			TermAST termAST = getTermClause(i);
			if ((termAST != null) && (termAST.isWild())) {
				return true;
			}
		}
		return false;
	}

	public Query addRelevance(Query query) throws IOException {
		return query;
	}

	public String getTermVal() {
		int n = size();
		if (n < 1) {
			return null;
		}
		String termVal = "";
		for (int i = 0; i < n; i++) {
			String termVal2 = getClause(i).getTermVal();
			if ((termVal2 != null) && (termVal2.length() > 0)) {
				if (termVal.length() > 0)
					termVal = termVal + " " + termVal2;
				else {
					termVal = termVal + termVal2;
				}
			}
		}
		return termVal;
	}

	public List<String> getWords() throws IOException {
		List<String> words = new ArrayList<String>();

		int n = size();
		for (int i = 0; i < n; i++) {
			ParserTerm origTerm = getTermClause(i).term;
			ParserTerm term = origTerm.filterTerm(true, false, false, false);
			if (term != null) {
				List<String> words1 = term
						.splitWords(term.field, term.text, (modifiers.stemWords) && (origTerm.field.isText()));

				int n1 = words1.size();

				for (int j = 0; j < n1; j++) {
					String word = (String) words1.get(j);
					words.add(word);
				}
			}
		}

		return words;
	}

	public List<String> getActualWords() throws IOException {
		List<String> words = new ArrayList<String>();

		int n = size();
		for (int i = 0; i < n; i++) {
			ParserTerm term = getTermClause(i).term;
			term = term.filterTerm(true, true, false, true);
			words.add(term.text);
		}

		return words;
	}
}
