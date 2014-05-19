package gaia.analysis;

import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;

public class GaiaPluralStemFilter extends TokenFilter {
	private final CharTermAttribute termAtt = (CharTermAttribute) addAttribute(CharTermAttribute.class);
	private final KeywordAttribute keywordAtt = (KeywordAttribute) addAttribute(KeywordAttribute.class);
	private final GaiaStemRules rules;

	public GaiaPluralStemFilter(TokenStream in, GaiaStemRules rules) {
		super(in);
		this.rules = rules;
	}

	public GaiaPluralStemFilter(TokenStream in) {
		this(in, new GaiaStemRules());
	}

	public final boolean incrementToken() throws IOException {
		if (!input.incrementToken()) {
			return false;
		}
		if (!keywordAtt.isKeyword()) {
			rules.mapWord(termAtt);
		}
		return true;
	}
}
