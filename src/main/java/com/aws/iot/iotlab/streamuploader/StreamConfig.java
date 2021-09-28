/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.aws.iot.iotlab.streamuploader;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.aws.greengrass.sitewatch.videorecorder.model.ContainerType;
import com.aws.greengrass.sitewatch.videorecorder.model.CameraType;
import com.aws.iot.iotlab.streamuploader.model.SingleConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Config file for streaming
 */
@Slf4j
public class StreamConfig {
    // recorder config
    public CameraType REC_TYPE;
    public ContainerType CONTAINER_TYPE;
    public String RTSP_SRC_URL;
    public int STREAM_BUFFER_SIZE = 100000;

    // uploader config
    public Region REGION;
    public String STREAM_PATH;
    public String KVS_STREAM_NAME;

    public StreamConfig(SingleConfig singleConfig) {
        // recorder config
        REC_TYPE = CameraType.RTSP;
        CONTAINER_TYPE = ContainerType.MATROSKA;
        RTSP_SRC_URL = singleConfig.getRtspUrl();
        if (singleConfig.getStreamBufferSize() != 0) {
            STREAM_BUFFER_SIZE = singleConfig.getStreamBufferSize();
        }

        // uploader config
        STREAM_PATH = "/";
        KVS_STREAM_NAME = singleConfig.getKvsStreamName();
        REGION = RegionUtils.getRegion(System.getenv("AWS_REGION"));
        if (StringUtils.isNotEmpty(System.getenv("KVS_STREAM_REGION"))) {
            REGION = RegionUtils.getRegion(System.getenv("KVS_STREAM_REGION"));
        }
    }
}
