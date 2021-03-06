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

package gaia.commons.server.controller.spi;


import gaia.commons.server.Error;

import java.util.List;
/**
 * Indicates that a resource doesn't exist.
 */
@SuppressWarnings("serial")
public class NoSuchResourceException extends Exception {
	
	private List<Error> errors;

	/**
	 * Constructor.
	 * 
	 * @param msg
	 *            message
	 * @param throwable
	 *            root exception
	 */
//	public NoSuchResourceException(String msg, Throwable throwable) {
//		super(msg, throwable);
//	}
	public NoSuchResourceException(List<Error> errors, Throwable throwable) {
		super(throwable);
		this.errors = errors;
	}
	
	public List<Error> getErrors() {
		return errors;
	}
}
