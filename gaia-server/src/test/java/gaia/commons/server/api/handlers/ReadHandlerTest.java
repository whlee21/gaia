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

package gaia.commons.server.api.handlers;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import gaia.commons.server.api.query.Query;
import gaia.commons.server.api.resources.ResourceInstance;
import gaia.commons.server.api.services.Request;
import gaia.commons.server.api.services.Result;
import gaia.commons.server.api.services.ResultStatus;
import gaia.commons.server.controller.spi.NoSuchParentResourceException;
import gaia.commons.server.controller.spi.NoSuchResourceException;
import gaia.commons.server.controller.spi.Predicate;
import gaia.commons.server.controller.spi.Resource;
import gaia.commons.server.controller.spi.SystemException;
import gaia.commons.server.controller.spi.TemporalInfo;
import gaia.commons.server.controller.spi.UnsupportedPropertyException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.easymock.Capture;
import org.junit.Test;

/**
 * Unit tests for ReadHandler.
 */
public class ReadHandlerTest {

	@Test
	public void testHandleRequest__InvalidField() {
		Request request = createNiceMock(Request.class);
		ResourceInstance resource = createNiceMock(ResourceInstance.class);
		Query query = createStrictMock(Query.class);

		Map<String, TemporalInfo> mapPartialResponseFields = new HashMap<String, TemporalInfo>();
		mapPartialResponseFields.put("foo/bar", null);

		expect(request.getResource()).andReturn(resource);
		expect(request.getFields()).andReturn(mapPartialResponseFields);
		expect(resource.getQuery()).andReturn(query);

		query.addProperty("foo", "bar", null);
		expectLastCall().andThrow(new IllegalArgumentException("testMsg"));

		replay(request, resource, query);

		ReadHandler handler = new ReadHandler();
		Result result = handler.handleRequest(request);

		assertEquals(ResultStatus.STATUS.BAD_REQUEST, result.getStatus()
				.getStatus());
		assertEquals("testMsg", result.getStatus().getMessage());

		verify(request, resource, query);
	}

	@Test
	public void testHandleRequest__OK() throws Exception {
		Request request = createStrictMock(Request.class);
		ResourceInstance resource = createStrictMock(ResourceInstance.class);
		Query query = createMock(Query.class);
		Predicate predicate = createMock(Predicate.class);
		Result result = createStrictMock(Result.class);
		Capture<ResultStatus> resultStatusCapture = new Capture<ResultStatus>();

		Map<String, TemporalInfo> mapPartialResponseFields = new HashMap<String, TemporalInfo>();
		mapPartialResponseFields.put("foo", null);
		mapPartialResponseFields.put("bar/c", null);
		mapPartialResponseFields.put("bar/d/e", null);
		mapPartialResponseFields.put("category/", null);
		// expectations
		expect(request.getResource()).andReturn(resource);
		expect(resource.getQuery()).andReturn(query);

		expect(request.getFields()).andReturn(mapPartialResponseFields);
		query.addProperty(null, "foo", null);
		query.addProperty("bar", "c", null);
		query.addProperty("bar/d", "e", null);
		query.addProperty("category", "", null);

		expect(request.getQueryPredicate()).andReturn(predicate);
		query.setUserPredicate(predicate);
		expect(query.execute()).andReturn(result);
		result.setResultStatus(capture(resultStatusCapture));

		replay(request, resource, query, predicate, result);

		// test
		ReadHandler handler = new ReadHandler();
		assertSame(result, handler.handleRequest(request));
		assertEquals(ResultStatus.STATUS.OK, resultStatusCapture.getValue()
				.getStatus());
		verify(request, resource, query, predicate, result);
	}

	// FIXME:
//	@Test
//	public void testHandleRequest__SystemException() throws Exception {
//		Request request = createStrictMock(Request.class);
//		ResourceInstance resource = createStrictMock(ResourceInstance.class);
//		Query query = createMock(Query.class);
//		Predicate predicate = createMock(Predicate.class);
//
//		expect(request.getResource()).andReturn(resource);
//		expect(resource.getQuery()).andReturn(query);
//
//		expect(request.getFields()).andReturn(
//				Collections.<String, TemporalInfo> emptyMap());
//
//		expect(request.getQueryPredicate()).andReturn(predicate);
//		query.setUserPredicate(predicate);
//		SystemException systemException = new SystemException("testMsg",
//				new RuntimeException());
//		expect(query.execute()).andThrow(systemException);
//
//		replay(request, resource, query, predicate);
//
//		// test
//		ReadHandler handler = new ReadHandler();
//		Result result = handler.handleRequest(request);
//		assertEquals(ResultStatus.STATUS.SERVER_ERROR, result.getStatus()
//				.getStatus());
//		assertEquals(systemException.toString(), result.getStatus()
//				.getMessage());
//		verify(request, resource, query, predicate);
//	}

//	@Test
//	public void testHandleRequest__NoSuchParentResourceException()
//			throws Exception {
//		Request request = createStrictMock(Request.class);
//		ResourceInstance resource = createStrictMock(ResourceInstance.class);
//		Query query = createMock(Query.class);
//		Predicate predicate = createMock(Predicate.class);
////		NoSuchParentResourceException exception = new NoSuchParentResourceException(
////				"exceptionMsg", new RuntimeException());
//
//		expect(request.getResource()).andReturn(resource);
//		expect(resource.getQuery()).andReturn(query);
//
//		expect(request.getFields()).andReturn(
//				Collections.<String, TemporalInfo> emptyMap());
//
//		expect(request.getQueryPredicate()).andReturn(predicate);
//		query.setUserPredicate(predicate);
//
//		expect(query.execute()).andThrow(exception);
//
//		replay(request, resource, query, predicate);
//
//		// test
//		ReadHandler handler = new ReadHandler();
//		Result result = handler.handleRequest(request);
//		assertEquals(ResultStatus.STATUS.NOT_FOUND, result.getStatus()
//				.getStatus());
//		assertEquals("exceptionMsg", result.getStatus().getMessage());
//		verify(request, resource, query, predicate);
//	}

	@Test
	public void testHandleRequest__UnsupportedPropertyException()
			throws Exception {
		Request request = createStrictMock(Request.class);
		ResourceInstance resource = createStrictMock(ResourceInstance.class);
		Query query = createMock(Query.class);
		Predicate predicate = createMock(Predicate.class);
		UnsupportedPropertyException exception = new UnsupportedPropertyException(
				Resource.Type.Collection, Collections.singleton("foo"));

		expect(request.getResource()).andReturn(resource);
		expect(resource.getQuery()).andReturn(query);

		expect(request.getFields()).andReturn(
				Collections.<String, TemporalInfo> emptyMap());

		expect(request.getQueryPredicate()).andReturn(predicate);
		query.setUserPredicate(predicate);

		expect(query.execute()).andThrow(exception);

		replay(request, resource, query, predicate);

		// test
		ReadHandler handler = new ReadHandler();
		Result result = handler.handleRequest(request);
		assertEquals(ResultStatus.STATUS.BAD_REQUEST, result.getStatus()
				.getStatus());
		assertEquals(exception.getMessage(), result.getStatus().getMessage());
		verify(request, resource, query, predicate);
	}

//	@Test
//	public void testHandleRequest__NoSuchResourceException_OK()
//			throws Exception {
//		Request request = createStrictMock(Request.class);
//		ResourceInstance resource = createStrictMock(ResourceInstance.class);
//		Query query = createMock(Query.class);
//		Predicate predicate = createMock(Predicate.class);
//		NoSuchResourceException exception = new NoSuchResourceException("msg",
//				new RuntimeException());
//
//		expect(request.getResource()).andReturn(resource);
//		expect(resource.getQuery()).andReturn(query);
//
//		expect(request.getFields()).andReturn(
//				Collections.<String, TemporalInfo> emptyMap());
//
//		expect(request.getQueryPredicate()).andReturn(predicate).anyTimes();
//		query.setUserPredicate(predicate);
//
//		expect(query.execute()).andThrow(exception);
//
//		replay(request, resource, query, predicate);
//
//		// test
//		ReadHandler handler = new ReadHandler();
//		Result result = handler.handleRequest(request);
//		// ok because this is a query that returned no rows
//		assertEquals(ResultStatus.STATUS.OK, result.getStatus().getStatus());
//		verify(request, resource, query, predicate);
//	}

//	@Test
//	public void testHandleRequest__NoSuchResourceException_NOT_FOUND()
//			throws Exception {
//		Request request = createStrictMock(Request.class);
//		ResourceInstance resource = createStrictMock(ResourceInstance.class);
//		Query query = createMock(Query.class);
//		NoSuchResourceException exception = new NoSuchResourceException("msg",
//				new RuntimeException());
//
//		expect(request.getResource()).andReturn(resource);
//		expect(resource.getQuery()).andReturn(query);
//
//		expect(request.getFields()).andReturn(
//				Collections.<String, TemporalInfo> emptyMap());
//
//		expect(request.getQueryPredicate()).andReturn(null).anyTimes();
//		query.setUserPredicate(null);
//
//		expect(query.execute()).andThrow(exception);
//
//		replay(request, resource, query);
//
//		// test
//		ReadHandler handler = new ReadHandler();
//		Result result = handler.handleRequest(request);
//		// not a query, so not found
//		assertEquals(ResultStatus.STATUS.NOT_FOUND, result.getStatus()
//				.getStatus());
//		assertEquals(exception.getMessage(), result.getStatus().getMessage());
//		verify(request, resource, query);
//	}

	// todo: reverted to just logging the exception and re-throwing it
//	@Test
//	public void testHandleRequest__RuntimeException() throws Exception {
//		Request request = createStrictMock(Request.class);
//		ResourceInstance resource = createStrictMock(ResourceInstance.class);
//		Query query = createMock(Query.class);
//		RuntimeException exception = new RuntimeException("msg");
//		
//		expect(request.getResource()).andReturn(resource);
//		expect(resource.getQuery()).andReturn(query);
//
//		expect(request.getFields()).andReturn(
//				Collections.<String, TemporalInfo> emptyMap());
//
//		expect(request.getQueryPredicate()).andReturn(null).anyTimes();
//		query.setUserPredicate(null);
//
//		expect(query.execute()).andThrow(exception);
//
//		replay(request, resource, query);
//
//		// test
//		ReadHandler handler = new ReadHandler();
//		Result result = handler.handleRequest(request);
//		// not a query, so not found
//		assertEquals(ResultStatus.STATUS.SERVER_ERROR, result.getStatus()
//				.getStatus());
//		assertEquals(exception.toString(), result.getStatus().getMessage());
//		verify(request, resource, query);
//	}
}
