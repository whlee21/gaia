package gaia.bigdata.hadoop.ingest;

import gaia.bigdata.hbase.documents.DocumentKeySerializer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IngestJob extends AbstractJob {
	private static final Logger log = LoggerFactory.getLogger(IngestJob.class);
	public static final String COLLECTION_OPTION = "collection";
	public static final String MIME_TYPE_OPTION = "mimeType";
	public static final String ZK_CONNECT_OPTION = "zkConnect";
	public static final String MAPPER_OPTION = "mapperClass";
	public static final String OVERWRITE_OPTION = "overwrite";
	public static final String INPUT_FORMAT = "inputFormat";
	public static final String NUMBER_OF_REGIONS_OPTION = "numberOfRegions";
	public static final String DEFAULT_MAPPER_CLASS = DirectoryIngestMapper.class.getCanonicalName();
	public static final String INPUT_FORMAT_OVERRIDE = "inputFormatOverride";

	public static void main(String[] args) throws Exception {
		System.exit(ToolRunner.run(new IngestJob(), args));
	}

	public int run(String[] args) throws Exception {
		addInputOption();
		addOption("zkConnect", "zk", "Connection string for ZooKeeper", true);
		addOption("collection", "c", "Collection name", true);
		addOption("mimeType", "mt", "a mimeType known to Tika", false);
		addOption("mapperClass", "cls", "Custom AbstractHBaseIngestMapper", false);
		addOption("overwrite", "ov", "Overwrite any existing documents", "true");
		addOption("numberOfRegions", "reg", "Number of HBase regions", false);
		addOption(
				"inputFormat",
				"if",
				"The Hadoop InputFormat type to use.  Must be a fully qualified class name and the class must be available on the classpath.  Note: Some Mapper Classes may ignore this.  Also note, your Input Format must be able to produce the correct kind of records for the mapper.");

		if (parseArguments(args) == null) {
			return 1;
		}
		Path input = getInputPath();
		Configuration conf = getConf();
		Job job = new Job(conf);
		job.setJobName(getClass().getName());
		job.setJarByClass(IngestJob.class);

		conf.set(AbstractHBaseIngestMapper.COLLECTION, getOption("collection"));
		conf.set(AbstractHBaseIngestMapper.ZK_CONNECT, getOption("zkConnect"));
		conf.set(AbstractHBaseIngestMapper.MIME_TYPE, getOption("mimeType", ""));
		conf.set(AbstractHBaseIngestMapper.TEMP_DIR, getTempPath().toString());
		boolean overwrite = Boolean.parseBoolean(getOption("overwrite", "true"));
		conf.set(AbstractHBaseIngestMapper.OVERWRITE, overwrite ? "true" : "false");
		conf.set(AbstractHBaseIngestMapper.NUMBER_OF_REGIONS,
				getOption("numberOfRegions", DocumentKeySerializer.getNumberOfRegions() + ""));

		job.setOutputFormatClass(NullOutputFormat.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(NullWritable.class);
		job.setNumReduceTasks(0);
		job.setMapSpeculativeExecution(false);
		FileInputFormat.setInputPaths(job, new Path[] { input });

		String mapperClass = getOption("mapperClass", DEFAULT_MAPPER_CLASS);
		Class mapperClazz;
		AbstractHBaseIngestMapper mapper;
		try {
			mapperClazz = Class.forName(mapperClass);
			mapper = (AbstractHBaseIngestMapper) mapperClazz.newInstance();
		} catch (Exception e) {
			log.error("Unable to instantiate AbstractHBaseIngestMapper class " + mapperClass, e);
			return 1;
		}
		job.setMapperClass(mapperClazz);

		String inputFormatName = getOption("inputFormat");
		if (inputFormatName != null) {
			Class ifClass = Class.forName(inputFormatName).asSubclass(InputFormat.class);
			job.setInputFormatClass(ifClass);
			conf.setBoolean("inputFormatOverride", true);
		}
		mapper.getFixture().init(job);

		// RunningJob job = JobClient.runJob(conf);
		job.waitForCompletion(true);

		mapper.getFixture().close();

		if (job.isSuccessful()) {
			long added = job.getCounters().findCounter(AbstractHBaseIngestMapper.Counters.DOCS_ADDED).getValue();
			if (added == 0L) {
				log.warn("Didn't ingest any documents, failing");
				return 1;
			}

			return 0;
		}

		log.warn("Something went wrong, failing");
		return 1;
	}
}
