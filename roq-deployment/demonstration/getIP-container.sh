#!/usr/bin/env bash

#Log the IP addresses
PUBIP=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQPUB);
GCMIP=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQGCM);
HCMIP=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQHCM);
SUPIP=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQSUB);
ZKIP=$(sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQZK);

echo "GCM IP: $GCMIP"
echo "HCM IP: $HCMIP"
echo "PUB IP: $PUBIP"
echo "SUB IP: $SUBIP"
echo "ZK IP: $ZKIP"
