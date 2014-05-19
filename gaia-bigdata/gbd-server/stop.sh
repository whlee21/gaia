#!/bin/bash

SERVER_PID=`jps | grep GaiaBigDataServer | tr -d 'GaiaBigDataServer'`
[[ -n $SERVER_PID ]] && kill $SERVER_PID
