#!/bin/bash
CLASSPATH=.:/data/sda/lucidworks-sda-1.1.0-final-10009/lib/*
java -Dregions.number=2 -cp $CLASSPATH com.lucid.sda.hbase.HBaseBootstrap lwbd:2181
