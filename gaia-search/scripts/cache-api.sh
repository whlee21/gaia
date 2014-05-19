# GET Caches
#curl http://localhost:8088/api/v1/collections/collection1/caches?fields=Caches


# POST create a Cache
#curl -H 'Content-type: application/json' -d '{"name":"newcache","class":"solr.LRUCache","size":500}' http://localhost:8088/api/v1/collections/collection1/caches/
