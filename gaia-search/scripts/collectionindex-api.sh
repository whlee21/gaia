
# Delete the Index for a Collection
#curl -X DELETE http://localhost:8088/api/v1/collections/collection1/index?key=iaccepttherisk


# Delete Indexed Data for a Data Source
curl -X DELETE http://localhost:8088/api/v1/collections/collection1/datasources/8/index
