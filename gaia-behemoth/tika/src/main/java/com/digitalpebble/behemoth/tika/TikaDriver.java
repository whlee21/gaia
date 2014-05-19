package com.digitalpebble.behemoth.tika;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
//import org.apache.hadoop.mapred.FileInputFormat;
//import org.apache.hadoop.mapred.FileOutputFormat;
//import org.apache.hadoop.mapred.JobClient;
//import org.apache.hadoop.mapred.SequenceFileInputFormat;
//import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothConfiguration;
import com.digitalpebble.behemoth.BehemothDocument;

public class TikaDriver extends Configured implements Tool, TikaConstants {
	private static transient Logger log = LoggerFactory.getLogger(TikaDriver.class);

	public TikaDriver() {
		super();
	}

	public TikaDriver(Configuration conf) {
		super(conf);
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(BehemothConfiguration.create(), new TikaDriver(), args);

		System.exit(res);
	}

	public int run(String[] args) throws Exception {
		FileSystem fs = FileSystem.get(getConf());
		GroupBuilder gBuilder = new GroupBuilder().withName("Options:");
		List<Option> options = new ArrayList<Option>();
		Option inputOpt = buildOption("input", "i", "The input path", true, true, null);

		options.add(inputOpt);
		Option outOpt = buildOption("output", "o", "The output path", true, true, null);

		options.add(outOpt);
		Option tikaOpt = buildOption("tikaProcessor", "t",
				"The fully qualified name of a TikaProcessor class that handles the extraction", true, false, null);

		options.add(tikaOpt);
		Option mimeTypeOpt = buildOption("mimeType", "m", "The mime type to use", true, false, "");

		options.add(mimeTypeOpt);
		for (Option opt : options) {
			gBuilder = gBuilder.withOption(opt);
		}

		boolean success = false;
		Group group = gBuilder.create();
		try {
			Parser parser = new Parser();
			parser.setGroup(group);

			CommandLine cmdLine = parser.parse(args);
			Path inputPath = new Path(cmdLine.getValue(inputOpt).toString());
			Path outputPath = new Path(cmdLine.getValue(outOpt).toString());
			String handlerName = null;
			if (cmdLine.hasOption(tikaOpt)) {
				handlerName = cmdLine.getValue(tikaOpt).toString();
			}

			Job job = new Job(getConf());
			job.setJarByClass(getClass());

			if (cmdLine.hasOption(mimeTypeOpt)) {
				String mimeType = cmdLine.getValue(mimeTypeOpt).toString();
				job.getConfiguration().set("tika.mime.type", mimeType);
			}

			if ((handlerName != null) && (!handlerName.equals(""))) {
				job.getConfiguration().set("tika.processor", handlerName);
			}

			job.setJobName("Processing with Tika");

			job.setInputFormatClass(SequenceFileInputFormat.class);
			job.setOutputFormatClass(SequenceFileOutputFormat.class);

			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(BehemothDocument.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(BehemothDocument.class);

			job.setMapperClass(TikaMapper.class);

			job.setNumReduceTasks(0);

			FileInputFormat.addInputPath(job, inputPath);
			FileOutputFormat.setOutputPath(job, outputPath);
			try {
				long start = System.currentTimeMillis();
//				JobClient.runJob(job);
				success = job.waitForCompletion(true);
				long finish = System.currentTimeMillis();
				if (log.isInfoEnabled())
					log.info("TikaDriver completed. Timing: " + (finish - start) + " ms");
			} catch (Exception e) {
				e.printStackTrace();
				fs.delete(outputPath, true);
			} finally {
			}
		} catch (OptionException e) {
			log.error("Exception", e);
		}

		return success ? 0 : 1;
	}

	private Option buildOption(String name, String shortName, String description, boolean hasArg, boolean required,
			String defaultValue) {
		DefaultOptionBuilder optBuilder = new DefaultOptionBuilder().withLongName(name).withDescription(description)
				.withRequired(required);

		if (shortName != null) {
			optBuilder.withShortName(shortName);
		}

		if (hasArg) {
			ArgumentBuilder argBuilder = new ArgumentBuilder().withName(name).withMinimum(1).withMaximum(1);

			if (defaultValue != null) {
				argBuilder = argBuilder.withDefault(defaultValue);
			}

			optBuilder.withArgument(argBuilder.create());
		}

		return optBuilder.create();
	}
}
