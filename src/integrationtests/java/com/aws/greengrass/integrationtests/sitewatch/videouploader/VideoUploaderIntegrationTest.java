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

package com.aws.greengrass.integrationtests.sitewatch.videouploader;

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
import com.amazonaws.services.kinesisvideo.model.CreateStreamRequest;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideo;
import com.amazonaws.services.kinesisvideo.AmazonKinesisVideoClient;
import com.amazonaws.services.kinesisvideo.model.CreateStreamResult;

import com.aws.greengrass.sitewatch.videouploader.VideoUploaderClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
public class VideoUploaderIntegrationTest {

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
    private static final String TEST_NAME = "VideoUploaderIntegrationTest-" + TEST_DATE.getTime();

    // KVS stream name
    private static final String KVS_STREAM_NAME = TEST_NAME;

    // Temp folder prefix
    private static final String TEST_FOLDER_PREFIX = TEST_NAME + "-";
    private static Path tempDir = null;

    // Sample file
    private static final String SAMPLE_MKV_RESOURCE_NAME = "sample-mkv-file.mkv";
    private static final String SAMPLE_MKV_FILENAME = "sample.mkv";
    private static final Long SAMPLE_MKV_DURATION_MS = 55008L;
    private static Path sampleFilePath = null;

    @BeforeAll
    public static void setupForAll() {
        tempDir = createTempFolder(TEST_FOLDER_PREFIX);

        if (tempDir != null) {
            sampleFilePath = downloadSampleFile(SAMPLE_MKV_RESOURCE_NAME, tempDir, SAMPLE_MKV_FILENAME);
        }
    }

    @AfterAll
    public static void cleanupForAll() throws IOException {
        // Clean up temp folders
        if (tempDir != null ) {
            cleanUpFolder(tempDir);
        }
    }

    @Test
    public void uploadHistoricalVideo_1MinRecWithin5min_noExceptions() {
        long videoRecordPeriod = 20;
        long videoPeriodToUpload = 5;

        Assumptions.assumeTrue(tempDir != null && sampleFilePath != null);

        // Create KVS Stream
        Assumptions.assumeTrue(createKvsStream(awsCredentials, region, KVS_STREAM_NAME, 1));

        // Setup video files
        Path recordFilePath = Paths.get(tempDir.toString(), "record_60_minutes");
        Instant now = Instant.now();
        Assumptions.assumeTrue(
                generateVideoFiles(recordFilePath, sampleFilePath, SAMPLE_MKV_DURATION_MS,
                        new Date(now.toEpochMilli() - TimeUnit.MINUTES.toMillis(videoRecordPeriod)),
                        new Date(now.toEpochMilli())));

        // Setup test
        VideoUploaderClient videoUploaderClient = VideoUploaderClient.builder()
                .awsCredentialsProvider(new AWSStaticCredentialsProvider(awsCredentials))
                .region(region)
                .recordFilePath(recordFilePath.toString())
                .kvsStreamName(KVS_STREAM_NAME)
                .build();

        Date testStart = Date.from(Instant.now());
        Assertions.assertDoesNotThrow(() ->
                videoUploaderClient.uploadHistoricalVideo(
                        new Date(now.toEpochMilli() - TimeUnit.MINUTES.toMillis(videoPeriodToUpload)),
                        new Date(now.toEpochMilli()),
                        null,
                        null));
        Date testEnd = Date.from(Instant.now());
        log.info("Time cost: " + (testEnd.getTime() - testStart.getTime()) + " ms");
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

    private static Path downloadSampleFile(String resourceName, Path tempDir, String filename) {
        Path sampleFilePath = null;

        // Download sample mkv file and save it in the temp folder
        log.info("Downloading test file...");
        try {
            Files.copy(ClassLoader.getSystemResourceAsStream(resourceName), Paths.get(tempDir.toString(), filename));
            log.info("sample file saved to " + Paths.get(tempDir.toString(), SAMPLE_MKV_FILENAME));
            sampleFilePath = Paths.get(tempDir.toString(), filename);
        } catch (MalformedURLException ex ) {
            log.error("Invalid URL format");
        } catch (IOException ex) {
            log.error("Unable to download sample video");
        }

        return sampleFilePath;
    }

    public static void cleanUpFolder(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static boolean generateVideoFiles(Path destinationPath, Path sampleFilePath,
                                             long sampleFileDuration, Date timeBegin, Date timeEnd) {
        boolean result = false;

        try {
            Path path = Files.createDirectories(destinationPath);
            for (Instant videoDate = timeBegin.toInstant(); Date.from(videoDate).before(timeEnd); videoDate =
                    videoDate.plusMillis(sampleFileDuration)) {
                String videoFilename = "video_" + videoDate.toEpochMilli() + ".mkv";
                Files.copy(sampleFilePath, Paths.get(path.toString(), videoFilename));
            }
            log.info("video files created for timestamp from " + timeBegin.getTime() + " to " + timeEnd.getTime());
            result = true;
        } catch (Exception ex) {
            log.info("Unable to crate video files");
        }

        return result;
    }
}
