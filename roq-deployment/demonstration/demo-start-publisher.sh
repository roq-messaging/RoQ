#!/usr/bin/env bash

# Get Containers IP addresses
PUBIP=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQPUB);
ZKIP=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQZK);

# Ensure that ssh key has been removed
ssh-keygen -R $PUBIP

# Open an ssh connection to the publisher container 
# and run the publisher's daemon inside
sshpass -pscreencast ssh -o StrictHostKeyChecking=no root@$PUBIP \
		java -Djava.library.path=/usr/local/lib -cp \
		/lib/RoQ/roq-demonstration/target/roq-demonstration-1.0-SNAPSHOT-jar-with-dependencies.jar 	\
		org.roq.demonstration.RoQDemonstrationPublisherLauncher $ZKIP testQ
