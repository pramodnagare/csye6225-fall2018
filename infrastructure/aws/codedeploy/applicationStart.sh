#!/bin/bash
sudo systemctl start tomcat.service
cd /
source env.sh
cd /opt/tomcat/webapps
java -jar demo-0.0.1-SNAPSHOT.war