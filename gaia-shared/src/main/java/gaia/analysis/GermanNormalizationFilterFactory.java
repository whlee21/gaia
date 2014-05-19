package gaia.analysis;

import java.util.Map;

@Deprecated
public class GermanNormalizationFilterFactory extends org.apache.lucene.analysis.de.GermanNormalizationFilterFactory {
	public GermanNormalizationFilterFactory(Map<String, String> args) {
		super(args);
		if (!args.isEmpty())
			throw new IllegalArgumentException("Unknown parameters: " + args);
	}
}
