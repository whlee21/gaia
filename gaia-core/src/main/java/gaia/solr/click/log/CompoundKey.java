package gaia.solr.click.log;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Partitioner;

public class CompoundKey implements WritableComparable<CompoundKey> {
	Text queryId;
	Text userId;
	long val;

	public CompoundKey() {
		queryId = new Text();
		userId = new Text();
	}

	public String toString() {
		return "K[uid=" + userId + ",qid=" + queryId + ",val=" + val + "]";
	}

	public void readFields(DataInput in) throws IOException {
		val = in.readLong();
		queryId.readFields(in);
		userId.readFields(in);
	}

	public void write(DataOutput out) throws IOException {
		out.writeLong(val);
		queryId.write(out);
		userId.write(out);
	}

	public int compareTo(CompoundKey o) {
		int res = userId.compareTo(o.userId);
		if (res != 0) {
			return res;
		}
		res = queryId.compareTo(o.queryId);
		if (res != 0) {
			return res;
		}
		if (val < o.val)
			return -1;
		if (val > o.val) {
			return 1;
		}
		return 0;
	}

	public boolean equals(Object o) {
		return compareTo((CompoundKey) o) == 0;
	}

	public static class CompoundKeyPartitioner extends Partitioner<CompoundKey, Writable> {
		public int getPartition(CompoundKey key, Writable value, int numPartitions) {
			Text k = new Text();
			if (key.userId.getLength() == 0) {
				String uid = key.queryId.toString();
				uid = uid.substring(0, uid.indexOf('~'));
				k.set(uid);
			} else {
				k.set(key.userId);
			}
			return Math.abs(k.hashCode() % numPartitions);
		}
	}

	public static class CompoundSortComparator extends WritableComparator {
		protected CompoundSortComparator() {
			super(WritableComparable.class, true);
		}

		public int compare(WritableComparable o1, WritableComparable o2) {
			CompoundKey k1 = (CompoundKey) o1;
			CompoundKey k2 = (CompoundKey) o2;
			int res = k1.queryId.compareTo(k2.queryId);
			if (res == 0) {
				if (k1.val < k2.val)
					res = -1;
				else if (k1.val > k2.val)
					res = 1;
				else {
					res = 0;
				}
			}

			return res;
		}
	}

	public static class CompoundGroupingComparator extends WritableComparator {
		protected CompoundGroupingComparator() {
			super(WritableComparable.class, true);
		}

		public int compare(WritableComparable o1, WritableComparable o2) {
			return ((CompoundKey) o1).queryId.compareTo(((CompoundKey) o2).queryId);
		}
	}
}
