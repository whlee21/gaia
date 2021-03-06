/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.digitalpebble.behemoth.languageidentification;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
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

public class LanguageIdDriver extends Configured implements Tool {
	private transient static Logger log = LoggerFactory.getLogger(LanguageIdDriver.class);

	public LanguageIdDriver() {
		super(null);
	}

	public LanguageIdDriver(Configuration conf) {
		super(conf);
	}

	public static void main(String args[]) throws Exception {
		int res = ToolRunner.run(BehemothConfiguration.create(), new LanguageIdDriver(), args);
		System.exit(res);
	}

	public int run(String[] args) throws Exception {

		final FileSystem fs = FileSystem.get(getConf());

		Options options = new Options();
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		// create the parser
		CommandLineParser parser = new GnuParser();

		options.addOption("h", "help", false, "print this message");
		options.addOption("i", "input", true, "input file or directory");
		options.addOption("o", "output", true, "output Behemoth corpus");
		options.addOption("w", "overwrite", false, "overwrite the output");

		Path inputPath = null;
		Path outputPath = null;

		boolean overWrite = false;

		// parse the command line arguments
		CommandLine cmdLine = null;
		try {
			cmdLine = parser.parse(options, args);
			String input = cmdLine.getOptionValue("i");
			String output = cmdLine.getOptionValue("o");
			if (cmdLine.hasOption("help")) {
				formatter.printHelp("LanguageIdDriver", options);
				return 0;
			}
			if (input == null | output == null) {
				formatter.printHelp("LanguageIdDriver", options);
				return -1;
			}
			inputPath = new Path(input);
			outputPath = new Path(output);
			if (cmdLine.hasOption("overwrite")) {
				overWrite = true;
			}
		} catch (ParseException e) {
			formatter.printHelp("LanguageIdDriver", options);
		}

		// check whether needs overwriting
		if (FileSystem.get(outputPath.toUri(), getConf()).exists(outputPath)) {
			if (!overWrite) {
				System.out.println("Output path " + outputPath + " already exists. Use option -w to overwrite.");
				return 0;
			} else
				fs.delete(outputPath, true);
		}

		Job job = new Job(getConf());
		job.setJarByClass(this.getClass());

		job.setJobName("Processing with Language Identifier");

		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(BehemothDocument.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(BehemothDocument.class);

		job.setMapperClass(LanguageIdMapper.class);

		job.setNumReduceTasks(0);

		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);

		boolean success = false;
		try {
			long start = System.currentTimeMillis();
			// JobClient.runJob(job);
			job.waitForCompletion(true);
			long finish = System.currentTimeMillis();
			if (log.isInfoEnabled()) {
				log.info("LanguagedIdDriver completed. Timing: " + (finish - start) + " ms");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			fs.delete(outputPath, true);
			return -1;
		} finally {
		}

		return success ? 0 : 1;
	}

}
