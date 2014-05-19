package com.digitalpebble.behemoth.io.warc;

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

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.nutch.metadata.HttpHeaders;
import org.apache.nutch.protocol.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothConfiguration;
import com.digitalpebble.behemoth.BehemothDocument;

import edu.cmu.lemurproject.WarcFileInputFormat;
import edu.cmu.lemurproject.WarcRecord;
import edu.cmu.lemurproject.WritableWarcRecord;

/**
 * Converts a WARC archive into a Behemoth datastructure for further processing
 */
public class WARCConverterJob extends Configured implements Tool {

	public static final Logger LOG = LoggerFactory.getLogger(WARCConverterJob.class);

	public WARCConverterJob() {
		this(null);
	}

	public WARCConverterJob(Configuration conf) {
		super(conf);
	}

	public void convert(Path warcpath, Path output) throws IOException, ClassNotFoundException, InterruptedException {

		Job job = new Job(getConf());
		job.setJobName("Convert WARC " + warcpath);

		job.setJarByClass(this.getClass());

		FileInputFormat.addInputPath(job, warcpath);
		job.setInputFormatClass(WarcFileInputFormat.class);

		job.setMapperClass(WARCConverterMapper.class);

		// no reducers
		job.setNumReduceTasks(0);

		FileOutputFormat.setOutputPath(job, output);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(BehemothDocument.class);

		long start = System.currentTimeMillis();
		job.waitForCompletion(true);
		long finish = System.currentTimeMillis();
		if (LOG.isInfoEnabled()) {
			LOG.info("WARCConverterJob completed. Timing: " + (finish - start) + " ms");
		}
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(BehemothConfiguration.create(), new WARCConverterJob(), args);
		System.exit(res);
	}

	public int run(String[] args) throws Exception {
		String usage = "Usage: WARCConverterJob archive output";

		if (args.length == 0) {
			System.err.println(usage);
			System.exit(-1);
		}
		Path segment = new Path(args[0]);
		Path output = new Path(args[1]);
		convert(segment, output);
		return 0;
	}

	private class WARCConverterMapper extends Mapper<LongWritable, WritableWarcRecord, Text, BehemothDocument> {

		@Override
		protected void setup(Context context) {
			setConf(context.getConfiguration());
		}

		@Override
		protected void cleanup(Context context) {
		}

		private Text newKey = new Text();

		@Override
		protected void map(LongWritable key, WritableWarcRecord record, Context context) throws IOException,
				InterruptedException {

			BehemothDocument behemothDocument = new BehemothDocument();

			WarcRecord wr = record.getRecord();

			if (wr.getHeaderRecordType().equals("response") == false)
				return;

			byte[] binarycontent = wr.getContent();

			String uri = wr.getHeaderMetadataItem("WARC-Target-URI");

			// skip non http documents
			if (uri.startsWith("http") == false)
				return;

			HttpResponse response;
			try {
				response = new HttpResponse(binarycontent);
			} catch (ProtocolException e) {
				return;
			}

			behemothDocument.setUrl(uri);
			newKey.set(uri);

			String contentType = response.getHeader(HttpHeaders.CONTENT_TYPE);
			behemothDocument.setContentType(contentType);
			behemothDocument.setContent(response.getContent());

			context.write(newKey, behemothDocument);
		}
	}

}
