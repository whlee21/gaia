# GET dynamic fields
#curl http://localhost:8088/api/v1/collections/collection1/dynamicfields

# GET a dynamic field
#curl http://localhost:8088/api/v1/collections/collection1/dynamicfields/*_sh

# POST (create) a dynamic field
#curl -v -H 'Content-type: application/json' -d '{ "name":"*_sh", "indexed":true, "stored":true, "field_type":"text_en"}' http://localhost:8088/api/v1/collections/collection1/dynamicfields
#curl -v -H 'Content-type: application/json' -d '{ "name":"*_dsh", "indexed":true, "stored":true, "field_type":"text_en"}' http://localhost:8088/api/v1/collections/collection1/dynamicfields

# PUT (update) a dynamic field
#curl -X PUT -H 'Content-type: application/json' -d '{"multi_valued":true}' http://localhost:8088/api/v1/collections/collection1/dynamicfields/*_sh

# DELETE a dynamic field
#curl -X DELETE http://localhost:8088/api/v1/collections/collection1/dynamicfields/*_sh


