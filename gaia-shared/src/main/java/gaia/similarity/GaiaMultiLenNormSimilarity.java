package gaia.similarity;

import java.util.Map;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.PerFieldSimilarityWrapper;
import org.apache.lucene.search.similarities.Similarity;

public class GaiaMultiLenNormSimilarity extends PerFieldSimilarityWrapper {
	static final long serialVersionUID = -512120257L;
	private static final Similarity defaultSim = IndexSearcher.getDefaultSimilarity();
	Map<String, Similarity> similarities;

	public GaiaMultiLenNormSimilarity(Map<String, Similarity> similarities) {
		this.similarities = similarities;
	}

	public Similarity get(String field) {
		Similarity sim = (Similarity) similarities.get(field);

		return sim == null ? defaultSim : sim;
	}

	public float coord(int overlap, int maxOverlap) {
		return defaultSim.coord(overlap, maxOverlap);
	}

	public float queryNorm(float valueForNormalization) {
		return defaultSim.queryNorm(valueForNormalization);
	}

	public Map<String, Similarity> getSimilarityMap() {
		return similarities;
	}
}
