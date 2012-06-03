#!/bin/bash
echo Starting Global configuration process


if [ -n "$1" ]; then
	echo "Input starting script $@"
	 java -Djava.library.path=/usr/local/lib -cp roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.management.launcher.GlobalConfigurationLauncher $@
else
	 java -Djava.library.path=/usr/local/lib -cp roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.management.launcher.GlobalConfigurationLauncher localhost
fi
