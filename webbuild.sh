./gaia-search-web-core/target/gaia-search-web-core-0.1-dist/gaia-search-web/stop.sh
rm -rf gaia-search-web-core/target &&
mvn package -pl gaia-search-web -DskipTests
mvn package -pl gaia-search-web-core -DskipTests
[ $? -eq 0 ] && ./gaia-search-web-core/target/gaia-search-web-core-0.1-dist/gaia-search-web/start.sh
