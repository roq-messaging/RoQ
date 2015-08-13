#!/usr/bin/env bash

cd $(dirname $0)
echo `pwd`

# Create inventory for Ansible
echo [test] > ../ansible/testInventory
echo localhost ansible_connection=local >> ../ansible/testInventory

# Launch the ansible playbook that  intall ROQ on the local machine
ansible-playbook -i ../ansible/testInventory ../ansible/gimmeROQ.yml --tags "full-install"

# Build the zookeeper docker image
sudo docker build -t debian:zookeeper ../docker/zookeeper

# Build the roq image
sudo docker build -t ubuntu:roqdemo ../docker/roq 