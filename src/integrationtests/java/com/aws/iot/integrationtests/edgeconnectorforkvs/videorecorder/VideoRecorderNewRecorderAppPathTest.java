package com.aws.iot.integrationtests.edgeconnectorforkvs.videorecorder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import lombok.Getter;
import lombok.Setter;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.ContainerType;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.RecorderStatus;
import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorderBuilder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.CameraType;

public class VideoRecorderNewRecorderAppPathTest {
    static final String RTSP_URL =
            "rtspt://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov";
    private ByteArrayOutputStream rec1ByteOut1;
    private ByteArrayOutputStream rec1ByteOut2;
    private ByteArrayOutputStream rec2ByteOut1;
    private ByteArrayOutputStream rec2ByteOut2;
    private final int TEST_IDLE_INTERVAL = 5;

    @Getter
    @Setter
    private class RecorderTest {
        private VideoRecorder recorder;
        private String name;
        private ByteArrayOutputStream osCb;
        private ByteArrayOutputStream osOs;

        public RecorderTest(ByteArrayOutputStream cbOut, ByteArrayOutputStream osOut, String name) {
            this.name = name;
            this.osCb = cbOut;
            this.osOs = osOut;

            VideoRecorderBuilder builder = new VideoRecorderBuilder((rec, status, description) -> {
                System.out.println(name + "status changed: " + status + ", " + description);
                Assertions.assertNotEquals(RecorderStatus.FAILED, status);
            });

            builder.registerCamera(CameraType.RTSP, RTSP_URL);
            builder.registerAppDataCallback(ContainerType.MATROSKA, (rec, bBuff) -> {
                byte[] array = new byte[bBuff.remaining()];
                bBuff.get(array);
                try {
                    this.osCb.write(array);
                } catch (IOException e) {
                    Assertions.fail();
                }
            });
            builder.registerAppDataOutputStream(ContainerType.MATROSKA, this.osOs);

            this.recorder = builder.construct();
        }

        public void run() {
            System.out.println("Start running: " + this.name);
            this.recorder.startRecording();
            System.out.println("Stop running: " + this.name);
        }

        public void stop() throws IOException {
            this.toggle(false);
            this.recorder.stopRecording();
        }

        public void toggle(boolean toEnable) throws IOException {
            if (toEnable) {
                this.recorder.toggleAppDataCallback(true);
                this.recorder.toggleAppDataOutputStream(true);
            } else {
                this.recorder.toggleAppDataCallback(false);
                this.recorder.toggleAppDataOutputStream(false);
            }
        }
    }

    private void initByteArray(boolean toInit) throws IOException {
        if (toInit) {
            this.rec1ByteOut1 = new ByteArrayOutputStream();
            this.rec1ByteOut2 = new ByteArrayOutputStream();
            this.rec2ByteOut1 = new ByteArrayOutputStream();
            this.rec2ByteOut2 = new ByteArrayOutputStream();
        } else {
            if (this.rec1ByteOut1 != null) {
                this.rec1ByteOut1.close();
                this.rec1ByteOut1 = null;
            }
            if (this.rec1ByteOut2 != null) {
                this.rec1ByteOut2.close();
                this.rec1ByteOut2 = null;
            }
            if (this.rec2ByteOut1 != null) {
                this.rec2ByteOut1.close();
                this.rec2ByteOut1 = null;
            }
            if (this.rec2ByteOut2 != null) {
                this.rec2ByteOut2.close();
                this.rec2ByteOut2 = null;
            }
        }
    }

    private void checkByteArray(boolean isZero) throws IOException {
        ByteArrayOutputStream byteArray[] = new ByteArrayOutputStream[] {this.rec1ByteOut1,
                this.rec1ByteOut2, this.rec2ByteOut1, this.rec2ByteOut2};

        for (ByteArrayOutputStream b : byteArray) {
            long len = b.toByteArray().length;

            System.out.println("len = " + len);

            if (isZero) {
                Assertions.assertEquals(0, len);
            } else {
                Assertions.assertTrue(len > 0);
            }
        }
    }

    @AfterEach
    public void unSetupTests() throws IOException {
        this.initByteArray(false);
    }

    @Test
    public void newRecTest_newRecorder_sizeNoneZero() throws InterruptedException, IOException {
        // Start recording in their own threads
        System.out.println("Test Start");
        this.initByteArray(true);
        final RecorderTest recorder1 =
                new RecorderTest(this.rec1ByteOut1, this.rec1ByteOut2, "recorder1");
        final RecorderTest recorder2 =
                new RecorderTest(this.rec2ByteOut1, this.rec2ByteOut2, "recorder2");
        Thread recordThread1 = new Thread(() -> recorder1.run());
        Thread recordThread2 = new Thread(() -> recorder2.run());
        recorder1.toggle(true);
        recorder2.toggle(true);
        recordThread1.start();
        recordThread2.start();
        TimeUnit.SECONDS.sleep(this.TEST_IDLE_INTERVAL);
        recorder1.stop();
        recorder2.stop();
        checkByteArray(false);
        this.initByteArray(false);

        // stop toggle
        System.out.println("Re-toggle stop");
        this.initByteArray(true);
        final RecorderTest recorder3 =
                new RecorderTest(this.rec1ByteOut1, this.rec1ByteOut2, "recorder3");
        final RecorderTest recorder4 =
                new RecorderTest(this.rec2ByteOut1, this.rec2ByteOut2, "recorder4");
        Thread recordThread3 = new Thread(() -> recorder3.run());
        Thread recordThread4 = new Thread(() -> recorder4.run());
        recordThread3.start();
        recordThread4.start();
        TimeUnit.SECONDS.sleep(this.TEST_IDLE_INTERVAL);
        recorder3.stop();
        recorder4.stop();
        checkByteArray(true);
        this.initByteArray(false);

        // start toggle
        System.out.println("Re-toggle start");
        this.initByteArray(true);
        final RecorderTest recorder5 =
                new RecorderTest(this.rec1ByteOut1, this.rec1ByteOut2, "recorder5");
        final RecorderTest recorder6 =
                new RecorderTest(this.rec2ByteOut1, this.rec2ByteOut2, "recorder6");
        Thread recordThread5 = new Thread(() -> recorder5.run());
        Thread recordThread6 = new Thread(() -> recorder6.run());
        recorder5.toggle(true);
        recorder6.toggle(true);
        recordThread5.start();
        recordThread6.start();
        TimeUnit.SECONDS.sleep(this.TEST_IDLE_INTERVAL);
        recorder5.stop();
        recorder6.stop();
        checkByteArray(false);
        this.initByteArray(false);
        System.out.println("Test Bye");
    }
}
