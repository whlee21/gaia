package gaia.bigdata.hadoop.ingest;

import gaia.bigdata.hbase.documents.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;

public class DirectoryIngestMapper extends AbstractHBaseIngestMapper<Text, NullWritable> {
	private final AbstractJobFixture fixture = new AbstractJobFixture() {
		public void init(Job job) throws IOException {
			Configuration conf = job.getConfiguration();
			Path actualInput = new Path(conf.get(AbstractHBaseIngestMapper.TEMP_DIR), "inputs.seq");
			DirectoryIngestMapper.expandGlob(conf, actualInput, FileInputFormat.getInputPaths(job));

			job.setInputFormatClass(SequenceFileInputFormat.class);
			FileInputFormat.setInputPaths(job, new Path[] { actualInput });
			job.setMapperClass(DirectoryIngestMapper.class);
		}
	};

	public AbstractJobFixture getFixture() {
		return fixture;
	}

	public static void expandGlob(Configuration conf, Path output, Path[] pathsToExpand) throws IOException {
		log.info("Expanding glob to a sequence file of inputs");
		SequenceFile.Writer writer = SequenceFile.createWriter(output.getFileSystem(conf), conf, output, Text.class,
				NullWritable.class);

		long i = 0L;
		for (Path path : pathsToExpand) {
			for (FileStatus fstat : path.getFileSystem(conf).globStatus(path)) {
				writer.append(new Text(fstat.getPath().toUri().toString()), NullWritable.get());
				i += 1L;
			}
			writer.sync();
			writer.close();
		}
		log.info("Wrote {} values to {}", String.valueOf(i), output.toString());
	}

	public Document[] toDocuments(Text uri, NullWritable _, Context context) throws IOException {
		Path file;
		try {
			file = new Path(new URI(uri.toString()));
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
		FileSystem fs = file.getFileSystem(context.getConfiguration());
		byte[] ba = new byte[(int) fs.getFileStatus(file).getLen()];
		FSDataInputStream fis = fs.open(file);
		fis.readFully(ba);
		fis.close();
		Document doc = new Document(uri.toString(), getCollection());
		doc.contentType = context.getConfiguration().get(MIME_TYPE, null);
		doc.content = ba;
		return new Document[] { doc };
	}
}
