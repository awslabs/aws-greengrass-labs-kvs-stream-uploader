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

echo "Installing prerequisites"
sudo apt-get update && sudo apt-get install -y openjdk-11-jdk curl unzip

echo "copy certificate files"
mkdir -p /greengrass/v2 && mkdir -p ~/GreengrassCore
cp certs/* /greengrass/v2
cp config.yaml ~/GreengrassCore

echo "Downloading Greengrass v2"
cd ~ && curl -s https://d2s8p88vqu9w66.cloudfront.net/releases/greengrass-nucleus-latest.zip > \
  greengrass-nucleus-latest.zip && yes | unzip greengrass-nucleus-latest.zip -d GreengrassCore && \
  rm greengrass-nucleus-latest.zip

echo "Installing and provisioning Greengrass v2"
sudo -E java -Droot='/greengrass/v2' -Dlog.store=FILE \
  -jar ~/GreengrassCore/lib/Greengrass.jar \
  --init-config ~/GreengrassCore/config.yaml \
  --component-default-user ggc_user:ggc_group \
  --setup-system-service true \
  --deploy-dev-tools true

echo "Greengrass v2 installed"
