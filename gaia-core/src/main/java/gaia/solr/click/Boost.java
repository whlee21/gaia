package gaia.solr.click;

import gaia.solr.click.log.TermFreq;

public class Boost {
	private static final TermFreq[] EMPTY_FREQS = new TermFreq[0];
	private static final float[] EMPTY_VALUES = new float[0];
	private static final long[] EMPTY_TIMES = new long[0];

	public TermFreq[] topTerms = EMPTY_FREQS;
	public float currentPosBoost;
	public float currentTimeBoost;
	public float[] posWeightLog = EMPTY_VALUES;

	public float[] timeWeightLog = EMPTY_VALUES;

	public long[] timeLog = EMPTY_TIMES;

	public Boost() {
	}

	public Boost(String[] terms) {
		if ((terms == null) || (terms.length == 0)) {
			return;
		}
		topTerms = new TermFreq[terms.length];
		for (int i = 0; i < terms.length; i++)
			topTerms[i] = new TermFreq(terms[i], 1.0F, 0.0F);
	}

	public Boost(TermFreq[] topTerms, float posBoost, float timeBoost) {
		if (topTerms != null)
			this.topTerms = topTerms;
		currentPosBoost = posBoost;
		currentTimeBoost = timeBoost;
	}

	public float getCombinedBoost() {
		return currentPosBoost + currentTimeBoost;
	}

	public void set(Boost other) {
		currentPosBoost = other.currentPosBoost;
		currentTimeBoost = other.currentTimeBoost;
		topTerms = other.topTerms;
		timeLog = other.timeLog;
		posWeightLog = other.posWeightLog;
		timeWeightLog = other.timeWeightLog;
	}

	public String toString() {
		String terms = "";
		if ((topTerms != null) && (topTerms.length > 0)) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < topTerms.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append(new StringBuilder().append(topTerms[i].term).append(":").append(topTerms[i].posWeight).append(":")
						.append(topTerms[i].timeWeight).toString());
			}
			terms = sb.toString();
		}
		String posVals = "";
		if ((posWeightLog != null) && (posWeightLog.length > 0)) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < posWeightLog.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append(Float.toString(posWeightLog[i]));
			}
			posVals = sb.toString();
		}
		String timeVals = "";
		if ((timeWeightLog != null) && (timeWeightLog.length > 0)) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < timeWeightLog.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append(Float.toString(timeWeightLog[i]));
			}
			timeVals = sb.toString();
		}
		String times = "";
		if ((timeLog != null) && (timeLog.length > 0)) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < timeLog.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append(Long.toString(timeLog[i]));
			}
			times = sb.toString();
		}
		return new StringBuilder().append(currentPosBoost).append("\t").append(currentTimeBoost).append("\t").append(terms)
				.append("\t").append(posVals).append("\t").append(timeVals).append("\t").append(times).toString();
	}
}
