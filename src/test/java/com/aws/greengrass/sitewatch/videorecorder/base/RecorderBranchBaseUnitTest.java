package com.aws.greengrass.sitewatch.videorecorder.base;

import static org.mockito.Mockito.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import static org.mockito.BDDMockito.*;
import com.aws.greengrass.sitewatch.videorecorder.model.ContainerType;
import com.aws.greengrass.sitewatch.videorecorder.model.RecorderCapability;
import com.aws.greengrass.sitewatch.videorecorder.util.GstDao;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.PadProbeInfo;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Pad.PROBE;
import org.freedesktop.gstreamer.event.EOSEvent;
import org.freedesktop.gstreamer.event.FlushStopEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecorderBranchBaseUnitTest {
    @Mock
    private GstDao mockGst;
    @Mock
    private Pipeline mockPipeline;
    @Mock
    private Element mockMuxer;
    @Mock
    private Pad mockAudioPad;
    @Mock
    private Pad mockVideoPad;
    @Mock
    private Element mockTee;
    @Mock
    private Element mockTeeDup;
    @Mock
    private Pad mockTeePad;
    @Mock
    private Pad mockQuePad;
    @Mock
    private PadProbeInfo mockProbeInfo;
    @Mock
    private EOSEvent mockEosEvent;
    @Mock
    private FlushStopEvent mockFlushEvent;

    private class RecorderBranchTest extends RecorderBranchBase {
        public RecorderBranchTest(RecorderCapability cap) {
            super(cap, mockGst, mockPipeline);
        }

        @Override
        public Pad getEntryAudioPad() {
            return mockAudioPad;
        }

        @Override
        public Pad getEntryVideoPad() {
            return mockVideoPad;
        }

        public GstDao getGstDao() {
            return this.getGstCore();
        }

        public Pipeline getPipe() {
            return this.getPipeline();
        }

        public Element getMuxer(ContainerType type, boolean isFilePath) {
            return this.getMuxerFromType(type, isFilePath);
        }

        public String getExtension(ContainerType type) {
            return this.getFileExtensionFromType(type);
        }

        public PROBE getProbe() {
            return this.getTeeBlockProbe();
        }

        public void detachPub() {
            this.detach();
        }

        public void attachPub() {
            this.attach();
        }
    }

    @Test
    public void getMembersTest_invokeGetter_nonNullValue() {
        RecorderCapability cap = RecorderCapability.VIDEO_AUDIO;
        RecorderBranchTest testBranch = new RecorderBranchTest(cap);

        Assertions.assertEquals(cap, testBranch.getCapability());
        Assertions.assertNotNull(testBranch.getGstDao());
        Assertions.assertNotNull(testBranch.getPipe());
    }

    @Test
    public void getMuxerTest_getMuxerByType_nonNullValue() {
        willReturn(this.mockMuxer).given(this.mockGst).newElement(anyString());

        RecorderCapability cap = RecorderCapability.VIDEO_AUDIO;
        ContainerType type = ContainerType.MATROSKA;
        RecorderBranchTest testBranch = new RecorderBranchTest(cap);

        Assertions.assertNotNull(testBranch.getMuxer(type, true));
        Assertions.assertNotNull(testBranch.getMuxer(type, false));
    }

    @Test
    public void getMuxerTest_getMuxerByInvalidType_throwException() {
        RecorderCapability cap = RecorderCapability.VIDEO_AUDIO;
        ContainerType type = ContainerType.UNSUPPORTED;
        RecorderBranchTest testBranch = new RecorderBranchTest(cap);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> testBranch.getMuxer(type, true));
    }

    @Test
    public void getExtensionTest_getExtensionByType_mkv() {
        RecorderCapability cap = RecorderCapability.VIDEO_AUDIO;
        ContainerType type = ContainerType.MATROSKA;
        RecorderBranchTest testBranch = new RecorderBranchTest(cap);

        Assertions.assertEquals("mkv", testBranch.getExtension(type));
    }

    @Test
    public void getExtensionTest_getExtensionByInvalidType_throwException() {
        RecorderCapability cap = RecorderCapability.VIDEO_AUDIO;
        ContainerType type = ContainerType.UNSUPPORTED;
        RecorderBranchTest testBranch = new RecorderBranchTest(cap);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> testBranch.getExtension(type));
    }

    @Test
    public void bindBranchTest_bindInvalidParameter_throwException() {
        RecorderCapability cap = RecorderCapability.VIDEO_ONLY;
        RecorderBranchTest testBranch = new RecorderBranchTest(cap);

        // Invalid capability
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> testBranch.bindPath(mockTee, RecorderCapability.VIDEO_AUDIO));

        // Null parameter
        Assertions.assertThrows(NullPointerException.class, () -> testBranch.bindPath(null, null));
        Assertions.assertThrows(NullPointerException.class,
                () -> testBranch.bindPath(mockTee, null));
        Assertions.assertThrows(NullPointerException.class, () -> testBranch.bindPath(null, cap));
    }

    @Test
    public void bindBranchTest_bindValidCapability_noException() {
        RecorderBranchTest testBranchFull = new RecorderBranchTest(RecorderCapability.VIDEO_AUDIO);
        RecorderBranchTest testBranchVideo = new RecorderBranchTest(RecorderCapability.VIDEO_ONLY);
        RecorderBranchTest testBranchAudio = new RecorderBranchTest(RecorderCapability.AUDIO_ONLY);

        Assertions.assertDoesNotThrow(
                () -> testBranchFull.bindPath(mockTee, RecorderCapability.VIDEO_ONLY));
        Assertions.assertDoesNotThrow(
                () -> testBranchFull.bindPath(mockTee, RecorderCapability.AUDIO_ONLY));
        Assertions.assertDoesNotThrow(
                () -> testBranchVideo.bindPath(mockTee, RecorderCapability.VIDEO_ONLY));
        Assertions.assertDoesNotThrow(
                () -> testBranchVideo.bindPath(mockTee, RecorderCapability.AUDIO_ONLY));
        Assertions.assertDoesNotThrow(
                () -> testBranchAudio.bindPath(mockTee, RecorderCapability.VIDEO_ONLY));
        Assertions.assertDoesNotThrow(
                () -> testBranchAudio.bindPath(mockTee, RecorderCapability.AUDIO_ONLY));
    }

    @Test
    public void reattach_reattach_noException() {
        willReturn(mockQuePad).given(mockGst).getElementStaticPad(any(), anyString());

        RecorderBranchTest testBranch = new RecorderBranchTest(RecorderCapability.VIDEO_AUDIO);

        testBranch.bindPath(mockTee, RecorderCapability.VIDEO_ONLY);
        testBranch.bindPath(mockTeeDup, RecorderCapability.AUDIO_ONLY);

        Runnable padIdle = () -> {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                Assertions.fail();
            }
            testBranch.getProbe().probeCallback(mockTeePad, mockProbeInfo);
            testBranch.getProbe().probeCallback(mockTeePad, mockProbeInfo);
        };
        willReturn(false).given(mockGst).isPadLinked(any());
        new Thread(padIdle).start();
        testBranch.detachPub();
        testBranch.attachPub();
        willReturn(true).given(mockGst).isPadLinked(any());
        new Thread(padIdle).start();
        testBranch.detachPub();
        testBranch.attachPub();
    }

    @Test
    public void reattach_reattach_awaitException() {
        try (MockedConstruction<ReentrantLock> mockCondLock =
                mockConstruction(ReentrantLock.class)) {
            willReturn(mockQuePad).given(mockGst).getElementStaticPad(any(), anyString());

            RecorderBranchTest testBranch = new RecorderBranchTest(RecorderCapability.VIDEO_AUDIO);

            testBranch.bindPath(mockTee, RecorderCapability.VIDEO_ONLY);

            // Use NullPointerException instead of InterruptException because of JUnit
            // The exception of Condition.await is handle in recorder
            Assertions.assertDoesNotThrow(() -> testBranch.detachPub());
        }
    }
}
