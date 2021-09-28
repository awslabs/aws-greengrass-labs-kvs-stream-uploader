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

if [ $# -ne 2 ]; then
  echo 1>&2 "Usage: $0 AwsProfile ComponentArtifactsBucket"
  exit 1
fi
# aws profile
AwsProfile=$1
# information of this component.
ComponentArtifactsBucket=$2

# path to necessary folder.
TempPath="temp"
AwsRegion=$(aws configure get $AwsProfile.region)
ListBuckets="ListBuckets"
ListStreams="ListStreams"

# create temp path
mkdir -p $TempPath

# creating S3 bucket
BucketExisted=0
aws --profile $AwsProfile s3api list-buckets > $TempPath/$ListBuckets.json
ListedBucketName=($(jq -r '.Buckets[].Name' $TempPath/$ListBuckets.json))

for i in "${ListedBucketName[@]}"
do
  if [ $i = $ComponentArtifactsBucket ]; then
    echo "The same bucket $ComponentArtifactsBucket exists."
    BucketExisted=1
    break
  fi
done

if [ $BucketExisted -eq 0 ]; then
  echo "Creating the new bucket $ComponentArtifactsBucket"
  aws --profile $AwsProfile s3 mb s3://$ComponentArtifactsBucket --region $AwsRegion > $TempPath/tmps3.json
fi

# creating KVS stream
aws --profile $AwsProfile kinesisvideo list-streams > $TempPath/$ListStreams.json
ListedStreamName=($(jq -r '.StreamInfoList[].StreamName' $TempPath/$ListStreams.json))
ListedStreamToCreate=($(jq -r '.configList[].KvsStreamName' streamuploaderconfig.json))

for KvsStreamName in "${ListedStreamToCreate[@]}"
do
  StreamExisted=0
  for i in "${ListedStreamName[@]}"
  do
    if [ $i = $KvsStreamName ]; then
      echo "The same stream $KvsStreamName exists."
      StreamExisted=1
      break
    fi
  done

  if [ $StreamExisted -eq 0 ]; then
    echo "Creating the new stream $KvsStreamName"
    aws --profile $AwsProfile kinesisvideo create-stream --stream-name $KvsStreamName --data-retention-in-hours 24
  fi
done

rm -rf $TempPath
