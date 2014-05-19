package gaia.solr.behemoth;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.digitalpebble.behemoth.BehemothDocument;

public class GaiaSearchOutputFormat extends FileOutputFormat<Text, BehemothDocument> {

	@Override
	public RecordWriter<Text, BehemothDocument> getRecordWriter(TaskAttemptContext job)
			throws IOException, InterruptedException {
		final GaiaSearchWriter writer = new GaiaSearchWriter(job);
		writer.open(job.getConfiguration());

		return new RecordWriter<Text, BehemothDocument>() {
			@Override
			public void write(Text key, BehemothDocument doc) throws IOException {
				writer.write(doc);
			}

			@Override
			public void close(TaskAttemptContext context) throws IOException, InterruptedException {
				writer.close();
			}
		};
	}
}
