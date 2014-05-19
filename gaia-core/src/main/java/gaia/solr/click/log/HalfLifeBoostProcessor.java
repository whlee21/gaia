package gaia.solr.click.log;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.Text;

public class HalfLifeBoostProcessor extends Configured implements BoostProcessor {
	public static final String HISTORY_LENGTH = "boost.processor.halflife.history.length";
	public static final String HALFLIFE_PERIOD = "boost.processor.halflife.period";
	int historyLength;
	long halfLifeTime;

	public HalfLifeBoostProcessor() {
	}

	public HalfLifeBoostProcessor(int historyLength, long halfLifeTime) {
		this.historyLength = historyLength;
		this.halfLifeTime = halfLifeTime;
	}

	public void setConf(Configuration conf) {
		super.setConf(conf);
		if (conf == null)
			return;
		historyLength = conf.getInt(HISTORY_LENGTH, 10);
		halfLifeTime = conf.getLong(HALFLIFE_PERIOD, 2592000000L);
	}

	public BoostWritable processCurrent(Text id, BoostWritable current) {
		float value = 0.0F;
		for (TermFreq tf : current.topTerms) {
			value += tf.posWeight;
		}
		value /= 100.0F;
		current.currentPosBoost = value;
		return current;
	}

	public BoostWritable processHistory(Text id, BoostWritable current, BoostWritable previous, long updateTime) {
		current.posWeightLog = new float[historyLength];
		current.timeWeightLog = new float[historyLength];
		current.timeLog = new long[historyLength];
		if (previous != null) {
			int len = Math.min(previous.posWeightLog.length, historyLength);
			System.arraycopy(previous.posWeightLog, 0, current.posWeightLog, 0, len);
			System.arraycopy(previous.timeWeightLog, 0, current.timeWeightLog, 0, len);
			System.arraycopy(previous.timeLog, 0, current.timeLog, 0, len);
		}

		float lastPosVal = current.posWeightLog[(historyLength - 1)];
		float lastTimeVal = current.timeWeightLog[(historyLength - 1)];
		long lastTime = current.timeLog[(historyLength - 1)];
		if (lastTime > 0L) {
			long prevTime = current.timeLog[(historyLength - 2)];

			float deltaNorm = (float) (prevTime - lastTime) / (float) halfLifeTime;
			float posWIncr = decayValue(lastPosVal, prevTime - lastTime, halfLifeTime);
			float timeWIncr = decayValue(lastTimeVal, prevTime - lastTime, halfLifeTime);

			if ((posWIncr > lastPosVal * (1.0F - deltaNorm)) && (deltaNorm < 1.0F)) {
				posWIncr = lastPosVal * (1.0F - deltaNorm);
			}
			if ((timeWIncr > lastTimeVal * (1.0F - deltaNorm)) && (deltaNorm < 1.0F)) {
				timeWIncr = lastTimeVal * (1.0F - deltaNorm);
			}
			current.posWeightLog[(historyLength - 2)] += posWIncr;
			current.timeWeightLog[(historyLength - 2)] += timeWIncr;
		}

		for (int i = historyLength - 1; i >= 1; i--) {
			current.posWeightLog[i] = current.posWeightLog[(i - 1)];
			current.timeWeightLog[i] = current.timeWeightLog[(i - 1)];
			current.timeLog[i] = current.timeLog[(i - 1)];
		}

		current.posWeightLog[0] = current.currentPosBoost;
		current.timeWeightLog[0] = current.currentTimeBoost;
		current.timeLog[0] = updateTime;

		float currentPosBoost = current.currentPosBoost;
		float currentTimeBoost = current.currentTimeBoost;
		for (int i = 1; i < historyLength; i++) {
			currentPosBoost += decayValue(current.posWeightLog[i], updateTime - current.timeLog[i], halfLifeTime);
			currentTimeBoost += decayValue(current.timeWeightLog[i], updateTime - current.timeLog[i], halfLifeTime);
		}
		current.currentPosBoost = currentPosBoost;
		current.currentTimeBoost = currentTimeBoost;
		return current;
	}

	private float decayValue(float value, long delta, long halfTime) {
		double exp = delta / halfLifeTime;
		return (float) (value * Math.pow(2.0D, -exp));
	}
}
