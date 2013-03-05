RoQ Messaging
=============

RoQ, Elastically scalable MOM.
Release-0.2 "Valerie"

Building & installing
---------------------

This procedure is meant for Ubuntu. It should be easy, though, to adapt it to another distribution or operating system.

### 1 Install necessary tools
What you need:

*   Java JDK (building RoQ)
*   Maven 2 (building RoQ)
*   Git versioning system (fetching the code)
*   build tools (to build jzmq)

All you need to do is:

    sudo aptitude install openjdk-6-jdk maven2 git build-essential


### 2 Install zeromq
We will install from a Launchpad PPA.

#### 2.1 Add the repository

Use either method (a) or method (b).

##### 2.1.a using add-apt-repository
    sudo add-apt-repository ppa:chris-lea/zeromq

##### 2.1.b manually
Add the repository:

```sh
sudo vim /etc/apt/sources.list.d/zeromq.list
# add the following line to the file:
# deb http://ppa.launchpad.net/chris-lea/zeromq/ubuntu oneiric main
```
Register the key:

    sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys B9316A7BC7917B12

#### 2.2 Install the necessary packages
    sudo apt-get update
    sudo apt-get install libzmq-dbg libzmq-dev libzmq1

### 3 Install jzmq binary library
```sh
cd /home/username/temp/
git clone https://github.com/zeromq/jzmq.git -b v1.0.0
cd jzmq
./configure
make
sudo make install
```

### 4 Install jzmq maven package (bindings to the binary library)
```sh
cd /home/username/temp/jzmq
mvn install -DskipTests
```

### 5 Fetch RoQ

```sh
cd /home/username/
git clone git@github.com:roq-messaging/RoQ.git
```

### 6 Build and install using maven

```sh
cd /home/username/RoQ
mvn install
```

To launch a release or the RoQ nightly build just check out the last build archive on http://dev.roq-messaging.org/ci/
```sh
cd roq/bin
./startGCM.sh
```

In other terminal you can start the hosts deamons:
```sh
cd roq/bin
./startHost.sh
```

To test the code locally on 1 VM, we have created a simple launcher which instantiates the complete RoQ stack. To start this class just open a terminal:

```
 java -Djava.library.path=/usr/local/lib -cp roq-simulation-1.0-SNAPSHOT-jar-with-dependencies.jar org.roq.simulation.RoQAllLocalLauncher 
```


