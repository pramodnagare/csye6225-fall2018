

#Course Name: CSYE6225 Network Structure and Cloud Computing

Team Memmbers:

    Pramod Nagare
    Pallavi Patel
    Harsh Shah
    Midhun Vugge

Install the AWS Command Line Interface: Link: https://docs.aws.amazon.com/cli/latest/userguide/awscli-install-linux.html

Configure your AWS CLI: aws configure

To create Network structure using aws cli and shell scripting: ./csye6225_aws_networking_setup []

The above execution will ask for VPC CIDR Block, VPC Name, Number of Public and Private Subnets and name, Internet Gateway, Route Table and Public CIDR block.

This script is to get the good exposure to Shell scripting and AWS CLI commands.

To delete the created VPC and related components: ./csye6225_aws_networking_teardown <VPC_ID>

The above script will delete VPC and all its related components for the vpc id mentioned in the arguments.
