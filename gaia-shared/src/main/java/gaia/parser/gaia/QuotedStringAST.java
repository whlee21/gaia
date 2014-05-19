package gaia.parser.gaia;

class QuotedStringAST extends StringAST {
	public QuotedStringAST(GaiaQueryParser parser, ParserTermModifiers modifiers) {
		super(parser, modifiers);
		quoted = true;
	}
}
