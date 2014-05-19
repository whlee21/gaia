# GET collection list
#curl http://localhost:8088/api/v1/collections

# GET a collection
#curl http://localhost:8088/api/v1/collections/collection1?fields=Collections

# POST
#curl -v -H 'Content-type: application/json' -d '{"name":"social8","num_shards":2,"template":"default.zip" }' http://localhost:8088/api/v1/collections

# DELETE
#curl -X DELETE http://localhost:8088/api/v1/collections/social

# GET a collection info
#curl http://localhost:8088/api/v1/collections/coll1/info

# GET a collection info
#curl http://localhost:8088/api/v1/collections/coll1/info/data_dir,free_disk_space,index_is_current

# GET templates
#curl http://localhost:8088/api/v1/collectiontemplates

# DELETE
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/index

# GET collection activities
#curl http://localhost:8088/api/v1/collections/coll1/activities

# GET a collection activity
#curl http://localhost:8088/api/v1/collections/coll1/activities/1

# POST
#curl -H 'Content-type: application/json' -d '{"Activities" : {"start_time": "2011-03-29T19:10:32+0000", "period":6000,"type":"autocomplete", "active":"true"}}' http://localhost:8088/api/v1/collections/coll1/activities

# PUT
#curl -X PUT -H 'Content-type: application/json' -d '{"Activities" : {"period":6000}}' http://localhost:8088/api/v1/collections/coll1/activities/1

# DELETE
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/activities/1

# GET collection activity status
#curl http://localhost:8088/api/v1/collections/coll1/activities/1/status

# GET all collection activity histories
#curl http://localhost:8088/api/v1/collections/coll1/activities/all/history

# GET a specific collection activity history
#curl http://localhost:8088/api/v1/collections/coll1/activities/1/history

# GET data sources
#curl http://localhost:8088/api/v1/collections/coll1/datasources

# GET a data source
#curl http://localhost:8088/api/v1/collections/coll1/datasources/1

# PUT a data source
#curl -X PUT -H 'Content-type: application/json' -d '{"DataSources" : {"indexing": true}}'  http://localhost:8088/api/v1/collections/coll1/datasources/1

# DELETE a data source
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/datasources/1

# GET a data source schedule
#curl http://localhost:8088/api/v1/collections/coll1/datasources/1/schedule

# PUT a data source schedule
#curl -X PUT -H 'Content-type: application/json' -d '{"DataSourceSchedules" : { "period" : 300, "" : "crawl", "start_time": "2011-03-18T12:10:32-0700", "active" : true }}' http://localhost:8088/api/v1/collections/coll1/datasources/1/schedule

# GET a data source job
#curl http://localhost:8088/api/v1/collections/coll1/datasources/1/job

# PUT (Start) a data source job
#curl -X PUT -H 'Content-type: application/json' http://localhost:8088/api/v1/collections/coll1/datasources/1/job

# DELETE (stop) a data source job
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/datasources/1/job

# GET all data source jobs
#curl http://localhost:8088/api/v1/collections/coll1/datasources/all/job

# PUT (start) a data source job
#curl -X PUT -H 'Content-type: application/json' http://localhost:8088/api/v1/collections/coll1/datasources/all/job

# DELETE (stop) a data source job
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/datasources/all/job

# GET a data source status
#curl http://localhost:8088/api/v1/collections/coll1/datasources/1/status

# GET a data source history
#curl http://localhost:8088/api/v1/collections/coll1/datasources/1/history

# DELETE a data source crawl data
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/datasources/1/crawldata

# DELETE a data source index
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/datasources/1/index

# GET all fields of collection
#curl http://localhost:8088/api/v1/collections/collection1/fields?fields=Fields

# GET sepcial field of collection
#curl http://localhost:8088/api/v1/collections/collection1/fields/fileSize

# POST
#curl -H 'Content-type: application/json' -d '{ "Fields" : {  "name": "my_new_field",  "default_value": "lucid rocks",  "multi_valued": true,  "stored": true,  "indexed": true,  "facet": true,  "index_for_spellcheck": true,  "synonym_expansion": true,  "field_type": "text_en",  "copy_fields": [      "text_medium",      "text_all"  ] }  }' 'http://localhost:8088/api/v1/collections/collection1/fields'

#DELETE a Field 
#curl -X DELETE 'http://localhost:8088/api/v1/collections/collection1/fields/my_new_field'

#PUT a Field
#curl -X PUT -H 'Content-type: application/json' -d '{ "Fields" : { "default_value": "lucid really rocks",  "multi_valued": false,  "index_for_spellcheck": false}}' 'http://localhost:8088/api/v1/collections/collection1/fields/my_new_field'


# GET batches
#curl http://localhost:8088/api/v1/collections/coll1/batches

# DELETE batches
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/batches

# GET crawler
#curl http://localhost:8088/api/v1/collections/coll1/batches/aperture

# DELETE crawler
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/batches/aperture

# GET crawler batch jobs
#curl http://localhost:8088/api/v1/collections/coll1/batches/aperture/job/1

# PUT (start) crawler batch job
#curl -X PUT http://localhost:8088/api/v1/collections/coll1/batches/aperture/job/1

# DELETE (stop) crawler batch job
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/batches/aperture/job/1

# GET jdbc drivers
#curl http://localhost:8088/api/v1/collections/coll1/jdbcdrivers

# POST (upload) jdbc driver
#curl -F file=@./start.sh http://localhost:8088/api/v1/collections/coll1/jdbcdrivers

# DELETE jdbc drivers
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/jdbcdrivers/mysql.jar

# GET jdbc driver classes
#curl http://localhost:8088/api/v1/collections/coll1/jdbcdrivers/classes

# GET field types
#curl http://localhost:8088/api/v1/collections/coll1/fieldtypes

# GET a field type
#curl http://localhost:8088/api/v1/collections/coll1/fieldtypes/aaa?fields=FieldTypes

# POST (create) a field type
#curl -H 'Content-type: application/json' -d '{"FieldTypes" : {"field_class" : "solr.TextField", "field_type" : "newfieldtype"}}' http://localhost:8088/api/v1/collections/coll1/fieldtypes

# PUT (update) a field type
#curl -X PUT -H 'Content-type: application/json' -d '{"FieldTypes" : {"pos_inc_gap" : "50"}}' http://localhost:8088/api/v1/collections/coll1/fieldtypes/aaa

# DELETE a field type
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/fieldtypes/aaa

# GET dynamic fields
#curl http://localhost:8088/api/v1/collections/coll1/dynamicfields

# GET a dynamic field
#curl http://localhost:8088/api/v1/collections/coll1/dynamicfields/aaa?fields=DynamicFields

# POST (create) a dynamic field
#curl -H 'Content-type: application/json' -d '{"DynamicFields" : {"field_name" : "attr_*", "field_class" : "solr.TextField"}}' http://localhost:8088/api/v1/collections/coll1/dynamicfields

# PUT (update) a dynamic field
#curl -X PUT -H 'Content-type: application/json' -d '{"DynamicFields" : {"field_class" : "solr.TextField"}}' http://localhost:8088/api/v1/collections/coll1/dynamicfields/aaa

# DELETE a dynamic field
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/dynamicfields/aaa

# GET SSL CONFIG
#curl http://localhost:8088/api/v1/config/ssl?fields=SslConfigs

# PUT (update) a SSL CONFIG
#curl -X PUT -H 'Content-type: application/json' -d '{"SslConfigs" : {"auth_require_authorization":"true","auth_authorized_clients":["cn=solr"]}}' http://localhost:8088/api/v1/config/ssl



# GET Users
#curl http://localhost:8088/api/v1/users

# GET a User
#curl http://localhost:8088/api/v1/users/test

# POST (create) a user
#curl -H 'Content-type: application/json' -d '{"Users" : {"username" : "smiller", "email" : "goodhere.com", "authorization" : "admin", "password" : "batman"}}' http://localhost:8088/api/v1/users

# PUT (update) a user
#curl -X PUT -H 'Content-type: application/json' -d '{"Users" : {"authorization" : "admin", "password" : "batman"}}' http://localhost:8088/api/v1/users/smiller

# DELETE a user
#curl -X DELETE http://localhost:8088/api/v1/users/smiller

# GET Alerts
#curl http://localhost:8088/api/v1/alerts?fields=Alerts

# GET List Alerts for a User
# curl http://localhost:8088/api/v1/alerts?Alerts/username=adminl

# GET a Alert
#curl http://localhost:8088/api/v1/alerts/1

# POST a Alert
#curl -H 'Content-type: application/json' -d '{"Alerts" : {"name" : "Solr documents", "collection" : "coll1", "username" : "admin", "query" : "solr", "period" : 60000, "email":"test.com"}}' http://localhost:8088/api/v1/alerts

# PUT (update) a Alert
#curl -X PUT -H 'Content-type: application/json' -d '{"Alerts" : {"query" : "solr3"}}' http://localhost:8088/api/v1/alerts/1


# DELETE a Alert
#curl -X DELETE http://localhost:8088/api/v1/alerts/1


# GET a Alert Check
#curl http://localhost:8088/api/v1/alerts/1/check


# GET Roles
#curl http://localhost:8088/api/v1/collections/collection1/roles?fields=Roles

# GET a Role
#curl http://localhost:8088/api/v1/collections/collection1/roles/ONLY_PUBLIC

# POST create a Role
#curl -H 'Content-type: aplication/json' -d '{"Roles" : {"role_name": "ONLY_PUBLIC", "groups": [ "group1", "group2"],  "filters": ["status:public"],  "users": ["user1"]}}' http://localhost:8088/api/v1/collections/collection1/roles/

# PUT (update) a Role
#curl -X PUT -H 'Content-type: application/json' -d '{"Roles" : {"groups" : ["solr3"]}}' http://localhost:8088/api/v1/collections/collection1/roles/ONLY_PUBLIC


# DELETE a Role
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/roles/ONLY_PUBLIC


# GET a click
#curl http://localhost:8088/api/v1/collections/coll1/click?fields=Clicks


# PUT (update) a click - record a click
#curl -X PUT -H 'Content-type: application/json' -d '{"Clicks" : {"type":"q", "req":"34509586770","q":"ipod", "qt":1329157201,  "hits":545}}' http://localhost:8088/api/v1/collections/coll1/click


# GET Caches
#curl http://localhost:8088/api/v1/collections/coll1/caches?fields=Caches


# POST create a Cache
#curl -H 'Content-type: aplication/json' -d '{"Caches" : {"cache_name": "new_cache", "cache_class_name": "solr.LRUCache", "size": 400}}' http://localhost:8088/api/v1/collections/coll1/caches/

# PUT (update) a Role
#curl -X PUT -H 'Content-type: application/json' -d '{"Caches" : {"autowarm_count" : 400}}' http://localhost:8088/api/v1/collections/coll1/caches/new_cache


# DELETE a Role
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/caches/new_cache


# GET Settings
#curl http://localhost:8088/api/v1/collections/coll1/settings?fields=Settings


# GET a particular Setting
#curl http://localhost:8088/api/v1/collections/coll1/settings/query_parser

# PUT (update) a Setting
#curl -X PUT -H 'Content-type: application/json' -d '{"Settings" : {"spell_check" : true}}' http://localhost:8088/api/v1/collections/coll1/settings



# GET filterings
#curl http://localhost:8088/api/v1/collections/coll1/filtering?fields=Filterings

# GET a filtering component
#curl http://localhost:8088/api/v1/collections/coll1/filtering/aaa?fields=Filterings

# POST (create) a filtering component
#curl -H 'Content-type: application/json' -d '{"Filterings" : {"filterer_class": "com.lucid.security.WindowsACLQueryFilterer",      "provider_class": "com.lucid.security.ad.ADACLTagProvider",      "provider_config": { "java.naming.provider.url": "ldap://10.0.0.50/", "java.naming.security.principal": "user@dc.domain.example",  "java.naming.security.credentials": "password"}}}' http://localhost:8088/api/v1/collections/coll1/filtering

# PUT (update) a filtering component
#curl -X PUT -H 'Content-type: application/json' -d '{"Filterings" : {"filterer_class": "com.lucid.security.WindowsACLQueryFilterer", "provider_class": "com.lucid.security.ad.ADACLTagProvider", "provider_config":     { "java.naming.provider.url": "ldap://10.0.0.50/", "java.naming.security.principal": "user@dc.domain.example", "java.naming.security.credentials": "password"}}}' http://localhost:8088/api/v1/collections/coll1/filtering/ad

# DELETE a filtering component
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/filtering/ad



# GET a filtering component
#curl http://localhost:8088/api/v1/collections/coll1/components/all?Components/handler_name=/lucid


# PUT (update) a filtering component
#curl -X PUT -H 'Content-type: application/json' -d '{"Components" : {"components": ["adfiltering","query","mlt","stats","feedback","highlight","facet","spellcheck","debug"]}}' http://localhost:8088/api/v1/collections/coll1/components/all?Components/handler_name=/lucid


# GET  Crawler Status
# curl http://localhost:8088/api/v1/crawlers/status


