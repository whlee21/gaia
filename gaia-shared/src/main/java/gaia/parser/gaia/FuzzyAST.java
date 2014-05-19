package gaia.parser.gaia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;

class FuzzyAST extends TermAST {
	public float minimumSimilarity = 2.0F;

	public FuzzyAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		super(parser, modifiers);
		minimumSimilarity = parser.defaultMinimumSimilarity;
	}

	public Query generateQuery(boolean allowStopWords) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		Query q = null;

		float minSim = minimumSimilarity >= 1.0F ? (int) minimumSimilarity : Math.max(0.0F,
				Math.min(0.999F, minimumSimilarity));

		if (term != null) {
			ParserTerm newTerm = term.filterTerm(allowStopWords, true, modifiers.stemWords, false);
			if (newTerm == null) {
				return q;
			}
			ParserField field = newTerm.field;
			String text = newTerm.text;

			if (text.length() == 0) {
				return q;
			}
			ArrayList<String> words = term.splitWords(term.field, text, false);
			int n = words.size();
			if (n == 1)
				q = generateFuzzyQuery(field, text, minSim);
			else if (n > 1) {
				if (parser.spanFuzzy) {
					if ((field.isDefault) || (field.isAll)) {
						SpanQuery firstQ = null;
						DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

						ArrayList<ParserField> qf = field.isDefault ? parser.queryFields : parser.allFields;
						int nf = qf.size();
						int k = 0;
						for (int fi = 0; fi < nf; fi++) {
							ParserField field1 = (ParserField) qf.get(fi);

							List<SpanQuery> clauses = new ArrayList<SpanQuery>();
							for (int i = 0; i < n; i++) {
								String word = (String) words.get(i);
								if (word.length() > 0) {
									SpanQuery fq = generateSpanFuzzyQuery(field1.fieldName(), word, minSim);
									clauses.add(fq);
								}
							}

							SpanQuery nq = generateSpanNearQuery(clauses);
							bq.add(nq);
							if (k == 0)
								firstQ = nq;
							k++;
						}
						if (k == 0)
							q = null;
						else if (k == 1)
							q = firstQ;
						else
							q = bq;
					} else {
						List<SpanQuery> clauses = new ArrayList<SpanQuery>();
						for (int i = 0; i < n; i++) {
							String word = (String) words.get(i);
							if (word.length() > 0) {
								SpanQuery fq = generateSpanFuzzyQuery(field.fieldName(), word, minSim);
								clauses.add(fq);
							}
						}

						q = generateSpanNearQuery(clauses);
					}
				} else {
					BooleanQuery bq = generateBooleanQuery();
					for (int i = 0; i < n; i++) {
						String word = (String) words.get(i);
						Query fq = generateFuzzyQuery(field, word, minSim);
						bq.add(fq, BooleanClause.Occur.MUST);
					}
					q = bq;
				}
			} else {
				q = null;
			}
		}

		if (q != null) {
			ParserField field = term.field;
			float fieldQueryBoost = field.queryBoost;
			if ((field != null) && ((field.isAll) || (field.isDefault)) && (!(q instanceof DisjunctionMaxQuery)))
				fieldQueryBoost = q.getBoost();
			if (haveBoost)
				q.setBoost(boost * fieldQueryBoost);
			else {
				q.setBoost(fieldQueryBoost);
			}
		}
		return q;
	}
}
