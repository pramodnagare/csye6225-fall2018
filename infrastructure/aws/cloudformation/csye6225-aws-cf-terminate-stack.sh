#!/bin/bash
# List all stack for account

StackList=$(aws cloudformation list-stacks --stack-status-filter CREATE_COMPLETE UPDATE_IN_PROGRESS CREATE_IN_PROGRESS --query 'StackSummaries[].StackName' --output text )
if [[ -z "$StackList" ]]
then
  echo "There is no Stack available to delete" 
  exit 1
else
  echo "Enter Application stack Name to be deleted from the list"
  echo $StackList
  read StackName
  echo "Deleting the stack $StackName"
fi

echo "Checking the Attachments S3 Bucket"

HostedZoneName=$(aws route53 list-hosted-zones --query HostedZones[].{Name:Name} --output text)
com="csye6225.com"
BucketName=$HostedZoneName$com
echo $BucketName

S3Deletion=$(aws s3 ls s3://$BucketName)
echo $S3Deletion
if [[ -z "$S3Deletion" ]]
then
  echo "$HostedZoneName$com is empty. Deleting stack"
  break
else
  echo $HostedZoneName$com
  echo "Without file deletion. S3 bucket cannot be deleted"
  echo "Do you want to delete the file then chose - Yes/No"
  read response
  if [[ "$response" == "yes" || "$response" == "Yes" ]]
  then
    delete=$(aws s3 rm s3://$BucketName --recursive)
    echo $delete
  else
    exit 1
  fi
fi

Delete=$(aws cloudformation delete-stack --stack-name $StackName)
if [ $? -ne "0" ]
then
  echo "$StackName stack is not deleted....."
  echo "$Delete"
  exit 1
else
  echo "Deletion in Progress....."
fi

Success=$(aws cloudformation wait stack-delete-complete --stack-name $StackName)
if [[ -z "$Success" ]]
then
  echo "$StackName Application stack is deleted successfully"
else
  echo "Deletion of $StackName stack failed."
  echo "$Success"
  exit 1
fi

