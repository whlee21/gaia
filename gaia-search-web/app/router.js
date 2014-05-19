var App = require('app');
App.Router.map(function() {
	this.route("hello", { path : "/hello" });
	this.route("login", { path : "/login" });
});

App.IndexRoute = Ember.Route.extend({
	beforeModel : function() {
		this.transitionTo('login');
	}
});

App.HelloRoute = Ember.Route.extend({
	index : Ember.Route.extend({
		route : '/',
		enter : function(router, context) {
			alert(1122);
		},
		redirectsTo : 'hello'
	}),

	hello : Ember.Route.extend({
		route : '/hello',

		enter : function(router, context) {
			alert(11);
		},

		connectOutlets : function(router, context) {
			$('title').text(Em.I18n.t('app.name'));
			router.get('applicationController').connectOutlet('hello');
		},

		getMessage : function(router, event) {
			App.router.get('helloController').getMessage();
		},
	})

});

App.LoginRoute = Ember.Route.extend({
		route : '/login',

		enter : function(router, context) {

		},

		connectOutlets : function(router, context) {
			$('title').text(Em.I18n.t('app.name'));
			router.get('applicationController').connectOutlet('login');
		}

});