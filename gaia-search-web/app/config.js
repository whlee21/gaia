
var App = require('app');

App.testMode = (location.port == '3333'); // test mode is automatically enabled if running on brunch server
App.testModeDelayForActions = 10000;
//App.apiPrefix = '/api';
App.timeout = 180000; // default AJAX timeout
App.maxRetries = 3; // max number of retries for certain AJAX calls
App.bgOperationsUpdateInterval = 6000;
App.componentsUpdateInterval = 6000;
App.contentUpdateInterval = 15000;
App.maxRunsForAppBrowser = 500;
App.pageReloadTime=3600000;

$.ajaxSetup({
  cache: false
});