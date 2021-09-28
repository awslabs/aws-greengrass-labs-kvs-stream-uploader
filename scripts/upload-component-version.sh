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

if [ $# -ne 4 ]; then
  echo 1>&2 "Usage: $0 AwsProfile ComponentName ComponentVersion ComponentArtifactsBucket"
  exit 1
fi

AwsProfile=$1
ComponentName=$2
ComponentVersion=$3
ComponentArtifactsBucket=$4
AwsRegion=$(aws configure get $AwsProfile.region)
JarFile="StreamUploader.jar"
ConfigFile="streamuploaderconfig.json"

aws s3api put-object --profile $AwsProfile --bucket $ComponentArtifactsBucket --key artifacts/$ComponentName/$ComponentVersion/$JarFile --body target/$JarFile
aws s3api put-object --profile $AwsProfile --bucket $ComponentArtifactsBucket --key artifacts/$ComponentName/$ComponentVersion/$ConfigFile --body $ConfigFile

aws greengrassv2 create-component-version --profile $AwsProfile --inline-recipe fileb://recipes/$ComponentName-$ComponentVersion.json --region $AwsRegion
