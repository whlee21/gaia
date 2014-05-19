package org.apache.solr.analysis;

import java.util.Map;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

@Deprecated
public class JapanesePunctuationFilterFactory extends TokenFilterFactory {
	public JapanesePunctuationFilterFactory(Map<String, String> args) {
		super(args);
	}

	public TokenStream create(TokenStream input) {
		return input;
	}
}
