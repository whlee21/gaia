package gaia.solr.behemoth;

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.digitalpebble.behemoth.BehemothConfiguration;
import com.digitalpebble.behemoth.BehemothDocument;

public class GaiaSearchIndexerJob extends Configured implements Tool {
	private static final Log LOG = LogFactory.getLog(GaiaSearchIndexerJob.class);

	public GaiaSearchIndexerJob() {
	}

	public GaiaSearchIndexerJob(Configuration conf) {
		super(conf);
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(BehemothConfiguration.create(), new GaiaSearchIndexerJob(), args);

		System.exit(res);
	}

	public int run(String[] args) throws Exception {
		FileSystem fs = FileSystem.get(getConf());

		if (args.length != 2) {
			String syntax = "com.digitalpebble.solr.GaiaSearchIndexerJob in solrURL";
			System.err.println(syntax);
			return -1;
		}

		Path inputPath = new Path(args[0]);
		String solrURL = args[1];

		Job job = new Job(getConf());

		job.setJarByClass(getClass());

		job.setJobName("Indexing " + inputPath + " into GaiaSearch");

		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setOutputFormatClass(GaiaSearchOutputFormat.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(BehemothDocument.class);

		job.setMapperClass(IdentityMapper.class);

		job.setNumReduceTasks(0);

		FileInputFormat.addInputPath(job, inputPath);
		Path tmp = new Path("tmp_" + System.currentTimeMillis() + "-" + new Random().nextInt());

		FileOutputFormat.setOutputPath(job, tmp);

		job.getConfiguration().set("solr.server.url", solrURL);
		try {
			long start = System.currentTimeMillis();
//			JobClient.runJob(job);
			job.submit();
			job.waitForCompletion(true);
			long finish = System.currentTimeMillis();
			if (LOG.isInfoEnabled())
				LOG.info("GaiaSearchIndexerJob completed. Time " + (finish - start) + " ms");
		} catch (Exception e) {
			LOG.error(e);
		} finally {
			fs.delete(tmp, true);
		}

		return 0;
	}
}
