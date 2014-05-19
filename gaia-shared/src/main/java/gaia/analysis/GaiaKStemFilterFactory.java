package gaia.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class GaiaKStemFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
	private CharArraySet protectedWords = null;

	public void inform(ResourceLoader loader) {
		assureMatchVersion();

		String wordFile = get(new HashMap<String, String>(getOriginalArgs()), "protected");
		if (wordFile != null)
			try {
				List<String> wlist = getLines(loader, wordFile);
				protectedWords = new CharArraySet(luceneMatchVersion, wlist, false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
	}

	public TokenFilter create(TokenStream input) {
		if (protectedWords != null)
			input = new SetKeywordMarkerFilter(input, protectedWords);
		return new KStemFilter(input);
	}

	public GaiaKStemFilterFactory(Map<String, String> args) {
		super(args);
	}
}
