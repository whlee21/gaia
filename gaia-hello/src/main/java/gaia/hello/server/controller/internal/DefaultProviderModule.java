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

package gaia.hello.server.controller.internal;

import gaia.commons.server.controller.internal.AbstractProviderModule;
import gaia.commons.server.controller.spi.Resource;
import gaia.commons.server.controller.spi.ResourceProvider;
import gaia.commons.server.controller.utilities.PropertyHelper;
import gaia.hello.server.controller.GaiaHelloController;
import gaia.hello.server.controller.GaiaHelloServer;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * The default provider module implementation.
 */
public class DefaultProviderModule extends AbstractProviderModule {
	private static transient Logger LOG = LoggerFactory
			.getLogger(DefaultProviderModule.class);

	@Inject
	private GaiaHelloController helloController;

	// ----- Constructors ------------------------------------------------------

	/**
	 * Create a default provider module.
	 */
	public DefaultProviderModule() {
		if (helloController == null) {
			helloController = GaiaHelloServer.getController();
		}
	}

	// ----- utility methods ---------------------------------------------------

	@Override
	protected ResourceProvider createResourceProvider(Resource.Type type) {
		Set<String> propertyIds = PropertyHelper.getPropertyIds(type);
		Map<Resource.Type, String> keyPropertyIds = PropertyHelper
				.getKeyPropertyIds(type);
		switch (type) {
		// case Workflow:
		// return new WorkflowResourceProvider(propertyIds, keyPropertyIds);
		// case Job:
		// return new JobResourceProvider(propertyIds, keyPropertyIds);
		// case TaskAttempt:
		// return new TaskAttemptResourceProvider(propertyIds, keyPropertyIds);
		default:
			return AbstractControllerResourceProvider.getResourceProvider(type,
					propertyIds, keyPropertyIds, helloController);
		}
	}
}
