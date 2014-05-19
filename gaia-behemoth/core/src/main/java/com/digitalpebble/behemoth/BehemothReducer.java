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
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Reducer which can filter documents before they are written out.
 ***/

public class BehemothReducer extends Reducer<Text, BehemothDocument, Text, BehemothDocument> {

	public static final Logger LOG = LoggerFactory.getLogger(BehemothReducer.class);

	private DocumentFilter docFilter;

	/**
	 * Checks whether any filters have been specified in the configuration
	 **/
	public static boolean isRequired(Configuration conf) {
		if (DocumentFilter.isRequired(conf))
			return true;
		return false;
	}

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		docFilter = DocumentFilter.getFilters(context.getConfiguration());
	}

	@Override
	protected void reduce(Text key, Iterable<BehemothDocument> docs, Context context) throws IOException,
			InterruptedException {

		for (BehemothDocument inputDoc : docs) {
			boolean keep = docFilter.keep(inputDoc);
			if (!keep) {
				context.getCounter("BehemothReducer", "DOC SKIPPED BY FILTERS").increment(1L);
				continue;
			}
			context.write(key, inputDoc);

		}

	}
}
