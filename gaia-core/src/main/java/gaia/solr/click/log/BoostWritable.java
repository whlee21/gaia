package gaia.solr.click.log;

import gaia.solr.click.Boost;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;

public class BoostWritable extends Boost implements Writable {
	private static final TermFreq[] EMPTY_FREQS = new TermFreq[0];
	private static final float[] EMPTY_VALUES = new float[0];
	private static final long[] EMPTY_TIMES = new long[0];

	public BoostWritable() {
	}

	public BoostWritable(String[] terms) {
		super(terms);
	}

	public BoostWritable(TermFreq[] topTerms, float posBoost, float timeBoost) {
		super(topTerms, posBoost, timeBoost);
	}

	public void readFields(DataInput in) throws IOException {
		topTerms = EMPTY_FREQS;
		posWeightLog = EMPTY_VALUES;
		timeWeightLog = EMPTY_VALUES;
		timeLog = EMPTY_TIMES;
		currentPosBoost = in.readFloat();
		currentTimeBoost = in.readFloat();
		int len = WritableUtils.readVInt(in);
		if (len > 0) {
			topTerms = new TermFreq[len];
			for (int i = 0; i < len; i++) {
				topTerms[i] = new TermFreq();
				topTerms[i].term = WritableUtils.readString(in);
				topTerms[i].posWeight = in.readFloat();
			}
		}
		len = WritableUtils.readVInt(in);
		if (len > 0) {
			posWeightLog = new float[len];
			for (int i = 0; i < len; i++) {
				posWeightLog[i] = in.readFloat();
			}
		}
		len = WritableUtils.readVInt(in);
		if (len > 0) {
			timeWeightLog = new float[len];
			for (int i = 0; i < len; i++) {
				timeWeightLog[i] = in.readFloat();
			}
		}
		len = WritableUtils.readVInt(in);
		if (len > 0) {
			timeLog = new long[len];
			for (int i = 0; i < len; i++)
				timeLog[i] = WritableUtils.readVLong(in);
		}
	}

	public void write(DataOutput out) throws IOException {
		out.writeFloat(currentPosBoost);
		out.writeFloat(currentTimeBoost);
		WritableUtils.writeVInt(out, topTerms.length);
		for (int i = 0; i < topTerms.length; i++) {
			WritableUtils.writeString(out, topTerms[i].term);
			out.writeFloat(topTerms[i].posWeight);
		}
		WritableUtils.writeVInt(out, posWeightLog.length);
		for (int i = 0; i < posWeightLog.length; i++) {
			out.writeFloat(posWeightLog[i]);
		}
		WritableUtils.writeVInt(out, timeWeightLog.length);
		for (int i = 0; i < timeWeightLog.length; i++) {
			out.writeFloat(timeWeightLog[i]);
		}
		WritableUtils.writeVInt(out, timeLog.length);
		for (int i = 0; i < timeLog.length; i++)
			WritableUtils.writeVLong(out, timeLog[i]);
	}

	public static BoostWritable fromString(String val) {
		if ((val == null) || (val.length() == 0)) {
			return null;
		}
		String[] fields = val.split("\t");
		float curPosBoost = Float.parseFloat(fields[0]);
		float curTimeBoost = Float.parseFloat(fields[1]);
		TermFreq[] topTerms = EMPTY_FREQS;
		if (fields.length > 2) {
			String[] tfs = fields[2].split(",");
			ArrayList<TermFreq> atfs = new ArrayList<TermFreq>();
			for (int i = 0; i < tfs.length; i++)
				if (tfs[i].trim().length() != 0) {
					String[] tff = tfs[i].split(":");
					if (tff.length == 3) {
						atfs.add(new TermFreq(tff[0], Float.parseFloat(tff[1]), Float.parseFloat(tff[2])));
					}
				}
			topTerms = (TermFreq[]) atfs.toArray(new TermFreq[atfs.size()]);
		}
		float[] posWeightLog = EMPTY_VALUES;
		if (fields.length > 3) {
			String[] vals = fields[3].split(",");
			posWeightLog = new float[vals.length];
			for (int i = 0; i < vals.length; i++) {
				posWeightLog[i] = Float.parseFloat(vals[i]);
			}
		}
		float[] timeWeightLog = EMPTY_VALUES;
		if (fields.length > 4) {
			String[] vals = fields[4].split(",");
			timeWeightLog = new float[vals.length];
			for (int i = 0; i < vals.length; i++) {
				timeWeightLog[i] = Float.parseFloat(vals[i]);
			}
		}
		long[] timeLog = EMPTY_TIMES;
		if (fields.length > 5) {
			String[] times = fields[5].split(",");
			timeLog = new long[times.length];
			for (int i = 0; i < times.length; i++) {
				timeLog[i] = Long.parseLong(times[i]);
			}
		}
		BoostWritable res = new BoostWritable(topTerms, curPosBoost, curTimeBoost);
		res.posWeightLog = posWeightLog;
		res.timeWeightLog = timeWeightLog;
		res.timeLog = timeLog;
		return res;
	}
}
