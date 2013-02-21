#!/bin/bash

date >> /tmp/log-init.log

if [ -f /mnt/context/context.sh ]
then
  . /mnt/context/context.sh
fi
 
#Mount datablock if specified, and change default owner
if [ -n "$DATABLOCK" ]; then
    mount -t ext3 /dev/$DATABLOCK /mnt/data
	#if [ -n "$DISTFS" ]; then
	#fi
    chown enx:enx /mnt/data    
fi 

#Change hostname by the one specified in context
if [ -n "$HOSTNAME" ]; then
    echo $HOSTNAME > /etc/hostname
    hostname $HOSTNAME
    rm /etc/hosts
    echo 127.0.0.1 localhost > /etc/hosts
    echo 127.0.0.1 $HOSTNAME $HOSTNAME.local >> /etc/hosts
fi
 
#Specify IP
if [ -n "$IP_PUBLIC" ]; then
    ifconfig eth0 $IP_PUBLIC
fi
 
#Specify Netmask
if [ -n "$MASK" ]; then
    ifconfig eth0 netmask $MASK
fi

#Specify Gateway
if [ -n "$GATEWAY" ]; then
    route add default gw $GATEWAY eth0
    service networking restart
fi

#RoQ Start
if [ -n "$GCM" ]; then
	sed -i "/period=60000/a GCM.address=$GCM" /var/lib/RoQ/RoQ/roq/config/GCM.properties
fi
