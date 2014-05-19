#!/bin/sh

mvn clean &&
mvn package -DskipTests &&
cp target/*uberjar.jar ../../gaia-search/crawler/ &&
ls -l ../../gaia-search/crawler/
