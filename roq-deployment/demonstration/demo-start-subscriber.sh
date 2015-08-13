#!/usr/bin/env bash

cd $(dirname $0)

# Make the script idempotent, stop the containers
# if they are already running
if [ $(sudo docker ps -n=5 | grep ROQGCM | wc -l) -eq 1 ]
then
	sudo docker stop ROQHCM
	sudo docker rm ROQHCM
fi
if [ $(sudo docker ps -n=5 | grep ROQGCM | wc -l) -eq 1 ]
then
	sudo docker stop ROQGCM
	sudo docker rm ROQGCM
fi
if [ $(sudo docker ps -n=5 | grep ROQPUB | wc -l) -eq 1 ]
then
	sudo docker stop ROQPUB
	sudo docker rm ROQPUB
fi
if [ $(sudo docker ps -n=5 | grep ROQSUB | wc -l) -eq 1 ]
then
	sudo docker stop ROQSUB
	sudo docker rm ROQSUB
fi
if [ $(sudo docker ps -n=5 | grep ROQZK | wc -l) -eq 1 ]
then
	sudo docker stop ROQZK
	sudo docker rm ROQZK
fi

# Call the script which deploy the containers
./deploy-roq-containers.sh

# Get Containers IP addresses
SUBIP=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQSUB);
ZKIP=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQZK);

# Wait for SSH server availability
sleep 3

# Ensure that ssh key has been removed
ssh-keygen -R $SUBIP

echo 'ZKADD: $(ZKIP)'

# Open an ssh connection to the publisher container 
# and run the publisher's daemon inside
sshpass -pscreencast ssh -o StrictHostKeyChecking=no root@$SUBIP \
		java -Djava.library.path=/usr/local/lib -cp \
		/lib/RoQ/roq-demonstration/target/roq-demonstration-1.0-SNAPSHOT-jar-with-dependencies.jar \
		org.roq.demonstration.RoQDemonstrationSubscriberLauncher $ZKIP testQ
