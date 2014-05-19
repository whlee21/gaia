package gaia.bigdata.hadoop.behemoth;

import gaia.solr.behemoth.IdentityMapper;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.commoncrawl.hadoop.io.mapreduce.ARCFileInputFormat;
import org.commoncrawl.protocol.shared.ArcFileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothDocument;

public class ExtractDocsToBehemothDocument extends AbstractJob {
	private static transient Logger log = LoggerFactory.getLogger(ExtractDocsToBehemothDocument.class);
	private static final String MIME_TYPE = ExtractDocsToBehemothDocument.class.getName() + ".mimeType";

	private static final Logger LOG = LoggerFactory.getLogger(ExtractDocsToBehemothDocument.class);

	public int run(String[] args) throws Exception {
		addInputOption();
		addOutputOption();
		addOption("type", "t", "one of: " + Arrays.asList(InputType.values()));
		addOption("mimeType", "mt", "a mimeType known to Tika");

		if (parseArguments(args) == null) {
			return -1;
		}

		Path input = getInputPath();
		Path output = getOutputPath();
		InputType type = null;
		try {
			type = InputType.valueOf(getOption("type").toUpperCase().trim());
		} catch (Exception e) {
			System.err.println("Unknown type: " + getOption("type") + ". Must be one of: "
					+ Arrays.asList(InputType.values()));
			return 1;
		}
		String mimeType = getOption("mimeType");

		Configuration conf = getConf();
		Job job = new Job(conf);
		job.setJobName(getClass().getName());
		job.setJarByClass(ExtractDocsToBehemothDocument.class);

		conf.set(MIME_TYPE, mimeType);

		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		SequenceFileOutputFormat.setOutputPath(job, output);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(BehemothDocument.class);

		job.setReducerClass(IdentityReducer.class);
		// DIR, SEQ_TEXT, SEQ_BIN, SEQ_SKIP, SEQ, ARC
		switch (type) {
		case DIR:
			Path actualInput = new Path(getTempPath(), "inputs.seq");
			ToolRunner.run(new ExpandGlobToSequenceFile(), new String[] { input.toString(), actualInput.toString() });

			job.setInputFormatClass(SequenceFileInputFormat.class);
			SequenceFileInputFormat.setInputPaths(job, new Path[] { actualInput });
			job.setMapperClass(DirectoryBehemothMapper.class);
			break;
		case SEQ_TEXT:
			job.setInputFormatClass(SequenceFileInputFormat.class);
			SequenceFileInputFormat.setInputPaths(job, new Path[] { input });
			job.setMapperClass(BytesWritableBehemothMapper.class);
			break;
		case SEQ_BIN:
			job.setInputFormatClass(SequenceFileInputFormat.class);
			SequenceFileInputFormat.setInputPaths(job, new Path[] { input });
			job.setMapperClass(TextBehemothMapper.class);
			break;
		case SEQ_SKIP:
			job.setInputFormatClass(SequenceFileInputFormat.class);
			SequenceFileInputFormat.setInputPaths(job, new Path[] { input });
			job.setMapperClass(IdentityMapper.class);
			break;
		case SEQ:
			job.setInputFormatClass(SequenceFileInputFormat.class);
			SequenceFileInputFormat.setInputPaths(job, new Path[] { input });
			job.setMapperClass(WritableBehemothMapper.class);
			break;
		case ARC:
			job.setInputFormatClass(ARCFileInputFormat.class);
			FileInputFormat.setInputPaths(job, new Path[] { input });
			// FIXME: by whlee21
			// ARCFileInputFormat.setARCSourceClass(conf, ARCInputSource.class);
			// ARCFileInputFormat.setIOTimeout(conf, 120000L);
			// ARCFileInputFormat.s
			job.setMapperClass(ArcBehemothMapper.class);
		}

		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
		ToolRunner.run(new ExtractDocsToBehemothDocument(), args);
	}

	public static class ArcBehemothMapper extends ExtractDocsToBehemothDocument.AbstractBehemothMapper<ArcFileItem> {
		protected void toBehemoth(Text uri, ArcFileItem value, BehemothDocument doc) throws IOException {
			doc.setContent(value.getContent().getReadOnlyBytes());
			doc.setContentType(value.getMimeType());
			doc.setUrl(value.getUri());
		}
	}

	public static class WritableBehemothMapper extends ExtractDocsToBehemothDocument.AbstractBehemothMapper<Writable> {
		public void toBehemoth(Text uri, Writable value, BehemothDocument doc) throws IOException {
			doc.setContent(WritableUtils.toByteArray(new Writable[] { value }));
		}
	}

	public static class TextBehemothMapper extends ExtractDocsToBehemothDocument.AbstractBehemothMapper<Text> {
		public void toBehemoth(Text uri, Text value, BehemothDocument doc) throws IOException {
			doc.setText(value.toString());
			doc.setContentType("text/plain");
		}
	}

	public static class BytesWritableBehemothMapper extends
			ExtractDocsToBehemothDocument.AbstractBehemothMapper<BytesWritable> {
		public void toBehemoth(Text uri, BytesWritable value, BehemothDocument doc) throws IOException {
			doc.setContent(value.getBytes());
		}
	}

	public static class DirectoryBehemothMapper extends
			ExtractDocsToBehemothDocument.AbstractBehemothMapper<NullWritable> {
		public void toBehemoth(Text uri, NullWritable _, BehemothDocument doc) throws IOException {
			Path file = new Path(uri.toString());
			FileSystem fs = file.getFileSystem(conf);
			byte[] ba = new byte[(int) fs.getFileStatus(file).getLen()];
			FSDataInputStream fis = fs.open(file);
			fis.readFully(ba);
			fis.close();
			doc.setContent(ba);
		}
	}

	public static abstract class AbstractBehemothMapper<T extends Writable> extends
			Mapper<Text, T, Text, BehemothDocument> {
		protected Configuration conf;
		protected BehemothDocument reuseDoc = new BehemothDocument();

		protected BehemothDocument newBehemothDocument() {
			reuseDoc.setAnnotations(null);
			reuseDoc.setContent(null);
			reuseDoc.setContentType(null);
			reuseDoc.setMetadata(null);
			reuseDoc.setText(null);
			reuseDoc.setUrl(null);
			return reuseDoc;
		}

		@Override
		protected void setup(Context context) {
			conf = context.getConfiguration();
		}

		@Override
		public void cleanup(Context context) {
		}

		@Override
		protected void map(Text uri, T value, Context context) throws IOException, InterruptedException {
			BehemothDocument doc = newBehemothDocument();
			ExtractDocsToBehemothDocument.LOG.debug("processing {}", uri);
			doc.setUrl(uri.toString());
			doc.setContentType(conf.get(ExtractDocsToBehemothDocument.MIME_TYPE));
			toBehemoth(uri, value, doc);
			context.write(uri, doc);
		}

		protected abstract void toBehemoth(Text paramText, T paramT, BehemothDocument document) throws IOException;
	}

	public static class ExpandGlobToSequenceFile extends AbstractJob {
		public int run(String[] args) throws Exception {
			addInputOption();
			addOutputOption();
			if (parseArguments(args) == null) {
				return -1;
			}
			Path input = getInputPath();
			Path output = getOutputPath();
			SequenceFile.Writer writer = SequenceFile.createWriter(output.getFileSystem(getConf()), getConf(), output,
					Text.class, NullWritable.class);

			int i = 0;
			for (FileStatus fstat : input.getFileSystem(getConf()).globStatus(input)) {
				writer.append(new Text(fstat.getPath().toUri().toString()), NullWritable.get());
				i++;
			}
			writer.sync();
			writer.close();
			ExtractDocsToBehemothDocument.LOG.info("Wrote {} input values to {}", Integer.valueOf(i), output);
			return 0;
		}

		public void setConf(Configuration conf) {
			try {
				super.setConf(conf);
			} catch (NullPointerException e) {
			}

			String oozieActionConfXml = System.getProperty("oozie.action.conf.xml");
			if ((oozieActionConfXml != null) && (conf != null)) {
				conf.addResource(new Path("file:///", oozieActionConfXml));
				ExtractDocsToBehemothDocument.log.info(
						"Added Oozie action Configuration resource {0} to the Hadoop Configuration", oozieActionConfXml);
			}
		}

		public static void main(String[] args) throws Exception {
			ToolRunner.run(new Configuration(), new ExpandGlobToSequenceFile(), args);
		}
	}

	static enum InputType {
		DIR, SEQ_TEXT, SEQ_BIN, SEQ_SKIP, SEQ, ARC;
	}
}
