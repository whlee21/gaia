#!/bin/sh

BASEDIR=$(dirname $0)
cd $BASEDIR

CLASSPATH=$(find "$PWD/target/jetty-jersey-guice-hello-0.1-dist" -name '*.jar' |xargs echo  |tr ' ' ':'):.

nohup java -classpath $CLASSPATH -Dserver.home=$PWD gaia.server.controller.HelloServer > $PWD/hello.log 2>&1 &
