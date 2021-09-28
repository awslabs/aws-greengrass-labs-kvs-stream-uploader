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

import com.aws.iot.edgeconnectorforkvs.videouploader.VideoUploader;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.time.Instant;
import java.util.Date;

/**
 * Uploader runnable class for multi-thread usage
 */
@Slf4j
public class RunnableUploader implements Runnable {
    private VideoUploader videoUploader;
    private InputStream inputStream;

    public RunnableUploader(VideoUploader uploader, InputStream stream) {
        videoUploader = uploader;
        inputStream = stream;
    }

    @Override
    public void run() {
        final Runnable uploadCallBack = () -> {
            log.info("uploadCallback set status to true");
        };

        videoUploader.uploadStream(inputStream, Date.from(Instant.now()), null, uploadCallBack);
    }
}
