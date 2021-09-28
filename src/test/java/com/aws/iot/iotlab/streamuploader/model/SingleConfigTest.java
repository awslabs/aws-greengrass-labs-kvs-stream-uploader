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

package com.aws.iot.iotlab.streamuploader.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingleConfigTest {
    @Test
    public void configTest() {
        SingleConfig.SingleConfigBuilder singleConfigBuilder = SingleConfig.builder().
                KvsStreamName("test-kvs-stream-12345").streamBufferSize(12345).RtspUrl("rtsp://abc.com/test.mkv");
        System.out.println(singleConfigBuilder.toString());
        SingleConfig singleConfig = singleConfigBuilder.build();

        Assertions.assertEquals("test-kvs-stream-12345", singleConfig.getKvsStreamName());
        Assertions.assertEquals("rtsp://abc.com/test.mkv", singleConfig.getRtspUrl());
        Assertions.assertEquals(12345, singleConfig.getStreamBufferSize());
    }
}
