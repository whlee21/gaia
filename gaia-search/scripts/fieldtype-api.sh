# GET field types
#curl http://localhost:8088/api/v1/collections/coll1/fieldtypes

# GET a field type
#curl http://localhost:8088/api/v1/collections/coll1/fieldtypes/aaa

# POST (create) a field type
#curl -H 'Content-type: application/json' -d '{"field_class" : "solr.TextField", "field_type" : "newfieldtype"}' http://localhost:8088/api/v1/collections/coll1/fieldtypes

# PUT (update) a field type
#curl -X PUT -H 'Content-type: application/json' -d '{ "pos_inc_gap" : "50"}' http://localhost:8088/api/v1/collections/coll1/fieldtypes/aaa

# DELETE a field type
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/fieldtypes/aaa

# GET dynamic fields
#curl http://localhost:8088/api/v1/collections/coll1/dynamicfields

# GET a dynamic field
#curl http://localhost:8088/api/v1/collections/coll1/dynamicfields/aaa

# POST (create) a dynamic field
#curl -H 'Content-type: application/json' -d '{"field_name" : "attr_*", "field_class" : "solr.TextField"}' http://localhost:8088/api/v1/collections/coll1/dynamicfields

# PUT (update) a dynamic field
#curl -X PUT -H 'Content-type: application/json' -d '{"field_class" : "solr.TextField"}' http://localhost:8088/api/v1/collections/coll1/dynamicfields/aaa

# DELETE a dynamic field
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/dynamicfields/aaa
