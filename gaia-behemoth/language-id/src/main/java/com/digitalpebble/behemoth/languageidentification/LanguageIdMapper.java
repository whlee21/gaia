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

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothDocument;
import com.digitalpebble.behemoth.DocumentFilter;

public class LanguageIdMapper extends Mapper<Text, BehemothDocument, Text, BehemothDocument> {

	private static final Logger LOG = LoggerFactory.getLogger(LanguageIdMapper.class);

	protected static LanguageIdProcessor processor;

	private DocumentFilter filter;

	@Override
	protected void map(Text text, BehemothDocument inputDoc, Context context) throws IOException, InterruptedException {

		BehemothDocument[] documents = processor.process(inputDoc, context);
		if (documents != null) {
			for (int i = 0; i < documents.length; i++) {
				boolean keep = filter.keep(documents[i]);
				if (keep)
					context.write(text, documents[i]);
				else
					context.getCounter("LanguageIDMapper", "FILTERED").increment(1);
			}
		}
	}

	@Override
	protected void setup(Context context) {
		filter = DocumentFilter.getFilters(context.getConfiguration());
		if (processor == null) {
			long start = System.currentTimeMillis();
			processor = new LanguageIdProcessor();
			processor.setConf(context.getConfiguration());
			long end = System.currentTimeMillis();
			LOG.info("LanguageIdProcessor initialised in " + (end - start) + " msec");
		} else
			LOG.info("Reusing existing language processor");

	}
}
