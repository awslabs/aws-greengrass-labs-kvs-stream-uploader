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

import com.amazonaws.regions.RegionUtils;
import com.aws.greengrass.sitewatch.videorecorder.model.CameraType;
import com.aws.greengrass.sitewatch.videorecorder.model.ContainerType;
import com.aws.iot.iotlab.streamuploader.model.SingleConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class StreamConfigTest {
    @SystemStub
    private static EnvironmentVariables environment;

    @BeforeAll
    public static void setupAll(){
        environment.set("AWS_REGION", "us-west-2");
    }

    @Test
    public void configTest() {
        environment.set("KVS_STREAM_REGION", "us-east-1");
        SingleConfig.SingleConfigBuilder singleConfigBuilder = SingleConfig.builder().
                KvsStreamName("test-kvs-stream").streamBufferSize(10000).RtspUrl("rtsp://abc.com/test.mkv");
        SingleConfig singleConfig = singleConfigBuilder.build();
        Assertions.assertNotNull(singleConfigBuilder.toString());
        StreamConfig streamConfig = new StreamConfig(singleConfig);

        Assertions.assertEquals(CameraType.RTSP, streamConfig.REC_TYPE);
        Assertions.assertEquals(ContainerType.MATROSKA, streamConfig.CONTAINER_TYPE);
        Assertions.assertEquals("rtsp://abc.com/test.mkv", streamConfig.RTSP_SRC_URL);
        Assertions.assertEquals(10000, streamConfig.STREAM_BUFFER_SIZE);
        Assertions.assertEquals("/", streamConfig.STREAM_PATH);
        Assertions.assertEquals(RegionUtils.getRegion("us-east-1"), streamConfig.REGION);
        Assertions.assertEquals("test-kvs-stream", streamConfig.KVS_STREAM_NAME);
    }

    @Test
    public void invalidBufferTest() {
        SingleConfig singleConfig = SingleConfig.builder().
                KvsStreamName("test-kvs-stream").streamBufferSize(0).RtspUrl("rtsp://abc.com/test.mkv").build();
        StreamConfig streamConfig = new StreamConfig(singleConfig);
        Assertions.assertEquals(100000, streamConfig.STREAM_BUFFER_SIZE);
    }

    @Test
    public void emptyRegionTest() {
        environment.set("KVS_STREAM_REGION", "");
        SingleConfig singleConfig = SingleConfig.builder().
                KvsStreamName("test-kvs-stream").streamBufferSize(10000).RtspUrl("rtsp://abc.com/test.mkv").build();
        StreamConfig streamConfig = new StreamConfig(singleConfig);
        Assertions.assertEquals(RegionUtils.getRegion("us-west-2"), streamConfig.REGION);
    }
}
