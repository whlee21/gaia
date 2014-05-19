package gaia.bigdata.hadoop.commoncrawl;

import org.commoncrawl.hadoop.io.deprecated.ARCInputFormat;
import gaia.solr.behemoth.GaiaSearchOutputFormat;

import java.io.IOException;
import java.util.Date;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.commoncrawl.hadoop.io.mapreduce.ARCFileInputFormat;
import org.commoncrawl.protocol.shared.ArcFileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothDocument;
import com.digitalpebble.behemoth.tika.TikaProcessor;

//public class CommonCrawlIngestJob extends Configured implements Tool {
//	private static final Logger LOG = LoggerFactory.getLogger(CommonCrawlIngestJob.class);
//
//	public int run(String[] args) throws Exception {
//		if (args.length != 3) {
//			String syntax = "hadoop jar job.jar " + CommonCrawlIngestJob.class.getName() + " input zkHost solrCollection";
//			System.err.println(syntax);
//			return -1;
//		}
//
//		Path inputPath = new Path(args[0]);
//		String zkHost = args[1];
//		String collection = args[2];
//
//		LOG.info("ARC input: {}", inputPath.toUri());
//		LOG.info("ZooKeeper URL: {}", zkHost);
//		LOG.info("Solr colleciton: {}", collection);
//
//		JobConf conf = new JobConf(getConf(), getClass());
//		Job job = new Job(conf);
//		job.setJobName(getClass().getName());
//		job.setJarByClass(CommonCrawlIngestJob.class);
//
//		job.setInputFormatClass(ARCFileInputFormat.class);
//		FileInputFormat.setInputPaths(job, new Path[] { inputPath });
////		ARCInputFormat.setARCSourceClass(conf, ARCInputSource.class);
////		ARCInputFormat.setIOTimeout(conf, 120000L);
//
//		job.setOutputFormatClass(GaiaSearchOutputFormat.class);
//		FileOutputFormat.setOutputPath(job,
//				new Path("/tmp/" + CommonCrawlIngestJob.class.getName() + "-" + new Date().getTime()));
//		job.setOutputKeyClass(Text.class);
//		job.setOutputValueClass(BehemothDocument.class);
//		conf.set("solr.zkhost", zkHost);
//		conf.set("solr.zk.collection", collection);
//
//		job.setMapperClass(CommonCrawlMapper.class);
//		job.setNumReduceTasks(0);
//
//		// JobClient.runJob(conf);
//
//		return job.waitForCompletion(true) ? 0 : 1;
//	}
//
//	public static void main(String[] args) throws Exception {
//		ToolRunner.run(new CommonCrawlIngestJob(), args);
//	}
//
//	public static class CommonCrawlMapper extends Mapper<Text, ArcFileItem, Text, BehemothDocument> {
//		protected TikaProcessor processor;
//
//		@Override
//		protected void setup(Context context) {
//			this.processor = new TikaProcessor();
//		}
//
//		public void map(Text key, ArcFileItem doc, Context context) throws IOException, InterruptedException {
//			BehemothDocument newDoc = new BehemothDocument();
//			newDoc.setContent(doc.getContent().getReadOnlyBytes());
//			newDoc.setContentType(doc.getMimeType());
//			newDoc.setUrl(doc.getUri());
//
//			for (BehemothDocument outDoc : this.processor.process(newDoc, context))
//				context.write(key, outDoc);
//		}
//
//		@Override
//		protected void cleanup(Context context) throws IOException, InterruptedException {
//			this.processor.cleanup();
//		}
//	}
//}
public class CommonCrawlIngestJob {
	
}