var App = require('app');
App.db = {};

if (typeof Storage !== 'undefined') {
  Storage.prototype.setObject = function (key, value) {
    this.setItem(key, JSON.stringify(value));
  }

  Storage.prototype.getObject = function (key) {
    var value = this.getItem(key);
    return value && JSON.parse(value);
  }
} else {
  // stub for unit testing purposes
  window.localStorage = {};
  localStorage.setItem = function (key, val) {
    this[key] = val;
  }
  localStorage.getItem = function (key) {
    return this[key];
  }
  window.localStorage.setObject = function (key, value) {
    this[key] = value;
  };
  window.localStorage.getObject = function (key, value) {
    return this[key];
  };
}

App.db.cleanUp = function () {
  console.log('TRACE: Entering db:cleanup function');
  App.db.data = {
    'app': {
      'loginName': '',
      'authenticated': false,
    },
  };
  console.log("In cleanup./..");
  localStorage.setObject('gaiasearch', App.db.data);
};

// called whenever user logs in
if (localStorage.getObject('gaiasearch') == null) {
  console.log('doing a cleanup');
  App.db.cleanUp();
}

/*
 * setter methods
 */

App.db.setLoginName = function (name) {
  console.log('TRACE: Entering db:setLoginName function');
  App.db.data = localStorage.getObject('gaiasearch');
  App.db.data.app.loginName = name;
  localStorage.setObject('gaiasearch', App.db.data);
};

/**
 * Set user model to db
 * @param user
 */
App.db.setUser = function (user) {
  console.log('TRACE: Entering db:setUser function');
  App.db.data = localStorage.getObject('gaiasearch');
  App.db.data.app.user = user;
  localStorage.setObject('gaiasearch', App.db.data);
};

App.db.setAuthenticated = function (authenticated) {
  console.log('TRACE: Entering db:setAuthenticated function');

  App.db.data = localStorage.getObject('gaiasearch');
  console.log('present value of authentication is: ' + App.db.data.app.authenticated);
  console.log('desired value of authentication is: ' + authenticated);
  App.db.data.app.authenticated = authenticated;
  localStorage.setObject('gaiasearch', App.db.data);
  App.db.data = localStorage.getObject('gaiasearch');
  console.log('Now present value of authentication is: ' + App.db.data.app.authenticated);
};

/*
 *  getter methods
 */

/**
 * Get user model from db
 * @return {*}
 */
App.db.getUser = function () {
  console.log('TRACE: Entering db:getUser function');
  App.db.data = localStorage.getObject('gaiasearch');
  return App.db.data.app.user;
};

App.db.getLoginName = function () {
  console.log('Trace: Entering db:getLoginName function');
  App.db.data = localStorage.getObject('gaiasearch');
  return App.db.data.app.loginName;
};

App.db.getAuthenticated = function () {
  console.log('Trace: Entering db:getAuthenticated function');
  App.db.data = localStorage.getObject('gaiasearch');
  return App.db.data.app.authenticated;
};

module.exports = App.db;
