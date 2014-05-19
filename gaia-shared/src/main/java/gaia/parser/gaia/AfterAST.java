package gaia.parser.gaia;

import java.io.IOException;
import org.apache.lucene.search.Query;

class AfterAST extends ProximityAST {
	public AfterAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		super(parser, modifiers, true, true);
	}

	public QueryAST getLastParserTerm() {
		if (size() > 0) {
			return getClause(0).getLastParserTerm();
		}
		return null;
	}

	public QueryAST getParserTerm() {
		int n = size();
		if (n > 0) {
			return getClause(n - 1).getParserTerm();
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

			QueryAST term1 = cl2.getLastParserTerm();
			QueryAST term2 = cl1.getParserTerm();

			if ((term1 != null) && (term2 != null)) {
				query = addBigramRelevance(query, term1, term2);
			}
		}
		return query;
	}
}
