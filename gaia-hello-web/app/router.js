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

var App = require('app');
App.Router = Em.Router.extend({


	  enableLogging: true,
	  isFwdNavigation: true,
	  backBtnForHigherStep: false,

	  loggedIn: false,

	  setAuthenticated: function (authenticated) {
		    console.log("TRACE: Entering router:setAuthenticated function");
		    App.db.setAuthenticated(authenticated);
		    this.set('loggedIn', authenticated);
		  },
		  
	  login: function () {
	    var controller = this.get('loginController');
	    var loginName = controller.get('loginName').toLowerCase();
	    controller.set('loginName', loginName);
	    var hash = window.btoa(loginName + ":" + controller.get('password'));
	    var usr = '';

	    if (App.testMode) {
	      if (loginName === "admin" && controller.get('password') === 'admin') {
	        usr = 'admin';
	      } else if (loginName === 'user' && controller.get('password') === 'user') {
	        usr = 'user';
	      }
	    }

	    App.ajax.send({
	      name: 'router.login',
	      sender: this,
	      data: {
	        auth: "Basic " + hash,
	        usr: usr,
	        loginName: loginName
	      },
	      beforeSend: 'authBeforeSend',
	      success: 'loginSuccessCallback',
	      error: 'loginErrorCallback'
	    });

	  },

	  authBeforeSend : function(opt, xhr, data) {
				xhr.setRequestHeader("Authorization", data.auth);
	  },

	  loginErrorCallback : function(request, ajaxOptions, error, opt) {
		  var controller = this.get('loginController');
		  console.log("login error: " + error);
		  this.setAuthenticated(false);
		  controller.postLogin(false);
	  },
	  
	  root: Em.Route.extend({
	    index: Em.Route.extend({
	      route: '/',
	      redirectsTo: 'login'
	    }),

	    login: Em.Route.extend({
	      route: '/login',

	      /**
	       *  If the user is already logged in, redirect to where the user was previously
	       */
	      enter: function (router, context) {
//	        if (router.getAuthenticated()) {
//	          Ember.run.next(function () {
//	            console.log(router.getLoginName() + ' already authenticated.  Redirecting...');
//	            router.transitionTo(router.getSection(), context);
//	          });
//	        }
	      },

	      connectOutlets: function (router, context) {
	        $('title').text(Em.I18n.t('app.name'));
	        console.log('/login:connectOutlet');
//	        console.log('currentStep is: ' + router.getInstallerCurrentStep());
//	        console.log('authenticated is: ' + router.getAuthenticated());
	        router.get('applicationController').connectOutlet('login');
	      }
	    }),

//	    installer: require('routes/installer'),

//	    main: require('routes/main'),

	    logoff: function (router, context) {
	      router.logOff(context);
	    }

	  })
});
