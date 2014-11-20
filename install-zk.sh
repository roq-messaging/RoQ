#!/bin/bash

ZK_VERSION=3.4.6
ZK_FOLDER=zookeeper-$ZK_VERSION
ZK_ARCHIVE=$ZK_FOLDER.tar.gz
ZK_ARCHIVE_WEB=http://apache.belnet.be/zookeeper/zookeeper-$ZK_VERSION/$ZK_ARCHIVE
ZK_SYMLINK=zookeeper
ZK_SAMPLE_CFG=$ZK_SYMLINK/conf/zoo_sample.cfg
ZK_CFG=$ZK_SYMLINK/conf/zoo.cfg

if [[ -z $1 ]]; then
    echo "
-------------Missing Parameter---------------------
Usage : ./install-zk.sh path/to/installation
    "
    exit
fi

INSTALL_DIR=$1
echo "Installing ZooKeeper in directory $INSTALL_DIR"

# Create the installation directory if it does not exist
echo "Creating directory $INSTALL_DIR"
mkdir -p $INSTALL_DIR

# Changing to the installation directory
cd $INSTALL_DIR

echo "Fetching archive from $ZK_ARCHIVE_WEB"
# -O parameter makes sure we overwrite the file if it already exists
wget $ZK_ARCHIVE_WEB -O $ZK_ARCHIVE

echo "Extracting archive $ZK_ARCHIVE"
tar -xf $ZK_ARCHIVE

echo "Creating zookeeper symlink $ZK_SYMLINK"
ln -s $ZK_FOLDER $ZK_SYMLINK

echo "Creating default zookeeper configuration"
cp $ZK_SAMPLE_CFG $ZK_CFG

echo "Finished"
