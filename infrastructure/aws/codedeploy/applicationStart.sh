#!/bin/bash
#sudo systemctl stop tomcat.service
sudo cd /
source env.sh
sudo systemctl start tomcat.service
#cd /
#source env.sh
#cd /opt/tomcat
#java -jar demo-0.0.1-SNAPSHOT.war
