#!/bin/bash
clear
echo "$(date)"
if [ $# -gt 0 ]
then
	echo "Good morning, $1."
	echo "Number of arguments: $#"
else
	echo "WARNING: No argument specified!"
fi
aws --version
aws configure

#---------------------------------------------------------------------------
echo "Creating vpc"

echo "Enter CIDR Block: Ex. 10.0.0.0/16"
read cidrBlock

vpc_J_Response=$(aws ec2 create-vpc --cidr-block $cidrBlock --no-amazon-provided-ipv6-cidr-block --no-dry-run --instance-tenancy "default" --output json)

echo "$vpc_J_Response"

vpcId=$(echo -e "$vpc_J_Response"| /usr/bin/jq '.Vpc.VpcId' | tr -d '"')

echo "Enter VPC Name:"

read vpcName

aws ec2 create-tags --resources "$vpcId" --tags Key=Name,Value="$vpcName"

echo "VPC Created Successfully!"

#-----------------------------------------------------------------------------
echo "Creating Private Subnets:"

echo "How many number of subnets you want to create?"

read subCount

#echo "$subCount"

if [ $subCount -gt 0 ]
then
t=0
for ((i=1; i<=$subCount; i++))
do

echo "Enter CIDR-Block for subnet"

read cidrBlock

echo "Enter availability zone: "

read az

subnet_J_Response=$(aws ec2 create-subnet --cidr-block $cidrBlock --availability-zone $az --vpc-id $vpcId --output json)

echo "$subnet_J_Response"

subnetId=$(echo -e "$subnet_J_Response" |  /usr/bin/jq '.Subnet.SubnetId' | tr -d '"')

tsnids[t]=$subnetId

t=$((t+1))

echo "Enter Subnet Name:"

read subName

aws ec2 create-tags --resources "$subnetId" --tags Key=Name,Value="$subName"

done
else
    echo "Process terminated"
fi

echo "All Private subnets created successfully!"

#---------------------------------------------------------------------------------
echo "Creating Public Subnets:"

echo "How many number of public subnets you want to create?"

read subCount

a=$(($a+$subCount))

#echo "$subCount"

if [ $subCount -gt 0 ]
then
#t=0
for ((i=1; i<=$subCount; i++))
do

echo "Enter CIDR-Block for subnet"

read cidrBlock

echo "Enter availability zone: "

read az

subnet_J_Response=$(aws ec2 create-subnet --cidr-block $cidrBlock --availability-zone $az --vpc-id $vpcId --output json)

echo "$subnet_J_Response"

subnetId=$(echo -e "$subnet_J_Response" |  /usr/bin/jq '.Subnet.SubnetId' | tr -d '"')

tsnids[t]=$subnetId

t=$((t+1))

echo "Enter Subnet Name:"

read subName

aws ec2 create-tags --resources "$subnetId" --tags Key=Name,Value="$subName"

aws ec2 modify-subnet-attribute --subnet-id "$subnetId" --map-public-ip-on-launch

done
else
    echo "Process terminated"
fi

echo "All Public subnets created successfully!"

#---------------------------------------------------------------------------------

for ((i=0; i<a; i++))
do
echo "${tsnids[i]} "
done
#----------------------------------------------------------------------------------
echo "Creating internet gateways!"

gateway_J_response=$(aws ec2 create-internet-gateway --output json)

echo "$gateway_J_Response"

gatewayId=$(echo -e "$gateway_J_response" |  /usr/bin/jq '.InternetGateway.InternetGatewayId' | tr -d '"')

echo "Enter Gateway name:"

read gatewayName

aws ec2 create-tags --resources "$gatewayId" --tags Key=Name,Value="$gatewayName"

attach_J_response=$(aws ec2 attach-internet-gateway --internet-gateway-id "$gatewayId" --vpc-id $vpcId)

echo "Internet Gateway Successfully created!"
#-------------------------------------------------------------------------------------

echo "Creating RouteTable"

route_table_response=$(aws ec2 create-route-table --vpc-id "$vpcId" --output json)

echo "$route_table_response"

routeTableId=$(echo -e "$route_table_response" |  /usr/bin/jq '.RouteTable.RouteTableId' | tr -d '"')

echo "Enter Route Table Name:"

read routeTableName

aws ec2 create-tags --resources "$routeTableId" --tags Key=Name,Value="$routeTableName"

echo "RouteTable created successfully!"

echo "Enter public route CIDR:"

read publicCIDR

route_response=$(aws ec2 create-route --route-table-id "$routeTableId" --destination-cidr-block "$publicCIDR" --gateway-id "$gatewayId")

temp=${#tsnids[@]}

for ((i=0; i<temp; i++))
do
associate_response=$(aws ec2 associate-route-table --subnet-id "${tsnids[i]}" --route-table-id "$routeTableId")
done

echo "Network infrastructure has been setup successfully!"
