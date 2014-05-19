package gaia.parser.gaia;

class FilteredParserTerm {
	String text;
	ParserTermModifiers modifiers;
	String filteredText;
	ParserTerm filteredTerm;
	boolean keepStopWords;
	boolean keepWildcards;
	boolean stem;
	boolean keepEmbeddedPunctuation;

	public FilteredParserTerm(GaiaQueryParser parser, ParserTermModifiers modifiers, String text, String filteredText,
			boolean keepStopWords, boolean keepWildcards, boolean stem, boolean keepEmbeddedPunctuation) {
		this.modifiers = modifiers;
		this.text = text;
		this.filteredText = filteredText;
		this.keepStopWords = keepStopWords;
		this.keepWildcards = keepWildcards;
		this.stem = stem;
		this.keepEmbeddedPunctuation = keepEmbeddedPunctuation;

		if ((filteredText == null) || (filteredText.length() < 1))
			this.filteredTerm = null;
		else
			this.filteredTerm = new ParserTerm(parser, modifiers, filteredText);
	}
}
