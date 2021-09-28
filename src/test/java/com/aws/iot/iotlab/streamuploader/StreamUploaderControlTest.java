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

import com.aws.greengrass.sitewatch.videorecorder.VideoRecorder;
import com.aws.greengrass.sitewatch.videouploader.VideoUploader;
import com.aws.iot.iotlab.streamuploader.model.SingleConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class StreamUploaderControlTest {
    private static StreamUploaderControl streamUploaderControl;

    @SystemStub
    private static EnvironmentVariables environment;

    @BeforeAll
    public static void setUpAll() {
        environment.set("AWS_REGION", "us-west-2");
        environment.set("KVS_STREAM_REGION", "us-east-1");
        SingleConfig singleConfig = SingleConfig.builder().
                KvsStreamName("test-kvs-stream").streamBufferSize(100000).RtspUrl("rtsp://test.com").build();
        StreamConfig streamConfig = new StreamConfig(singleConfig);
        streamUploaderControl = new StreamUploaderControl(streamConfig);
        streamUploaderControl.initPipedStream();
    }

    @Test
    public void mainTest() {
        try {
            streamUploaderControl.run();
        } catch (Exception e) {
            Assertions.fail("Main function fail to invoke");
        }
    }

    @Test
    public void initPipedStreamTest() {
        Assertions.assertNotNull(streamUploaderControl.pipedOutputStream);
        Assertions.assertNotNull(streamUploaderControl.pipedInputStream);
    }

    @Test
    public void initPipedStreamFailureTest() {
        SingleConfig singleConfig = SingleConfig.builder().
                KvsStreamName("test-kvs-stream").streamBufferSize(-1).RtspUrl("rtsp://test.com").build();
        StreamConfig streamConfig = new StreamConfig(singleConfig);
        StreamUploaderControl streamUploaderControl = new StreamUploaderControl(streamConfig);
        streamUploaderControl.initPipedStream();
        Assertions.assertNotNull(streamUploaderControl.pipedOutputStream);
        Assertions.assertNull(streamUploaderControl.pipedInputStream);
    }

    @Test
    public void initRecorderTest() {
        VideoRecorder videoRecorder = streamUploaderControl.initRecorder();
        Assertions.assertNotNull(videoRecorder);
    }

    @Test
    public void initUploaderTest() {
        VideoUploader videoUploader = streamUploaderControl.initUploader();
        Assertions.assertNotNull(videoUploader);
    }
}
