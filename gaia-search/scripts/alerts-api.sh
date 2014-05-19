
# GET Alerts
#curl http://localhost:8088/api/v1/alerts?fields=Alerts

# GET List Alerts for a User
# curl http://localhost:8088/api/v1/alerts?Alerts/username=adminl

# GET a Alert
#curl http://localhost:8088/api/v1/alerts/1

# POST a Alert
#curl -H 'Content-type: application/json' -d '{"Alerts" : {"name" : "Solr documents", "collection" : "collection1", "username" : "admin", "query" : "solr", "period" : 60000, "email":"test.com"}}' http://localhost:8088/api/v1/alerts

# PUT (update) a Alert
#curl -X PUT -H 'Content-type: application/json' -d '{"Alerts" : {"query" : "solr3"}}' http://localhost:8088/api/v1/alerts/1


# DELETE a Alert
#curl -X DELETE http://localhost:8088/api/v1/alerts/1


# GET a Alert Check
#curl http://localhost:8088/api/v1/alerts/1/check


