#!/bin/bash
echo Starting Global configuration process
#Currenty no agruments is required by the GCM


if [ -n "$1" ]; then
	echo "Input starting script $@"
	 java -Djava.library.path=/usr/local/lib -cp roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.management.launcher.GlobalConfigurationLauncher $@
else
	 java -Djava.library.path=/usr/local/lib -cp roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.management.launcher.GlobalConfigurationLauncher
fi
