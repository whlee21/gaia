# GET a data source schedule
#curl http://localhost:8088/api/v1/collections/collection1/datasources/a7a13089488346a98ff48bf41e6834e4/schedule

# PUT a data source schedule
#curl -X PUT -H 'Content-type: application/json' -d '{ "period" : 300, "" : "crawl", "start_time": "2013-11-18T12:10:32-0700", "active" : true }' http://localhost:8088/api/v1/collections/collection1/datasources/a7a13089488346a98ff48bf41e6834e4/schedule

