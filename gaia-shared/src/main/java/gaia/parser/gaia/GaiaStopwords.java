package gaia.parser.gaia;

import org.apache.lucene.analysis.util.CharArraySet;

public class GaiaStopwords {
	private final CharArraySet stopwords;

	public GaiaStopwords(CharArraySet stopwords) {
		assert (stopwords != null);
		this.stopwords = stopwords;
	}

	public boolean isStopword(String word) {
		return stopwords.contains(word);
	}
}
