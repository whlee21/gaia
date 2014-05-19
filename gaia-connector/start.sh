#!/bin/sh

BASEDIR=$(dirname $0)
cd $BASEDIR

#SOLRHOME=$PWD/conf/solr
#GAIA_DATA_HOME=$PWD/data/solr

CLASSPATH=./conf:$(find "$PWD/target/gaia-connector-0.1-dist" -name '*.jar' |xargs echo  |tr ' ' ':')

nohup java -cp $CLASSPATH -Duser.language=ko -Duser.country=KR   -Dlog4j.configuration=file:"./conf/log4j.properties" gaia.crawl.connector.GaiaConnectorServer > $PWD/gaia.log 2>&1 &
#nohup java -cp $CLASSPATH -Duser.language=en -Duser.country=US   -Dlog4j.configuration=file:"./conf/log4j.properties" -DGAIA_DATA_HOME=$GAIA_DATA_HOME gaia.search.server.controller.GaiaSearchServer > $PWD/gaia.log 2>&1 &

#tail -f $PWD/gaia.log
