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

package gaia.commons.server.api.services;

import gaia.commons.server.Error;
import gaia.commons.server.GaiaException;
import gaia.commons.server.api.handlers.RequestHandler;
import gaia.commons.server.api.predicate.InvalidQueryException;
import gaia.commons.server.api.predicate.PredicateCompiler;
import gaia.commons.server.api.resources.ResourceInstance;
import gaia.commons.server.controller.internal.TemporalInfoImpl;
import gaia.commons.server.controller.spi.Predicate;
import gaia.commons.server.controller.spi.TemporalInfo;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request implementation.
 */
public abstract class BaseRequest implements Request {

	/**
	 * URI information
	 */
	private UriInfo m_uriInfo;

	/**
	 * Http headers
	 */
	private HttpHeaders m_headers;

	/**
	 * Http Body
	 */
	private RequestBody m_body;

	/**
	 * Query Predicate
	 */
	private Predicate m_predicate;

	/**
	 * Associated resource definition
	 */
	private ResourceInstance m_resource;

	protected ResultPostProcessor resultPostProcessor;

	/**
	 * Logger instance.
	 */
	private final static Logger LOG = LoggerFactory.getLogger(Request.class);

	/**
	 * Constructor.
	 * 
	 * @param headers
	 *          http headers
	 * @param body
	 *          http body
	 * @param uriInfo
	 *          uri information
	 * @param resource
	 *          associated resource definition
	 * 
	 */
	public BaseRequest(HttpHeaders headers, RequestBody body, UriInfo uriInfo, ResourceInstance resource) {
		m_headers = headers;
		m_uriInfo = uriInfo;
		m_resource = resource;
		m_body = body;
	}

	@Override
	public Result process() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Handling API Request: '" + getURI() + "'");
		}

		Result result;
		try {
			parseQueryPredicate();
			result = getRequestHandler().handleRequest(this);
		} catch (InvalidQueryException e) {
			result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, new Error("exception",
					Error.E_EXCEPTION, "Unable to compile query predicate: " + e.getMessage())));
		} catch (GaiaException e) {
			result = new ResultImpl(new ResultStatus(e.getStatus(), e.getErrors().toString()));
		}

		if (!result.getStatus().isErrorState()) {
			getResultPostProcessor().process(result);
		}

		return result;
	}

	@Override
	public ResourceInstance getResource() {
		return m_resource;
	}

	@Override
	public String getURI() {
		try {
			return URLDecoder.decode(m_uriInfo.getRequestUri().toASCIIString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unable to decode URI: " + e, e);
		}
	}

	@Override
	public int getAPIVersion() {
		return 1;
	}

	@Override
	public Predicate getQueryPredicate() {
		return m_predicate;
	}

	@Override
	public Map<String, TemporalInfo> getFields() {
		Map<String, TemporalInfo> mapProperties;
		String partialResponseFields = m_uriInfo.getQueryParameters().getFirst("fields");
		if (partialResponseFields == null) {
			mapProperties = Collections.emptyMap();
		} else {
			Set<String> setMatches = new HashSet<String>();
			// Pattern basically splits a string using ',' as the deliminator
			// unless
			// ',' is between '[' and ']'.
			// Actually, captures char sequences between ',' and all chars
			// between
			// '[' and ']' including ','.
			Pattern re = Pattern.compile("[^,\\[]*?\\[[^\\]]*?\\]|[^,]+");
			Matcher m = re.matcher(partialResponseFields);
			while (m.find()) {
				for (int groupIdx = 0; groupIdx < m.groupCount() + 1; groupIdx++) {
					setMatches.add(m.group(groupIdx));
				}
			}

			mapProperties = new HashMap<String, TemporalInfo>(setMatches.size());
			for (String field : setMatches) {
				TemporalInfo temporalInfo = null;
				if (field.contains("[")) {
					String[] temporalData = field.substring(field.indexOf('[') + 1, field.indexOf(']')).split(",");
					field = field.substring(0, field.indexOf('['));
					long start = Long.parseLong(temporalData[0].trim());
					long end = -1;
					long step = -1;
					if (temporalData.length >= 2) {
						end = Long.parseLong(temporalData[1].trim());
						if (temporalData.length == 3) {
							step = Long.parseLong(temporalData[2].trim());
						}
					}
					temporalInfo = new TemporalInfoImpl(start, end, step);
				}
				mapProperties.put(field, temporalInfo);
			}
		}

		return mapProperties;
	}

	@Override
	public Map<String, List<String>> getHttpHeaders() {
		return m_headers.getRequestHeaders();
	}

	@Override
	public RequestBody getBody() {
		return m_body;
	}

	/**
	 * Obtain the result post processor for the request.
	 * 
	 * @return the result post processor
	 */
	protected ResultPostProcessor getResultPostProcessor() {
		// todo: inject
		return new ResultPostProcessorImpl(this);
	}

	/**
	 * Obtain the predicate compiler which is used to compile the query string
	 * into a predicate.
	 * 
	 * @return the predicate compiler
	 */
	protected PredicateCompiler getPredicateCompiler() {
		return new PredicateCompiler();
	}

	/**
	 * Parse the query string and compile it into a predicate. The query string
	 * may have already been extracted from the http body. If the query string
	 * didn't exist in the body use the query string in the URL.
	 * 
	 * @throws InvalidQueryException
	 *           if unable to parse a non-null query string into a predicate
	 */
	private void parseQueryPredicate() throws InvalidQueryException {
		String queryString = m_body.getQueryString();
		if (queryString == null) {
			String uri = getURI();
			int qsBegin = uri.indexOf("?");

			queryString = (qsBegin == -1) ? null : uri.substring(qsBegin + 1);
		}

		if (queryString != null) {
			m_predicate = getPredicateCompiler().compile(queryString);
		}
	}

	/**
	 * Obtain the underlying request handler for the request.
	 * 
	 * @return the request handler
	 */
	protected abstract RequestHandler getRequestHandler();
}
