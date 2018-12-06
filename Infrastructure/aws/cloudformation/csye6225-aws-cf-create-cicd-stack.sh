#!/bin/bash

set -e
##Check if enough arguements are passed
if [ $# -lt 1 ]; then
  echo "Kindly provide stack name! Try Again."
  exit 1
fi

stackname=$1
domain_name=$(aws route53 list-hosted-zones --query HostedZones[0].Name --output text)
cut_domain=${domain_name::-1}
s3domainname="code-deploy.$cut_domain"
echo "S3 Domain: $s3domainname"
echo "HostedZone Name: $cut_domain"
accountid=$(aws sts get-caller-identity --output text --query 'Account')
echo "AccountId: $accountid"

export TravisUser=travis
##Creating Stack
echo "Creating Stack $stackname"
response=$(aws cloudformation create-stack --stack-name "$stackname" --template-body file://csye6225-cf-cicd-template.json --parameters ParameterKey=s3domainname,ParameterValue=$s3domainname ParameterKey=TravisUser,ParameterValue=$TravisUser ParameterKey=accountid,ParameterValue=$accountid --capabilities CAPABILITY_NAMED_IAM)


if [ $? -eq 0 ]; then
    echo "Waiting for Stack $stackname to be created"
    echo "$response"
    aws cloudformation wait stack-create-complete --stack-name $stackname
    echo "stack $stackname is created"
else
	echo "Error in creating of stack"
	echo $response
fi;









