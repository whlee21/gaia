# GET collection activities
#curl http://localhost:8088/api/v1/collections/collection1/activities

# GET a collection activity
#curl http://localhost:8088/api/v1/collections/collection1/activities/1
curl http://localhost:8088/api/v1/collections/collection1/activities/a87e624d549d43e8b0584f698c571e27


# POST
#curl -H 'Content-type: application/json' -d '{"Activities" : {"start_time": "2014-03-29T19:10:32+0000", "period":6000,"type":"autocomplete", "active":"true"}}' http://localhost:8088/api/v1/collections/collection1/activities

# PUT
#curl -X PUT -H 'Content-type: application/json' -d '{"Activities" : {"period":6000}}' http://localhost:8088/api/v1/collections/collection1/activities/90a8ca9f29bd4f368dd9bf208d63b1ac
curl -X PUT -H 'Content-type: application/json' -d '{"period": 60000}' http://localhost:8088/api/v1/collections/collection1/activities/a87e624d549d43e8b0584f698c571e27
# DELETE
#curl -X DELETE http://localhost:8088/api/v1/collections/collection1/activities/a87e624d549d43e8b0584f698c571e27

# GET collection activity status
#curl http://localhost:8088/api/v1/collections/collection1/activities/a87e624d549d43e8b0584f698c571e27/status

# GET all collection activity histories
#curl http://localhost:8088/api/v1/collections/collection1/activities/all/history

# GET a specific collection activity history
#curl http://localhost:8088/api/v1/collections/collection1/activities/a87e624d549d43e8b0584f698c571e27/history
#curl http://localhost:8088/api/v1/collections/collection1/activities/all/history
