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

package com.digitalpebble.behemoth;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Mapper which can filter documents before they are written out.
 ***/

public class BehemothMapper extends Mapper<Text, BehemothDocument, Text, BehemothDocument> {

	public static final Logger LOG = LoggerFactory.getLogger(BehemothMapper.class);

	private DocumentFilter docFilter;

	/**
	 * Checks whether any filters have been specified in the configuration
	 * 
	 * @throws InterruptedException
	 **/
	public static boolean isRequired(Configuration conf) {
		return (DocumentFilter.isRequired(conf));
	}

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		docFilter = DocumentFilter.getFilters(context.getConfiguration());
	}

	@Override
	protected void map(Text key, BehemothDocument inputDoc, Context context) throws IOException, InterruptedException {
		boolean keep = docFilter.keep(inputDoc);
		if (!keep) {
			context.getCounter("BehemothMapper", "DOC SKIPPED BY FILTERS").increment(1);
		}
		context.write(key, inputDoc);
	}

}
