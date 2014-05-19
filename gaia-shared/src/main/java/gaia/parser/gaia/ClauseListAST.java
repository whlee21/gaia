package gaia.parser.gaia;

import java.io.IOException;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

class ClauseListAST extends QueryAST {
	public ClauseListAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		super(parser, modifiers);
	}

	public Query generateQuery() throws IOException {
		if (parser.maxGenTermsExceeded) {
			return null;
		}
		int n = size();
		Query query = null;
		int numClauses = 0;

		if ((n == 1) && (getClause(0).clauseOp == null)) {
			query = getClause(0).generateQuery();
		} else if (n > 0) {
			BooleanQuery bq = null;
			boolean allNegative = true;
			boolean sawClause = false;

			BooleanClause.Occur prevOccur = null;

			for (int i = 0; (i < n) && (!parser.maxGenTermsExceeded); i++) {
				boolean prevAllNegative = allNegative;
				boolean prevSawClause = sawClause;

				QueryAST cl = getClause(i);

				String op = cl.getClauseOp();
				BooleanClause.Occur occur = null;
				BooleanClause.Occur leftOccur = null;
				if ((op.equals("OR")) || (op.equals("||"))) {
					occur = BooleanClause.Occur.SHOULD;
					leftOccur = occur;
					allNegative = false;
				} else if ((op.equals("+")) || (op.equals("AND")) || (op.equals("&&"))) {
					occur = BooleanClause.Occur.MUST;
					leftOccur = occur;
					allNegative = false;
				} else if ((op.equals("-")) || (op.equals("&&!")) || (op.equals("!"))) {
					if (((cl instanceof TermListAST)) || ((cl instanceof ClauseListAST)) || (cl.size() < 2) || (i > 0))
						occur = BooleanClause.Occur.MUST_NOT;
					else
						occur = BooleanClause.Occur.MUST;
					leftOccur = BooleanClause.Occur.MUST;
				} else if (op.equals("||!")) {
					occur = BooleanClause.Occur.SHOULD;
					leftOccur = BooleanClause.Occur.SHOULD;
				}

				Query q = cl.generateQuery();

				sawClause = true;

				if (q != null) {
					if (((occur != prevOccur) && (parser.leftToRightPrec)) || (bq == null)) {
						if ((prevAllNegative) && (prevSawClause) && (bq != null)) {
							addBooleanQueryClause(bq, generateMatchAllDocsQuery(), BooleanClause.Occur.MUST);
						}
						BooleanQuery newBq = generateBooleanQuery();

						if (leftOccur == BooleanClause.Occur.MUST_NOT) {
							leftOccur = BooleanClause.Occur.MUST;
						}
						if (bq != null) {
							Query q1 = GaiaQueryParserUtils.collapseSingleBooleanQuery(bq);

							q1 = GaiaQueryParserUtils.collapseSingleBooleanQuery(q1);

							addBooleanQueryClause(newBq, q1, leftOccur);
						}

						bq = newBq;
					}

					numClauses++;
					if (numClauses > parser.maxGenTerms)
						GaiaQueryParser.LOG.warn("Ignoring term <<" + cl.getTermVal() + ">> since more than " + parser.maxGenTerms
								+ " terms in query");
					else
						addBooleanQueryClause(bq, q, occur);
					prevOccur = occur;
				}
			}

			if ((allNegative) && (sawClause)) {
				addBooleanQueryClause(bq, generateMatchAllDocsQuery(), BooleanClause.Occur.MUST);
			}

			if (bq != null) {
				query = GaiaQueryParserUtils.collapseSingleBooleanQuery(bq);
			}
			if ((query != null) && (haveBoost)) {
				query.setBoost(boost);
			}
		}
		return query;
	}
}
