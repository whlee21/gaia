package gaia.parser.gaia;

public class GaiaASTGenerator {
	public AfterAST newAfterAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new AfterAST(parser, modifiers);
	}

	public BeforeAST newBeforeAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new BeforeAST(parser, modifiers);
	}

	public ClauseListAST newClauseListAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new ClauseListAST(parser, modifiers);
	}

	public FuzzyAST newFuzzyAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new FuzzyAST(parser, modifiers);
	}

	public FuzzyStringAST newFuzzyStringAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new FuzzyStringAST(parser, modifiers);
	}

	public NearAST newNearAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new NearAST(parser, modifiers);
	}

	public QueryAST newNotAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new NotAST(parser, modifiers);
	}

	public PhraseAST newPhraseAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new PhraseAST(parser, modifiers);
	}

	public QuotedStringAST newQuotedStringAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new QuotedStringAST(parser, modifiers);
	}

	public RangeAST newRangeAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new RangeAST(parser, modifiers);
	}

	public RelOpAST newRelOpAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new RelOpAST(parser, modifiers);
	}

	public StringAST newStringAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new StringAST(parser, modifiers);
	}

	public StringRelOpAST newStringRelOpAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new StringRelOpAST(parser, modifiers);
	}

	public TermAST newTermAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new TermAST(parser, modifiers);
	}

	public TermListAST newTermListAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		return new TermListAST(parser, modifiers);
	}
}
