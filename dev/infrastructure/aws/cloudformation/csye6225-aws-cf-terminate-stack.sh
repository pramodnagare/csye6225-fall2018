#!/bin/bash

set -e
##Check if enough arguements are passed
if [ $# -lt 1 ]; then
  echo "Kindly provide stack name"
  exit 1
fi

echo "Deleting Stack $1"
aws cloudformation delete-stack --stack-name $1
echo "Waiting for Stack $1 to be deleted"
aws cloudformation wait stack-delete-complete --stack-name $1
echo "Stack $1 deleted!"
