package gaia.analysis;

import java.util.Map;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class RussianNormalizationFilterFactory extends TokenFilterFactory {
	public RussianNormalizationFilterFactory(Map<String, String> args) {
		super(args);
	}

	public TokenStream create(TokenStream stream) {
		return new RussianNormalizationFilter(stream);
	}
}
