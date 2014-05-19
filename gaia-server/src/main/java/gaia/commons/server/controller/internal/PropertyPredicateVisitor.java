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

import gaia.commons.server.controller.predicate.AlwaysPredicate;
import gaia.commons.server.controller.predicate.ArrayPredicate;
import gaia.commons.server.controller.predicate.CategoryPredicate;
import gaia.commons.server.controller.predicate.ComparisonPredicate;
import gaia.commons.server.controller.predicate.PredicateVisitor;
import gaia.commons.server.controller.predicate.PropertyPredicate;
import gaia.commons.server.controller.predicate.UnaryPredicate;
import gaia.commons.server.controller.spi.Predicate;
import gaia.commons.server.controller.utilities.PredicateHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Predicate visitor for extracting property values from the
 * {@link PropertyPredicate}s of a predicate graph.
 */
public class PropertyPredicateVisitor implements PredicateVisitor {
	private final Map<String, Object> properties = new HashMap<String, Object>();

	@Override
	public void acceptComparisonPredicate(ComparisonPredicate predicate) {
		properties.put(predicate.getPropertyId(), predicate.getValue());
	}

	@Override
	public void acceptArrayPredicate(ArrayPredicate predicate) {
		Predicate[] predicates = predicate.getPredicates();
		for (Predicate predicate1 : predicates) {
			PredicateHelper.visit(predicate1, this);
		}
	}

	@Override
	public void acceptUnaryPredicate(UnaryPredicate predicate) {
		// Do nothing
	}

	@Override
	public void acceptAlwaysPredicate(AlwaysPredicate predicate) {
		// Do nothing
	}

	@Override
	public void acceptCategoryPredicate(CategoryPredicate predicate) {
		// Do nothing
	}

	// ----- accessors ---------------------------------------------------------

	/**
	 * Get the properties.
	 * 
	 * @return the properties
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}
}
