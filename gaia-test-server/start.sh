#!/bin/sh

BASEDIR=$(dirname $0)
cd $BASEDIR

CLASSPATH=$(find "$PWD/target/gaia-server-0.1-dist" -name '*.jar' |xargs echo  |tr ' ' ':'):.

nohup java -classpath $CLASSPATH -Dserver.home=$PWD gaia.server.controller.GaiaServer > $PWD/gaia.log 2>&1 &
