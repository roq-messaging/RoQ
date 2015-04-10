#!/usr/bin/env bash

# Create inventory for Ansible
echo [test] > ../ansible/testInventory
echo localhost ansible_connection=local >> ../ansible/testInventory

# Launch the ansible playbook that  intall ROQ on the local machine
ansible-playbook -i ../ansible/testInventory ../ansible/gimmeROQ.yml --tags "full-install"