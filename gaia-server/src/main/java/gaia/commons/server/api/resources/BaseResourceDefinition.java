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

package gaia.commons.server.api.resources;

import gaia.commons.server.api.services.Request;
import gaia.commons.server.api.util.TreeNode;
import gaia.commons.server.controller.spi.Resource;
import gaia.commons.server.controller.spi.Schema;
import gaia.commons.server.controller.spi.ServerController;
import gaia.commons.server.controller.utilities.ServerControllerHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Base resource definition. Contains behavior common to all resource types.
 */
public abstract class BaseResourceDefinition implements ResourceDefinition {

	/**
	 * Resource type. One of {@link Resource.Type}
	 */
	private Resource.Type m_type;

	/**
	 * Constructor.
	 * 
	 * @param resourceType
	 *            resource type
	 */
	public BaseResourceDefinition(Resource.Type resourceType) {
		m_type = resourceType;
	}

	@Override
	public Resource.Type getType() {
		return m_type;
	}

	@Override
	public Set<SubResourceDefinition> getSubResourceDefinitions() {
		return Collections.emptySet();
	}

	@Override
	public List<PostProcessor> getPostProcessors() {
		List<PostProcessor> listProcessors = new ArrayList<PostProcessor>();
		listProcessors.add(new BaseHrefPostProcessor());

		return listProcessors;
	}

	protected ServerController getServerController() {
		return ServerControllerHelper.getServerController();
	}

	@Override
	public boolean equals(Object o) {
		boolean result = false;
		if (this == o)
			result = true;
		if (o instanceof BaseResourceDefinition) {
			BaseResourceDefinition other = (BaseResourceDefinition) o;
			if (m_type == other.m_type)
				result = true;
		}
		return result;
	}

	@Override
	public int hashCode() {
		return m_type.hashCode();
	}

	class BaseHrefPostProcessor implements PostProcessor {
		@Override
		public void process(Request request, TreeNode<Resource> resultNode,
				String href) {
			Resource r = resultNode.getObject();
			TreeNode<Resource> parent = resultNode.getParent();

			if (parent.getName() != null) {
				Schema schema = getServerController()
						.getSchema(r.getType());
				Object id = r.getPropertyValue(schema.getKeyPropertyId(r
						.getType()));

				int i = href.indexOf("?");
				if (i != -1) {
					href = href.substring(0, i);
				}

				if (!href.endsWith("/")) {
					href = href + '/';
				}
				href = "true".equals(parent.getProperty("isCollection")) ? href
						+ id : href + parent.getName() + '/' + id;
			}
			resultNode.setProperty("href", href);
		}
	}
}
