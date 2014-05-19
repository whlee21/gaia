package gaia.bigdata.hadoop.clustering;

import gaia.bigdata.hbase.documents.DocumentKeySerializer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.apache.mahout.common.distance.SquaredEuclideanDistanceMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterLoader extends AbstractJob {
	private static final Logger log = LoggerFactory.getLogger(ClusterLoader.class);
	public static final String ZK_CONNECT_OPTION = "zkConnect";
	public static final String CLUSTERS_DIR = "centroids";
	public static final String ZK_CONNECT = ClusterLoader.class.getName() + ".zkConnect";
	public static final String DISTANCE = "distance";
	public static final String NUMBER_OF_REGIONS_OPTION = "numberOfRegions";
	public static final String NUMBER_OF_REGIONS = ClusterLoader.class.getName() + ".numberOfRegions";

	public int run(String[] args) throws Exception {
		addInputOption();
		addOption(ZK_CONNECT_OPTION, "zk", "Connection string for ZooKeeper", true);
		addOption(CLUSTERS_DIR, "c", "The location of the centroids", false);
		addOption(DefaultOptionCreator.distanceMeasureOption().create());
		addOption(NUMBER_OF_REGIONS_OPTION, "reg", "Number of HBase regions", false);

		if (parseArguments(args) == null) {
			return 1;
		}

		Configuration conf = getConf();
		conf.set(ZK_CONNECT, getOption(ZK_CONNECT_OPTION));
		conf.set(NUMBER_OF_REGIONS, getOption(NUMBER_OF_REGIONS_OPTION, DocumentKeySerializer.getNumberOfRegions() + ""));

		String measureClass = getOption("distanceMeasure");
		if (measureClass == null) {
			measureClass = SquaredEuclideanDistanceMeasure.class.getName();
		}
		conf.set(DISTANCE, measureClass);
		Path input = getInputPath();
		String centroids = getOption(CLUSTERS_DIR, input.getParent().toString());
		conf.set(CLUSTERS_DIR, centroids);
		Job job = new Job(conf);

		job.setJobName("Invert cluster ids");
		job.setJarByClass(getClass());

		job.setInputFormatClass(SequenceFileInputFormat.class);
		SequenceFileInputFormat.setInputPaths(job, new Path[] { input });

		job.setMapperClass(JoinClusterIdToDocuments.class);
		job.setOutputFormatClass(NullOutputFormat.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(NullWritable.class);
		job.setNumReduceTasks(0);
		job.setMapSpeculativeExecution(false);

		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
		System.exit(ToolRunner.run(new ClusterLoader(), args));
	}
}
