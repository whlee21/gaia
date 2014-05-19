/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gaia.commons.server.api.services.parsers;

import gaia.commons.server.api.services.RequestBody;

import java.util.Set;

/**
 * Parse the provided String into a map of properties and associated values.
 */
public interface RequestBodyParser {

	/**
	 * RequestInfo category path.
	 */
	public static final String REQUEST_INFO_PATH = "RequestInfo";
	/**
	 * Name of the query property which may exist under REQUEST_INFO_PATH.
	 */
	public static final String QUERY_FIELD_NAME = "query";

	/**
	 * Path to the body object.
	 */
	public static final String BODY_TITLE = "Body";

	/**
	 * Parse the provided string into request bodies based on the properties in
	 * the given body string.
	 * 
	 * @param body
	 *            the string body to be parsed
	 * 
	 * @return a set of {@link RequestBody} instances
	 */
	public Set<RequestBody> parse(String body) throws BodyParseException;
}
