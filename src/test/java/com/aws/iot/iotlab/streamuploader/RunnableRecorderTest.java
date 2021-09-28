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

import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.RecorderStatus;
import com.aws.iot.iotlab.streamuploader.model.SingleConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class RunnableRecorderTest {
    private static final Long STATUS_CHANGED_TIME = 30L;
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
    public void runnerTest() throws InterruptedException {
        VideoRecorder videoRecorder = streamUploaderControl.initRecorder();
        Thread recorderThread = new Thread(new RunnableRecorder(videoRecorder));
        recorderThread.start();
        while (videoRecorder.getStatus() == RecorderStatus.STOPPED)
            Thread.sleep(STATUS_CHANGED_TIME);
        recorderThread.interrupt();
    }
}
