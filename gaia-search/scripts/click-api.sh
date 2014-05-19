#Get Recent Events
# curl http://localhost:8088/api/v1/collections/collection1/click

#Record User Queries and Clicks
#curl -X PUT -H 'Content-type: application/json' -d '{"type":"q","req":"34509586770","q":"ipod","qt":1329157201,"hits":545}' http://localhost:8088/api/v1/collections/collection1/click
# curl -X PUT -H 'Content-type: application/json' -d '{"type":"c","req":"34509598766","ct":1329157350,"doc":"http://www.apple.com","pos":6}' http://localhost:8088/api/v1/collections/collection1/click
