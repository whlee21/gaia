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

package gaia.commons.server.controller.predicate;

import gaia.commons.server.controller.spi.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Predicate that checks equality of a given value to a {@link Resource}
 * property.
 */
public class EqualsPredicate<T> extends ComparisonPredicate<T> {
	
	private static final Logger LOG = LoggerFactory.getLogger(EqualsPredicate.class);

	public EqualsPredicate(String propertyId, Comparable<T> value) {
		super(propertyId, value);
	}

	@Override
	public boolean evaluate(Resource resource) {
		Object propertyValue = resource.getPropertyValue(getPropertyId());
		Object predicateValue = getValue();
		return predicateValue == null ? propertyValue == null
				: propertyValue != null && compareValueTo(propertyValue) == 0;
	}

	@Override
	public String getOperator() {
		return "=";
	}
}
