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

import gaia.commons.server.controller.spi.PropertyProvider;
import gaia.commons.server.controller.spi.ProviderModule;
import gaia.commons.server.controller.spi.Resource;
import gaia.commons.server.controller.spi.ResourceProvider;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract provider module implementation.
 */

public abstract class AbstractProviderModule implements ProviderModule,
		ResourceProviderObserver {

	protected final static Logger LOG = LoggerFactory
			.getLogger(AbstractProviderModule.class);

	private static final int PROPERTY_REQUEST_CONNECT_TIMEOUT = 5000;
	private static final int PROPERTY_REQUEST_READ_TIMEOUT = 10000;

	private volatile boolean initialized = false;

	/**
	 * The map of resource providers.
	 */
	private final Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<Resource.Type, ResourceProvider>();

	/**
	 * The map of lists of property providers.
	 */
	private final Map<Resource.Type, List<PropertyProvider>> propertyProviders = new HashMap<Resource.Type, List<PropertyProvider>>();

	@Override
	public ResourceProvider getResourceProvider(Resource.Type type) {
		if (!propertyProviders.containsKey(type)) {
			registerResourceProvider(type);
		}
		return resourceProviders.get(type);
	}

	@Override
	public List<PropertyProvider> getPropertyProviders(Resource.Type type) {

		if (!propertyProviders.containsKey(type)) {
			createPropertyProviders(type);
		}
		return propertyProviders.get(type);
	}

	protected abstract ResourceProvider createResourceProvider(
			Resource.Type type);

	protected void registerResourceProvider(Resource.Type type) {
		ResourceProvider resourceProvider = createResourceProvider(type);

		if (resourceProvider instanceof ObservableResourceProvider) {
			((ObservableResourceProvider) resourceProvider).addObserver(this);
		}

		putResourceProvider(type, resourceProvider);
	}

	protected void putResourceProvider(Resource.Type type,
			ResourceProvider resourceProvider) {
		resourceProviders.put(type, resourceProvider);
	}

	protected void putPropertyProviders(Resource.Type type,
			List<PropertyProvider> providers) {
		propertyProviders.put(type, providers);
	}

	protected void createPropertyProviders(Resource.Type type) {

		List<PropertyProvider> providers = new LinkedList<PropertyProvider>();

		URLStreamProvider streamProvider = new URLStreamProvider(
				PROPERTY_REQUEST_CONNECT_TIMEOUT, PROPERTY_REQUEST_READ_TIMEOUT);

		switch (type) {
		case Hello:
			// providers.add(new GangliaReportPropertyProvider(PropertyHelper
			// .getGangliaPropertyIds(type), streamProvider, this,
			// PropertyHelper.getPropertyId("Clusters", "cluster_name")));
			break;
		default:
			break;
		}
		putPropertyProviders(type, providers);
	}

	@Override
	public void update(ResourceProviderEvent event) {
		Resource.Type type = event.getResourceType();

		if (type == Resource.Type.Hello) {
			resetInit();
		}
	}

	private void resetInit() {
		if (initialized) {
			synchronized (this) {
				initialized = false;
			}
		}
	}
}
