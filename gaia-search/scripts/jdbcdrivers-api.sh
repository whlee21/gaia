# GET jdbc drivers
#curl http://localhost:8088/api/v1/collections/collection1/jdbcdrivers

# POST (upload) jdbc driver
#curl -F file=@./start.sh http://localhost:8088/api/v1/collections/collection1/jdbcdrivers

# DELETE jdbc drivers
#curl -X DELETE http://localhost:8088/api/v1/collections/collection1/jdbcdrivers/mysql.jar

# GET jdbc driver classes
#curl http://localhost:8088/api/v1/collections/collection1/jdbcdrivers/classes
