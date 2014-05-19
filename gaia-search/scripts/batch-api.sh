# Get Batches
curl http://localhost:8088/api/v1/collections/collection1/batches

# Delete Batchs
curl -X DELETE http://localhost:8088/api/v1/collections/collection1/batches

# List Batches for the crawler
curl http://localhost:8088/api/v1/collections/collection1/batches/lucid.aperture

# Delete All Batches For A Specific Crawler Controller
curl -X DELETE http://localhost:8088/api/v1/collections/collection1/batches/lucid.aperture

# List All Batch Jobs For A Specific Crawler Controller
curl http://localhost:8088/api/v1/collections/collection1/batches/lucid.aperture/job

# Define a batch job for the lucid.aperture controller
curl -X PUT -H 'Content-type: application/json' -d '{"batch_id":"00000000-0000-0000-1243-4df8a1b27888", "collection":"collection1", "crawler":"lucid.aperture", "ds_id":"4", "parse":true, "index":true}' http://localhost:8088/api/v1/collections/collection1/batches/lucid.aperture/job

# Start a Batch Processing Job
curl -X PUT -H 'Content-type: application/json' -d  '{"batch_id":"00000000-0000-0000-1243-344f2becaaa0","ds_id":"4","collection":"collection1"}' http://localhost:8088/api/v1/collections/collection1/batches/lucid.aperture/job

# Get The Status Of A Running Batch Processing Job
curl http://localhost:8088/api/v1/collections/collection1/batches/lucid.aperture/job/00000000-0000-0000-0000-06c639b86e43

# Stop A Running Batch Processing Job
curl -X DELETE http://localhost:8088/api/v1/collections/collection1/batches/lucid.aperture/job/00000000-0000-0000-0000-06c639b86e43


