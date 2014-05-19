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

import gaia.commons.server.controller.spi.Resource;
import gaia.commons.server.controller.spi.ResourceProvider;
import gaia.commons.server.controller.spi.Schema;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple schema implementation.
 */
public class SchemaImpl implements Schema {
	private static final Logger LOG = LoggerFactory.getLogger(SchemaImpl.class);
	/**
	 * The associated resource provider.
	 */
	private final ResourceProvider resourceProvider;

	// ----- Constructors ------------------------------------------------------

	/**
	 * Create a new schema for the given providers.
	 * 
	 * @param resourceProvider
	 *            the resource provider
	 * 
	 */
	public SchemaImpl(ResourceProvider resourceProvider) {
		this.resourceProvider = resourceProvider;
	}

	// ----- Schema ------------------------------------------------------------

	@Override
	public String getKeyPropertyId(Resource.Type type) {
		return resourceProvider.getKeyPropertyIds().get(type);
	}
}
