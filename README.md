## aws-greengrass-kvs-stream-uploader
This package builds an [AWS Greengrass component](https://docs.aws.amazon.com/greengrass/v2/developerguide/manage-components.html) that can be deployed to Greengrass core device and enable [Amazon Kinesis Video Streams (KVS)](https://aws.amazon.com/kinesis/video-streams) service to ingest video workload from brownfield IP cameras. Kinesis Video Streams (KVS) is an AWS service that lets IP cameras stream video to AWS for storage, replay and analytics.

## Build Instruction

### Download
```
git clone https://github.com/awslabs/aws-greengrass-labs-kvs-stream-uploader.git
```

### Prerequisite Setup

#### Java
Java is required to run greengrass core software. See [Set Up Your Environment](https://docs.aws.amazon.com/greengrass/v2/developerguide/getting-started.html#set-up-environment) if target machine is a raspberry pi. Last Java version we have tested is OpenJDK 11. Any future Java release may cause unit tests to fail in this package.

#### GreenGrass Core Device
StreamUploader can only deploy to device set up as greengrass core device. See [Install the AWS IoT Greengrass Core software](https://docs.aws.amazon.com/greengrass/v2/developerguide/install-greengrass-core-v2.html). Automatic provisioning is recommended.

#### GStreamer
GStreamer is the framework used for media streaming. See [Instruction to install GStreamer](https://gstreamer.freedesktop.org/documentation/installing/index.html) on different platform.

#### AWS CLI (Automation Script Only)
AWS CLI enables users to interact with variant AWS services through command line. In this package, we use AWS CLI for S3 and GreenGrass communication. See [Configuring the AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-configure.html).

#### JQ (Automation Script Only)
JQ is a JSON processor used for component automation scripts. See [Download jq](https://stedolan.github.io/jq/download/).

### Build
StreamUploader is a [Maven](https://maven.apache.org/) project. Please make sure Maven is installed before building the package. To build package, run:
```
mvn clean package
```
If current Java version causes unit tests to fail, simply build the package and skip the test.
```
mvn clean package -Dmaven.test.skip=true
```
Java executable `StreamUploader.jar` is generated under `target` folder.

### Component Deployment Automation

We have provided [automation scripts for deployment](/scripts). If you want to deploy Stream Uploader to any Linux device, make sure the prerequisite setup is finished and jar file exists under `target` folder. Follow the steps below.

**Step 1:** Fill in necessary properties used for stream upload (e.g. RtspUrl, KvsStreamName) in `streamuploaderconfig.json` file. Stream Uploader can support multiple RTSP camera/server simultaneously and upload to different KVS streams. Config file is configured as sample below.
```
{
    "configList": [
        {
            "RtspUrl": "rtsp://rtsp-url-1",
            "KvsStreamName": "kvs-stream-name-1"
        },
        {
            "RtspUrl": "rtsp://rtsp-url-2",
            "KvsStreamName": "kvs-stream-name-2"
        }
    ]
}
```

**Step 2:** Provide the name of your IoT Thing to attach the necessary policies and AWS resources including S3 bucket and KVS stream. This script will create policies and AWS resources if you do not have them, and generate recipe according to your setting.
```
bash scripts/generate-iot-greengrass.sh AwsProfile IotThingName ComponentName ComponentVersion ComponentArtifactsBucket
```
Example:
```
bash scripts/generate-iot-greengrass.sh default MyGreengrassCore aws.greengrass.labs.kvs.stream.uploader 1.0.0 greengrass-kvs-bucket
```

**Step 3:** Upload component and create component on IoT Core. For more info, see [Upload components to deploy to your core devices](https://docs.aws.amazon.com/greengrass/v2/developerguide/upload-components.html).
```
bash scripts/upload-component-version.sh AwsProfile ComponentName ComponentVersion ComponentArtifactsBucket
```
Example:
```
bash scripts/upload-component-version.sh default aws.greengrass.labs.kvs.stream.uploader 1.0.0 greengrass-kvs-bucket
```

**Step 4:** Deploy component with its dependency. Make sure rtsp server is up and running before component deployment.
```
bash scripts/deploy-component.sh AwsProfile IotThingName ComponentName ComponentVersion
```
Example:
```
bash scripts/deploy-component.sh default MyGreengrassCore aws.greengrass.labs.kvs.stream.uploader 1.0.0
```

**Step 5:** (Optional) Remove component from GreenGrass core device. If component is deployed prematurely or unsuccessfully run, remove component from core device and re-deploy it. This can be done from AWS IoT Core console as well.
```
bash scripts/remove-component.sh AwsProfile IotThingName
```
Example:
```
bash scripts/remove-component.sh default MyGreengrassCore
```

### Log Debug
On device running greengrass core, logs are stored in specific greengrass folder.<br>
For greengrass log, check `/greengrass/v2/logs/greengrass.log`.<br>
For stream uploader log, check `/greengrass/v2/logs/aws.greengrass.labs.kvs.stream.uploader.log`. If other component name is defined, use `/greengrass/v2/logs/{component_name}.log`.<br>
Log level can be configured in [log4j.properties](src/main/resources/log4j.properties). Package needs to re-build and deploy before log configuration takes effect.

## Dependency
### GreenGrass Component
* [Token Exchange Service](https://docs.aws.amazon.com/greengrass/v2/developerguide/token-exchange-service-component.html)

### Library
* [AWS Kinesis Video Stream Parser Library](https://github.com/aws/amazon-kinesis-video-streams-parser-library)
* [AWS SDK for Java](https://aws.amazon.com/sdk-for-java/)
* [GStreamer Java](https://github.com/gstreamer-java)

## Appendix
If you want to install GreengrassV2 on your remote devices manually, you can use the following command after `scripts/generate-iot-greengrass.sh` generates the config.yaml, certificate, and other necessary stuff. For more info, see [manual resource provisioning](https://docs.aws.amazon.com/greengrass/v2/developerguide/manual-installation.html).
```
bash scripts/install-ggv2.sh
```
Note that install script uses `apt` as package tool. If your device has other package tool installed, simply replace `apt-get` in [install script](scripts/install-ggv2.sh) with other package tool command to install prerequisites in script.

## License
This library is licensed under the Apache 2.0 License.