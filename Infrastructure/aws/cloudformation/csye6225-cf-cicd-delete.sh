#!/bin/bash
echo "Enter Stack to be deleted!"
read stack
{
  aws cloudformation describe-stacks --stack-name $stack &&
  echo "stack $stack is available!"
} || {
  echo "Unable to find stack $stack . Please try again!"
  exit 1
}
echo "Enter your domain name"
read dn
dn=$dn
codedeploy='code-deploy.'
bn=$codedeploy$dn
echo $bn


{
    aws s3 rm s3://$bn --recursive &&
    aws s3api delete-bucket --bucket $bn --region us-east-1 &&
    echo "Deleted s3 Bucket $bn."

} || {


    aws s3api delete-bucket --bucket $bn --region us-east-1 &&
    echo "Deleted s3 Bucket $bn ."


} || {
  echo "Oops! Unable to delete s3 Bucket $bn ! Please try again!"
  exit 1
}

{
    aws cloudformation delete-stack --stack-name $stack &&
    echo "Deleting stack $stack. Please wait!"
    resp=$(aws cloudformation wait stack-delete-complete --stack-name $stack)
    if [[ -z "$resp" ]]; then
      echo Stack "$stack" sucessfully terminated
    else
      echo "$resp"
      exit 1
    fi

} || {
  echo "Cannot delete stack $stack ! Please try again!"
  exit 1
}
