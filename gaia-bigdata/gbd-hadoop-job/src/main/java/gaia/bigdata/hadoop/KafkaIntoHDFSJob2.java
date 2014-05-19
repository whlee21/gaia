package gaia.bigdata.hadoop;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import kafka.etl.KafkaETLInputFormat;
import kafka.etl.KafkaETLJob;
import kafka.etl.KafkaETLKey;
import kafka.etl.KafkaETLRequest;
import kafka.etl.KafkaETLUtils;
import kafka.etl.Props;
import kafka.message.Message;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaIntoHDFSJob2 extends AbstractJob {
	private static final Logger LOG = LoggerFactory.getLogger(KafkaIntoHDFSJob2.class);
	private static final String LOG_MOS = "logs";
	private static final String FILE_OUTPUT = "gaia.bigdata.hadoop.output";
	public static final String KAFKA_URI_OPT = "kafkaUri";
	public static final String KAFKA_TOPIC_OPT = "kafkaTopic";
	public static final String KAFKA_TOPIC_PARTITIONS_OPT = "kafkaTopicPartitions";

	public int run(String[] args) throws Exception {
		addInputOption();
		addOutputOption();
		addOption("kafkaUri", "s", "Kafka URIs (tcp://hostname:port)");
		addOption("kafkaTopic", "t", "Kafka topic");
		addOption("kafkaTopicPartitions", "n", "Number of partitions for topic");

		if (parseArguments(args) == null) {
			return -1;
		}

		Path offsets = getInputPath();
		Path output = getOutputPath();
		String topic = getOption("kafkaTopic");
		String uri = getOption("kafkaUri");
		Integer parts = Integer.valueOf(Integer.parseInt(getOption("kafkaTopicPartitions")));
		String[] uriArray = uri.split(",");

		Set<String> uriSet = new HashSet<String>();
		Collections.addAll(uriSet, uriArray);
		if (uriSet.size() != uriArray.length) {
			throw new RuntimeException("Duplicate Kafka server found. Each host:port for Kafka should be different");
		}

		for (String kafkaURI : uriSet) {
			Path offsetPath = new Path(offsets, URLEncoder.encode(kafkaURI, "UTF-8"));
			LOG.info("Running with args: offsetsDir={}, kafkaUri={}, kafkaTopic={}, kafkaTopicPartitions={}, outputDir={}",
					new Object[] { offsets, kafkaURI, topic, parts, output });

			Job job = newKafkaIntoHDFSJob(offsetPath, output, topic, kafkaURI, parts);
			job.waitForCompletion(true);
		}
		return 0;
	}

	public static Job newKafkaIntoHDFSJob(Path offsets, Path output, String topic, String uri, Integer parts)
			throws Exception {
		Properties props = new Properties();
		props.put("kafka.server.uri", uri);

		JobConf conf = KafkaETLJob.createJobConf(KafkaIntoHDFSJob2.class.getName(), topic, new Props(
				new Properties[] { props }), KafkaIntoHDFSJob2.class);

		conf.setJobName(KafkaIntoHDFSJob2.class.getName() + " - " + uri);
		conf.setJarByClass(KafkaIntoHDFSJob2.class);

		Job job = new Job(conf);

		long epoch = System.currentTimeMillis();
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd/HH");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		conf.set(FILE_OUTPUT, output + "/" + format.format(new Date(epoch)) + "/id=" + epoch);

		Path offsetPathGlob = new Path(offsets, "*/offsets_*");
		FileSystem fs = offsets.getFileSystem(conf);
		FileStatus[] fstats = fs.globStatus(offsetPathGlob);
		Path offsetPath;
		if (fstats.length == 0) {
			LOG.info("No offset files found, starting consuming from the beginning");
			offsetPath = new Path(offsets, "offsets_init");
			KafkaETLKey dummyKey = new KafkaETLKey();
			SequenceFile.setDefaultCompressionType(conf, SequenceFile.CompressionType.NONE);
			SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf, offsetPath, KafkaETLKey.class,
					BytesWritable.class);

			for (int i = 0; i < parts.intValue(); i++) {
				KafkaETLRequest request = new KafkaETLRequest(topic, uri, i);
				byte[] bytes = request.toString().getBytes("UTF-8");
				writer.append(dummyKey, new BytesWritable(bytes));
			}
			writer.close();
		} else {
			FileStatus mrFstat = fstats[0];
			for (FileStatus fstat : fstats) {
				if (fstat.getModificationTime() > mrFstat.getModificationTime()) {
					mrFstat = fstat;
				}
			}

			offsetPath = new Path(mrFstat.getPath().getParent(), "offsets_*");
		}
		LOG.info("Reading in offsets from: " + offsetPath);
		conf.setInputFormat(KafkaETLInputFormat.class);
		KafkaETLInputFormat.setInputPaths(conf, new Path[] { offsetPath });

		// FIXME: by whlee21
		MultipleOutputs.addNamedOutput(job, LOG_MOS, SequenceFileOutputFormat.class, NullWritable.class,
				BytesWritable.class);

		LazyOutputFormat.setOutputFormatClass(job, SequenceFileOutputFormat.class);

		MultipleOutputs.setCountersEnabled(job, true);
		SequenceFileOutputFormat.setCompressOutput(job, true);
		SequenceFileOutputFormat.setOutputPath(job, new Path(offsets, Long.toString(epoch)));
		// KafkaIntoHDFSOutputFormat.setCompressOutput(conf, true);
		// KafkaIntoHDFSOutputFormat.setOutputPath(conf, new Path(offsets,
		// Long.toString(epoch)));

		job.setMapperClass(KafkaIntoHDFSMapper.class);
		conf.setNumReduceTasks(0);
		conf.setMapSpeculativeExecution(false);

		return job;
	}

	public static void main(String[] args) throws Exception {
		ToolRunner.run(new KafkaIntoHDFSJob2(), args);
	}

	// public static class KafkaIntoHDFSOutputFormat extends
	// MultipleSequenceFileOutputFormat<NullWritable, BytesWritable> {
	// protected String generateFileNameForKeyValue(NullWritable key,
	// BytesWritable value, String name) {
	// return name;
	// }
	//
	// protected String getInputFileBasedOutputFileName(JobConf conf, String name)
	// {
	// return conf.get(FILE_OUTPUT) + "/" + name;
	// }
	// }

	public static class KafkaIntoHDFSMapper extends Mapper<KafkaETLKey, BytesWritable, NullWritable, BytesWritable> {
		MultipleOutputs<NullWritable, BytesWritable> mos;

		@Override
		protected void map(KafkaETLKey key, BytesWritable val, Context context) throws IOException, InterruptedException {
			byte[] bytes = KafkaETLUtils.getBytes(val);

			Message message = new Message(bytes);
			long checksum = key.getChecksum();
			if (checksum != message.checksum()) {
				throw new IOException("Invalid message checksum " + message.checksum() + ". Expected " + key + ".");
			}

			// OutputCollector collector = mos.getCollector(LOG_MOS, reporter);
			// collector.collect(NullWritable.get(), val);
			mos.write(NullWritable.get(), val, generateFileName(context.getConfiguration(), LOG_MOS));
		}

		private String generateFileName(Configuration conf, String name) {
			String fileName = conf.get(FILE_OUTPUT) + "/" + name;
			return fileName;
		}

		@Override
		protected void setup(Context context) {
			mos = new MultipleOutputs<NullWritable, BytesWritable>((TaskInputOutputContext) context.getConfiguration());
		}

		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			mos.close();
		}
	}
}
