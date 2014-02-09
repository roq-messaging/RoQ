#!/bin/bash
echo "Starting Host manager process"
#Get the local directory in order to set the full path to the log4j config file
ROQ_BIN=$(dirname "$(readlink -f "$(type -P $0 || echo $0)")")
ROQ_HOME=$(dirname $ROQ_BIN)
VERSION="1.1"
echo "ROQ BIN Home= $ROQ_HOME"
echo "Launching RoQ $VERSION ..."

# INPUT: either <CGM> for stopping the GCM or <HCM> for stopping the HCM
if [ -n "$1" ]; then
	echo "Input starting script $@"
	java -Djava.library.path=/usr/local/lib -Dlog4j.configuration="file:$ROQ_HOME/config/log4j.properties" -cp $ROQ_HOME/lib/roq-management-$VERSION-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.management.launcher.RoQClusterStopper $@
else
	echo "The usage is ./stopRoQ GCM or ./stopRoQ HCM"
fi

