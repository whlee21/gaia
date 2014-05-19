./gaia-search/stop.sh
./gaia-connector/stop.sh
rm -rf gaia-search-web/public gaia-search-web/target
mvn clean prepare-package package -DskipTests
[ $? -eq 0 ] && ./gaia-connector/start.sh
[ $? -eq 0 ] && ./gaia-search/start.sh
