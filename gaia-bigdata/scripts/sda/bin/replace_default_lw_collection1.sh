#!/bin/bash
#
# The SDA Replace Collection Script
# 

if [ ! $# == 2 ]; then
  echo "Usage: $0 sda_hostname:sda_port lw1_hostname:lw1_port"
  echo "For example: $0 localhost:8341 localhost:15200"
  exit
fi

sda_host_port="$1"
lw1_host_port="$2"

curl -X DELETE http://$lw1_host_port/api/collections/collection1
curl -X POST -H 'Content-type: application/json' -d "{\"collection\":\"collection1\"}" http://$sda_host_port/sda/v1/data/collections
