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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideo;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoPutMedia;
import com.amazonaws.services.kinesisvideo.PutMediaAckResponseHandler;
import com.amazonaws.services.kinesisvideo.model.GetDataEndpointRequest;
import com.amazonaws.services.kinesisvideo.model.GetDataEndpointResult;
import com.amazonaws.services.kinesisvideo.model.PutMediaRequest;
import com.aws.greengrass.sitewatch.videouploader.VideoUploaderClient;
import com.aws.iot.iotlab.streamuploader.model.SingleConfig;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
public class RunnableUploaderTest {
    private static final String DATA_ENDPOINT = "testDataEndpoint";
    private static final Long STATUS_CHANGED_TIME = 30L;
    private static StreamUploaderControl streamUploaderControl;

    @Mock
    private AWSCredentialsProvider awsCredentialsProvider;

    private Region region = Region.getRegion(Regions.US_WEST_2);

    @Mock
    private AmazonKinesisVideo kvsFrontendClient;

    @Mock
    private AmazonKinesisVideoPutMedia kvsDataClient;

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

    private boolean mockPrivateMember(VideoUploaderClient client, String fieldName, Object value) {
        boolean result = false;
        try {
            Field field = VideoUploaderClient.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(client, value);
            result = true;
        } catch (NoSuchFieldException exception) {
            System.out.println("Failed to mock " + fieldName + ", NoSuchFieldException");
        } catch (IllegalAccessException exception) {
            System.out.println("Failed to mock " + fieldName + ", IllegalAccessException");
        }
        return result;
    }

    @Test
    public void runnerTest() throws InterruptedException {
        streamUploaderControl.initPipedStream();
        VideoUploaderClient videoUploaderClient = VideoUploaderClient.builder()
                .awsCredentialsProvider(awsCredentialsProvider)
                .region(region)
                .recordFilePath("/")
                .kvsStreamName("test-kvs-stream")
                .build();
        Assumptions.assumeTrue(mockPrivateMember(videoUploaderClient, "kvsFrontendClient", kvsFrontendClient));
        Assumptions.assumeTrue(mockPrivateMember(videoUploaderClient, "kvsDataClient", kvsDataClient));

        Thread uploaderThread = new Thread(new RunnableUploader(videoUploaderClient, streamUploaderControl.pipedInputStream));

        when(kvsFrontendClient.getDataEndpoint(any(GetDataEndpointRequest.class))).thenReturn(new GetDataEndpointResult().withDataEndpoint(DATA_ENDPOINT));
        doNothing().when(kvsDataClient).putMedia(any(PutMediaRequest.class), any(PutMediaAckResponseHandler.class));

        uploaderThread.start();

        // wait until task start
        if (!videoUploaderClient.isOpen()) {
            System.out.println("task is not running");
            Thread.sleep(STATUS_CHANGED_TIME);
        }

        // wait until task end
        while (videoUploaderClient.isOpen()) {
            videoUploaderClient.close();
            System.out.println("task is running");
            Thread.sleep(STATUS_CHANGED_TIME);
        }

        uploaderThread.interrupt();
    }
}
