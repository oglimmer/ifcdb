#!/bin/sh

export MAVEN_OPTS="-Dcom.sun.management.jmxremote.port=1$1 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/..

mvn jetty:run -Djetty.port=$1 -Difcdb.config=conf/config.properties -Dlogback.configurationFile=conf/logback$1.xml