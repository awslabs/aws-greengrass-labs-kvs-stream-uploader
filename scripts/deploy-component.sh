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

# https://docs.aws.amazon.com/greengrass/v2/developerguide/create-deployments.html

if [ $# -ne 4 ]; then
  echo 1>&2 "Usage: $0 AwsProfile IotThingName ComponentName ComponentVersion"
  exit 1
fi
AwsProfile=$1
IotThingName=$2
ComponentName=$3
ComponentVersion=$4

# path to necessary folder
TempPath="temp"
DeploymentName="Deployment"

# aws profile
AwsRegion=$(aws configure get $AwsProfile.region)

# create temp patch
mkdir -p $TempPath

aws --profile $AwsProfile iot describe-thing --thing-name $IotThingName > $TempPath/$IotThingName.json
ThingTargetArn=$(jq -r '.thingArn' $TempPath/$IotThingName.json)
echo "TargetARN: $ThingTargetArn"

cat > $TempPath/$DeploymentName.json<<EOF
{
    "targetArn": "$ThingTargetArn",
    "deploymentName": "Deployment for MyGreengrassCore",
    "components": {
        "$ComponentName":{
            "componentVersion": "$ComponentVersion"
        }
    },
    "deploymentPolicies": {
        "failureHandlingPolicy": "ROLLBACK",
        "componentUpdatePolicy": {
            "timeoutInSeconds": 60,
            "action": "NOTIFY_COMPONENTS"
        }
    },
    "iotJobConfiguration": {}
}
EOF

aws greengrassv2 create-deployment --cli-input-json file://$TempPath/$DeploymentName.json

rm -rf $TempPath
