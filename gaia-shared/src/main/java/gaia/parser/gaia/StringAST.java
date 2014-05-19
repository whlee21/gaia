package gaia.parser.gaia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.search.FunctionQParserPlugin;
import org.apache.solr.search.QParser;

class StringAST extends TermAST {
	boolean quoted = false;

	public StringAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		super(parser, modifiers);
	}

	public Query generateQuery(boolean allowStopWords) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		Query q = null;
		boolean boosted = false;

		if (term != null) {
			ParserField field = term.field;
			String text = term.text;

			if ((term.modifiers.isAllStarWild) && (modifiers.allFields)) {
				if (parser.countGenTerms()) {
					return null;
				}
				q = generateMatchAllDocsQuery();
			} else if (field.isDate()) {
				if (parser.countGenTerms()) {
					return null;
				}
				String fieldName = field.fieldName();
				if (((field.isAll) || (field.isDefault)) && (field.schemaField != null)) {
					fieldName = field.schemaField.getName();
				}
				text = getCleanDateVal();
				boolean isWild = (text.contains("*")) || (text.contains("?"));

				boolean doExactDateMatch = false;
				if (text.length() >= 19)
					doExactDateMatch = true;
				else if ((text.length() >= 3) && (text.substring(0, 3).equalsIgnoreCase("now"))) {
					doExactDateMatch = true;
				}
				boolean allowWildcardInDate = true;

				text = checkLeadWild(text, modifiers);

				if ((allowWildcardInDate) && (isWild)) {
					String prefix = text.substring(0, text.length() - 1);

					boolean useMatchAll = false;

					if (term.modifiers.isAllStarWild) {
						if ((useMatchAll) || (modifiers.allFields))
							q = generateMatchAllDocsQuery();
						else
							q = generatePrefixQuery(fieldName, "", term.modifiers.termCountHardCutoff);
					} else if (term.modifiers.hasAllStarWildSuffix)
						q = generatePrefixQuery(fieldName, prefix, term.modifiers.termCountHardCutoff);
					else if (text.length() > 0)
						q = generateWildcardQuery(field.schemaField, text, term.modifiers.termCountHardCutoff);
				} else if (doExactDateMatch) {
					String originalText = text;
					ParserTerm newTerm = new ParserTerm(parser, modifiers, text);
					newTerm = newTerm.filterTerm(allowStopWords, true, false, false);
					if (newTerm == null)
						return null;
					text = newTerm.text;

					if ((text.length() == 19) && (originalText.length() >= 3)
							&& (originalText.substring(0, 3).equalsIgnoreCase("NOW")))
						text = text + ".000";
					else if (text.length() == 21)
						text = text + "00";
					else if (text.length() == 22)
						text = text + "0";
					q = generateTermQuery(fieldName, text);
				} else {
					String lowerVal = getMinTermVal();
					String upperVal = getMaxTermVal();
					boolean includeLower = true;
					boolean includeUpper = true;

					if ((lowerVal.length() >= 3) && (lowerVal.substring(0, 3).equalsIgnoreCase("NOW"))) {
						if (lowerVal.length() == 19)
							lowerVal = lowerVal + ".000";
						else if (lowerVal.length() == 21)
							lowerVal = lowerVal + "00";
						else if (lowerVal.length() == 22) {
							lowerVal = lowerVal + "0";
						}
					}

					if ((upperVal.length() >= 3) && (upperVal.substring(0, 3).equalsIgnoreCase("NOW"))) {
						if (upperVal.length() == 19)
							upperVal = upperVal + ".000";
						else if (upperVal.length() == 21)
							upperVal = upperVal + "00";
						else if (upperVal.length() == 22) {
							upperVal = upperVal + "0";
						}
					}

					q = generateRangeQuery(field, modifiers, lowerVal, upperVal, false, false, includeLower, includeUpper);
				}

				if ((q != null) && ((field.isAll) || (field.isDefault))) {
					ParserField field1 = null;
					if (field.isAll)
						field1 = (ParserField) parser.allFields.get(0);
					else {
						field1 = (ParserField) parser.queryFields.get(0);
					}
					q.setBoost(field.isDefault ? field1.queryBoost : field1.allQueryBoost);
					boosted = true;
				}
			} else if (field.isVal) {
				if (parser.countGenTerms())
					return null;
				try {
					QParser nested = parser.qParser.subQuery(text, FunctionQParserPlugin.NAME);
					q = nested.getQuery();
				} catch (Exception e) {
					String msg = e.getMessage();
					String logMsg = "_val_ got exception for <<" + text + ">>: " + msg;
					GaiaQueryParser.LOG.warn(logMsg, e);
					q = null;
				}
			} else if ((field.isDefault) || (field.isAll)) {
				Query firstQ = null;
				DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);
				int k = 0;

				ArrayList qf = field.isDefault ? parser.queryFields : parser.allFields;
				int n = qf.size();
				for (int i = 0; i < n; i++) {
					ParserField field1 = (ParserField) qf.get(i);
					String fieldName = field1.fieldName();

					if ((!modifiers.synonymExpansion) || (field1.expandSynonyms)) {
						List<String> words = null;
						if ((!quoted) && ((text.contains("*")) || (text.contains("?")))) {
							words = new ArrayList<String>();
							words.add(text);
						} else {
							words = field1.analyze(text);
						}

						int n1 = words.size();
						if (n1 == 1) {
							if (parser.countGenTerms()) {
								return null;
							}
							text = checkLeadWild(text, modifiers);

							if ((!quoted) && (term.modifiers.isWild)) {
								String prefix = text.substring(0, text.length() - 1);
								boolean useMatchAll = false;

								if (term.modifiers.isAllStarWild) {
									if ((useMatchAll) || (modifiers.allFields))
										q = generateMatchAllDocsQuery();
									else
										q = generatePrefixQuery(fieldName, "", term.modifiers.termCountHardCutoff);
								} else if (term.modifiers.hasAllStarWildSuffix)
									q = generatePrefixQuery(fieldName, prefix, term.modifiers.termCountHardCutoff);
								else
									q = generateWildcardQuery(field1.schemaField, text, term.modifiers.termCountHardCutoff);
							} else if (text.length() > 0) {
								q = generateTermQuery(fieldName, text);
							}
						} else if (n1 > 1) {
							PhraseQuery pq = generatePhraseQuery();

							for (int i1 = 0; (i1 < n1) && (!parser.countGenTerms()); i1++) {
								pq.add(generateTerm(fieldName, (String) words.get(i1)));
							}

							q = pq;
						} else {
							q = null;
						}
						if (q != null) {
							q.setBoost(field.isDefault ? field1.queryBoost : field1.allQueryBoost);
							bq.add(q);
							if (k == 0)
								firstQ = q;
							k++;
						}
					}
				}
				if (k == 0) {
					q = null;
				} else if (k == 1) {
					q = firstQ;
					boosted = true;
				} else {
					q = bq;
				}
			} else {
				if (parser.countGenTerms()) {
					return null;
				}
				String fieldName = field.fieldName();

				List<String> words = null;
				if ((!quoted) && (term.modifiers.isWild)) {
					words = new ArrayList<String>();
					words.add(text);
				} else {
					words = field.analyze(text);
				}

				int n = words.size();
				if (n == 1) {
					text = (String) words.get(0);
					text = checkLeadWild(text, modifiers);

					if ((!quoted) && (term.modifiers.isWild)) {
						String prefix = text.substring(0, text.length() - 1);
						boolean useMatchAll = false;

						if (term.modifiers.isAllStarWild) {
							if ((useMatchAll) || (modifiers.allFields))
								q = generateMatchAllDocsQuery();
							else
								q = generatePrefixQuery(fieldName, "", term.modifiers.termCountHardCutoff);
						} else if (term.modifiers.hasAllStarWildSuffix)
							q = generatePrefixQuery(fieldName, prefix, term.modifiers.termCountHardCutoff);
						else
							q = generateWildcardQuery(field.schemaField, text, term.modifiers.termCountHardCutoff);
					} else if (text.length() > 0) {
						q = generateTermQuery(fieldName, text);
					}
				} else if (n > 1) {
					PhraseQuery pq = generatePhraseQuery();

					for (int i = 0; i < n; i++) {
						pq.add(generateTerm(fieldName, (String) words.get(i)));
					}
					q = pq;
				}
			}

			if (q != null) {
				float fieldQueryBoost = field.queryBoost;
				if ((field != null) && ((field.isAll) || (field.isDefault)) && (!(q instanceof DisjunctionMaxQuery))
						&& (boosted))
					fieldQueryBoost = q.getBoost();
				if (haveBoost)
					q.setBoost(boost * fieldQueryBoost);
				else {
					q.setBoost(fieldQueryBoost);
				}
			}
		}
		return q;
	}

	public Term getTerm() {
		return term.getTerm();
	}

	public List<String> getWords() {
		return getActualWords();
	}

	public List<String> getActualWords() {
		if (term != null) {
			List<String> words = new ArrayList<String>();
			String text = getMinTermVal();
			words.add(text);
			return words;
		}
		return null;
	}
}
