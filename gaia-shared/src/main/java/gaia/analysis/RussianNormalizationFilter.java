package gaia.analysis;

import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public final class RussianNormalizationFilter extends TokenFilter {
	private final CharTermAttribute termAtt = (CharTermAttribute) addAttribute(CharTermAttribute.class);

	public RussianNormalizationFilter(TokenStream input) {
		super(input);
	}

	public boolean incrementToken() throws IOException {
		if (input.incrementToken()) {
			char[] buffer = termAtt.buffer();
			int length = termAtt.length();
			for (int i = 0; i < length; i++) {
				if (buffer[i] == 'ё')
					buffer[i] = 'е';
				else if (buffer[i] == 'Ё')
					buffer[i] = 'Е';
			}
			return true;
		}
		return false;
	}
}
