var App = require('app');

App.LoginController = Ember.ObjectController.extend({

  name: 'loginController',

  loginName: '',
  password: '',

  errorMessage: '',
  
  actions : {
  	login: function () {
	    this.set('errorMessage', '');
	    this.login();
	}
  },
  
  login : function() {
    var loginName = this.get('loginName').toLowerCase();
    var hash = window.btoa(loginName + ":" + this.get('password'));
	var usr = '';

	if (App.testMode) {
		if (loginName === "admin" && controller.get('password') === 'admin') {
			usr = 'admin';
		} else if (loginName === 'user' && controller.get('password') === 'user') {
			usr = 'user';
		}
	}

	App.ajax.send({
		name : 'router.login',
		sender : this,
		data : {
			auth : "Basic " + hash,
			usr : usr,
			loginName : loginName
		},
		beforeSend : 'authBeforeSend',
		success : 'loginSuccessCallback',
		error : 'loginErrorCallback'
	});
  },
  
  authBeforeSend : function(opt, xhr, data) {
	xhr.setRequestHeader("Authorization", data.auth);
  },

  loginSuccessCallback : function(data, opt, params) {
	console.log('login success');
	data = eval("(" + data + ")");
	console.log('username = ' + data.username);
	console.log('roles = ' + data.roles);

	var isAdmin = data.roles.indexOf('admin') >= 0;
	if (isAdmin) {
		this.setAuthenticated(true);
		App.db.setLoginName(data.username);
		App.db.setUser(data.username);
		this.postLogin(true);
		this.transitionToRoute('hello');
	} else {
		//TODO 
	}
  },

  loginErrorCallback : function(request, ajaxOptions, error, opt) {
	console.log("login error: " + error);
	this.setAuthenticated(false);
	this.postLogin(false);
  },
  
  setAuthenticated : function(authenticated) {
	console.log("TRACE: Entering router:setAuthenticated function");
	App.db.setAuthenticated(authenticated);
  },

  postLogin: function (isAuthenticated) {
    if (!isAuthenticated) {
      console.log('Failed to login as: ' + this.get('loginName'));
      this.set('errorMessage', Em.I18n.t('login.error'));
    }
  }

});