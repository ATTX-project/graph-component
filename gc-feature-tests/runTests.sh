#!/bin/sh

: ${SLEEP_LENGTH:=2}

wait_for() {
  echo Waiting for $1 to listen on $2... >> /tmp/log
  while ! nc -z $1 $2; do echo sleeping >> /tmp/log ; sleep $SLEEP_LENGTH; done
}


wait_for "fuseki" "3030"
wait_for "essiren" "9200"
wait_for "frontend" "8080"
wait_for "gmapi" "4302"
wait_for "wfapi" "4301"

#gradle -b /tmp/build.gradle test
gradle -b /tmp/build.gradle startFusekiContainer

