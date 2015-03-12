RoQ Messaging
=============

RoQ, Elastically scalable MOM.
Release-0.3.1 "Marina"

Installation
------------

We cover different use cases in order to provide you the most suitable way to get your first experience of ROQ.
You just want to get a try of ROQ ? Go to the 'demonstration' section, you will run ROQ on local docker containers that allow you to get your first experience without disturbing your working environment.
Are you sure that ROQ is a suitable messaging service for your system ? Let's go to the 'local deployment' section in order to develop your service which relies to ROQ or to contribute to ROQ.
Get reday for the production ? Go to the 'production' section, we provide you an automatic deployment script which allows to deploy ROQ on Amazon (we plan to support other environments, stay tunned).

Demonstration
-------------

This procedure allows you to run ROQ on your local machine. All the ROQ components and their dependencies will be installed in isolated docker container. In this way, that your system environment will be not impacted by the procedure.

### Prerequisite (get these packages via yum or apt-get):
- docker-io (tested with version 1.12)
- sshpass
- git
- ansible (tested with version 1.8.4)

/!\ Don't forget to run the docker service once the package installed. 
/!\ If you have an issue when trying to running the docker service on fedora, looks at the following post: http://stackoverflow.com/questions/24288616/permission-denied-on-accessing-host-directory-in-docker

### Launch the demonstration

Pull this git repository on your machine. And runs the following bash script:
```
RoQ/roq-deployment/demo-start-subscriber.sh
```
Once that the terminal shows subscriber connected and so on.
Open a second terminal and runs this script: 
```
RoQ/roq-deployment/demo-start-publisher.sh
```
Once the publisher connected, write your message in the second terminal, the message will appear on the first one. ROQ is working !

Now you can get the GCM local ip address thanks to the following command:
```
sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQGCM
```

Note this address and use it with your own application to communicate with ROQ, see the tutorial to know how to use the ROQ API.

Finish ? Launch this script to stop the roq containers: 
```
RoQ/roq-deployment/demo-stop.sh
```

Hope that you enjoyed ROQ !

Local installation and local deployment
---------------------------------------

You are sure that Roq is a suitable solution for your system ? This section allows you to install the ROQ components and their dependencies on you local machine in order to develop your own system with ROQ or to contribute to ROQ.
The script is working for the following OS: Ubuntu (x86, x64), CentOS (x64), Fedora (x64).

### Prerequisite (get these packages via yum or apt-get):
- docker-io (tested with version 1.12)
- sshpass
- git
- ansible (tested with version 1.8.4)

Pull this git repository on your machine.

### Install RoQ
Launch the following script (be sure that the prerequisite package have been installed):
```
RoQ/roq-deployment/development/install-roq.sh
```

### Launch RoQ
Once that the script has finished to install ROQ dependencies, run the following script:
```
RoQ/roq-deployment/development/restart-components.sh
```

This script is idempotent and can start and restart roq components, GCM and HCM. The Roq code in the repository will be reompiled before to take into account your modifications.

If you want to be sure that all the ROQ processes have been killed, launch this script:
```
RoQ/roq-deployment/development/stop-roq.sh
```

The script will install:
* Maven 2
* Zeromq 3.X and its dependency
* JZMQ 2.1
* JDK 1.7

Production
----------

Ready for the deployment of your application ? We give provide an amazon script able to deploy a complete ROQ cluster automatically on amazon !

### Prerequisite (get these packages via yum or apt-get):
- ansible (tested with version 1.8.4)

### Configuration step
First, you must set environment variables to allow ansible to communicate with your Amazon account:
Run the following commands in the shell:
```
export AWS_ACCESS_KEY_ID='YOU AMAZON ACCESS KEY'
export AWS_SECRET_ACCESS_KEY='your AMAZON SECRET KEY'
export ANSIBLE_HOSTS='YOUR PATH TO ROQ/roq-deployment/amazon/plugin/ec2.py'
export export EC2_INI_PATH='YOUR PATH TO ROQ/roq-deployment/amazon/plugin/ec2.ini'
```

Second go into the following file:
```
roq-deployment/amazon/group_vars/all/vars.yml
```

Add you amazon ssh keys with ssh-add (the keys set must be created in amazon EC2).

And set the number of instances for each ROQ components.
Don't forget to set the key_path var to the value or your ssh pem key from amazon (the value must match with the key name that you get when launching "ssh-add -L").

You are ready to launch your first ROQ cluster on amazon !
Run the following script: 
```
ansible-playbook "PATH TO ROQ"/roq-deployment/ --skip-tags "demonstration"
```

Note: If you launch several times this script, the instances number stay fixed to the values that you set in the config file.

Your cluster is ready !

Don't forget to terminate through EC2 console to don't lost your credits.

Hope that you enjoyed this straightforward tutorial that allows you tu use ROQ !
Trouble ? Contact me at vanmelle.benjamin@gmail.com