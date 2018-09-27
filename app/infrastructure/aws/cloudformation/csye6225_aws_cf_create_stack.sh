#!/bin/bash

set -e
##Check if enough arguements are passed
if [ $# -lt 1 ]; then
  echo "Kindly provide stack name! Try Again."
  exit 1
fi

##Creating Stack
echo "Creating Stack $1"
response=$(aws cloudformation create-stack --stack-name "$1" --template-body file://csye6225_cf_networking_template.json --parameters file://csye6225_cf_networking_parameters.json)
echo "Waiting for Stack $1 to be created"
echo "$response"
aws cloudformation wait stack-create-complete --stack-name $1
echo "stack $1 is created"