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

package gaia.hello.server.api.resources;

import gaia.commons.server.api.resources.ResourceDefinition;
import gaia.commons.server.api.resources.ResourceInstance;
import gaia.commons.server.api.resources.ResourceInstanceFactory;
import gaia.commons.server.api.resources.ResourceInstanceImpl;
import gaia.commons.server.controller.spi.Resource;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating resource instances.
 */
public class ResourceInstanceFactoryImpl implements ResourceInstanceFactory {

	private static final Logger LOG = LoggerFactory
			.getLogger(ResourceInstanceFactoryImpl.class);

	// private ResourceDependencyManager dependencyManager = new
	// ResourceDependencyManager();

	@Override
	public ResourceInstance createResource(Resource.Type type,
			Map<Resource.Type, String> mapIds) {

		/**
		 * The resource definition for the specified type.
		 */

		// Class<? extends ResourceDefinition> clazz = dependencyManager
		// .getResourceDefinition(type);
		ResourceDefinition resourceDefinition;
		//
		// try {
		// resourceDefinition = clazz.newInstance();
		// } catch (InstantiationException e) {
		// throw new IllegalArgumentException("Unsupported resource type: "
		// + type);
		// } catch (IllegalAccessException e) {
		// throw new IllegalArgumentException("Unsupported resource type: "
		// + type);
		// }

		// todo: consider ResourceDependencyManager : Map<Resource.Type,
		// ResourceDefinition>
		switch (type) {
		case Hello:
			resourceDefinition = new HelloResourceDefinition();
			break;

		// case Request:
		// resourceDefinition = new RequestResourceDefinition();
		// break;

		default:
			throw new IllegalArgumentException("Unsupported resource type: "
					+ type);
		}

		return new ResourceInstanceImpl(mapIds, resourceDefinition, this);
	}
}
