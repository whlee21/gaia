package gaia.bigdata.hadoop.ingest;

import gaia.bigdata.hbase.documents.Document;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;

public class SequenceFileIngestMapper extends AbstractHBaseIngestMapper<Writable, Writable> {
	private final AbstractJobFixture fixture;

	public SequenceFileIngestMapper() {
		fixture = new AbstractJobFixture() {
			public void init(Job job) throws IOException {
				job.setInputFormatClass(SequenceFileInputFormat.class);
			}
		};
	}

	public AbstractJobFixture getFixture() {
		return fixture;
	}

	public Document[] toDocuments(Writable key, Writable value, Context context) throws IOException {
		Document doc = new Document(key.toString(), getCollection());
		if ((value instanceof Text)) {
			doc.text = ((Text) value).toString();
			context.getCounter(Counters.TEXT).increment(1L);
		} else if ((value instanceof BytesWritable)) {
			BytesWritable value_ = (BytesWritable) value;
			doc.content = new byte[value_.getLength()];
			System.arraycopy(value_.getBytes(), 0, doc.content, 0, doc.content.length);
			context.getCounter(Counters.BYTES_WRITABLE).increment(1L);
		} else {
			doc.content = WritableUtils.toByteArray(new Writable[] { value });
			context.getCounter(Counters.RAW_WRITABLE).increment(1L);
		}
		return new Document[] { doc };
	}

	public static enum Counters {
		TEXT, BYTES_WRITABLE, RAW_WRITABLE;
	}
}
