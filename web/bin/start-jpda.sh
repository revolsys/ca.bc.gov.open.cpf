#!/bin/bash
export JPDA_ADDRESS=*:8000
/usr/local/tomcat/bin/catalina.sh jpda run
