package gaia.similarity;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.DefaultSimilarity;

public class GaiaSimilarity extends DefaultSimilarity {
	static final long serialVersionUID = 4034457415228610173L;
	public static final double DEFAULT_TF_ADD = -0.06D;
	public final double tfAdd;
	private float tfNormalizer;
	public final float idfAdd;
	public final float idfRatio;
	public final int idfRatioAtCount;
	private double idf_C;
	public static final float DEFAULT_IDF_ADD = 0.25F;
	public static final float DEFAULT_IDF_RATIO = 128.0F;
	public static final int DEFAULT_RATIO_AT_COUNT = 10000000;

	public GaiaSimilarity() {
		this(DEFAULT_TF_ADD);
	}

	public GaiaSimilarity(double tf_add) {
		this(tf_add, DEFAULT_IDF_ADD, DEFAULT_IDF_RATIO, DEFAULT_RATIO_AT_COUNT);
	}

	public GaiaSimilarity(double idf_add, double idf_max, int idf_ratio_at_count) {
		this(DEFAULT_TF_ADD, idf_add, idf_max, idf_ratio_at_count);
	}

	public GaiaSimilarity(double tf_add, double idf_add, double idf_max, int idf_ratio_at_count) {
		tfAdd = ((float) tf_add);

		tfNormalizer = 1.0F;
		tfNormalizer = ((float) (1.0D / tf(1.0F)));

		idfRatio = ((float) idf_max);
		idfAdd = ((float) idf_add);
		idfRatioAtCount = idf_ratio_at_count;
		double scalingFactor = idfRatio / unscaledIdf(1L, idfRatioAtCount);
		idf_C = Math.pow(scalingFactor, 1.0D / (idfRatioAtCount - 1.0D));
	}

	public float lengthNorm(FieldInvertState state) {
		return state.getBoost() * (float) (10.0D / Math.sqrt(state.getLength() + 99));
	}

	public float tf(float freq) {
		return tfNormalizer * (float) Math.log1p(freq + tfAdd);
	}

	public float idf(long docFreq, long numDocs) {
		return (float) Math.sqrt(unscaledIdf(docFreq, numDocs) * Math.pow(idf_C, numDocs - docFreq));
	}

	private double unscaledIdf(long docFreq, long numDocs) {
		return 1.0D + Math.log(((float) numDocs + idfAdd) / ((float) docFreq + idfAdd));
	}
}
