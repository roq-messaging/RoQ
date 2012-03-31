#!/bin/bash
JAR_DEST=/usr/local/share/java/roq/
SCRIPT_DEST=/usr/bin/roq/

echo "Copying files to local installation"
echo "Copying in $JAR_DEST and $SCRIPT_DEST"
sudo cp ../../../target/roq-core-1.0-SNAPSHOT-jar-with-dependencies.jar $JAR_DEST
sudo cp startMonitor.sh $SCRIPT_DEST
sudo cp startXchange.sh $SCRIPT_DEST


