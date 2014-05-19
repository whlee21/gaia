# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


fs   = require 'fs'
path = require 'path'

# See docs at http://brunch.readthedocs.org/en/latest/config.html.

exports.config =

  files:

    javascripts:
      joinTo:
        'js/app.js': /^app/
        'js/vendor.js': /^vendor/
        'test/js/test.js': /^test(\/|\\)(?!vendor)/
        'test/js/test-vendor.js': /^test(\/|\\)(?=vendor)/
      order:
        before: [
          'vendor/js/jquery-1.10.2.js',
          'vendor/js/handlebars-1.1.2.js',
          'vendor/js/ember-1.2.0.js',
          'vendor/js/ember-states.js',
          'vendor/js/ember-i18n-1.4.1.js',
          'vendor/js/bootstrap.min.js',
          ]

    stylesheets:
      defaultExtension: 'css'
      joinTo: 'css/app.css'
      order:
        before: [
          'vendor/css/bootstrap.min.css',
          'vendor/css/bootstrap-theme.min.css'
        ]

    templates:
      precompile: true
      defaultExtension: 'hbs'
      joinTo: 'js/app.js' : /^app/
      paths:
        jquery: 'vendor/js/jquery-1.10.2.js'
        handlebars: 'vendor/js/handlebars-1.1.2.js'
        ember: 'vendor/js/ember-1.2.0.js'

  server:
    port: 3333
    base: '/'
    run: no


