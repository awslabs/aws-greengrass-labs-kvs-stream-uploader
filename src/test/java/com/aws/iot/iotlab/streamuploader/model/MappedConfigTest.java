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

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class MappedConfigTest {
    private static Gson gson = new Gson();

    @Test
    public void configTest() {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("streamuploaderconfig.json");
        MappedConfig mappedConfig = gson.fromJson(new InputStreamReader(inputStream), MappedConfig.class);
        Assertions.assertNotNull(mappedConfig.getConfigList());
        List<SingleConfig> configList = mappedConfig.getConfigList();

        SingleConfig firstConfig = configList.get(0);
        Assertions.assertEquals("test-greengrass-kvs-stream-1", firstConfig.getKvsStreamName());
        Assertions.assertEquals("rtsp://test-1.com", firstConfig.getRtspUrl());
        Assertions.assertEquals(0, firstConfig.getStreamBufferSize());

        SingleConfig secondConfig = configList.get(1);
        Assertions.assertEquals("test-greengrass-kvs-stream-2", secondConfig.getKvsStreamName());
        Assertions.assertEquals("rtsp://test-2.com", secondConfig.getRtspUrl());
        Assertions.assertEquals(0, secondConfig.getStreamBufferSize());
    }
}
