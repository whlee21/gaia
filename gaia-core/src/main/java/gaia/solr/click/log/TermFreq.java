package gaia.solr.click.log;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

public class TermFreq implements Writable {
	public String term = "";
	public float posWeight;
	public float timeWeight;
	public static final TermFreqComparator COMPARATOR = new TermFreqComparator();

	public TermFreq() {
	}

	public TermFreq(String term, float posWeight, float timeWeight) {
		this.term = term;
		this.posWeight = posWeight;
		this.timeWeight = timeWeight;
	}

	public String toString() {
		return term + ":" + posWeight + ":" + timeWeight;
	}

	public float getCombinedWeight() {
		return posWeight + timeWeight;
	}

	public void readFields(DataInput in) throws IOException {
		term = Text.readString(in);
		posWeight = in.readFloat();
		timeWeight = in.readFloat();
	}

	public void write(DataOutput out) throws IOException {
		Text.writeString(out, term);
		out.writeFloat(posWeight);
		out.writeFloat(timeWeight);
	}

	public static class TermFreqQueue {
		TreeMap<String, TermFreq> vals = new TreeMap<String, TermFreq>();
		int maxSize;
		float minValue = 1.4E-45F;
		String minKey = "";
		boolean collate;

		public TermFreqQueue(int size, boolean collate) {
			maxSize = size;
			this.collate = collate;
		}

		public TermFreq add(TermFreq tf) {
			TermFreq old = (TermFreq) vals.get(tf.term);
			if (old != null) {
				old.posWeight += tf.posWeight;
				old.timeWeight += tf.timeWeight;
				return tf;
			}

			if (collate) {
				for (Map.Entry<String, TermFreq> e : vals.entrySet()) {
					String key = (String) e.getKey();
					int pos = key.indexOf(tf.term);
					if (pos != -1) {
						boolean startOk = (pos == 0) || ((pos > 0) && (key.charAt(pos - 1) == ' '));
						int end = pos + tf.term.length();
						boolean endOk = (end == key.length()) || ((end < key.length()) && (key.charAt(end) == ' '));
						if ((startOk) && (endOk)) {
							TermFreq sub = (TermFreq) e.getValue();
							sub.posWeight += tf.posWeight;
							sub.timeWeight += tf.timeWeight;
							return sub;
						}
					} else {
						int pos1 = tf.term.indexOf(key);
						if (pos1 != -1) {
							boolean startOk = (pos1 == 0) || ((pos1 > 0) && (tf.term.charAt(pos1 - 1) == ' '));
							int end = pos1 + key.length();
							boolean endOk = (end == tf.term.length()) || ((end < tf.term.length()) && (tf.term.charAt(end) == ' '));
							if ((startOk) && (endOk)) {
								TermFreq sub = (TermFreq) e.getValue();
								tf.posWeight += sub.posWeight;
								tf.timeWeight += sub.timeWeight;

								vals.remove(key);
								vals.put(tf.term, tf);
								return tf;
							}
						}
					}
				}
			}
			if (vals.size() >= maxSize) {
				if (tf.getCombinedWeight() < minValue) {
					return tf;
				}

				old = (TermFreq) vals.remove(minKey);
				vals.put(tf.term, tf);

				minValue = 3.4028235E+38F;
				for (Map.Entry<String, TermFreq> e : vals.entrySet()) {
					if (((TermFreq) e.getValue()).getCombinedWeight() < minValue) {
						minValue = ((TermFreq) e.getValue()).getCombinedWeight();
						minKey = ((TermFreq) e.getValue()).term;
					}
				}
				return old;
			}
			vals.put(tf.term, tf);
			if ((minValue == 1.4E-45F) || (tf.getCombinedWeight() < minValue)) {
				minValue = tf.getCombinedWeight();
				minKey = tf.term;
			}
			return null;
		}

		public TermFreq[] getElements() {
			return (TermFreq[]) vals.values().toArray(new TermFreq[vals.size()]);
		}
	}

	public static class TermFreqComparator implements Comparator<TermFreq> {
		public int compare(TermFreq o1, TermFreq o2) {
			return (int) (o1.getCombinedWeight() - o2.getCombinedWeight());
		}
	}
}
