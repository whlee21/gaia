# GET collection list
#curl http://localhost:8088/api/v1/collections

# GET a collection
curl http://localhost:8088/api/v1/collections/collection1

# POST
#curl -H 'Content-type: application/json' -d '{ "collection_name":"social3","num_shards":2,"template":"default.zip" }' http://localhost:8088/api/v1/collections

# DELETE
#curl -X DELETE http://localhost:8088/api/v1/collections/social

# GET a collection info
#curl http://localhost:8088/api/v1/collections/coll1/info

# GET a collection info
#curl http://localhost:8088/api/v1/collections/coll1/info?data_dir,free_disk_space,index_is_current

# GET templates
#curl http://localhost:8088/api/v1/collectiontemplates

# DELETE
#curl -X DELETE http://localhost:8088/api/v1/collections/coll1/index