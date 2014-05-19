
# GET a filtering component
#curl http://localhost:8088/api/v1/collections/collection1/components/all?Components/handler_name=/lucid


# PUT (update) a filtering component
#curl -v -X PUT -H "Accept: application/json" -H "Content-Type: application/json" -d '["adfiltering","query","mlt","stats","feedback","highlight","facet","spellcheck","debug"]' http://localhost:8088/api/v1/collections/collection1/components/all?Components/handler_name=/lucid

