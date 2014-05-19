#!/bin/bash

SERVER_PID=`jps | grep GaiaSearchUIServer | tr -d 'GaiaSearchUIServer'`
[[ -n $SERVER_PID ]] && kill $SERVER_PID
