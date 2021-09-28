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

import com.aws.greengrass.sitewatch.videorecorder.base.RecorderBranchBase;
import com.aws.greengrass.sitewatch.videorecorder.model.ContainerType;
import com.aws.greengrass.sitewatch.videorecorder.util.Config;
import com.aws.greengrass.sitewatch.videorecorder.util.GstDao;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.elements.AppSink;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class RecorderBranchApp extends RecorderBranchBase {
    private Element muxer;
    private AppSink appSink;
    private boolean isEmitSignalEnabled;
    private GstDao gstCore;
    private Pipeline pipeline;

    RecorderBranchApp(ContainerType type, GstDao dao, Pipeline pipeline) {
        super(Config.APP_PATH_CAPABILITY, dao, pipeline);
        this.gstCore = this.getGstCore();
        this.pipeline = this.getPipeline();

        this.isEmitSignalEnabled = false;
        this.muxer = this.getMuxerFromType(type, false);
        this.appSink = (AppSink) this.gstCore.newElement("appsink");
        this.gstCore.setElement(appSink, "emit-signals", false);
        this.gstCore.setElement(appSink, "sync", false);

        // Link elements
        this.gstCore.addPipelineElements(this.pipeline, this.muxer, this.appSink);
        this.gstCore.linkManyElement(this.muxer, this.appSink);
    }

    public void registerNewSample(AppSink.NEW_SAMPLE listener) {
        this.gstCore.connectAppSink(this.appSink, listener);
    }

    public boolean isEmitEnabled() {
        return this.isEmitSignalEnabled;
    }

    public boolean toggleEmit(boolean toEnable) {
        boolean isChanged = false;

        if (this.isEmitEnabled() != toEnable) {
            this.gstCore.setElement(this.appSink, "emit-signals", toEnable);
            this.isEmitSignalEnabled = toEnable;
            isChanged = true;

            if (!toEnable) {
                this.detach();
                this.gstCore.stopElement(this.appSink);
                this.gstCore.stopElement(this.muxer);
            } else {
                this.gstCore.syncElementParentState(this.muxer);
                this.gstCore.syncElementParentState(this.appSink);
                this.attach();
            }
        } else {
            log.info("App path emit signal is already toggled " + toEnable);
        }

        return isChanged;
    }

    @Override
    public Pad getEntryAudioPad() {
        return this.gstCore.getElementRequestPad(this.muxer, "audio_%u");
    }

    @Override
    public Pad getEntryVideoPad() {
        return this.gstCore.getElementRequestPad(this.muxer, "video_%u");
    }
}
