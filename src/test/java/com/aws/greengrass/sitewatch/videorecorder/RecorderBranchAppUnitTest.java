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

package com.aws.greengrass.sitewatch.videorecorder;

import com.aws.greengrass.sitewatch.videorecorder.model.ContainerType;
import com.aws.greengrass.sitewatch.videorecorder.util.GstDao;
import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.elements.AppSink.NEW_SAMPLE;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecorderBranchAppUnitTest {
    @Mock
    private GstDao mockGst;
    @Mock
    private Pipeline mockPipeline;

    @Test
    void createBranchAppTest_toggleEmitSignal_noException() {
        RecorderBranchApp branch =
                new RecorderBranchApp(ContainerType.MATROSKA, this.mockGst, this.mockPipeline);
        NEW_SAMPLE listener = sink -> {
            return FlowReturn.OK;
        };

        branch.registerNewSample(listener);
        Assertions.assertFalse(branch.isEmitEnabled());

        // Already disabled
        Assertions.assertFalse(branch.toggleEmit(false));
        Assertions.assertFalse(branch.isEmitEnabled());

        // From disabled to enabled
        Assertions.assertTrue(branch.toggleEmit(true));
        Assertions.assertTrue(branch.isEmitEnabled());

        // Already enabled
        Assertions.assertFalse(branch.toggleEmit(true));
        Assertions.assertTrue(branch.isEmitEnabled());

        // From enabled to disabled
        Assertions.assertTrue(branch.toggleEmit(false));
        Assertions.assertFalse(branch.isEmitEnabled());
    }

    @Test
    void getPadTest_invokeMethod_noException() {
        RecorderBranchApp branch =
                new RecorderBranchApp(ContainerType.MATROSKA, this.mockGst, this.mockPipeline);

        Assertions.assertDoesNotThrow(() -> branch.getEntryAudioPad());
        Assertions.assertDoesNotThrow(() -> branch.getEntryVideoPad());
    }
}
