package gaia.feedback;

public class NumTermsFeedback extends Rocchio {
	protected float calculateScore(float factor, float tf, float idf) {
		return factor * Math.max(idf, 1.0E-06F);
	}
}
