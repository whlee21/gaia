./gaia-connector/stop.sh
mvn clean prepare-package package -DskipTests
[ $? -eq 0 ] && ./gaia-connector/start.sh
