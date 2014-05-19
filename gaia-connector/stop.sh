#!/bin/bash

SERVER_PID=`jps | grep GaiaConnectorServer | tr -d 'GaiaConnectorServer'`
[[ -n $SERVER_PID ]] && kill $SERVER_PID
