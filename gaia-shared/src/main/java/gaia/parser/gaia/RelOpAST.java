package gaia.parser.gaia;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;

class RelOpAST extends TermAST {
	public RelOpAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		super(parser, modifiers);
	}

	public Query generateQuery(boolean allowStopWords) throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		Query q = null;
		String genLowerVal = null;
		String genUpperVal = null;
		ParserField field = term.field;
		String text = null;
		boolean includeLower = false;
		boolean includeUpper = false;

		if (field.isDate()) {
			if ((op.equals("==")) || (op.equals("!="))) {
				ParserTerm tempLowerTerm = new ParserTerm(parser, modifiers, getMinTermVal());
				ParserTerm lowerTerm = tempLowerTerm.filterTerm(true, true, modifiers.stemWords, false);
				if (lowerTerm != null) {
					genLowerVal = lowerTerm.text;
				}
				ParserTerm tempUpperTerm = new ParserTerm(parser, modifiers, getMaxTermVal());
				ParserTerm upperTerm = tempUpperTerm.filterTerm(true, true, modifiers.stemWords, false);
				if (upperTerm != null) {
					genUpperVal = upperTerm.text;
				}
				includeUpper = true;
				includeLower = true;
			} else if (op.equals("<")) {
				ParserTerm tempUpperTerm = new ParserTerm(parser, modifiers, getMinTermVal());
				ParserTerm upperTerm = tempUpperTerm.filterTerm(true, true, modifiers.stemWords, false);
				if (upperTerm != null) {
					genUpperVal = upperTerm.text;
				}
				includeUpper = false;
				includeLower = true;
			} else if (op.equals("<=")) {
				ParserTerm tempUpperTerm = new ParserTerm(parser, modifiers, getMaxTermVal());
				ParserTerm upperTerm = tempUpperTerm.filterTerm(true, true, modifiers.stemWords, false);
				if (upperTerm != null) {
					genUpperVal = upperTerm.text;
				}
				includeUpper = true;
				includeLower = true;
			} else if (op.equals(">")) {
				ParserTerm tempLowerTerm = new ParserTerm(parser, modifiers, getMaxTermVal());
				ParserTerm lowerTerm = tempLowerTerm.filterTerm(true, true, modifiers.stemWords, false);
				if (lowerTerm != null) {
					genLowerVal = lowerTerm.text;
				}
				includeUpper = true;
				includeLower = false;
			} else if (op.equals(">=")) {
				ParserTerm tempLowerTerm = new ParserTerm(parser, modifiers, getMinTermVal());
				ParserTerm lowerTerm = tempLowerTerm.filterTerm(true, true, modifiers.stemWords, false);
				if (lowerTerm != null) {
					genLowerVal = lowerTerm.text;
				}
				includeUpper = true;
				includeLower = true;
			}
		} else {
			if ((op.equals("==")) || (op.equals("!=")))
				return super.generateQuery(allowStopWords);
			if (op.equals("<")) {
				ParserTerm newTerm = term.filterTerm(true, true, modifiers.stemWords, false);
				if (newTerm != null) {
					text = newTerm.text;

					text = collapseWords(text);

					genUpperVal = text;
					includeLower = true;
					includeUpper = false;
				}
			} else if (op.equals("<=")) {
				ParserTerm newTerm = term.filterTerm(true, true, modifiers.stemWords, false);
				if (newTerm != null) {
					text = newTerm.text;

					text = collapseWords(text);

					genUpperVal = text;
					includeLower = true;
					includeUpper = true;
				}
			} else if (op.equals(">")) {
				ParserTerm newTerm = term.filterTerm(true, true, modifiers.stemWords, false);
				if (newTerm != null) {
					text = newTerm.text;

					text = collapseWords(text);

					genLowerVal = text;
					includeLower = false;
					includeUpper = true;
				}
			} else if (op.equals(">=")) {
				ParserTerm newTerm = term.filterTerm(true, true, modifiers.stemWords, false);
				if (newTerm != null) {
					text = newTerm.text;

					text = collapseWords(text);

					genLowerVal = text;
					includeLower = true;
					includeUpper = true;
				}
			}
		}

		if ((genLowerVal != null) || (genUpperVal != null)) {
			if ((field.isDefault) || (field.isAll)) {
				Query firstQ = null;
				DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

				ArrayList qf = field.isDefault ? parser.queryFields : parser.allFields;
				int n = qf.size();
				int k = 0;
				for (int i = 0; (i < n) && (!parser.countGenTerms()); i++) {
					ParserField field1 = (ParserField) qf.get(i);
					Query rq = generateRangeQuery(field1.schemaField, genLowerVal, genUpperVal, includeLower, includeUpper);
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
				q = generateRangeQuery(field.schemaField, genLowerVal, genUpperVal, includeLower, includeUpper);
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
