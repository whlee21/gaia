# POST a data source
curl -X POST -H 'Content-type: application/json' -d '{"DataSources" : {
"type" : "file", 
"crawler" : "gaia.fs", 
"name" : "books", 
"path" : "/Users/whlee21/Documents/books", 
"max_bytes" : 1048576000, 
"crawl_depth" : -1, 
"bounds" : "tree", 
"include_paths" : "", 
"exclude_paths" : "", 
"index_directories" : 0, 
"remove_old_docs" : 0, 
"max_threads" : 1, 
"add_failed_docs" : 0, 
"add_failed_docs" : 1, 
"crawl_item_timeout" : 600000, 
"commit_within_sec" : 900, 
"commit_on_finish" : 0, 
"commit_on_finish" : 1, 
"output_type" : "solr", 
"output_args" : { "threads" : 2, "buffer" : 1 }
}}'  http://localhost:8088/api/v1/collections/collection1/datasources


# GET data sources
#curl http://localhost:8088/api/v1/collections/collection1/datasources

# GET a data source
#curl http://localhost:8088/api/v1/collection/collection1/datasources/1

# PUT a data source
#curl -X PUT -H 'Content-type: application/json' -d '{"DataSources" : {"indexing": true}}'  http://localhost:8088/api/v1/collections/collection1/datasources/1

# DELETE a data source
#curl -X DELETE http://localhost:8088/api/v1/collections/collection1/datasources/1


curl -X DELETE http://localhost:8088/api/v1/collections/collection1/datasources/8/index


# CREATE web content data source
curl -H 'Content-type: application/json' -d '{"crawler":"gaia.aperture","type":"web","url":"http://laonz.co.kr","crawl_depth":2,"name":"Laonz"}' http://localhost:8088/api/v1/collections/gaia/datasources

# CREATE twitter stream data source
curl -H 'Content-type: application/json' -d '{"crawler":"gaia.twitter.stream","type":"twitter_stream","name":"tw2","access_token":"1942927730-iVBXZMRgiwwKlIRo1IQTk2MOW4ZRWF1eeZIAqcb","consumer_key":"sxGPYpLfFIYP0BGQ0owF3g","consumer_secret":"Dx3bM7y5mlb4il4N1yIhw8Y2iaADIJ6wmwzuf5AbYZ0","sleep":"10000","token_secret":"zU0RddxU0wyTYFKqyT9sAC07LbuY9ii7QmYHcU922uo","url":"https://stream.twitter.com","max_docs":"100"}' http://localhost:8088/api/v1/collections/gaia/datasources

# CREATE solrxml data source
curl -H 'Content-type: application/json' -d '{"crawler":"gaia.solrxml","type":"solrxml","name":"xml0","path":"/home/kslee/temp"}' http://localhost:8088/api/v1/collections/gaia/datasources

# CREATE High Volume HDFS data source
curl -H 'Content-type: application/json' -d '{"crawler":"gaia.map.reduce.hdfs","type":"high_volume_hdfs","name":"behemoth0","hadoop_conf":"/home/kslee/hconf","path":"hdfs://mycluster/tmp/ida8c09c01_date592313","work_path":"hdfs://mycluster/tmp/hv_hdfs","zookeeper_host":"","solr_server_url":"http://192.168.1.135:8088/solr/gaia"}' http://localhost:8088/api/v1/collections/gaia/datasources

# CREATE ftp data source
curl -H 'Content-type: application/json' -d '{"crawler":"gaia.fs","type":"ftp","name":"ftp0","url":"ftp://ftp.ncbi.nih.gov/pub/apc","username":"anonymous","password":"crawl@example.com"}' http://localhost:8088/api/v1/collections/gaia/datasources

# CREATE hdfs data source
curl -H 'Content-type: application/json' -d '{"crawler":"gaia.fs","type":"hdfs","name":"hdfs0","url":"hdfs://vm2.laonz.net:8020/tmp"}' http://localhost:8088/api/v1/collections/gaia/datasources

# CREATE database data source
curl -H 'Content-type: application/json' -d '{"crawler":"gaia.jdbc","driver":"com.mysql.jdbc.Driver","username":"kslee","password":"kslee","sql_select_statement":"select * from t_sen_user","url":"jdbc:mysql://192.168.1.135:3306/test","type":"jdbc","name":"jdbc7"}' http://localhost:8088/api/v1/collections/gaia/datasources

# CREATE local file data source
	## gaia.fs
	curl -H 'Content-type: application/json' -d '{"crawler":"gaia.fs","type":"file","name":"localfile0","path":"/home/kslee/temp"}' http://localhost:8088/api/v1/collections/gaia/datasources
	
	## gaia.aperture
	curl -H 'Content-type: application/json' -d '{"crawler":"gaia.aperture","type":"file","name":"localfile1","path":"/home/kslee/temp"}' http://localhost:8088/api/v1/collections/gaia/datasources



