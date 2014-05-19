#!/bin/bash
#
# The SDA Start Script
#
# Env. Vars
#   SDA_JAVA_HOME The Java impl to use.  Overrides JAVA_HOME
#
#   SDA_HEAPSIZE  The max amount of heap to use, in MB.  Default is 512
#
#   SDA_OPTS  Extra Java runtime options
#
#   SDA_ADDRESS The hostname/IP of the services running here for use in Service Location.  Default is to use hostname.
#
#   SDA_PORT The port to listen on.  Default is 8431
#
#   SDA_CONF_DIR The configuration directory for use with SDA.  Default is SDA_HOME/conf
#
#

cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

# TODO: we don't support windows at this time for production
if $cygwin; then
  echo "Cygwin/Windows not supported"
  exit 1;
fi

# resolve links - $0 may be a softlink
THIS="$0"
while [ -h "$THIS" ]; do
  ls=`ls -ld "$THIS"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    THIS="$link"
  else
    THIS=`dirname "$THIS"`/"$link"
  fi
done

THIS_DIR=`dirname "$THIS"`
SDA_HOME=`cd "$THIS_DIR/.." ; pwd`

# some Java parameters
if [ "$SDA_JAVA_HOME" != "" ]; then
  #echo "run java in $SDA_JAVA_HOME"
  JAVA_HOME=$SDA_JAVA_HOME
fi

if [ "$JAVA_HOME" = "" ]; then
  echo "Error: JAVA_HOME is not set."
  exit 1
fi

JAVA=$JAVA_HOME/bin/java
JAVA_HEAP_MAX=-Xmx512m

# check envvars which might override default args
if [ "$SDA_HEAPSIZE" != "" ]; then
  #echo "run with heapsize $SDA_HEAPSIZE"
  JAVA_HEAP_MAX="-Xmx""$SDA_HEAPSIZE""m"
  #echo $JAVA_HEAP_MAX
fi

THE_HOSTNAME=`hostname`
if [ "$SDA_THE_HOSTNAME" != "" ]; then
  THE_HOSTNAME=$SDA_THE_HOSTNAME
fi

PORT=8341
if [ "$SDA_PORT" != "" ]; then
  PORT=$SDA_PORT
fi

CONFIG=$SDA_HOME/conf
if [ "$SDA_CONF_DIR" != "" ]; then
  CONFIG=$SDA_CONF_DIR
fi


CLASSPATH=${CLASSPATH}:$SDA_HOME/conf

# so that filenames w/ spaces are handled correctly in loops below
IFS=
for f in $SDA_HOME/lib/*.jar; do
    CLASSPATH=${CLASSPATH}:$f;
done

# restore ordinary behaviour
unset IFS

CLASS=com.lucid.commons.server.APIServer


exec "$JAVA" -Dcom.sun.management.jmxremote $JAVA_HEAP_MAX -Dsda.logs.dir=$SDA_HOME/logs $SDA_OPTS -classpath "$CLASSPATH" $CLASS --address $THE_HOSTNAME --port $PORT --config $CONFIG/run.properties "$@"

