#!/bin/bash
echo Starting Xchange process
if [ -n "$1" ]; then
	echo "Input starting script $@"
	java -Djava.library.path=/usr/local/lib -cp /usr/local/share/java/roq/roq-core-1.0-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.core.launcher.ExchangeLauncher $@
	else
	java -Djava.library.path=/usr/local/lib -cp /usr/local/share/java/roq/roq-core-1.0-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.core.launcher.ExchangeLauncher 5559 5560 tcp://localhost:5571 tcp://localhost:5800
fi

