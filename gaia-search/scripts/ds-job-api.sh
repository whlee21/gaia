# GET a data source job
#curl http://localhost:8088/api/v1/collections/collection1/datasources/a7a13089488346a98ff48bf41e6834e4/job

# PUT (Start) a data source job
#curl -X PUT -H 'Content-type: application/json' http://localhost:8088/api/v1/collections/collection1/datasources/a7a13089488346a98ff48bf41e6834e4/job

# DELETE (stop) a data source job
#curl -X DELETE http://localhost:8088/api/v1/collections/collection1/datasources/a7a13089488346a98ff48bf41e6834e4/job

# GET all data source jobs
#curl http://localhost:8088/api/v1/collections/collection1/datasources/all/job

# PUT (start) a data source job
#curl -X PUT -H 'Content-type: application/json' http://localhost:8088/api/v1/collections/collection1/datasources/all/job

# DELETE (stop) a data source job
#curl -X DELETE http://localhost:8088/api/v1/collections/collection1/datasources/all/job
