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

import gaia.commons.server.Error;
import gaia.commons.server.api.resources.ResourceInstance;
import gaia.commons.server.api.services.RequestBody;
import gaia.commons.server.api.services.Result;
import gaia.commons.server.api.services.ResultImpl;
import gaia.commons.server.api.services.ResultStatus;
import gaia.commons.server.controller.spi.NoSuchParentResourceException;
import gaia.commons.server.controller.spi.NoSuchResourceException;
import gaia.commons.server.controller.spi.RequestStatus;
import gaia.commons.server.controller.spi.SystemException;
import gaia.commons.server.controller.spi.UnsupportedPropertyException;

/**
 * Responsible for update requests.
 */
public class UpdateHandler extends BaseManagementHandler {

	@Override
	protected Result persist(ResourceInstance resource, RequestBody body) {
		Result result = null;
		try {
			RequestStatus status = getPersistenceManager().update(resource, body);

			result = createResult(status);
			if (result.isSynchronous()) {
				result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
			} else {
				result.setResultStatus(new ResultStatus(ResultStatus.STATUS.ACCEPTED));
			}

		} catch (UnsupportedPropertyException e) {
			result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, e));
		} catch (NoSuchParentResourceException e) {
			result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.NOT_FOUND, new Error("property", Error.E_NOT_FOUND,
					e.getMessage())));
		} catch (NoSuchResourceException e) {
			if (resource.isCollectionResource()) {
				// todo: what is the correct status code here. The query didn't
				// match any resource
				// todo: so no resource were updated. 200 may be ok but we would
				// need to return a collection
				// todo: of resources that were updated.
				result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK, e));
			} else {
				result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.NOT_FOUND, new Error("resource",
						Error.E_NOT_FOUND, e.getMessage())));
			}
		} catch (SystemException e) {
			result = new ResultImpl(new ResultStatus(e.getStatus(), e.getErrors()));
		}

		return result;
	}
}
