#!/bin/sh

BASEDIR=$(dirname $0)
cd $BASEDIR

CLASSPATH=./conf:$(find "$PWD/target/gbd-server-0.1-dist" -name '*.jar' | xargs echo  |tr ' ' ':')

nohup java -cp $CLASSPATH -Duser.language=ko -Duser.country=KR   -Dlog4j.configuration=file:"./conf/log4j.properties" gaia.bigdata.server.GaiaBigDataServer > $PWD/gaia.log 2>&1 &
