package gaia.search.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;

public class SpanFuzzyQuery extends SpanMultiTermQueryWrapper<FuzzyQuery> {
	final float minSimilarity;

	public SpanFuzzyQuery(Term term, float minimumSimilarity, int prefixLength) {
		super(new FuzzyQuery(term, FuzzyQuery.floatToEdits(minimumSimilarity,
				term.text().codePointCount(0, term.text().length())), prefixLength));

		minSimilarity = minimumSimilarity;
	}

	public SpanFuzzyQuery(Term term, float minimumSimilarity) throws IllegalArgumentException {
		super(new FuzzyQuery(term, FuzzyQuery.floatToEdits(minimumSimilarity,
				term.text().codePointCount(0, term.text().length()))));

		minSimilarity = minimumSimilarity;
	}

	public SpanFuzzyQuery(Term term) {
		super(new FuzzyQuery(term));
		minSimilarity = 2.0F;
	}

	public Term getTerm() {
		return ((FuzzyQuery) query).getTerm();
	}

	public float getMinSimilarity() {
		return minSimilarity;
	}

	public int getPrefixLength() {
		return ((FuzzyQuery) query).getPrefixLength();
	}
}
