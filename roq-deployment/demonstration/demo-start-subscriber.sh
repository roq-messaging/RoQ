#!/usr/bin/env bash

# Call the script which deploy the containers
./deploy-roq-containers.sh

# Get Containers IP addresses
SUBIP=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQSUB);
GCMIP=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQGCM);

# Wait for SSH server availability
sleep 3

# Ensure that ssh key has been removed
ssh-keygen -f "~/.ssh/known_hosts" -R $SUBIP

# Open an ssh connection to the publisher container 
# and run the publisher's daemon inside
sshpass -pscreencast ssh -o StrictHostKeyChecking=no root@$SUBIP \
		java -Djava.library.path=/usr/local/lib -cp \
		/lib/RoQ/roq-demonstration/target/roq-demonstration-1.0-SNAPSHOT-jar-with-dependencies.jar \
		org.roq.demonstration.RoQDemonstrationSubscriberLauncher $GCMIP testQ
