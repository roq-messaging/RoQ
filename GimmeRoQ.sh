#!/bin/sh
#  Copyright 2012 EURANOVA
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#  http://www.apache.org/licenses/LICENSE-2.0
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  @author Cyrille DUVERNE


if [ -n "$1" ] && [ -n "$2" ]
then
	if [ "$2" = "CI" ] || [ "$2" = "GIT" ]
	then
INSTALLDIR=$1
METHOD=$2

 if [ -e "$INSTALLDIR" ]
 then 
echo "RoQ Installation Initiated"
	#Flush previous install in same directory
	rm -rf $INSTALLDIR/RoQ 
	rm -rf $INSTALLDIR/jzmq

echo "----Fetching pre-requisites from apt----"

	#Install basics
	sudo apt-get install -y openjdk-6-jdk maven2 git build-essential automake perl libtool autoconf g++ uuid-dev make unzip libpgm-dev nodejs npm >> $INSTALLDIR/roq.log 2>&1

echo "----Installing 0MQ via apt----"

	#Install ZMQ
	sudo add-apt-repository --yes ppa:chris-lea/zeromq >> $INSTALLDIR/roq.log 2>&1
	sudo apt-get update >> $INSTALLDIR/roq.log 2>&1
	sudo apt-get install -y libzmq-dbg libzmq-dev libzmq1 >> $INSTALLDIR/roq.log 2>&1

echo "----Installing JZMQ in $INSTALLDIR/jzmq/----"

	#Install JZMQ
	cd $INSTALLDIR/
	git clone git://github.com/zeromq/jzmq.git >> $INSTALLDIR/roq.log 2>&1
	cd $INSTALLDIR/jzmq
	git checkout v1.0.0 >> $INSTALLDIR/roq.log 2>&1
	./configure >> $INSTALLDIR/roq.log 2>&1
	make >> $INSTALLDIR/roq.log 2>&1
	sudo make install >> $INSTALLDIR/roq.log 2>&1

	#Install Maven JZMQ
	mvn install -e -DskipTests >> $INSTALLDIR/roq.log 2>&1

echo "----Fetching RoQ----"

	#Fetch RoQ
		
			if [ "$METHOD" = "CI" ]
			then	

echo "----Gathering latest build on RoQ's Jenkins----"

				mkdir $INSTALLDIR/RoQ
				cd $INSTALLDIR/RoQ
				#For last CI archive, use this command
		        	#Gather the latest build of RoQ
			        wget http://dev.roq-messaging.org/ci/job/RoQ/lastSuccessfulBuild/artifact/releases/latest.tgz >> $INSTALLDIR/roq.log 2>&1

echo "----Pushing file to $INSTALLDIR/RoQ/-----"
echo "----Untaring latest.tgz----"

	        		#Untar it
			        tar zxvf latest.tgz >> $INSTALLDIR/roq.log 2>&1

		        	#At this step RoQ is installed

echo "----Installing Back-End Management----"

		        	#OPTIONAL : Install the backend management
	        		#Clone Git repository
		        	git clone git://github.com/roq-messaging/roq-backend.git >> $INSTALLDIR/roq.log 2>&1

			        cd roq-backend

			        git checkout develop >> $INSTALLDIR/roq.log 2>&1

				git submodule update --init --recursive >> $INSTALLDIR/roq.log 2>&1

				#cd roq-web-console

				#git submodule init >> $INSTALLDIR/roq.log 2>&1
				
				#git submodule update $INSTALLDIR/roq.log 2>&1

		        	#Node Packages installation
			        npm install >> $INSTALLDIR/roq.log 2>&1

echo "Installation log available at $INSTALLDIR/roq.log"
echo "Congratulations ! RoQ has been successfully installed on your system in $INSTALLDIR/RoQ/ !!!"

			elif [ "$METHOD" = "GIT" ]
			then

echo "----Gathering latest release fron http://www.github.com/roq-messaging/RoQ----"
echo "----Pushing files in $INSTALLDIR/RoQ/----"
				#For working instance use the line below
				git clone git://github.com/roq-messaging/RoQ.git >> $INSTALLDIR/roq.log 2>&1

echo "----Installing RoQ----"

				cd RoQ
				mvn install -e >> $INSTALLDIR/roq.log 2>&1

echo "----Installing Back-End Management----"

				#OPTIONAL : Install the backend management
                                #Clone Git repository
                                git clone git://github.com/roq-messaging/roq-backend.git >> $INSTALLDIR/roq.log 2>&1

                                cd roq-backend

                                git checkout develop >> $INSTALLDIR/roq.log 2>&1
				
				git submodule update --init --recursive >> $INSTALLDIR/roq.log 2>&1

                                #Node Packages installation
                                npm install >> $INSTALLDIR/roq.log 2>&1

				#cd roq-web-console

				#git submodule init >> $INSTALLDIR/roq.log 2>&1
				
				#git submodule update $INSTALLDIR/roq.log 2>&1

echo "Installation log available at $INSTALLDIR/roq.log"
echo "Congratulations ! RoQ has been successfully installed on your system in $INSTALLDIR/RoQ/ !!!"
			
			fi
	else
		echo "$INSTALLDIR doesn't exist : Please provide an existing path"
	fi

	else
		echo "Wrong method : CI or GIT"
	fi
else

 echo "-------------Missing Parameter---------------------
Usage : ./GimmeRoQ.sh path/to/installation method
	
path/to/installation : Path where you want to put your RoQ installation

method = CI or GIT
	CI : Get the latest build of RoQ
	GIT : Get the latest release of RoQ on GitHub"

fi
