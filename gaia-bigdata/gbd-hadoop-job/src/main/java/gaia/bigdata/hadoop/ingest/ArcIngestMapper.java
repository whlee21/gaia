package gaia.bigdata.hadoop.ingest;

import gaia.bigdata.hbase.documents.Document;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.commoncrawl.hadoop.io.mapreduce.ARCFileInputFormat;
import org.commoncrawl.protocol.shared.ArcFileItem;

public class ArcIngestMapper extends AbstractHBaseIngestMapper<Text, ArcFileItem> {
	private final AbstractJobFixture fixture = new AbstractJobFixture() {
		public void init(Job job) throws IOException {
			Configuration conf = job.getConfiguration();
			job.setInputFormatClass(ARCFileInputFormat.class);
			// ARCFileInputFormat.
			// ARCInputFormat.setARCSourceClass(conf, ARCInputSource.class);
			// ARCInputFormat.setIOTimeout(conf, 120000L);
		}
	};

	public AbstractJobFixture getFixture() {
		return fixture;
	}

	public Document[] toDocuments(Text key, ArcFileItem value, Context context) throws IOException {
		Document doc = new Document(key.toString(), getCollection());
		doc.content = value.getContent().getReadOnlyBytes();
		doc.contentType = value.getMimeType();
		return new Document[] { doc };
	}
}
