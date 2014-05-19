package gaia.parser.gaia;

import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;

class FuzzyStringAST extends FuzzyAST {
	public FuzzyStringAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		super(parser, modifiers);
	}

	public Query generateQuery(boolean allowStopWords) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		Query q = null;

		if (term != null) {
			if (parser.countGenTerms()) {
				return null;
			}
			String text = term.text;

			if (text.length() == 0) {
				return q;
			}
			q = generateFuzzyQuery(term.field, text, minimumSimilarity);
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
