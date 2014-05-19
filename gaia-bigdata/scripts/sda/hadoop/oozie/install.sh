#!/bin/bash

if [ -z $1 ]; then
  echo "Missing argument: Oozie HDFS path (e.g., /oozie/apps)";
  exit 1;
fi

OOZIE_PATH=$1

if [ -z $2 ]; then
  HADOOP_OPTS=""
else 
  HADOOP_OPTS="-Dfs.default.name=$2"
fi

copyLibs()
{
for dir in `ls $1`; do 
    echo "Copying libs to $dir";
    if [ ! -d "$1/$dir/lib" ]; then
        mkdir $1/$dir/lib
    fi
    cp lib/* $1/$dir/lib/
    if [ -d "$1/$dir/sub_wf" ]; then
        echo "Copying sub_wf $1/$dir/sub_wf"
        copyLibs $1/$dir/sub_wf
    fi
done
}

cleanLibs()
{
for dir in `ls $1`; do
    echo "Cleaning up libs from $dir";
    rm -r $1/$dir/lib/*
    if [ -d "$1/$dir/sub_wf" ]; then
        cleanLibs $1/$dir/sub_wf
    fi
done
}

copyLibs 'apps'

echo "Cleaning old Oozie apps: $OOZIE_PATH"
hadoop fs $HADOOP_OPTS -rmr "$OOZIE_PATH";
echo "Copying apps to Oozie HDFS path: $OOZIE_PATH";
hadoop fs $HADOOP_OPTS -mkdir "$OOZIE_PATH";
hadoop fs $HADOOP_OPTS -copyFromLocal apps/* $OOZIE_PATH;


SHARE_LIB="/user/hadoop/share/lib"
echo "Cleaning old share lib: $SHARE_LIB"
hadoop fs $HADOOP_OPTS -rmr $SHARE_LIB/*
echo "Making dir $SHARE_LIB"
hadoop fs $HADOOP_OPTS -mkdir $SHARE_LIB
echo "Copying jars to share lib location on HDFS: $SHARE_LIB"
hadoop fs $HADOOP_OPTS -copyFromLocal share/* $SHARE_LIB

cleanLibs 'apps'