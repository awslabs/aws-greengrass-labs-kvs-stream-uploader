package com.aws.greengrass.sitewatch.videorecorder.base;

import com.aws.greengrass.sitewatch.videorecorder.base.RecorderCameraBase.CapabilityListener;
import com.aws.greengrass.sitewatch.videorecorder.base.RecorderCameraBase.ErrorListener;
import com.aws.greengrass.sitewatch.videorecorder.base.RecorderCameraBase.NewPadListener;
import com.aws.greengrass.sitewatch.videorecorder.util.GstDao;
import org.freedesktop.gstreamer.Pipeline;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecorderCameraBaseUnitTest {
    @Mock
    private GstDao mockGst;
    @Mock
    private Pipeline mockPipeline;

    private CapabilityListener capListener;
    private NewPadListener padListener;
    private ErrorListener errListener;

    private class RecorderCameraTest extends RecorderCameraBase {
        public RecorderCameraTest() {
            super(mockGst, mockPipeline);
        }

        @Override
        public void setProperty(String property, Object val) {

        }
    }

    @BeforeEach
    void setupTest() {
        this.capListener = (audioCnt, videoCnt) -> {
        };

        this.padListener = (cap, newPad) -> {
        };

        this.errListener = desc -> {
        };
    }

    @Test
    void registerListener_nullListener_throwException() {
        RecorderCameraTest camera = new RecorderCameraTest();

        Assertions.assertThrows(NullPointerException.class,
                () -> camera.registerListener(null, null, null));
        Assertions.assertThrows(NullPointerException.class,
                () -> camera.registerListener(this.capListener, null, null));
        Assertions.assertThrows(NullPointerException.class,
                () -> camera.registerListener(null, this.padListener, null));
        Assertions.assertThrows(NullPointerException.class,
                () -> camera.registerListener(this.capListener, this.padListener, null));
        Assertions.assertThrows(NullPointerException.class,
                () -> camera.registerListener(null, null, this.errListener));
        Assertions.assertThrows(NullPointerException.class,
                () -> camera.registerListener(this.capListener, null, this.errListener));
        Assertions.assertThrows(NullPointerException.class,
                () -> camera.registerListener(null, this.padListener, this.errListener));
    }
}
