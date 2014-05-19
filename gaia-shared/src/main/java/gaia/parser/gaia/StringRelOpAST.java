package gaia.parser.gaia;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;

class StringRelOpAST extends TermAST {
	public StringRelOpAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		super(parser, modifiers);
	}

	public Query generateQuery(boolean allowStopWords) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		Query q = null;
		String lowerVal = null;
		String upperVal = null;
		ParserField field = term.field;
		boolean includeLower = false;
		boolean includeUpper = false;

		if (field.isDate()) {
			if ((op.equals("==")) || (op.equals("!="))) {
				lowerVal = getMinTermVal();
				upperVal = getMaxTermVal();
				includeUpper = true;
				includeLower = true;
			} else if (op.equals("<")) {
				upperVal = getMinTermVal();
				includeUpper = false;
				includeLower = true;
			} else if (op.equals("<=")) {
				upperVal = getMaxTermVal();
				includeUpper = true;
				includeLower = true;
			} else if (op.equals(">")) {
				lowerVal = getMaxTermVal();
				includeUpper = true;
				includeLower = false;
			} else if (op.equals(">=")) {
				lowerVal = getMinTermVal();
				includeUpper = true;
				includeLower = true;
			}
		} else {
			if ((op.equals("==")) || (op.equals("!=")))
				return super.generateQuery(allowStopWords);
			if (op.equals("<")) {
				upperVal = getTermVal();
				includeLower = true;
				includeUpper = false;
			} else if (op.equals("<=")) {
				upperVal = getTermVal();
				includeLower = true;
				includeUpper = true;
			} else if (op.equals(">")) {
				lowerVal = getTermVal();
				includeLower = false;
				includeUpper = true;
			} else if (op.equals(">=")) {
				lowerVal = getTermVal();
				includeLower = true;
				includeUpper = true;
			}
		}

		if ((lowerVal != null) || (upperVal != null)) {
			if ((field.isDefault) || (field.isAll)) {
				Query firstQ = null;
				DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

				ArrayList qf = field.isDefault ? parser.queryFields : parser.allFields;
				int n = qf.size();
				int k = 0;
				for (int i = 0; (i < n) && (!parser.countGenTerms()); i++) {
					ParserField field1 = (ParserField) qf.get(i);
					Query rq = generateRangeQuery(field, modifiers, lowerVal, upperVal, false, false, includeLower, includeUpper);
					rq.setBoost(field.isDefault ? field1.queryBoost : field1.allQueryBoost);
					bq.add(rq);
					if (k == 0)
						firstQ = rq;
					k++;
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
				q = generateRangeQuery(field, modifiers, lowerVal, upperVal, false, false, includeLower, includeUpper);
			}

			if (q != null) {
				float fieldQueryBoost = field.queryBoost;
				if ((field != null) && ((field.isAll) || (field.isDefault)) && (!(q instanceof DisjunctionMaxQuery)))
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
}
