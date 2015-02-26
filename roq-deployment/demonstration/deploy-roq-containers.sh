#!/usr/bin/env bash

# Launch Zookeeper container
sudo docker run --name ROQZK -d jplock/zookeeper:3.4.6

# Create inventory for Ansible
echo [demo] > ../ansible/testInventory
echo localhost ansible_connection=local >> ../ansible/testInventory
echo ZK.roq.org ansible_ssh_host=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQZK) \
		ansible_ssh_user=root >> ../ansible/testInventory

# Launch the ansible playbook that update Zk address in the GCM.properties
ansible-playbook -i ../ansible/testInventory ../ansible/gimmeROQ.yml --tags "update-gcm"

# Launch the GCM container and the GCM component inside
sudo docker run -d -v $HOME/.roq:/lib/ROQ/roq-config --name ROQGCM -p 5000:5000 ubuntu14.04:roq \
		java -Djava.library.path=/usr/local/lib \
		-cp /lib/ROQ/roq-management/target/roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar \
		org.roqmessaging.management.launcher.GlobalConfigurationLauncher \
		/lib/ROQ/roq-config/GCM.properties

# Launch the ansible playbook that update Zk address in the HCM.properties
echo GCM.roq.org ansible_ssh_host=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQGCM) \
		ansible_ssh_user=root >> ../ansible/testInventory

# Launch the ansible playbook that update  GCM address in the HCM.properties
ansible-playbook -i ../ansible/testInventory ../ansible/gimmeROQ.yml --tags "update-hcm"

# Launch the HCM container and the HCM component inside
sudo docker run -d -v $HOME/.roq:/lib/ROQ/roq-config --name ROQHCM --link ROQGCM:GCM ubuntu14.04:roq \
		java -Djava.library.path=/usr/local/lib \
		-cp /lib/ROQ/roq-management/target/roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar \
		org.roqmessaging.management.launcher.HostConfigManagerLauncher \
		/lib/ROQ/roq-config/HCM.properties

# Launch the PUB/SUB containers
sudo docker run -d --name ROQPUB --link ROQGCM:GCM ubuntu14.04:roq
sudo docker run -d --name ROQSUB --link ROQGCM:GCM ubuntu14.04:roq