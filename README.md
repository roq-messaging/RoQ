RoQ Messaging
=============

RoQ, Elastically scalable MOM.
Release-0.3.1 "Marina"

Building & installing
---------------------

This procedure is meant for Ubuntu. It should be easy, though, to adapt it to another distribution or operating system. In this case we advise the reader to look at the https://github.com/roq-messaging/RoQ/wiki/Manual-installation.

### Install RoQ
Get the GimmeRoQ.sh
To install RoQ: ./GimmeRoQ.sh path/to/installation method

```sh
./GimmeRoQ.sh path/to/installation method
```

Where "path/to/installation" : Path where you want to put your RoQ installation and "method" = CI or GIT
CI : Get the latest nightly build of RoQ from the Continuous Integration server (http://dev.roq-messaging.org/ci/).
GIT : Get the latest release of RoQ on GitHub", this option is recommended to contribute or browse the RoQ code.

We recommend to select CI. If you selected the GIT option, you will find examples of JUnit tests in the page https://github.com/roq-messaging/RoQ/wiki/Writing-&-executing-Tests.

The script will intall:
* Zeromq 3.X and its dependency
* JZMQ 2.1
* JDK 1.7

### Start RoQ
Once RoQ has been installed from the CI server, a RoQ cluster can be started by launching two main components: (1) The Global Configuration Manager (GCM) and (2) the Host deamon (HCM). Then, each new host machines that must join the cluster has just to start its own host deamon.

```sh
cd roq/bin
./startGCM.sh
```

In other terminal you can start the host deamons (on each host of the cluster. In case of local installation, just start it on the same machine as the GCM):
```sh
cd roq/bin
./startHost.sh
```

RoQ is running ! For creating a Queue, we can either create programatically a queue as described in https://github.com/roq-messaging/RoQ/wiki/Client-API or using a queue launcher:

```java
java -Djava.library.path=/usr/local/lib -Dlog4j.configuration="file:roq/config/log4j.properties" -cp roq/lib/roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.management.launcher.QueueManagementLauncher 127.0.0.1 add myqueue
```

Where the 127.0.0.1 (just an example) is the GCM address, the second argument can be "add" or "del" it specifies whether we need to create or remove a queue, and finally the last argument, "myqueue" here is the queue name we want to create. The queue is ready

To stop the cluster:
```sh
cd roq/bin
./stopRoQ.sh HCM
./stopRoQ.sh GCM
```

To test the code locally on 1 VM, we have created a simple launcher which instantiates the complete RoQ stack. To start this class just open a terminal:

```
 java -Djava.library.path=/usr/local/lib -cp roq-simulation-1.0-SNAPSHOT-jar-with-dependencies.jar org.roq.simulation.RoQAllLocalLauncher 
```

RoQ is shipped with a back-end for management. It is installed with the GimmeRoQ script and more information about usage of the CLI can be found on https://github.com/roq-messaging/roq-backend/blob/master/README.md.




