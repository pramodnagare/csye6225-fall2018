#!/bin/bash

set -e
##Check if enough arguements are passed
if [ $# -lt 1 ]; then
  echo "Kindly provide stack name! Try Again."
  exit 1
fi

##Creating Stack
echo "Creating Stack $1"
response=$(aws cloudformation create-stack --stack-name "$1" --template-body file://serverless.json --capabilities CAPABILITY_NAMED_IAM)
echo "Waiting for Stack $1 to be created"
echo "$response"
aws cloudformation wait stack-create-complete --stack-name $1
echo "stack $1 is created"
