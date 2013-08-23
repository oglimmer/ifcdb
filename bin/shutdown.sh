#!/bin/sh

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/..

mvn exec:java -Dexec.mainClass=de.oglimmer.ifcdb.control.ServerControl -Dexec.args=1$1