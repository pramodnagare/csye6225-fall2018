#!/bin/bash
echo Enter stack name to be deleted
read sn
{
  aws cloudformation describe-stacks --stack-name $sn &&
  echo stack "$sn" found
} || {
  echo cannot find stack "$sn".Exiting Script
  exit 1
}
echo Enter Domain Name for prefix used
read dn
dn=$dn
codedeploy='code-deploy.'
bn1=$codedeploy$dn
echo $bn1


{
    aws s3 rm s3://$bn1 --recursive &&
    aws s3api delete-bucket --bucket $bn1 --region us-east-1 &&
    echo Deleted s3 Bucket "$bn1".

} || {


    aws s3api delete-bucket --bucket $bn1 --region us-east-1 &&
    echo Deleted s3 Bucket "$bn1".


} || {
  echo **Cannot delete s3 Bucket "$bn1"
  exit 1
}


#aws cloudformation wait stack-exists $sn

{
    aws cloudformation delete-stack --stack-name $sn &&
    echo Deleting stack "$sn". Please wait...
    resp=$(aws cloudformation wait stack-delete-complete --stack-name $sn)
    if [[ -z "$resp" ]]; then
      echo Stack "$sn" sucessfully terminated
    else
      echo "$resp"
      exit 1
    fi

} || {
  echo Cannot delete stack "$sn"
  exit 1
}
