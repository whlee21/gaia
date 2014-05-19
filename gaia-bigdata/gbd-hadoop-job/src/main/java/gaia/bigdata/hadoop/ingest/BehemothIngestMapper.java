package gaia.bigdata.hadoop.ingest;

import gaia.bigdata.hbase.documents.Document;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;

import com.digitalpebble.behemoth.Annotation;
import com.digitalpebble.behemoth.BehemothDocument;

public class BehemothIngestMapper extends AbstractHBaseIngestMapper<Text, BehemothDocument> {
	private final AbstractJobFixture fixture = new AbstractJobFixture() {
		public void init(Job job) throws IOException {
			job.setInputFormatClass(SequenceFileInputFormat.class);
		}
	};

	public AbstractJobFixture getFixture() {
		return fixture;
	}

	public Document[] toDocuments(Text key, BehemothDocument value, Context context)
			throws IOException {
		Document doc = new Document(key.toString(), getCollection());
		doc.text = value.getText();
		doc.content = value.getContent();
		doc.contentType = value.getContentType();

		for (Map.Entry<Writable, Writable> entry : value.getMetadata(true).entrySet()) {
			doc.fields.put(((Writable) entry.getKey()).toString(), ((Writable) entry.getValue()).toString());
		}

		List<Annotation> annotations = value.getAnnotations();
		if ((annotations != null) && (annotations.size() > 0)) {
			if (doc.annotations != null)
				doc.annotations.addAll(annotations);
			else {
				doc.annotations = annotations;
			}
		}

		return new Document[] { doc };
	}
}
