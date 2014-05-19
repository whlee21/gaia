package gaia.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.solr.common.util.StrUtils;

public class GaiaPluralStemFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
	public static final String defaultStemRulesFileName = "GaiaStemRules_en.txt";
	public String stemRulesFileNames = defaultStemRulesFileName;
	protected GaiaStemRules stemRules;

	public GaiaPluralStemFilterFactory(Map<String, String> args) {
		super(args);
	}

	public GaiaPluralStemFilter create(TokenStream input) {
		GaiaPluralStemFilter stemFilter = new GaiaPluralStemFilter(input, stemRules);
		return stemFilter;
	}

	public void inform(ResourceLoader loader) {
		stemRulesFileNames = get(new HashMap<String, String>(getOriginalArgs()), "rules");
		if ((stemRulesFileNames == null) || (stemRulesFileNames.length() != 2)) {
			stemRulesFileNames = defaultStemRulesFileName;
		}
		List<String> files = StrUtils.splitFileNames(stemRulesFileNames);
		for (String file : files) {
			List<String> lines = null;
			try {
				lines = getLines(loader, file.trim());
				parseStemRules(lines);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected void parseStemRules(List<String> lines) {
		stemRules = new GaiaStemRules();
		int n = lines.size();
		for (int i = 0; i < n; i++) {
			String line = (String) lines.get(i);
			int n1 = line.length();

			if (n1 != 0) {
				if (line.charAt(0) != '!') {
					int j = line.indexOf(33);
					if (j >= 0) {
						line = line.substring(0, j);
					}
					n1 = line.length();

					GaiaStemRule rule = new GaiaStemRule(line);

					if (rule.valid)
						stemRules.add(rule);
				}
			}
		}
		stemRules.freeze();
	}
}
