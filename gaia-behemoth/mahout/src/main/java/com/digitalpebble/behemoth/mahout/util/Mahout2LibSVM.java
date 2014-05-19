package com.digitalpebble.behemoth.mahout.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.HadoopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothConfiguration;
import com.digitalpebble.behemoth.mahout.SparseVectorsFromBehemoth;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * Generates a vector file for libSVM from a Mahout Vector and Label file
 * {@link SparseVectorsFromBehemoth}
 **/

public class Mahout2LibSVM extends Configured implements Tool {

	private transient static Logger log = LoggerFactory.getLogger(Mahout2LibSVM.class);

	public static void main(String[] args) {
		int res;
		try {
			res = ToolRunner.run(BehemothConfiguration.create(), new Mahout2LibSVM(), args);
		} catch (Exception e) {
			res = -1;
		}
		System.exit(res);
	}

	public int run(String[] args) throws Exception {

		Options options = new Options();
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		// create the parser
		CommandLineParser parser = new GnuParser();

		options.addOption("h", "help", false, "print this message");
		options.addOption("v", "vector", true, "input vector sequencefile");
		options.addOption("l", "label", true, "input vector sequencefile");
		options.addOption("o", "output", true, "output Behemoth corpus");

		// parse the command line arguments
		CommandLine line = null;
		try {
			line = parser.parse(options, args);
			if (line.hasOption("help")) {
				formatter.printHelp("CorpusGenerator", options);
				return 0;
			}
			if (!line.hasOption("v") | !line.hasOption("o") | !line.hasOption("l")) {
				formatter.printHelp("CorpusGenerator", options);
				return -1;
			}
		} catch (ParseException e) {
			formatter.printHelp("CorpusGenerator", options);
		}

		Path vectorPath = new Path(line.getOptionValue("v"));
		Path labelPath = new Path(line.getOptionValue("l"));
		String output = line.getOptionValue("o");

		Path tempOutput = new Path(vectorPath.getParent(), "temp-" + System.currentTimeMillis());

		// extracts the string representations from the vectors
		int retVal = vectorToString(vectorPath, tempOutput);
		if (retVal != 0) {
			HadoopUtil.delete(getConf(), tempOutput);
			return retVal;
		}

		Path tempOutput2 = new Path(vectorPath.getParent(), "temp-" + System.currentTimeMillis());

		retVal = convert(tempOutput, labelPath, tempOutput2);

		// delete the temp output
		HadoopUtil.delete(getConf(), tempOutput);

		if (retVal != 0) {
			HadoopUtil.delete(getConf(), tempOutput2);
			return retVal;
		}

		// convert tempOutput to standard file
		BufferedWriter bow = new BufferedWriter(new FileWriter(new File(output)));

		// the label dictionary is not dumped to text
		int labelMaxIndex = 0;
		Map<String, Integer> labelIndex = new HashMap<String, Integer>();

		Configuration conf = getConf();
		FileSystem fs = FileSystem.get(conf);
		FileStatus[] fss = fs.listStatus(tempOutput2);
		try {
			for (FileStatus status : fss) {
				Path path = status.getPath();
				// skips the _log or _SUCCESS files
				if (!path.getName().startsWith("part-") && !path.getName().equals(tempOutput2.getName()))
					continue;
				SequenceFile.Reader reader = new SequenceFile.Reader(fs, path, conf);
				// read the key + values in that file
				Text key = new Text();
				Text value = new Text();
				while (reader.next(key, value)) {
					String label = key.toString();
					// replace the label by its index
					Integer indexLabel = labelIndex.get(label);
					if (indexLabel == null) {
						indexLabel = new Integer(labelMaxIndex);
						labelIndex.put(label, indexLabel);
						labelMaxIndex++;
					}
					String val = value.toString();
					bow.append(indexLabel.toString()).append(val).append("\n");
				}
				reader.close();
			}
			bow.flush();
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			bow.close();
			fs.delete(tempOutput2, true);
		}
		return 0;
	}

	public int vectorToString(Path vectorPath, Path output) throws IOException, InterruptedException,
			ClassNotFoundException {
		Job job = new Job(getConf());
		job.setJobName(this.getClass().getName());
		job.setJarByClass(this.getClass());
		FileInputFormat.addInputPath(job, vectorPath);
		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setNumReduceTasks(0);
		job.setMapperClass(Mahout2LibSVMMapper.class);
		job.setReducerClass(Mahout2LibSVMReducer.class);
		FileOutputFormat.setOutputPath(job, output);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		// RunningJob rj = JobClient.runJob(job);
		//
		// if (rj.isSuccessful() == false)
		// return -1;
		// return 0;
		return job.waitForCompletion(true) ? 0 : 1;
	}

	public int convert(Path vectorPath, Path labelPath, Path output) throws IOException, InterruptedException,
			ClassNotFoundException {
		Job job = new Job(getConf());
		job.setJobName(this.getClass().getName());
		job.setJarByClass(this.getClass());
		FileInputFormat.addInputPath(job, vectorPath);
		FileInputFormat.addInputPath(job, labelPath);
		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setMapperClass(IdentityMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		// 1 reducers
		job.setNumReduceTasks(1);
		job.setReducerClass(Mahout2LibSVMReducer.class);
		FileOutputFormat.setOutputPath(job, output);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		boolean success = job.waitForCompletion(true);
		if (log.isInfoEnabled()) {
			log.info("Conversion: done");
		}
		return success ? 0 : 1;
	}

}
