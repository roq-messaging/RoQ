# DOCKER-VERSION 1.0.1
# VERSION        0.5

FROM ubuntu:14.04
MAINTAINER Benjamin VM <vanmelle.benjamin@gmail.com>

# Get packages
RUN apt-get install -y software-properties-common && add-apt-repository ppa:chris-lea/zeromq && apt-get update && apt-get install -y libzmq1 libzmq-dev libzmq-dbg openjdk-7-jdk maven2 g++ libtool autoconf automake git make pkg-config wget zookeeper zookeeperd && cd /lib && git clone git://github.com/zeromq/jzmq

# Set JAVA_HOME environment variable
ENV JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64

# Install JZMQ
RUN cd /lib/jzmq && awk 'NR==165{print "<configuration> <skip>true</skip> </configuration>"}7' pom.xml  > pomAlt.xml && ./autogen.sh && ./configure && make && sudo make install && mvn install -Dmaven.test.skip=true -f pomAlt.xml

RUN wget -q -O - http://apache.mirrors.pair.com/zookeeper/zookeeper-3.4.6/zookeeper-3.4.6.tar.gz | tar -xzf - -C /opt && mv /opt/zookeeper-3.4.6 /opt/zookeeper && cp /opt/zookeeper/conf/zoo_sample.cfg /opt/zookeeper/conf/zoo.cfg && mkdir -p /tmp/zookeeper

# Install ROQ
RUN cd /lib && git clone git://github.com/roq-messaging/RoQ.git && cd RoQ && git checkout development && mvn install -Dmaven.test.skip=true