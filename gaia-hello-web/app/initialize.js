window.App = require('app');

require('config');

require('messages');

require('utils/base64');
require('utils/db');
require('utils/helper');

require('controllers');
require('templates');
require('views');
require('router');

require('utils/ajax');
//require('utils/updater');

require('utils/http_client');

App.initialize();

console.log('after initialize');