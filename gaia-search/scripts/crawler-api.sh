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
