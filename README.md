RoQ Messaging
=============

RoQ, Elastically scalable MOM.
Release-0.3.1 "Marina"

Installation
------------

We cover different use cases in order to provide you the most suitable way to get your first experience with ROQ.
You want to just get a try of ROQ ? Go to the 'demonstration' section, you will run ROQ on local docker containers which allow you to get your first experience with ROQ without disturbing your working environment.
Do you want to contribute to ROQ ? Let's go to the 'local deployment' section in order to deploy ROQ on you machine in order to debug etc.
Get reday for the production ? Go to the 'production' section, we provide you an automatic deployment script which allows to deploy ROQ on Amazon (we plan to support other environments, stay tunned).

Demonstration
-------------

This procedure allows you to run ROQ on your local machine. All the ROQ components and their dependencies will be installed on isolated docker containers. Therefore, your local system environment will be not impacted by the procedure.

### Prerequisite (get these packages via yum or apt-get):
- docker-io (tested with version 1.12)
- sshpass
- git
- ansible (tested with version 1.8.4)

/!\ Don't forget to run the docker service once the package installed. 
/!\ If you have an issue when trying to running the docker service on fedora, looks at the following post: http://stackoverflow.com/questions/24288616/permission-denied-on-accessing-host-directory-in-docker

### Launch the demonstration

Clone this git repository on your machine. And runs the following bash script:
```
RoQ/roq-deployment/demo-start-subscriber.sh
```
Once that the terminal shows that the subscriber is connected, open a second terminal and runs this script: 
```
RoQ/roq-deployment/demo-start-publisher.sh
```
Once the publisher connected, write your messages in the second terminal, the messages will appear on the first one. ROQ is working !

Now you can get the GCM local ip address thanks to the following command:
```
sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' ROQGCM
```

Take note of this address and use it with your own application to communicate with ROQ, see the tutorial to know how to use the ROQ API. You can stop the publisher and subscriber container with these docker commands (that is not required to use ROQ with your application):
```
sudo docker stop ROQPUB
sudo docker rm ROQPUB
sudo docker stop ROQSUB
sudo docker rm ROQSUB
```

Finish ? Launch this script to stop all the roq containers: 
```
RoQ/roq-deployment/demo-stop.sh
```

Hope that you enjoy ROQ !

Local installation and local deployment
---------------------------------------

You are sure that Roq is a suitable solution for your system ? This section allows you to install the ROQ components and their dependencies on you local machine in order to contribute to ROQ or to customize it.
The script is working for the following OS: Ubuntu (x86, x64), CentOS (x64), Fedora (x64).

### Prerequisite (get these packages via yum or apt-get):
- docker-io (tested with version 1.12)
- sshpass
- git
- ansible (tested with version 1.8.4)

Clone this git repository on your machine.

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

This script is idempotent and can start and restart ROQ main processes (GCM and HCM). The Roq code in the repository will be reompiled before, to take into account your modifications.

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

Ready for the deployment of your application in the cloud ? We provide an amazon script able to deploy a complete ROQ cluster automatically !

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
Second, Add you amazon ssh keys with ssh-add (the keys set must be created in amazon EC2).

Finally, go into the following file:
```
roq-deployment/amazon/group_vars/all/vars.yml
```
And set the number of instances for each ROQ components.
Don't forget to set the key_path var to the value of your amazon ssh pem key (the value must match with the key name that you get when launching "ssh-add -L").

### Deployment step

You are ready to launch your first ROQ cluster on amazon !
Run the following script: 
```
ansible-playbook "PATH TO ROQ"/roq-deployment/ --skip-tags "demonstration"
```

Note: If you launch several times this script, the instances number stay fixed to the values that you set in the config file.

Your cluster is ready !

Don't forget to terminate amazon instances through EC2 console to don't lost your credits.

Hope that you enjoyed this straightforward tutorial that allows you to use ROQ !
Trouble ? Contact me at vanmelle.benjamin@gmail.com