#!/bin/bash
echo Starting Host manager process

# INPUT: either the< global config manager server address> or
# < global config manager server address> <network interface to register> 
if [ -n "$1" ]; then
	echo "Input starting script $@"
	java -Djava.library.path=/usr/local/lib -cp roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar  org.roqmessaging.management.launcher.HostConfigManagerLauncher $@
else
	java -Djava.library.path=/usr/local/lib -cp roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar  org.roqmessaging.management.launcher.HostConfigManagerLauncher
fi

