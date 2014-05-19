#!/bin/bash

SERVER_PID=`jps | grep GaiaSearchServer | tr -d 'GaiaSearchServer'`
[[ -n $SERVER_PID ]] && kill $SERVER_PID
