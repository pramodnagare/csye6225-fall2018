#!/bin/bash

StackList=$(aws cloudformation list-stacks --stack-status-filter CREATE_COMPLETE UPDATE_IN_PROGRESS CREATE_IN_PROGRESS --query 'StackSummaries[].StackName' --output text )
if [[ -z "$StackList" ]]
then
  echo "No Stack is available for deletion! Please try after sometime!" 
  exit 1
else
  echo "Enter Stack to be deleted from above!"
  echo $StackList
  read StackName
  echo "Deleting Stack $StackName"
fi

echo "S3 Bucket check for "

HostedZone=$(aws route53 list-hosted-zones --query HostedZones[].{Name:Name} --output text)
ext="csye6225.com"
BucketName=$HostedZone$ext
echo $BucketName

S3Deletion=$(aws s3 ls s3://$BucketName)
echo $S3Deletion
if [[ -z "$S3Deletion" ]]
then
  echo "Bucket $HostedZone$ext is empty."
  break
else
  echo $HostedZone$ext
  echo "Files in S3 bucket has be deleted before deleting bucket, do you want to proceed? - Yes/No"
  read response
  if [[ "$response" == "yes" || "$response" == "Yes" || "$response" == "Y" || "$response" == "y" ]]
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
  echo "Stack deletion is in process! Please wait!"
fi

Success=$(aws cloudformation wait stack-delete-complete --stack-name $StackName)
if [[ -z "$Success" ]]
then
  echo "Stack $StackName is deleted successfully"
else
  echo "Failed to delete stack $StackName ! Please try again!"
  echo "$Success"
  exit 1
fi

