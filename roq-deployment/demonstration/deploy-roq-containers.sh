#!/usr/bin/env bash

# Create inventory for Ansible
echo [demo] > ../ansible/testInventory
echo localhost ansible_connection=local >> ../ansible/testInventory

# Build the roq image
sudo docker build -t ubuntu:roqdemo ../docker/roq 

# Build the zookeeper image
sudo docker build -t debian:zookeeper ../docker/zookeeper

echo Starting Zookeeper container
# Launch Zookeeper container
sudo docker run --name ROQZK -d debian:zookeeper

# Create inventory for Ansible
echo ZK.roq.org ansible_ssh_host=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQZK) \
		ansible_ssh_user=root >> ../ansible/testInventory

# Launch the ansible playbook that update Zk address in the GCM.properties
ansible-playbook -i ../ansible/testInventory ../ansible/gimmeROQ.yml --tags "update-gcm"

echo Starting GCM container
# Launch the GCM container and the GCM component inside
sudo docker run -d -v $HOME/.roq:/lib/RoQ/roq-config --name ROQGCM -p 5000:5000 ubuntu:roqdemo \
		java -Djava.library.path=/usr/local/lib \
		-cp /lib/RoQ/roq-management/target/roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar \
		org.roqmessaging.management.launcher.GlobalConfigurationLauncher \
		/lib/RoQ/roq-config/GCM.properties

# Launch the ansible playbook that update Zk address in the HCM.properties
echo GCM.roq.org ansible_ssh_host=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQGCM) \
		ansible_ssh_user=root >> ../ansible/testInventory

# Launch the ansible playbook that update  GCM address in the HCM.properties
ansible-playbook -i ../ansible/testInventory ../ansible/gimmeROQ.yml --tags "update-hcm"

echo Starting HCM container
# Launch the HCM container and the HCM component inside
sudo docker run -d -v $HOME/.roq:/lib/RoQ/roq-config --name ROQHCM --link ROQGCM:GCM ubuntu:roqdemo \
		java -Djava.library.path=/usr/local/lib \
		-cp /lib/RoQ/roq-management/target/roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar \
		org.roqmessaging.management.launcher.HostConfigManagerLauncher \
		/lib/RoQ/roq-config/HCM.properties

echo Starting PUB/SUB containers
# Launch the PUB/SUB containers
sudo docker run -d --name ROQPUB --link ROQGCM:GCM ubuntu:roqdemo
sudo docker run -d --name ROQSUB --link ROQGCM:GCM ubuntu:roqdemo