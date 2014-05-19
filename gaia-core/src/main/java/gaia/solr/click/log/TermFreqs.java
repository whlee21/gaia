package gaia.solr.click.log;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;

public class TermFreqs implements Writable {
	private static final TermFreq[] EMPTY = new TermFreq[0];
	TermFreq[] tfs;

	public TermFreqs() {
		tfs = EMPTY;
	}

	public TermFreqs(TermFreq[] tfs) {
		this.tfs = tfs;
	}

	public String toString() {
		return Arrays.toString(tfs);
	}

	public void readFields(DataInput in) throws IOException {
		int len = WritableUtils.readVInt(in);
		if (len == 0) {
			tfs = EMPTY;
			return;
		}
		tfs = new TermFreq[len];
		Text t = new Text();
		for (int i = 0; i < tfs.length; i++) {
			tfs[i] = new TermFreq();
			t.readFields(in);
			tfs[i].term = t.toString();
		}
		for (int i = 0; i < tfs.length; i++) {
			tfs[i].posWeight = in.readFloat();
		}
		for (int i = 0; i < tfs.length; i++)
			tfs[i].timeWeight = in.readFloat();
	}

	public void write(DataOutput out) throws IOException {
		if ((tfs == null) || (tfs.length == 0)) {
			WritableUtils.writeVInt(out, 0);
			return;
		}
		WritableUtils.writeVInt(out, tfs.length);
		Text t = new Text();
		for (TermFreq tf : tfs) {
			t.set(tf.term);
			t.write(out);
		}
		for (TermFreq tf : tfs) {
			out.writeFloat(tf.posWeight);
		}
		for (TermFreq tf : tfs)
			out.writeFloat(tf.timeWeight);
	}
}
