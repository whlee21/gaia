package gaia.parser.gaia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

class TermListAST extends QueryAST {
	public int moreLikeThreshold = 0;

	public TermListAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		super(parser, modifiers);
	}

	public void setMoreLikeThreshold(int n) {
		moreLikeThreshold = n;
	}

	public Query generateQuery() throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		int n = size();
		Query query = null;
		BooleanClause.Occur occur = null;

		int numOptionalClauses = 0;

		String op1 = getOp();
		String op2 = getClause(0).getOp();
		if ((n == 1) && (!op1.equals("!")) && (!op1.equals("-"))
				&& ((op2 == null) || ((!op2.equals("-")) && (!op2.equals("!")) && (!op2.equals("!="))))) {
			query = getClause(0).generateQuery();
		} else if (n > 0) {
			BooleanQuery bq = generateBooleanQuery();

			boolean allNegative = true;

			boolean noExplicitOps = true;
			boolean allowStopWords = true;
			for (int i = 0; i < n; i++) {
				QueryAST cl = getClause(i);

				ParserField pf = cl.getField();
				if (pf != null) {
					GaiaSchemaField sf = pf.schemaField;
					if ((sf != null) && (!sf.type.stopwordsIndexed)) {
						allowStopWords = false;
					}
				}
				if (!cl.isStopWord())
					allowStopWords = false;
				if ((cl.op != null) && (!cl.op.equals("|"))) {
					noExplicitOps = false;
				}
			}
			int k = 0;
			for (int i = 0; (i < n) && (!parser.maxGenTermsExceeded); i++) {
				List<QueryAST> terms = new ArrayList<QueryAST>();
				boolean isSynonym = false;
				int synonymIndex = -1;
				List<String> synonymTerms = null;
				ParserField synonymField = null;
				occur = BooleanClause.Occur.SHOULD;
				int ns = 0;

				if ((parser.expandSynonyms) && (modifiers.expandSynonyms)) {
					synonymField = null;
					for (int i1 = i; i1 < n; i1++) {
						QueryAST qa = getClause(i1);
						if (!qa.modifiers.expandSynonyms)
							break;
						if (!(qa instanceof TermAST))
							break;
						ParserField termField = qa.getField();
						if (!termField.expandSynonyms)
							break;
						String op = qa.getOp();
						if (!termField.isText())
							break;
						if ((synonymField == null) && (!op.equals("-"))) {
							synonymField = termField;
						} else {
							if (synonymField != termField)
								break;
							if ((op != null) && (op.equals("-"))) {
								break;
							}
						}
					}
					int synonymLimit = synonymField != null ? synonymField.schemaField.type.getLongestSynonymTermCount() : 0;
					int lastN = Math.min(n, i + synonymLimit);

					for (int n1 = lastN; n1 >= i + 1; n1--) {
						terms.clear();
						StringBuffer termListText = new StringBuffer();
						int termCount = 0;
						synonymField = null;
						for (int i1 = i; i1 < n1; i1++) {
							QueryAST qa = getClause(i1);
							if (!qa.modifiers.expandSynonyms)
								break;
							if (!(qa instanceof TermAST))
								break;
							ParserField termField = qa.getField();
							if (!termField.expandSynonyms)
								break;
							String op = qa.getOp();
							if (!termField.isText())
								break;
							if ((synonymField == null) && (!op.equals("-"))) {
								synonymField = termField;
							} else {
								if (synonymField != termField)
									break;
								if ((op != null) && (op.equals("-")))
									break;
							}
							if ((op != null) && ((op.equals("+")) || (op.equals("AND"))))
								occur = BooleanClause.Occur.MUST;
							List<String> termWords = qa.getActualWords();
							if (termWords != null) {
								int n2 = termWords.size();
								termCount++;
								if (n2 > 0) {
									for (int j = 0; j < n2; j++) {
										String word = (String) termWords.get(j);
										if (termListText.length() > 0)
											termListText.append(' ');
										termListText.append(word);
									}
								}
							}

							terms.add(qa);
						}

						if (termCount >= 1) {
							synonymIndex = synonymField.getSynonymIndex(termListText.toString());
							if (synonymIndex >= 0) {
								synonymTerms = synonymField.getSynonymTargetTerms(synonymIndex);
								if (synonymTerms != null) {
									isSynonym = true;
									ns = n1;
									break;
								}

							}

						}

					}

				}

				if (!isSynonym) {
					int n2 = isSynonym ? ns : i + 1;
					for (int i2 = i; i2 < n2; i2++) {
						QueryAST qa = getClause(i2);

						String op = qa.getOp();

						if ((parser.implicitNiceToHave) && (!noExplicitOps) && (qa.op == null)) {
							op = null;
						}
						Query q = qa.generateQuery((allowStopWords)
								|| ((op != null) && ((op.equals("+")) || (op.equals("-")) || (isRelOp(op)))));

						occur = null;
						if ((op != null)
								&& ((op.equals("+")) || (op.equals("AND")) || (op.equals("==")) || (op.equals("<"))
										|| (op.equals("<=")) || (op.equals(">")) || (op.equals(">=")))) {
							occur = BooleanClause.Occur.MUST;
							allNegative = false;
						} else if ((op != null) && ((op.equals("-")) || (op.equals("!=")))) {
							occur = BooleanClause.Occur.MUST_NOT;
						} else {
							occur = BooleanClause.Occur.SHOULD;
							allNegative = false;
						}

						if (q != null) {
							int nc = bq.getClauses().length;
							if (nc == parser.maxGenTerms) {
								GaiaQueryParser.LOG.warn("Ignoring term <<" + qa.getTermVal() + ">> since more than "
										+ parser.maxGenTerms + " terms in query");
							} else {
								addBooleanQueryClause(bq, q, occur);

								if (occur == BooleanClause.Occur.SHOULD)
									numOptionalClauses++;
							}
							k++;
						}
					}
				}

				if (isSynonym) {
					BooleanQuery sq = generateSynonym(bq, terms, synonymTerms, synonymField, modifiers.stemWords);
					if (sq != null) {
						allNegative = false;
						k++;
					}

					i = ns - 1;
				}

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

			if (k > 0) {
				if (allNegative) {
					addBooleanQueryClause(bq, generateMatchAllDocsQuery(), BooleanClause.Occur.MUST);
					query = bq;
				} else if (k == 1) {
					query = GaiaQueryParserUtils.collapseSingleBooleanQuery(bq);
				} else {
					query = bq;
				}
			}
		}
		return query;
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
				field = newTerm.field;
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
					List<SpanQuery> spanTerms = new ArrayList<SpanQuery>();

					for (int i = 0; i < nw; i++) {
						String word = (String) words.get(i);

						if ((modifiers.stemWords) && (field.isText())) {
							word = field.stemWord(word);
						}
						SpanTermQuery genTerm = generateSpanTermQuery(field.schemaField, word);
						spanTerms.add(genTerm);
					}

					SpanNearQuery nq = generateSpanNearQuery(spanTerms, 0, true);

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
			if (parser.countGenTerms()) {
				return null;
			}
			ArrayList<SpanQuery> spanTerms = new ArrayList<SpanQuery>();

			int firstNonStopword = -1;
			int lastNonStopword = -1;
			int posIncr = 0;

			for (int i = 0; i < n; i++) {
				ParserTerm sourceTerm = null;
				ParserTerm term = null;

				QueryAST rawClause = getClause(i);
				if (!rawClause.isNegativeOp()) {
					TermAST ta = getTermClause(i);
					if (ta != null)
						sourceTerm = ta.term;
					if (sourceTerm != null)
						term = sourceTerm.filterTerm(true, false, false, false);
					if (term != null) {
						String text = term.text;

						List<QueryAST> terms = new ArrayList<QueryAST>();
						boolean isSynonym = false;
						int synonymIndex = -1;
						List<String> synonymTerms = null;
						ParserField synonymField = field;
						int ns = 0;
						if ((parser.expandSynonyms) && (modifiers.expandSynonyms)) {
							int synonymLimit = synonymField != null ? synonymField.schemaField.type.getLongestSynonymTermCount() : 0;
							int lastN = Math.min(n, i + synonymLimit);

							for (int n1 = lastN; n1 >= i + 1; n1--) {
								terms.clear();
								String termListText = "";
								int termCount = 0;
								for (int i1 = i; i1 < n1; i1++) {
									QueryAST qa = getClause(i1);
									if (!qa.modifiers.expandSynonyms)
										break;
									if (!(qa instanceof TermAST))
										break;
									if (!field.expandSynonyms)
										break;
									op = qa.getOp();
									if (!field.isText())
										break;
									if (qa.isNegativeOp())
										break;
									List<String> termWords = qa.getActualWords();
									if (termWords != null) {
										int n2 = termWords.size();
										termCount++;
										if (n2 > 0) {
											for (int j = 0; j < n2; j++) {
												String word = (String) termWords.get(j);
												if (termListText.length() == 0)
													termListText = word;
												else {
													termListText = termListText + " " + word;
												}
											}
										}
									}
									terms.add(qa);
								}

								if (termCount >= 1) {
									synonymIndex = synonymField.getSynonymIndex(termListText);

									if (synonymIndex >= 0) {
										synonymTerms = synonymField.getSynonymTargetTerms(synonymIndex);
										if (synonymTerms != null) {
											isSynonym = true;
											ns = n1;
											break;
										}

									}

								}

							}

						}

						if (!isSynonym) {
							if ((field.schemaField.type.isStopWord(text)) && (!field.schemaField.type.stopwordsIndexed)) {
								posIncr++;
								continue;
							}

							if (firstNonStopword < 0)
								firstNonStopword = i;
							lastNonStopword = i;

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
								SpanTermQuery genTerm = generateSpanTermQuery(field.schemaField, word);
								spanTerms.add(genTerm);
							} else if (nw > 1) {
								ArrayList<SpanQuery> terms2 = new ArrayList<SpanQuery>();
								for (int j = 0; j < nw; j++) {
									String word = (String) words.get(j);

									if ((modifiers.stemWords) && (field.isText())) {
										word = field.stemWord(word);
									}
									SpanTermQuery genTerm = generateSpanTermQuery(field.schemaField, word);
									terms2.add(genTerm);
								}

								if (sourceTerm.modifiers.hyphenated) {
									SpanQuery nq = generateSpanNearQuery(terms2, 0, true);
									String singleTerm = combineWords(words);
									singleTerm = field.stemWord(singleTerm);
									SpanQuery tq = generateSpanTermQuery(field.schemaField, singleTerm);

									ArrayList<SpanQuery> sc = new ArrayList<SpanQuery>();

									sc.add(nq);
									sc.add(tq);

									SpanOrQuery oq = generateSpanOrQuery(sc);

									spanTerms.add(oq);
								} else {
									spanTerms.addAll(terms2);
								}
							}

						}

						if (isSynonym) {
							SpanQuery sq = generateSynonymSpanQuery(terms, synonymTerms, synonymField, modifiers.stemWords);
							if (sq != null) {
								spanTerms.add(sq);
							}

							i = ns - 1;
						}
					} else {
						QueryAST cl = getClause(i);
						if (cl != null) {
							SpanQuery sq = cl.generateSpanQuery(field);
							if (sq != null) {
								spanTerms.add(sq);
							}
						}
					}

				}

			}

			if (spanTerms.size() > 0) {
				if (spanTerms.size() == 1) {
					q = (SpanQuery) spanTerms.get(0);
				} else {
					int posIncr2 = 0;
					if (field.schemaField.type.stopwordPositionsIncremented)
						posIncr2 = posIncr - firstNonStopword - (n - 1 - lastNonStopword);
					q = generateSpanNearQuery(spanTerms, posIncr2, true);
				}
			}
			if (q != null) {
				float fieldQueryBoost = 1.0F;
				QueryAST cl = getTermClause(0);
				if ((cl != null) && ((cl instanceof TermAST))) {
					TermAST clt = (TermAST) cl;
					ParserTerm t = clt.term;
					if (t != null)
						fieldQueryBoost = t.field.queryBoost;
				}
				if (haveBoost)
					q.setBoost(boost * fieldQueryBoost);
				else {
					q.setBoost(fieldQueryBoost);
				}
			}
		}

		return q;
	}

	public Query addRelevance(Query query) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return query;
		}
		if (!isNegativeOp()) {
			int n = size();

			if (parser.boostBigrams) {
				for (int i = 0; i < n - 1; i++)
					query = addBigramRelevance(query, getClause(i), getClause(i + 1));
			}
			if (parser.boostTrigrams) {
				for (int i = 0; i < n - 2; i++)
					query = addTrigramRelevance(query, getClause(i), getClause(i + 1), getClause(i + 2));
			}
			for (int i = 0; (i < n) && (!parser.maxGenTermsExceeded); i++) {
				QueryAST clause1 = getClause(i);
				if ((clause1 instanceof QueryAST)) {
					query = clause1.addRelevance(query);
				}
			}
		}
		return query;
	}
}
