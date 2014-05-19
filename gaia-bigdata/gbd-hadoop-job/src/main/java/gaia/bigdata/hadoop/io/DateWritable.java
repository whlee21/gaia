package gaia.bigdata.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.WritableComparable;

public class DateWritable implements WritableComparable<DateWritable> {
	private Date date;
	private LongWritable epoch = new LongWritable();

	public DateWritable() {
	}

	public DateWritable(Date date) {
		this.date = date;
		this.epoch.set(date.getTime());
	}

	public Date get() {
		return this.date;
	}

	public void set(Date date) {
		this.date = date;
		this.epoch.set(date.getTime());
	}

	public void readFields(DataInput in) throws IOException {
		this.epoch.readFields(in);
		this.date = new Date(this.epoch.get());
	}

	public void write(DataOutput out) throws IOException {
		this.epoch.write(out);
	}

	public int compareTo(DateWritable other) {
		return this.epoch.compareTo(other.epoch);
	}

	public String toString() {
		return this.date.toString();
	}
}
