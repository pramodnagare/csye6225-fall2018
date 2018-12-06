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
S3Bucket="code-deploy.$hostedzone"
echo "S3 bucketName: $S3Bucket"

#Creating Stack
echo "Creating Stack $stackname"
response=$(aws cloudformation create-stack --stack-name "$stackname" --template-body file://serverless.json --parameters ParameterKey=S3Bucket,ParameterValue=$S3Bucket --capabilities CAPABILITY_NAMED_IAM)


if [ $? -eq 0 ]; then
    echo "Waiting for Stack $stackname to be created"
    echo "$response"
    aws cloudformation wait stack-create-complete --stack-name $stackname
    echo "stack $stackname is created"
else
	echo "Error in creating of stack"
	echo $response
fi;
