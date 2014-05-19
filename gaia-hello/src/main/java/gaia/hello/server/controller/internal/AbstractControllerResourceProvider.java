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

import gaia.commons.server.controller.internal.AbstractResourceProvider;
import gaia.commons.server.controller.predicate.ArrayPredicate;
import gaia.commons.server.controller.predicate.EqualsPredicate;
import gaia.commons.server.controller.spi.Predicate;
import gaia.commons.server.controller.spi.Resource;
import gaia.commons.server.controller.spi.ResourceProvider;
import gaia.hello.server.controller.GaiaHelloController;

import java.util.Map;
import java.util.Set;

/**
 * Abstract resource provider implementation that maps to an Ambari management
 * controller.
 */
public abstract class AbstractControllerResourceProvider extends
		AbstractResourceProvider {

	/**
	 * The management controller to delegate to.
	 */
	private final GaiaHelloController helloController;

	// ----- Constructors ------------------------------------------------------

	/**
	 * Create a new resource provider for the given management controller.
	 * 
	 * @param propertyIds
	 *            the property ids
	 * @param keyPropertyIds
	 *            the key property ids
	 * @param helloController
	 *            the management controller
	 */
	protected AbstractControllerResourceProvider(Set<String> propertyIds,
			Map<Resource.Type, String> keyPropertyIds,
			GaiaHelloController helloController) {
		super(propertyIds, keyPropertyIds);
		this.helloController = helloController;
	}

	// ----- accessors ---------------------------------------------------------

	/**
	 * Get the associated management controller.
	 * 
	 * @return the associated management controller
	 */
	protected GaiaHelloController getManagementController() {
		return helloController;
	}

	// ----- utility methods ---------------------------------------------------

	/**
	 * Factory method for obtaining a resource provider based on a given type
	 * and management controller.
	 * 
	 * @param type
	 *            the resource type
	 * @param propertyIds
	 *            the property ids
	 * @param helloController
	 *            the management controller
	 * 
	 * @return a new resource provider
	 */
	public static ResourceProvider getResourceProvider(Resource.Type type,
			Set<String> propertyIds, Map<Resource.Type, String> keyPropertyIds,
			GaiaHelloController helloController) {
		switch (type) {
		case Hello:
			return new HelloResourceProvider(propertyIds, keyPropertyIds,
					helloController);
		default:
			throw new IllegalArgumentException("Unknown type " + type);
		}
	}

	/**
	 * Extracting given query_paramater value from the predicate
	 * 
	 * @param queryParameterId
	 * @param predicate
	 * @return
	 */
	protected static Object getQueryParameterValue(String queryParameterId,
			Predicate predicate) {

		Object result = null;

		if (predicate instanceof ArrayPredicate) {
			ArrayPredicate arrayPredicate = (ArrayPredicate) predicate;
			for (Predicate predicateItem : arrayPredicate.getPredicates()) {
				if (predicateItem instanceof EqualsPredicate) {
					EqualsPredicate equalsPredicate = (EqualsPredicate) predicateItem;
					if (queryParameterId
							.equals(equalsPredicate.getPropertyId())) {
						return equalsPredicate.getValue();
					}
				} else {
					result = getQueryParameterValue(queryParameterId,
							predicateItem);
					if (result != null) {
						return result;
					}
				}
			}

		}
		return result;
	}
}
