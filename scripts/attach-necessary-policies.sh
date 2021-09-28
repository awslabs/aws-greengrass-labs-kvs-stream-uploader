#!/bin/bash

# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
# http://aws.amazon.com/apache2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.

if [ $# -ne 3 ]; then
  echo 1>&2 "Usage: $0 AwsProfile IamRole ComponentArtifactsBucket"
  exit 0
fi

AwsProfile=$1
IamRole=$2
ComponentArtifactsBucket=$3

# aws profile
AwsRegion=$(aws configure get $AwsProfile.region)

# policy name
ListRoles="testListRoles"
ListPolicies="testListPolicies"
IamPolicyArtifact="GreengrassV2ComponentArtifactPolicy"
IamPolicyKVS="GreengrassV2ComponentKVSPolicy"

# create temp patch
TempPath="temp"
mkdir -p $TempPath

echo "Start attaching necessary policies"
aws --profile $AwsProfile iam list-roles > $TempPath/$ListRoles.json
ListedRoleName=($(jq -r '.Roles[].RoleName' $TempPath/$ListRoles.json))
IamRoleExisted=0
for i in "${ListedRoleName[@]}"
do
  if [ $i = $IamRole ]; then
    echo "The IamRole exists"
    IamRoleExisted=1
    break
  fi
done

if [ $IamRoleExisted -eq 0 ]; then
  echo "The IamRole does not exist"
fi

aws --profile $AwsProfile iam list-policies --scope=Local > $TempPath/$ListPolicies.json
ListedPolicyName=($(jq -r '.Policies[].PolicyName' $TempPath/$ListPolicies.json))
ListedPolicyArn=($(jq -r '.Policies[].Arn' $TempPath/$ListPolicies.json))
IamPolicyArtifactExisted=-1
IamPolicyKVSExisted=-1
index=0
for i in "${ListedPolicyName[@]}"
do
  if [ $i = $IamPolicyArtifact ]; then
    echo "The policy $IamPolicyArtifact exists."
    IamPolicyArtifactExisted=$index
  elif [ $i = $IamPolicyKVS ]; then
    echo "The policy $IamPolicyKVS exists."
    IamPolicyKVSExisted=$index
  fi
  index=$(($index1))
done

if [ $IamPolicyArtifactExisted -eq -1 ]; then
  echo "Creating $IamPolicyArtifact for the role"
  # https://docs.aws.amazon.com/greengrass/v2/developerguide/device-service-role.html#device-service-role-access-s3-bucket
  cat > $TempPath/IamPermissionS3.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject"
      ],
      "Resource": [
        "arn:aws:s3:::$ComponentArtifactsBucket/*"
      ]
    }
  ]
}
EOF
  aws --profile $AwsProfile iam create-policy --policy-name $IamPolicyArtifact --policy-document file://$TempPath/IamPermissionS3.json > $TempPath/$IamPolicyArtifact.json
  echo "Attaching $IamPolicyArtifact to the role"
  aws --profile $AwsProfile iam attach-role-policy --role-name $IamRole --policy-arn $(jq --raw-output '.Policy.Arn' $TempPath/$IamPolicyArtifact.json)
else
  echo "Attaching $IamPolicyArtifact to the role"
  aws --profile $AwsProfile iam attach-role-policy --role-name $IamRole --policy-arn ${ListedPolicyArn[IamPolicyArtifactExisted]}
fi

if [ $IamPolicyKVSExisted -eq -1 ]; then
  echo "Creating $IamPolicyKVS for the role"
  echo '{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "kinesisvideo:DescribeStream",
                "kinesisvideo:PutMedia",
                "kinesisvideo:TagStream",
                "kinesisvideo:GetDataEndpoint"
            ],
            "Resource": "arn:aws:kinesisvideo:*:857591377198:stream/*/*"
        }
    ]
}' > $TempPath/IamPermissionKVS.json
  aws --profile $AwsProfile iam create-policy --policy-name $IamPolicyKVS --policy-document file://$TempPath/IamPermissionKVS.json > $TempPath/$IamPermissionKVS.json
  echo "Attaching $IamPolicyKVS to the role"
  aws --profile $AwsProfile iam attach-role-policy --role-name $IamRole --policy-arn $(jq --raw-output '.Policy.Arn' $TempPath/$IamPermissionKVS.json)
else
  echo "Attaching $IamPolicyKVS to the role"
  aws --profile $AwsProfile iam attach-role-policy --role-name $IamRole --policy-arn ${ListedPolicyArn[IamPolicyKVSExisted]}
fi

rm -rf $TempPath
