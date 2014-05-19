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

package gaia.commons.server.api.services.persistence;

import gaia.commons.server.api.resources.ResourceInstance;
import gaia.commons.server.api.services.NamedPropertySet;
import gaia.commons.server.api.services.RequestBody;
import gaia.commons.server.controller.spi.NoSuchParentResourceException;
import gaia.commons.server.controller.spi.NoSuchResourceException;
import gaia.commons.server.controller.spi.Request;
import gaia.commons.server.controller.spi.RequestStatus;
import gaia.commons.server.controller.spi.Resource;
import gaia.commons.server.controller.spi.ResourceAlreadyExistsException;
import gaia.commons.server.controller.spi.Schema;
import gaia.commons.server.controller.spi.ServerController;
import gaia.commons.server.controller.spi.SystemException;
import gaia.commons.server.controller.spi.UnsupportedPropertyException;
import gaia.commons.server.controller.utilities.PropertyHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistence Manager implementation.
 */
public class PersistenceManagerImpl implements PersistenceManager {

	private static final Logger LOG = LoggerFactory.getLogger(PersistenceManagerImpl.class);
	/**
	 * Cluster Controller reference.
	 */
	private ServerController m_controller;

	/**
	 * Constructor.
	 * 
	 * @param controller
	 *          the cluster controller
	 */
	public PersistenceManagerImpl(ServerController controller) {
		m_controller = controller;
	}

	@Override
	public RequestStatus create(ResourceInstance resource, RequestBody requestBody) throws UnsupportedPropertyException,
			SystemException, ResourceAlreadyExistsException, NoSuchParentResourceException {
		Map<Resource.Type, String> mapResourceIds = resource.getIds();
		Resource.Type type = resource.getResourceDefinition().getType();
		Schema schema = m_controller.getSchema(type);
		Set<NamedPropertySet> setProperties = requestBody.getNamedPropertySets();
		if (setProperties.size() == 0) {
			requestBody.addPropertySet(new NamedPropertySet("", new HashMap<String, Object>()));
		}
		for (NamedPropertySet propertySet : setProperties) {
			for (Map.Entry<Resource.Type, String> entry : mapResourceIds.entrySet()) {
				Map<String, Object> mapProperties = propertySet.getProperties();
				String property = schema.getKeyPropertyId(entry.getKey());
				if (!mapProperties.containsKey(property)) {
					mapProperties.put(property, entry.getValue());
				}
			}
		}
		return m_controller.createResources(type, createControllerRequest(requestBody));
	}

	@Override
	public RequestStatus update(ResourceInstance resource, RequestBody requestBody) throws UnsupportedPropertyException,
			SystemException, NoSuchParentResourceException, NoSuchResourceException {

		return m_controller.updateResources(resource.getResourceDefinition().getType(),
				createControllerRequest(requestBody), resource.getQuery().getPredicate());
	}

	@Override
	public RequestStatus delete(ResourceInstance resource, RequestBody requestBody) throws UnsupportedPropertyException,
			SystemException, NoSuchParentResourceException, NoSuchResourceException {
		// todo: need to account for multiple resources and user predicate
		return m_controller.deleteResources(resource.getResourceDefinition().getType(), resource.getQuery().getPredicate());

	}

	protected Request createControllerRequest(RequestBody body) {
		return PropertyHelper.getCreateRequest(body.getPropertySets(), body.getRequestInfoProperties());
	}
}
