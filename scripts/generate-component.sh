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
  echo 1>&2 "Usage: $0 ComponentName ComponentVersion ComponentArtifactsBucket"
  exit 1
fi
# information of this component.
# e.g. com.aws.iot.iotlab.streamuploader
ComponentName=$1
# e.g. 1.0.0
ComponentVersion=$2
# e.g. greengrass-kvs-bucket
ComponentArtifactsBucket=$3
AwsRegion=$(aws configure get $AwsProfile.region)

# generating the recipes.
cat > recipes/$ComponentName-$ComponentVersion.json<<EOF
{
  "RecipeFormatVersion": "2020-01-25",
  "ComponentName": "$ComponentName",
  "ComponentVersion": "$ComponentVersion",
  "ComponentType": "aws.greengrass.generic",
  "ComponentDescription": "Upload RTSP stream to KVS",
  "ComponentPublisher": "Amazon",
  "ComponentConfiguration": {
    "DefaultConfiguration": {
      "KVS_STREAM_REGION": "$AwsRegion"
    }
  },
  "ComponentDependencies": {
    "aws.greengrass.TokenExchangeService": {
      "VersionRequirement": ">=2.0.0 <3.0.0",
      "DependencyType": "HARD"
    }
  },
  "Manifests": [
    {
      "Platform": {
        "os": "linux"
      },
      "Name": "Linux",
      "Lifecycle": {
        "Run": "java -cp {artifacts:path}/StreamUploader.jar com.aws.iot.iotlab.streamuploader.StreamUploaderMain",
        "Setenv": {
          "KVS_STREAM_REGION": "{configuration:/KVS_STREAM_REGION}"
        }
      },
      "Artifacts": [
        {
          "Uri": "s3://$ComponentArtifactsBucket/artifacts/$ComponentName/$ComponentVersion/StreamUploader.jar",
          "Permission": {
            "Read": "OWNER",
            "Execute": "OWNER"
          }
        },
        {
          "Uri": "s3://$ComponentArtifactsBucket/artifacts/$ComponentName/$ComponentVersion/streamuploaderconfig.json",
          "Permission": {
            "Read": "OWNER",
            "Execute": "OWNER"
          }
        }
      ]
    }
  ],
  "Lifecycle": {}
}
EOF
