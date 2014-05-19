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

import gaia.commons.server.api.resources.ResourceInstance;
import gaia.commons.server.api.services.Request;
import gaia.commons.server.api.services.RequestBody;
import gaia.commons.server.api.services.Result;
import gaia.commons.server.api.services.ResultImpl;
import gaia.commons.server.api.services.persistence.PersistenceManager;
import gaia.commons.server.api.services.persistence.PersistenceManagerImpl;
import gaia.commons.server.api.util.TreeNode;
import gaia.commons.server.controller.spi.Predicate;
import gaia.commons.server.controller.spi.RequestStatus;
import gaia.commons.server.controller.spi.Resource;
import gaia.commons.server.controller.spi.ServerController;
import gaia.commons.server.controller.utilities.ServerControllerHelper;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base handler for operations that persist state to the back-end.
 */
public abstract class BaseManagementHandler implements RequestHandler {

	/**
	 * Logger instance.
	 */
	protected final static Logger LOG = LoggerFactory
			.getLogger(BaseManagementHandler.class);

	/**
	 * PersistenceManager implementation.
	 */
	PersistenceManager m_pm = new PersistenceManagerImpl(getServerController());

	/**
	 * Constructor.
	 */
	protected BaseManagementHandler() {
	}

	@Override
	public Result handleRequest(Request request) {
		Predicate queryPredicate = request.getQueryPredicate();
		if (queryPredicate != null) {
			request.getResource().getQuery().setUserPredicate(queryPredicate);
		}
		return persist(request.getResource(), request.getBody());
	}

	/**
	 * Create a result from a request status.
	 * 
	 * @param requestStatus
	 *            the request status to build the result from.
	 * 
	 * @return a Result instance for the provided request status
	 */
	protected Result createResult(RequestStatus requestStatus) {

		boolean isSynchronous = requestStatus.getStatus() == RequestStatus.Status.Complete;
		Result result = new ResultImpl(isSynchronous);
		TreeNode<Resource> tree = result.getResultTree();

		if (!isSynchronous) {
			tree.addChild(requestStatus.getRequestResource(), "request");
		}

		// todo: currently always empty
		Set<Resource> setResources = requestStatus.getAssociatedResources();
		if (!setResources.isEmpty()) {
			TreeNode<Resource> resourcesNode = tree.addChild(null, "resources");

			int count = 1;
			for (Resource resource : setResources) {
				// todo: provide a more meaningful node name
				resourcesNode.addChild(resource, resource.getType() + ":"
						+ count++);
			}
		}
		return result;
	}
	  //todo: inject ClusterController, PersistenceManager

	  /**
	   * Get the cluster controller instance.
	   *
	   * @return cluster controller
	   */
	  protected ServerController getServerController() {
	    return ServerControllerHelper.getServerController();
	  }

	  /**
	   * Get the persistence manager instance.
	   *
	   * @return persistence manager
	   */
	  protected PersistenceManager getPersistenceManager() {
	    return m_pm;
	  }
	  
	/**
	 * Persist the operation to the back end.
	 * 
	 * @param resource
	 *            associated resource
	 * @param body
	 *            associated request body
	 * 
	 * @return the result of the persist operation
	 */
	protected abstract Result persist(ResourceInstance resource,
			RequestBody body);
}
