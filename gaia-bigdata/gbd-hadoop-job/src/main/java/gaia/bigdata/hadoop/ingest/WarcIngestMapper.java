package gaia.bigdata.hadoop.ingest;

import java.io.IOException;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;

import edu.cmu.lemurproject.WarcFileInputFormat;
import edu.cmu.lemurproject.WarcRecord;
import edu.cmu.lemurproject.WritableWarcRecord;
import gaia.bigdata.hbase.documents.Document;

public class WarcIngestMapper extends AbstractHBaseIngestMapper<LongWritable, WritableWarcRecord> {
	private final AbstractJobFixture fixture = new AbstractJobFixture() {
		public void init(Job job) throws IOException {
			job.setInputFormatClass(WarcFileInputFormat.class);
		}
	};

	public AbstractJobFixture getFixture() {
		return fixture;
	}

	public Document[] toDocuments(LongWritable _, WritableWarcRecord value, Context context)
			throws IOException {
		String id = value.getRecord().getHeader().UUID;
		Document doc = new Document(id, getCollection());
		WarcRecord record = value.getRecord();
		doc.content = record.getContent();
		doc.contentType = null;
		for (Map.Entry<String, String> entry : record.getHeaderMetadata()) {
			doc.fields.put("warc." + (String) entry.getKey(), entry.getValue());
		}
		return new Document[] { doc };
	}
}
