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

package gaia.commons.server.controller.internal;

import gaia.commons.server.controller.spi.Request;
import gaia.commons.server.controller.spi.Resource;
import gaia.commons.server.controller.utilities.PropertyHelper;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

/**
 *
 */
public class RequestImplTest {

	private static final Set<String> propertyIds = new HashSet<String>();

	static {
		propertyIds.add(PropertyHelper.getPropertyId("c1", "p1"));
		propertyIds.add(PropertyHelper.getPropertyId("c1", "p2"));
		propertyIds.add(PropertyHelper.getPropertyId("c2", "p3"));
		propertyIds.add(PropertyHelper.getPropertyId("c3", "p4"));
	}

	@Test
	public void testGetPropertyIds() {
		Request request = PropertyHelper.getReadRequest(propertyIds);

		Assert.assertEquals(propertyIds, request.getPropertyIds());
	}

	@Test
	public void testValidPropertyIds() {
		Request request = PropertyHelper.getReadRequest(PropertyHelper
				.getPropertyIds(Resource.Type.Collection));
		Set<String> validPropertyIds = request.getPropertyIds();

		for (String id : validPropertyIds) {
			System.out.println("id: " + id);
		}
		// Collection resource properties
		Assert.assertFalse(validPropertyIds
				.contains("Collections/unsupported_property_id"));
		Assert.assertTrue(validPropertyIds.contains("Collections/collection_name"));
		Assert.assertTrue(validPropertyIds.contains("Collections/instance_dir"));
		Assert.assertTrue(validPropertyIds.contains("Collections/template"));
		Assert.assertTrue(validPropertyIds.contains("Collections/num_shards"));
	}
}
