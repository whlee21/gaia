#!/bin/sh

BASEDIR=$(dirname $0)
cd $BASEDIR

#CLASSPATH=./conf:$(find "$PWD/target/gaia-search-web-core-0.1-dist" -name '*.jar' |xargs echo  |tr ' ' ':')
CLASSPATH=./conf:$(find "$PWD/lib" -name '*.jar' |xargs echo  |tr ' ' ':')

nohup java -cp $CLASSPATH -Duser.language=ko -Duser.country=KR -Dhome.dir=$PWD -Dlog4j.configuration=file:"./conf/log4j.properties" gaia.search.ui.controller.GaiaSearchUIServer > $PWD/gaia-search-web.log 2>&1 &
#nohup java -cp $CLASSPATH -Duser.language=en -Duser.country=US   -Dlog4j.configuration=file:"./conf/log4j.properties" -DGAIA_DATA_HOME=$GAIA_DATA_HOME gaia.search.server.controller.GaiaSearchServer > $PWD/gaia.log 2>&1 &

tail -f $PWD/gaia-search-web.log
