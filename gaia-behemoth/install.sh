#!/bin/sh

mvn install:install-file -Dfile=core/target/behemoth-core-1.1-SNAPSHOT.jar -DgroupId=gaia -DartifactId=behemoth-core -Dversion=1.1-SNAPSHOT -Dpackaging=jar &&
mvn install:install-file -Dfile=gate/target/behemoth-gate-1.1-SNAPSHOT.jar -DgroupId=gaia -DartifactId=behemoth-gate -Dversion=1.1-SNAPSHOT -Dpackaging=jar &&
mvn install:install-file -Dfile=tika/target/behemoth-tika-1.1-SNAPSHOT.jar -DgroupId=gaia -DartifactId=behemoth-tika -Dversion=1.1-SNAPSHOT -Dpackaging=jar
