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

package com.aws.iot.integrationtests.edgeconnectorforkvs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideo;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoClient;
import com.amazonaws.services.kinesisvideo.model.CreateStreamRequest;
import com.amazonaws.services.kinesisvideo.model.CreateStreamResult;
import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorderBuilder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.ContainerType;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.CameraType;
import com.aws.iot.edgeconnectorforkvs.videouploader.VideoUploader;
import com.aws.iot.edgeconnectorforkvs.videouploader.VideoUploaderClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;

@Slf4j
public class HistoricalUploadingIntegrationTest {
    public static String SRC_URL = "rtsp://localhost/sample-mkv-file.mkv";

    // AWS credentials
    private static final String propertiesFilePath = System.getProperty("user.home")
            + "/.aws/awsTestAccount.properties";
    private static final String TEST_CREDENTIALS_PROFILE_NAME = "aws-java-sdk-test";
    private static final AWSCredentialsProviderChain awsCredentialsProviderChain = new AWSCredentialsProviderChain(
            new PropertiesFileCredentialsProvider(propertiesFilePath),
            new ProfileCredentialsProvider(TEST_CREDENTIALS_PROFILE_NAME),
            new EnvironmentVariableCredentialsProvider(),
            new SystemPropertiesCredentialsProvider(),
            new AWSStaticCredentialsProvider(new BasicAWSCredentials("accessKey", "secretKey")));
    private static final AWSCredentials awsCredentials = awsCredentialsProviderChain.getCredentials();

    // AWS region
    private static final Region region = Region.getRegion(Regions.US_EAST_1);

    // Test date and name
    private static final Date TEST_DATE = Date.from(Instant.now());
    private static final String TEST_NAME = "HistoricalUploadingIntegrationTest-" + TEST_DATE.getTime();

    // Temp folder prefix
    private static final String TEST_FOLDER_PREFIX = TEST_NAME + "-";
    private static Path tempDir = null;

    // Recorder settings
    private static final CameraType REC_TYPE = CameraType.RTSP;
    private static final ContainerType CON_TYPE = ContainerType.MATROSKA;

    // KVS stream name
    private static final String KVS_STREAM_NAME = TEST_NAME;

    private VideoRecorder videoRecorder;
    private VideoUploader videoUploader;

    @Test
    public void historicalUploading() throws IOException, InterruptedException {
        tempDir = createTempFolder(TEST_FOLDER_PREFIX);
        Assumptions.assumeTrue(tempDir != null);

        // setup builder of recorder
        VideoRecorderBuilder builder = new VideoRecorderBuilder((rec, st, desc) -> {
        });

        // add camera
        builder.registerCamera(REC_TYPE, SRC_URL);
        // add file sink
        builder.registerFileSink(CON_TYPE, Paths.get(tempDir.toString(), "video").toString());
        builder.setFilePathProperty("max-size-time", 5_000_000_000L);

        log.info("Start recording...");
        // setup recorder and run recording
        final Date videoUploadingStartTime = Date.from(Instant.now());
        videoRecorder = builder.construct();
        new Thread(() -> videoRecorder.startRecording()).start();

        // Recording for 1 minutes
        Thread.sleep(1 * 60 * 1000);

        // stop recording
        log.info("Stop recording");
        videoRecorder.stopRecording();
        final Date videoUploadingEndTime = Date.from(Instant.now());

        // create a KVS stream for testing
        Assumptions.assumeTrue(createKvsStream(awsCredentials, region, KVS_STREAM_NAME, 1));

        // Setup uploader
        videoUploader = VideoUploaderClient.builder()
                .awsCredentialsProvider(new AWSStaticCredentialsProvider(awsCredentials))
                .region(region)
                .recordFilePath(tempDir.toString())
                .kvsStreamName(KVS_STREAM_NAME)
                .build();

        // Start uploading
        log.info("Start uploading...");
        videoUploader.uploadHistoricalVideo(videoUploadingStartTime, videoUploadingEndTime, null, null);
    }

    private static Path createTempFolder(String tempFolderPrefix) {
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory(tempFolderPrefix);
            log.info("Created temp folder: " + tempDir);
        } catch (IOException ex) {
            log.error("Unable to create temp folder");
        }

        return tempDir;
    }

    private static boolean createKvsStream(AWSCredentials awsCredentials, Region region, String streamName,
                                           int dataRetentionInHours) {
        boolean result = false;

        AmazonKinesisVideo client = AmazonKinesisVideoClient.builder()
                .withRegion(region.toString())
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();

        try {
            CreateStreamResult stream = client.createStream(
                    new CreateStreamRequest().
                            withStreamName(streamName).
                            withDataRetentionInHours(dataRetentionInHours));
            if (stream.getSdkHttpMetadata().getHttpStatusCode() == 200) {
                log.info("Created KVS stream: " + streamName);
                result = true;
            } else {
                log.error("Unable to create KVS stream");
            }
        } catch (Exception ex) {
            log.error("Unable to create KVS stream");
        }

        return result;
    }
}
