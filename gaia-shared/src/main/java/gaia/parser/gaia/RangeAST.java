package gaia.parser.gaia;

import java.util.ArrayList;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;

class RangeAST extends QueryAST {
	public String lowerVal = null;
	public String upperVal = null;
	public boolean includeLower = true;
	public boolean includeUpper = true;
	public boolean quotedLowerVal = false;
	public boolean quotedUpperVal = false;

	public RangeAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		super(parser, modifiers);
	}

	public void dump(int level) {
		Class queryClass = null;
		String className = "null";
		queryClass = getClass();
		className = queryClass.getSimpleName();

		String dots = "";
		for (int i = 0; i <= level; i++) {
			dots = new StringBuilder().append(dots).append("..").toString();
		}
		GaiaQueryParser.LOG.info(new StringBuilder().append(dots).append(" AST L").append(level).append(": ")
				.append(className).append(" op: ").append(op).append(" clauseOp: ").append(clauseOp).append(" range ")
				.append(modifiers.field.fieldName()).append(":").append(includeLower ? "[" : "{")
				.append(lowerVal != null ? new StringBuilder().append("\"").append(lowerVal).append("\"").toString() : "*")
				.append(" TO ")
				.append(upperVal != null ? new StringBuilder().append("\"").append(upperVal).append("\"").toString() : "*")
				.append(includeUpper ? "]" : "}").append(" lower: ").append(quotedLowerVal ? "quoted" : "not quoted")
				.append(", upper: ").append(quotedUpperVal ? "quoted" : "not quoted")
				.append(haveBoost ? new StringBuilder().append(" boost: ").append(boost).toString() : "").toString());
	}

	public Query generateQuery() {
		ParserField field = modifiers.field;

		Query q = null;
		if ((field.isDefault) || (field.isAll)) {
			Query firstQ = null;
			DisjunctionMaxQuery bq = generateDisjunctionMaxQuery(parser.tieBreakerMultiplier);

			ArrayList qf = field.isDefault ? parser.queryFields : parser.allFields;
			int n = qf.size();
			int k = 0;
			for (int i = 0; (i < n) && (!parser.countGenTerms()); i++) {
				ParserField field1 = (ParserField) qf.get(i);
				Query rq = generateQuery(field1);
				if (rq != null) {
					rq.setBoost(field.isDefault ? field1.queryBoost : field1.allQueryBoost);
					bq.add(rq);
					if (k == 0)
						firstQ = rq;
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
			q = generateQuery(field);
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
		return q;
	}

	public Query generateQuery(ParserField field) {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		Query q = null;

		q = generateRangeQuery(field, modifiers, lowerVal, upperVal, quotedLowerVal, quotedUpperVal, includeLower,
				includeUpper);

		return q;
	}
}
