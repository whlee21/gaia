package gaia.parser.gaia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanQuery;

abstract class ProximityAST extends PhraseAST {
	boolean inOrder = false;
	boolean reverseOrder = false;

	public ProximityAST(GaiaQueryParser parser, ParserTermModifiers modifiers, boolean inOrder, boolean reverseOrder) {
		super(parser, modifiers);
		this.inOrder = inOrder;
		this.reverseOrder = reverseOrder;
		proximitySlop = parser.nearSlop;
	}

	public Query generateQuery() throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		Query q = null;

		int n = size();

		ParserField field = null;
		QueryAST termAST = null;
		if (n > 0)
			termAST = getClause(0).getParserTerm();
		if (termAST != null) {
			field = termAST.getField();
		}
		if (field != null) {
			if ((field.isDefault) || (field.isAll)) {
				Query firstQ = null;
				DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

				ArrayList qf = field.isDefault ? parser.queryFields : parser.allFields;
				int nq = qf.size();
				int k = 0;
				for (int i = 0; (i < nq) && (!parser.countGenTerms()); i++) {
					ParserField field1 = (ParserField) qf.get(i);

					Query pq = generateOneQuery(field1);

					if (pq != null) {
						float extraBoost = 1.0F;
						if (field1.isAll)
							extraBoost = field1.allQueryBoost;
						else
							extraBoost = field1.queryBoost;
						if (haveBoost)
							pq.setBoost(boost * extraBoost);
						else
							pq.setBoost(extraBoost);
						bq.add(pq);
						if (k == 0)
							firstQ = pq;
						k++;

						if (k == 0)
							q = null;
						else if (k == 1)
							q = firstQ;
						else
							q = bq;
					}
				}
			} else {
				q = generateOneQuery(field);
			}

		}

		if ((q != null) && (field != null)) {
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

	public Query generateOneQuery(ParserField field) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		if (field == null) {
			return null;
		}
		if (parser.countGenTerms()) {
			return null;
		}
		return generateSpanQuery(field);
	}

	public SpanQuery generateSpanQuery(ParserField field) throws IOException {
		if (field == null) {
			return null;
		}
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		SpanQuery q = null;

		int n = size();

		ArrayList<SpanQuery> spanQueries = new ArrayList<SpanQuery>();

		int ip = 0;
		if (reverseOrder) {
			ip = n - 1;
		}
		while (reverseOrder ? ip >= 0 : ip < n) {
			SpanQuery sq = getClause(ip).generateSpanQuery(field);
			if (sq != null) {
				spanQueries.add(sq);
			}
			ip += (reverseOrder ? -1 : 1);
		}

		if (spanQueries.size() == 0) {
			return null;
		}
		q = generateSpanNearQuery(spanQueries, proximitySlop, inOrder);

		List<TermAST> spanExcludeTerms = getNegativeTerms();

		int numExcludes = spanExcludeTerms == null ? 0 : spanExcludeTerms.size();
		if (numExcludes > 0) {
			SpanQuery spanExcludesQuery = null;
			if (numExcludes == 1) {
				spanExcludesQuery = ((TermAST) spanExcludeTerms.get(0)).generateSpanQuery(field);
			} else {
				ArrayList<SpanQuery> spanTerms = new ArrayList<SpanQuery>();

				for (int i = 0; i < numExcludes; i++) {
					SpanQuery sq = ((TermAST) spanExcludeTerms.get(i)).generateSpanQuery(field);
					if (sq != null) {
						spanTerms.add(sq);
					}
				}

				if (spanTerms.size() == 0) {
					return null;
				}
				spanExcludesQuery = generateSpanOrQuery(spanTerms);
			}

			SpanNotQuery spanNotQuery = generateSpanNotQuery(q, spanExcludesQuery);
			q = spanNotQuery;
		}

		float extraBoost = 1.0F;
		extraBoost = field.queryBoost;
		if (haveBoost)
			q.setBoost(boost * extraBoost);
		else {
			q.setBoost(extraBoost);
		}
		return q;
	}

	public ParserField getField() {
		if (size() >= 1) {
			QueryAST qa = getClause(0);
			if (qa != null) {
				return qa.getField();
			}
			return null;
		}
		return null;
	}

	public List<String> getWords() throws IOException {
		List<String> words = new ArrayList<String>();

		int n = size();
		for (int i = 0; i < n; i++) {
			QueryAST qa = getClause(i);
			List<String> words2 = qa.getWords();
			if (words2 != null) {
				words.addAll(words2);
			}

		}

		return words;
	}

	public List<String> getActualWords() throws IOException {
		List<String> words = new ArrayList<String>();

		int n = size();
		for (int i = 0; i < n; i++) {
			QueryAST qa = getClause(i);
			if ((qa instanceof TermAST)) {
				TermAST ta = (TermAST) qa;
				ParserTerm term = ta.term.filterTerm(true, true, false, true);
				words.add(term.text);
			} else if ((qa instanceof PhraseAST)) {
				PhraseAST pa = (PhraseAST) qa;
				List<String> phraseWords = pa.getActualWords();
				int n1 = phraseWords.size();
				for (int j = 0; j < n1; j++) {
					words.add(phraseWords.get(j));
				}
			}
		}
		return words;
	}

	public QueryAST getLastParserTerm() {
		int n = size();
		if (n > 0) {
			return getClause(n - 1).getLastParserTerm();
		}
		return null;
	}

	public QueryAST getParserTerm() {
		if (size() > 0) {
			return getClause(0).getParserTerm();
		}
		return null;
	}

	public Query addRelevance(Query query) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return query;
		}
		int n = size();

		if (n < 2) {
			return query;
		}
		for (int i = 0; i < n; i++) {
			if (parser.maxGenTermsExceeded) {
				return query;
			}
			QueryAST cl = getClause(i);

			query = cl.addRelevance(query);
		}

		for (int i = 0; (i < n - 1) && (!parser.maxGenTermsExceeded); i++) {
			QueryAST cl1 = getClause(i);
			QueryAST cl2 = getClause(i + 1);

			QueryAST term1 = cl1.getLastParserTerm();
			QueryAST term2 = cl2.getParserTerm();

			if ((term1 != null) && (term2 != null)) {
				query = addBigramRelevance(query, term1, term2);
			}
		}
		return query;
	}
}
