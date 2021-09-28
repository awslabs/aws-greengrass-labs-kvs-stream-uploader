package com.aws.greengrass.integrationtests.sitewatch.videorecorder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import lombok.Getter;
import lombok.Setter;
import com.aws.greengrass.sitewatch.videorecorder.model.ContainerType;
import com.aws.greengrass.sitewatch.videorecorder.model.RecorderStatus;
import com.aws.greengrass.sitewatch.videorecorder.VideoRecorder;
import com.aws.greengrass.sitewatch.videorecorder.VideoRecorderBuilder;
import com.aws.greengrass.sitewatch.videorecorder.model.CameraType;

public class VideoRecorderToggleAppPathTest {
    private final String RTSP_URL =
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

            this.toggle(true);
        }

        public void run() {
            System.out.println("Start running: " + this.name);
            this.recorder.startRecording();
            System.out.println("Stop running: " + this.name);
        }

        public void stop() {
            this.toggle(false);
            this.recorder.stopRecording();
        }

        public void toggle(boolean toEnable) {
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

    private void resetByteArray(boolean isZero) throws IOException {
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

            b.reset();
        }
    }

    @AfterEach
    public void unSetupTests() throws IOException {
        this.initByteArray(false);
    }

    @Test
    public void newToggleTest_toggleStream_sizeNoneZero() throws InterruptedException, IOException {
        // Create recorders
        System.out.println("Test Init");
        this.initByteArray(true);
        final RecorderTest recorder1 = new RecorderTest(rec1ByteOut1, rec1ByteOut2, "recorder1");
        final RecorderTest recorder2 = new RecorderTest(rec2ByteOut1, rec2ByteOut2, "recorder2");
        Thread recordThread1 = new Thread(() -> recorder1.run());
        Thread recordThread2 = new Thread(() -> recorder2.run());

        // Start recording in their own threads
        System.out.println("Test Start");
        recordThread1.start();
        recordThread2.start();
        TimeUnit.SECONDS.sleep(this.TEST_IDLE_INTERVAL);

        // stop toggle
        System.out.println("stop toggle");
        recorder1.toggle(false);
        recorder2.toggle(false);
        resetByteArray(false);
        TimeUnit.SECONDS.sleep(this.TEST_IDLE_INTERVAL);
        resetByteArray(true);

        // start toggle
        System.out.println("start toggle");
        recorder1.toggle(true);
        recorder2.toggle(true);
        TimeUnit.SECONDS.sleep(this.TEST_IDLE_INTERVAL);

        // stop rec
        System.out.println("stop toggle");
        recorder1.toggle(false);
        recorder2.toggle(false);
        resetByteArray(false);

        // stop test
        System.out.println("stop test");
        recorder1.stop();
        recorder2.stop();
        rec1ByteOut1.close();
        rec1ByteOut2.close();
        rec2ByteOut1.close();
        rec2ByteOut2.close();
        this.initByteArray(false);
        System.out.println("Bye");
    }
}
