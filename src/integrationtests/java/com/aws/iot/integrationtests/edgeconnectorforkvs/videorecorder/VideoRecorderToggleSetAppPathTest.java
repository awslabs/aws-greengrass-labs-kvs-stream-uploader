package com.aws.iot.integrationtests.edgeconnectorforkvs.videorecorder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import lombok.Getter;
import lombok.Setter;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.ContainerType;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.RecorderStatus;
import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.VideoRecorderBuilder;
import com.aws.iot.edgeconnectorforkvs.videorecorder.model.CameraType;

public class VideoRecorderToggleSetAppPathTest {
    private final String RTSP_URL =
            "rtspt://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov";
    private final String FILE_FOLDER = "/tmp/recorder_ut";
    private ByteArrayOutputStream rec1ByteOut1;
    private ByteArrayOutputStream rec1ByteOut2;
    private ByteArrayOutputStream rec2ByteOut1;
    private ByteArrayOutputStream rec2ByteOut2;
    private final int TEST_IDLE_INTERVAL = 5;
    private final int RECORDING_FILE_PREFIX_LEN = 8;

    @Getter
    @Setter
    private class RecorderTest {
        private VideoRecorder recorder;
        private String name;
        private ByteArrayOutputStream osCb;
        private ByteArrayOutputStream osOs;

        public RecorderTest(ByteArrayOutputStream cbOut, ByteArrayOutputStream osOut, String name) {
            final String filePrefix =
                    RandomStringUtils.randomAlphanumeric(RECORDING_FILE_PREFIX_LEN);
            final String filePathPrefix = FILE_FOLDER + "/" + filePrefix;

            this.name = name;
            this.osCb = cbOut;
            this.osOs = osOut;

            VideoRecorderBuilder builder = new VideoRecorderBuilder((rec, status, description) -> {
                System.out.println(name + "status changed: " + status + ", " + description);
                Assertions.assertNotEquals(RecorderStatus.FAILED, status);
            });

            builder.registerCamera(CameraType.RTSP, RTSP_URL);
            builder.registerFileSink(ContainerType.MATROSKA, filePathPrefix);
            builder.registerAppDataCallback(ContainerType.MATROSKA, (rec, bBuff) -> {
                byte[] array = new byte[bBuff.remaining()];
                bBuff.get(array);
                try {
                    this.osCb.write(array);
                } catch (IOException e) {
                    Assertions.fail();
                }
            });
            builder.registerAppDataOutputStream(ContainerType.MATROSKA,
                    new ByteArrayOutputStream());

            this.recorder = builder.construct();
        }

        public void run() {
            System.out.println("Start running: " + this.name);
            this.recorder.startRecording();
            System.out.println("Stop running: " + this.name);
        }

        public void stop() {
            this.recorder.stopRecording();
            this.recorder.toggleAppDataCallback(false);
            this.recorder.toggleAppDataOutputStream(false);
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

        public void setToggle(ByteArrayOutputStream byteCb, ByteArrayOutputStream byteOs) {
            this.osCb = byteCb;
            this.osOs = byteOs;
            this.recorder.setAppDataOutputStream(this.osOs);
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

    private boolean createTestFolder() {
        File f = new File(FILE_FOLDER);

        return f.exists() || f.mkdir();
    }

    @BeforeEach
    public void setupTests() {
        Assertions.assertTrue(createTestFolder());
    }

    @AfterEach
    public void unSetupTests() throws IOException {
        this.initByteArray(false);

        // Clean files
        File dir = new File(FILE_FOLDER);
        FileFilter fileFilter = new WildcardFileFilter("*.mkv");
        File[] files = dir.listFiles(fileFilter);

        for (int i = 0; i < files.length; ++i) {
            files[i].delete();
        }
    }

    @Test
    public void setToggleTest_toggleStream_sizeNoneZero() throws InterruptedException, IOException {
        System.out.println("Test Init");
        this.initByteArray(true);
        RecorderTest recorder1 =
                new RecorderTest(this.rec1ByteOut1, this.rec1ByteOut2, "recorder1");
        RecorderTest recorder2 =
                new RecorderTest(this.rec2ByteOut1, this.rec2ByteOut2, "recorder2");
        Thread recordThread1 = new Thread(() -> recorder1.run());
        Thread recordThread2 = new Thread(() -> recorder2.run());
        recorder1.setToggle(this.rec1ByteOut1, this.rec1ByteOut2);
        recorder2.setToggle(this.rec2ByteOut1, this.rec2ByteOut2);
        recorder1.toggle(true);
        recorder2.toggle(true);

        // Start recording in their own threads
        System.out.println("Test Start");
        recordThread1.start();
        recordThread2.start();
        TimeUnit.SECONDS.sleep(this.TEST_IDLE_INTERVAL);

        // stop toggle
        System.out.println("stop toggle");
        recorder1.toggle(false);
        recorder2.toggle(false);
        checkByteArray(false);
        this.initByteArray(false);
        this.initByteArray(true);
        recorder1.setToggle(this.rec1ByteOut1, this.rec1ByteOut2);
        recorder2.setToggle(this.rec2ByteOut1, this.rec2ByteOut2);
        TimeUnit.SECONDS.sleep(this.TEST_IDLE_INTERVAL);
        checkByteArray(true);
        this.initByteArray(false);

        // start toggle
        System.out.println("start toggle");
        this.initByteArray(true);
        recorder1.setToggle(this.rec1ByteOut1, this.rec1ByteOut2);
        recorder2.setToggle(this.rec2ByteOut1, this.rec2ByteOut2);
        recorder1.toggle(true);
        recorder2.toggle(true);
        TimeUnit.SECONDS.sleep(this.TEST_IDLE_INTERVAL);

        // stop rec
        System.out.println("stop rec");
        recorder1.toggle(false);
        recorder2.toggle(false);
        checkByteArray(false);
        this.initByteArray(false);
        recorder1.stop();
        recorder2.stop();
        System.out.println("Bye");
    }
}
