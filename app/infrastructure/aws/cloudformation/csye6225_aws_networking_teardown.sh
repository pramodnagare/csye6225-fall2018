vpcid=$1

echo "Deleting VPC $vpcid and its components!"

# Delete subnets
echo "Deleting Subnets"
for i in `aws ec2 describe-subnets --filters Name=vpc-id,Values="${vpcid}" | grep subnet- | sed -E 's/^.*(subnet-[a-z0-9]+).*$/\1/'`; do aws ec2 delete-subnet --subnet-id=$i; done

# Delete route tables
echo "Deleting route tables"
for i in `aws ec2 describe-route-tables --filters Name=vpc-id,Values="${vpcid}" --query "RouteTables[*].RouteTableId" --output text | tr -d '"'`; do aws ec2 delete-route-table --route-table-id=$i; done

# Detach internet gateways
echo "Detaching internet gateways"
for i in `aws ec2 describe-internet-gateways --filters Name=attachment.vpc-id,Values="${vpcid}" | grep igw- | sed -E 's/^.*(igw-[a-z0-9]+).*$/\1/'`; do aws ec2 detach-internet-gateway --internet-gateway-id=$i --vpc-id=${vpcid}; aws ec2 delete-internet-gateway --internet-gateway-id=$i; done

# Delete internet gateways
#echo "Deleting internet gateways"
#for i in `aws ec2 describe-internet-gateways --filters Name=attachment.vpc-id,Values="${vpcid}" | grep igw- | sed -E 's/^.*(igw-[a-z0-9]+).*$/\1/'`; do aws ec2 delete-internet-gateway --internet-gateway-id=$i; done

# Delete the VPC
echo "Finally deleting VPC"
aws ec2 delete-vpc --vpc-id ${vpcid}

#
