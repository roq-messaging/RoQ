# DOCKER-VERSION 1.0.1
# VERSION        0.5

FROM ubuntu:14.04
MAINTAINER Benjamin VM <vanmelle.benjamin@gmail.com>

# Get packages
RUN apt-get install -y software-properties-common && add-apt-repository ppa:chris-lea/zeromq && apt-get update && apt-get install -y libzmq1 libzmq-dev libzmq-dbg openssh-server openjdk-7-jdk maven2 g++ libtool autoconf automake git make pkg-config wget zookeeper zookeeperd && cd /lib && git clone git://github.com/zeromq/jzmq

RUN apt-get update && apt-get install -y openssh-server
RUN mkdir /var/run/sshd
RUN echo 'root:screencast' | chpasswd
RUN sed -i 's/PermitRootLogin without-password/PermitRootLogin yes/' /etc/ssh/sshd_config

# SSH login fix. Otherwise user is kicked off after login
RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd

# Set JAVA_HOME && JAVA_TOOL_OPTIONS environment variables
ENV JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64

# Install JZMQ
RUN cd /lib/jzmq && git checkout v2.1.3 && rm pom.xml && wget https://raw.githubusercontent.com/vanmellebenjamin/RoQ/QueueClient/roq-deployment/ansible/gimme-roq/files/pom.xml && ./autogen.sh && ./configure && make && sudo make install && mvn install

RUN wget -q -O - http://apache.mirrors.pair.com/zookeeper/zookeeper-3.4.6/zookeeper-3.4.6.tar.gz | tar -xzf - -C /opt && mv /opt/zookeeper-3.4.6 /opt/zookeeper && cp /opt/zookeeper/conf/zoo_sample.cfg /opt/zookeeper/conf/zoo.cfg && mkdir -p /tmp/zookeeper

ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF8

ENV NOTVISIBLE "in users profile"
RUN echo "export VISIBLE=now" >> /etc/profile

# Install ROQ
RUN cd /lib && git clone git://github.com/vanmellebenjamin/RoQ && cd RoQ && git checkout QueueClient && mvn install -Dmaven.test.skip=true

CMD ["/usr/sbin/sshd", "-D"]