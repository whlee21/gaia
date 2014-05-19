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

package com.digitalpebble.behemoth.gate;

import java.io.IOException;

import org.apache.hadoop.io.Text;

import com.digitalpebble.behemoth.BehemothDocument;

/**
 * Process BehemothDocuments with GATE and generate BehemothDocuments as output
 **/

public class GATEMapper extends AbstractGATEMapper {
	protected void map(Text key, BehemothDocument behedoc, Context context) throws IOException, InterruptedException {
		BehemothDocument[] outputDocs = processor.process(behedoc, context);
		for (BehemothDocument doc : outputDocs) {
			// TODO output under a different key?
			context.write(key, doc);
		}
	}
}
