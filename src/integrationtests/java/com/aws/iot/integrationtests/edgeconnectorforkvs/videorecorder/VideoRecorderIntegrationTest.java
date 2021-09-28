/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except
 * in compliance with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.aws.iot.integrationtests.edgeconnectorforkvs.videorecorder;

import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorderBuilder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.ContainerType;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.RecorderStatus;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.CameraType;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VideoRecorderIntegrationTest {
    private static final String SRC_URL =
            "rtspt://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov";
    private static final CameraType REC_TYPE = CameraType.RTSP;
    private static final ContainerType CON_TYPE = ContainerType.MATROSKA;
    private static final String FILE_FOLDER = "/tmp/recorder_ut";
    private static final int RECORDING_TIME_IN_SECONDS = 8;
    private static final int RECORDING_FILE_PREFIX_LEN = 8;

    private int addAppCbTestStoreLen = 0;
    private OutputStream testOutputStream;

    private static class RunnableRecorder implements Runnable {
        private VideoRecorder recorder;

        public RunnableRecorder(VideoRecorder r) {
            recorder = r;
        }

        @Override
        public void run() {
            recorder.startRecording();
        }
    }

    @BeforeEach
    public void setupTests() {
        Assertions.assertTrue(createTestFolder());

        addAppCbTestStoreLen = 0;
        testOutputStream = null;
    }

    @AfterEach
    public void unSetupTests() {
        if (testOutputStream != null) {
            try {
                testOutputStream.close();
            } catch (IOException e) {
                Assertions.fail();
            }
        }
    }

    @Test
    public void setPropertyTest_invalidUsage_exceptionThrow() {
        VideoRecorderBuilder builder = new VideoRecorderBuilder((rec, st, desc) -> {
        });

        Assertions.assertTrue(() -> builder.registerCamera(REC_TYPE, SRC_URL));
        Assertions.assertFalse(builder.setCameraProperty("_invalid_property", 0));
        Assertions.assertFalse(builder.setFilePathProperty("_invalid_property", 0));

        Assertions.assertThrows(RejectedExecutionException.class, () -> builder.construct());
    }

    @Test
    public void addFileSinkTest_startRecording_storeFiles() {
        VideoRecorderBuilder builder = new VideoRecorderBuilder((rec, st, desc) -> {
        });

        // add camera
        Assertions.assertTrue(() -> builder.registerCamera(REC_TYPE, SRC_URL));

        // add file sink
        final String filePrefix = RandomStringUtils.randomAlphanumeric(RECORDING_FILE_PREFIX_LEN);
        final String filePathPrefix = FILE_FOLDER + "/" + filePrefix;
        Assertions.assertTrue(builder.registerFileSink(CON_TYPE, filePathPrefix));
        Assertions.assertFalse(builder.registerFileSink(CON_TYPE, filePathPrefix));
        Assertions.assertTrue(builder.setFilePathProperty("max-size-time", 5_000_000_000L));

        VideoRecorder recorder = builder.construct();
        Thread recordThread = new Thread(new RunnableRecorder(recorder));

        recordThread.start();
        try {
            TimeUnit.SECONDS.sleep(RECORDING_TIME_IN_SECONDS);
        } catch (InterruptedException e) {
            Assertions.fail();
        }
        recorder.stopRecording();

        // Check stored files
        long fileLen = 0;
        File dir = new File(FILE_FOLDER);
        FileFilter fileFilter = new WildcardFileFilter(filePrefix + "_*.mkv");
        File[] files = dir.listFiles(fileFilter);
        for (int i = 0; i < files.length; ++i) {
            fileLen += files[i].length();
        }
        System.out.println("addFileSinkTest_startRecording_storeFiles: " + fileLen);
        Assertions.assertTrue(fileLen > 0);

        // Clean files
        for (int i = 0; i < files.length; ++i) {
            files[i].delete();
        }
    }

    @Test
    public void addAppCbTest_startRecording_invokeCallback() {
        VideoRecorderBuilder builder = new VideoRecorderBuilder((rec, st, desc) -> {
        });

        // add camera
        Assertions.assertTrue(() -> builder.registerCamera(REC_TYPE, SRC_URL));

        // register app callback
        Assertions.assertThrows(NullPointerException.class,
                () -> builder.registerAppDataCallback(ContainerType.MATROSKA, null));
        Assertions.assertTrue(
                builder.registerAppDataCallback(ContainerType.MATROSKA, (recorder, bBuff) -> {
                    addAppCbTestStoreLen += bBuff.limit();
                }));
        Assertions.assertFalse(
                builder.registerAppDataCallback(ContainerType.MATROSKA, (recorder, bBuff) -> {
                }));

        VideoRecorder recorder = builder.construct();

        // Start recording
        Thread recordThread = new Thread(new RunnableRecorder(recorder));
        recordThread.start();
        recorder.toggleAppDataCallback(true);

        try {
            TimeUnit.SECONDS.sleep(RECORDING_TIME_IN_SECONDS);
        } catch (InterruptedException e) {
            Assertions.fail();
        }
        recorder.toggleAppDataCallback(false);
        recorder.stopRecording();

        // Check received length
        System.out.println("addAppCbTest_startRecording_invokeCallback: " + addAppCbTestStoreLen);
        Assertions.assertTrue(addAppCbTestStoreLen > 0);
    }

    @Test
    public void addAppOsTest_startRecording_writeOStream() {
        VideoRecorderBuilder builder = new VideoRecorderBuilder((rec, st, desc) -> {
        });

        // register app callback
        final String filePath = FILE_FOLDER + "/"
                + RandomStringUtils.randomAlphanumeric(RECORDING_FILE_PREFIX_LEN) + ".mkv";

        try {
            testOutputStream = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            Assertions.fail();
        }

        Assertions.assertTrue(() -> builder.registerCamera(REC_TYPE, SRC_URL));
        Assertions.assertThrows(NullPointerException.class,
                () -> builder.registerAppDataOutputStream(ContainerType.MATROSKA, null));
        Assertions.assertTrue(
                builder.registerAppDataOutputStream(ContainerType.MATROSKA, testOutputStream));
        Assertions.assertFalse(
                builder.registerAppDataOutputStream(ContainerType.MATROSKA, testOutputStream));

        VideoRecorder recorder = builder.construct();

        // Start recording
        Thread recordThread = new Thread(new RunnableRecorder(recorder));
        recordThread.start();
        recorder.toggleAppDataOutputStream(true);

        try {
            TimeUnit.SECONDS.sleep(RECORDING_TIME_IN_SECONDS);
        } catch (InterruptedException e) {
            Assertions.fail();
        }
        recorder.toggleAppDataOutputStream(false);
        recorder.stopRecording();

        // Check stored files
        File f = new File(filePath);
        System.out.println("addAppOsTest_startRecording_writeOStream: " + f.length());
        Assertions.assertTrue(f.length() > 0);
        f.delete();
    }

    @Test
    public void addAppCbTest_restartRecording_invokeCallback() {
        VideoRecorderBuilder builder = new VideoRecorderBuilder((rec, st, desc) -> {
        });

        builder.registerCamera(REC_TYPE, SRC_URL);
        builder.registerAppDataCallback(ContainerType.MATROSKA, (recorder, bBuff) -> {
            addAppCbTestStoreLen += bBuff.limit();
        });
        VideoRecorder recorder = builder.construct();

        // Start recording
        Thread recordThread1 = new Thread(new RunnableRecorder(recorder));
        recordThread1.start();
        recorder.toggleAppDataCallback(true);
        try {
            TimeUnit.SECONDS.sleep(RECORDING_TIME_IN_SECONDS);
        } catch (InterruptedException e) {
            Assertions.fail();
        }
        recorder.toggleAppDataCallback(false);
        recorder.stopRecording();

        while (recorder.getStatus() != RecorderStatus.STOPPED) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Assertions.fail();
            }
        }

        // Restart recording
        Thread recordThread2 = new Thread(new RunnableRecorder(recorder));
        addAppCbTestStoreLen = 0;
        recordThread2.start();
        recorder.toggleAppDataCallback(true);
        try {
            TimeUnit.SECONDS.sleep(RECORDING_TIME_IN_SECONDS);
        } catch (InterruptedException e) {
            Assertions.fail();
        }
        recorder.toggleAppDataCallback(false);
        recorder.stopRecording();
        // Check received length
        System.out.println("addAppCbTest_restartRecording_invokeCallback: " + addAppCbTestStoreLen);
        Assertions.assertTrue(addAppCbTestStoreLen > 0);
    }

    private boolean createTestFolder() {
        File f = new File(FILE_FOLDER);

        return f.exists() || f.mkdir();
    }
}
