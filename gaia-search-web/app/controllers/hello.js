var App = require('app');

App.HelloController = Ember.ObjectController.extend({
	name : 'helloController',

	message : '',

	actions : {
		getMessage : function() {
			this.set('message', App.db.getLoginName());
		},
	},

});