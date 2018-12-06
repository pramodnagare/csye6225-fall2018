#!/bin/bash

set -e
##Check if enough arguements are passed
if [ $# -lt 1 ]; then
  echo "Kindly provide stack name! Try Again."
  exit 1
fi

stackname=$1
domainName=$(aws route53 list-hosted-zones --query HostedZones[0].Name --output text)
hostedzone=${domainName::-1}
#s3domainName="$hostedzone.csye6225.com"
#echo "S3 Domain: $s3domainName"
echo "HostedZone: $hostedzone"

export dynamoDBName=csye6225
export DBSCHEMA=csye6225
export DBPASSWORD=csye6225password
export DBUSERNAME=csye6225master

##Creating Stack
echo "Creating Stack $stackname"
response=$(aws cloudformation create-stack --stack-name "$stackname" --template-body file://csye6225-cf-application-template.json --parameters ParameterKey=hostedzone,ParameterValue=$hostedzone ParameterKey=dynamoDBName,ParameterValue=$dynamoDBName ParameterKey=DBSCHEMA,ParameterValue=$DBSCHEMA ParameterKey=DBPASSWORD,ParameterValue=$DBPASSWORD ParameterKey=DBUSERNAME,ParameterValue=$DBUSERNAME)


if [ $? -eq 0 ]; then
    echo "Waiting for Stack $stackname to be created"
    echo "$response"
    aws cloudformation wait stack-create-complete --stack-name $stackname
    echo "stack $stackname is created"
else
	echo "Error in creating of stack"
	echo $response
fi;

