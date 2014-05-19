package gaia.bigdata.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

public class LogMessageWritable implements Writable {
	private LongWritable date = new LongWritable();
	private Text level = new Text();
	private Text logger = new Text();
	private Text message = new Text();

	private static final String delim = new Character('\001').toString();

	public LogMessageWritable() {
	}

	public LogMessageWritable(Date date, String level, String logger, String message) {
		this.date.set(date.getTime());
		this.level.set(level);
		this.logger.set(logger);
		this.message.set(message);
	}

	public long getDate() {
		return date.get();
	}

	public String getLevel() {
		return level.toString();
	}

	public String getLogger() {
		return logger.toString();
	}

	public String getMessage() {
		return message.toString();
	}

	public void readFields(DataInput in) throws IOException {
		date.readFields(in);
		level.readFields(in);
		logger.readFields(in);
		message.readFields(in);
	}

	public void write(DataOutput out) throws IOException {
		date.write(out);
		level.write(out);
		logger.write(out);
		message.write(out);
	}

	public String toString() {
		return date.toString() + delim + level.toString() + delim + logger.toString() + delim + message.toString();
	}
}
