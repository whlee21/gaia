# GET all fields of collection
curl -v http://localhost:8088/api/v1/collections/collection1/fields

# GET sepcial field of collection
#curl http://localhost:8088/api/v1/collections/collection1/fields/fileSize

# POST
#curl -v -H 'Content-type: application/json' -d '{ "name": "my_new_field10",  "default_value": "lucid rocks",  "multi_valued": true,  "stored": true,  "indexed": true,  "facet": true,  "index_for_spellcheck": true,  "synonym_expansion": true,  "field_type": "text_en",  "copy_fields": [      "text_medium",      "text_all"  ]  }' 'http://localhost:8088/api/v1/collections/collection1/fields'

#DELETE a Field 
#curl -X DELETE http://localhost:8088/api/v1/collections/collection1/fields/my_new_field10

#PUT a Field
#curl -X PUT -H 'Content-type: application/json' -d '{ "default_value": "lucid really rocks",  "multi_valued": false,  "index_for_spellcheck": false}' 'http://localhost:8088/api/v1/collections/collection1/fields/my_new_field'
