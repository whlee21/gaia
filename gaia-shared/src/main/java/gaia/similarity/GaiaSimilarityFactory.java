package gaia.similarity;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.schema.SimilarityFactory;

public class GaiaSimilarityFactory extends SimilarityFactory {
	Map<String, Similarity> similarities;

	public void init(SolrParams args) {
		similarities = new HashMap<String, Similarity>();
		GaiaSimilarity gaiaSimilarity = new GaiaSimilarity();
		if (args != null) {
			String specialFieldsList = args.get("special_fields");
			if (specialFieldsList != null) {
				String[] specialFields = specialFieldsList.split(",");
				for (String field : specialFields)
					similarities.put(StringUtils.trim(field), gaiaSimilarity);
			}
		}
	}

	public Similarity getSimilarity() {
		return new GaiaMultiLenNormSimilarity(similarities);
	}

	public Map<String, Similarity> getSimilarities() {
		return similarities;
	}
}
