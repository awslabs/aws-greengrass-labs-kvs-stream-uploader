package com.aws.iot.integrationtests.edgeconnectorforkvs.videorecorder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.TimeUnit;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.ContainerType;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.RecorderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorderBuilder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.CameraType;

public class VideoRecorderPipeConsumerTest {
    private final String RTSP_URL =
            "rtspt://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov";
    private final int TEST_IDLE_INTERVAL = 8;
    private PipedOutputStream pipedOs;
    private PipedInputStream pipedIs;
    private ByteArrayOutputStream byteOut;
    private boolean isConsumerRunning;

    private void closeStreams() throws IOException {
        if (this.pipedOs != null) {
            pipedOs.close();
        }
        if (this.pipedIs != null) {
            pipedIs.close();
        }
        if (this.byteOut != null) {
            byteOut.close();
        }

        this.pipedOs = null;
        this.pipedIs = null;
        this.byteOut = null;
    }

    private void initPipedStream() throws IOException {
        this.pipedOs = new PipedOutputStream();
        this.pipedIs = new PipedInputStream(this.pipedOs, 102400);
        this.byteOut = new ByteArrayOutputStream();
    }

    @AfterEach
    public void unSetupTests() throws IOException {
        this.closeStreams();
    }

    private void consumerRun() {
        System.out.println("consumerStart");
        while (isConsumerRunning) {
            try {
                int len = pipedIs.available();
                byte[] array = new byte[len];
                pipedIs.read(array, 0, len);
                byteOut.write(array);
            } catch (IOException e) {
                Assertions.fail();
            }
        }
        System.out.println("consumerStop");
    }

    @Test
    public void producerConsumerTest_loop_fileStore() throws InterruptedException, IOException {
        VideoRecorderBuilder builder = new VideoRecorderBuilder((recorder, status, description) -> {
            System.out.println("Status changed: " + status + ", description: " + description);
            Assertions.assertNotEquals(RecorderStatus.FAILED, status);
        });
        builder.registerCamera(CameraType.RTSP, RTSP_URL);
        builder.registerAppDataCallback(ContainerType.MATROSKA, (recorder, bBuff) -> {
        });
        builder.registerAppDataOutputStream(ContainerType.MATROSKA, new ByteArrayOutputStream());

        final VideoRecorder recorder = builder.construct();
        new Thread(() -> recorder.startRecording()).start();

        isConsumerRunning = false;

        for (int i = 0; i < 3; ++i) {
            initPipedStream();

            System.out.println(String.format("Toggle %d ", i));

            isConsumerRunning = true;
            new Thread(() -> consumerRun()).start();
            recorder.setAppDataOutputStream(pipedOs);
            recorder.toggleAppDataOutputStream(true);
            TimeUnit.SECONDS.sleep(this.TEST_IDLE_INTERVAL);

            recorder.toggleAppDataOutputStream(false);
            isConsumerRunning = false;
            TimeUnit.SECONDS.sleep(1);

            int len = byteOut.toByteArray().length;
            System.out.println("consumer len = " + len);
            Assertions.assertTrue(len > 0);

            closeStreams();
        }

        recorder.stopRecording();
    }
}
