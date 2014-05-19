#!/bin/sh

export JAVA_HOME=/usr/lib/jvm/java-7-oracle
export SDA_VERSION=1.1.0-final-10009
export GAIA_BIGDATA_HOME=/data/sda/lucidworks-sda-1.1.0-final-10009
export GAIA_BIGDATA_HEAPSIZE=2048
export GAIA_BIGDATA_CONF_DIR=/data/sda/lucidworks-sda-1.1.0-final-10009/conf
export GAIA_BIGDATA_OPTS="-Dregions.number=2 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=3010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

exec 2>&1
exec  /data/sda/lucidworks-sda-1.1.0-final-10009/bin/start.sh --api gaia.bigdata.api.admin.AdminAPIModule --api gaia.bigdata.api.analytics.AnalyticsAPIModule --api gaia.bigdata.api.connector.gaiasearch.ConnectorAPIModule --api gaia.bigdata.api.client.ClientAPIModule --api gaia.bigdata.api.data.DataManagementAPIModule --api gaia.bigdata.api.document.DocumentAPIModule --api gaia.bigdata.api.JobAPIModule --api gaia.bigdata.api.gaiasearch.GaiaSearchProxyAPIModule --api gaia.bigdata.api.hdfs.WebHDFSProxyAPIModule --api gaia.bigdata.api.WorkflowAPIModule --api gaia.bigdata.api.user.UserAPIModule --api gaia.commons.api.ping.PingAPIModule --api gaia.bigdata.classification.ClassifierAPIModule --api gaia.bigdata.classification.ClassifierStateAPIModule
