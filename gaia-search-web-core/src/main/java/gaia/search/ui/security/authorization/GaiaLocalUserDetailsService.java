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
package gaia.search.ui.security.authorization;

import gaia.search.ui.configuration.Configuration;
import gaia.search.ui.orm.dao.RoleDAO;
import gaia.search.ui.orm.dao.UserDAO;
import gaia.search.ui.orm.entities.UserEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class GaiaLocalUserDetailsService implements UserDetailsService {
	private static final Logger LOG = LoggerFactory.getLogger(GaiaLocalUserDetailsService.class);

	Injector injector;
	Configuration configuration;
	private AuthorizationHelper authorizationHelper;
	UserDAO userDAO;
	RoleDAO roleDAO;

	@Inject
	public GaiaLocalUserDetailsService(Configuration configuration,
			AuthorizationHelper authorizationHelper, UserDAO userDAO, RoleDAO roleDAO) {
		this.injector = injector;
		this.configuration = configuration;
		this.authorizationHelper = authorizationHelper;
		this.userDAO = userDAO;
		this.roleDAO = roleDAO;
	}

	/**
	 * Loads Spring Security UserDetails from identity storage according to
	 * Configuration
	 * 
	 * @param username
	 *          username
	 * @return UserDetails
	 * @throws UsernameNotFoundException
	 *           when user not found or have empty roles
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		LOG.info("Loading user by name: " + username);

		UserEntity user = userDAO.findLocalUserByName(username);

		if (user == null) {
			LOG.info("user not found ");
			throw new UsernameNotFoundException("Username " + username + " not found");
		} else if (user.getRoleEntities().isEmpty()) {
			LOG.info("No authorities for user");
			throw new UsernameNotFoundException("Username " + username + " has no roles");
		}

		return new User(user.getUserName(), user.getUserPassword(), authorizationHelper.convertRolesToAuthorities(user
				.getRoleEntities()));
	}

}
