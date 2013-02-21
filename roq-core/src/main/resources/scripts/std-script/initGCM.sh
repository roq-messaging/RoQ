#!/bin/sh

nohup java -Djava.library.path=/usr/local/lib -Dlog4j.configuration=file:/var/lib/RoQ/RoQ/roq/config/log4j.properties -cp /var/lib/RoQ/RoQ/roq/lib/roq-management-1.0-SNAPSHOT-jar-with-dependencies.jar org.roqmessaging.management.launcher.GlobalConfigurationLauncher /var/lib/RoQ/RoQ/roq/config/GCM.properties & >> /var/log/roq.log

