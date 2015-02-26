#!/usr/bin/env bash

# Destroy all the demonstration containers
sudo docker stop ROQHCM
sudo docker rm ROQHCM
sudo docker stop ROQGCM
sudo docker rm ROQGCM
sudo docker stop ROQPUB
sudo docker rm ROQPUB
sudo docker stop ROQSUB
sudo docker rm ROQSUB
sudo docker stop ROQZK
sudo docker rm ROQZK