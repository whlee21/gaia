package gaia.similarity;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.DefaultSimilarity;

public class ScaledLengthNormSimilarity extends DefaultSimilarity {
	private static final float DEFAULT_NUMERATOR = 10.0F;
	private float numerator = DEFAULT_NUMERATOR;
	private static final double DEFAULT_DENOMINATOR_FACTOR = 99.0D;
	private double denominatorFactor = DEFAULT_DENOMINATOR_FACTOR;

	public ScaledLengthNormSimilarity() {
	}

	public ScaledLengthNormSimilarity(float numerator, double denominatorFactor) {
		this.numerator = numerator;
		this.denominatorFactor = denominatorFactor;
	}

	public float lengthNorm(FieldInvertState state) {
		return state.getBoost() * (float) (numerator / Math.sqrt(state.getLength() + denominatorFactor));
	}

	public double getDenominatorFactor() {
		return denominatorFactor;
	}

	public float getNumerator() {
		return numerator;
	}
}
