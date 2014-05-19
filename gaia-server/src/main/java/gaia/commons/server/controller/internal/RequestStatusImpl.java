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

import gaia.commons.server.controller.spi.RequestStatus;
import gaia.commons.server.controller.spi.Resource;
import gaia.commons.server.controller.utilities.PropertyHelper;

import java.util.Collections;
import java.util.Set;

/**
 * Default request status implementation.
 */
public class RequestStatusImpl implements RequestStatus {

	private final Resource requestResource;

	public RequestStatusImpl(Resource requestResource) {
		this.requestResource = requestResource;
	}

	@Override
	public Set<Resource> getAssociatedResources() {
		return Collections.emptySet(); // TODO : handle in M4
	}

	@Override
	public Resource getRequestResource() {
		return requestResource;
	}

	@Override
	public Status getStatus() {

		return requestResource == null ? Status.Complete : Status.valueOf((String) requestResource
				.getPropertyValue(PropertyHelper.getPropertyId("Requests", "status")));
	}
}
