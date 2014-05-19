#Create a New Filtering Component
#curl -v -H 'Content-type: application/json' -d '{"filterer.class":"gaia.security.WindowsACLQueryFilterer","provider.class": "gaia.security.ad.ADACLTagProvider","provider.config":{"java.naming.provider.url": "ldap://10.0.0.50/","java.naming.security.principal": "user@dc.domain.example","java.naming.security.credentials": "password"}}' http://localhost:8088/api/v1/collections/collection1/filtering/ad


# GET filterings
#curl http://localhost:8088/api/v1/collections/collection1/filtering

# GET a filtering component
#curl http://localhost:8088/api/v1/collections/collection1/filtering/ad

# update a filtering component
curl -v -X PUT -H 'Content-type: application/json' -d '{"filterer.class":"gaia.security.WindowsACLQueryFilterer","provider.class": "gaia.security.ad.ADACLTagProvider","provider.config":{"java.naming.provider.url":"ldap://10.0.0.50/","java.naming.security.principal":"user@dc.domain.example","java.naming.security.credentials": "password"}}' http://localhost:8088/api/v1/collections/collection1/filtering/ad


#DELETE a filtering component 
#curl -X DELETE http://localhost:8088/api/v1/collections/collection1/filtering/ad
