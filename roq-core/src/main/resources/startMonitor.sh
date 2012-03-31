#!/bin/bash
echo Starting monitor process
if [ -n "$1" ]; then
	echo "Input starting script $@"
	java -Djava.library.path=/usr/local/lib -cp /usr/local/share/java/roq/roq-core-1.0-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.core.launcher.MonitorLauncher $@
else
	java -Djava.library.path=/usr/local/lib -cp /usr/local/share/java/roq/roq-core-1.0-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.core.launcher.MonitorLauncher  5571 5800
fi

