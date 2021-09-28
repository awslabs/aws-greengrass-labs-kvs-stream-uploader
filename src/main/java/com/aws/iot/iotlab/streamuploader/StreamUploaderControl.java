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

import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.RecorderStatus;
import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorderBuilder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.callback.StatusCallback;
import com.aws.iot.edgeconnectorforkvs.videouploader.VideoUploader;
import com.aws.iot.edgeconnectorforkvs.videouploader.VideoUploaderClient;
import lombok.extern.slf4j.Slf4j;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Runnable controller to stream data between recorder and uploader
 */
@Slf4j
public class StreamUploaderControl implements Runnable {
    protected PipedOutputStream pipedOutputStream;
    protected PipedInputStream pipedInputStream;
    protected StreamConfig streamConfig;

    /**
     * StreamUploaderControl Constructor
     * @param streamConfig
     */
    public StreamUploaderControl (StreamConfig streamConfig) {
        this.streamConfig = streamConfig;
    }

    /**
     * StreamUploaderControl runnable class for multi-thread usage
     */
    public void run() {
        initPipedStream();

        VideoRecorder videoRecorder = initRecorder();
        VideoUploader videoUploader = initUploader();

        new Thread(new RunnableRecorder(videoRecorder)).start();
        new Thread(new RunnableUploader(videoUploader, pipedInputStream)).start();
    }

    /**
     * Initiate piped input and output stream for data transfer between threads
     */
    protected void initPipedStream() {
        pipedOutputStream = new PipedOutputStream();
        try {
            pipedInputStream = new PipedInputStream(pipedOutputStream, streamConfig.STREAM_BUFFER_SIZE);
        } catch (Exception e) {
            log.error("PipedInputStream initialization failed", e);
        }
    }

    /**
     * Initiate video recorder for stream data collection
     * @return VideoRecorder
     */
    protected VideoRecorder initRecorder() {
        StatusCallback callback = (recorder, status, description) -> {
            log.info("Status changed: " + status + ", description: " + description);
            if (status == RecorderStatus.FAILED) {
                new Thread(new RunnableRecorder((VideoRecorder) recorder)).start();
            }
        };
        VideoRecorderBuilder builder = new VideoRecorderBuilder(callback);
        builder.registerCamera(streamConfig.REC_TYPE, streamConfig.RTSP_SRC_URL);
        builder.registerAppDataOutputStream(streamConfig.CONTAINER_TYPE, pipedOutputStream);
        VideoRecorder recorder = builder.construct();
        recorder.toggleAppDataOutputStream(true);

        return recorder;
    }

    /**
     * Initiate video uploader for KVS upload
     * @return VideoUploader
     */
    protected VideoUploader initUploader() {
        VideoUploader uploader = VideoUploaderClient.builder()
                .awsCredentialsProvider(new EC2ContainerCredentialsProviderWrapper())
                .region(streamConfig.REGION)
                .recordFilePath(streamConfig.STREAM_PATH)
                .kvsStreamName(streamConfig.KVS_STREAM_NAME)
                .build();

        return uploader;
    }
}
