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

package com.aws.greengrass.sitewatch.videorecorder.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.aws.greengrass.sitewatch.videorecorder.model.ContainerType;
import com.aws.greengrass.sitewatch.videorecorder.model.RecorderCapability;
import com.aws.greengrass.sitewatch.videorecorder.util.ConfigMuxer;
import com.aws.greengrass.sitewatch.videorecorder.util.MuxerProperty;
import com.aws.greengrass.sitewatch.videorecorder.util.GstDao;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.PadProbeReturn;
import org.freedesktop.gstreamer.PadProbeType;
import org.freedesktop.gstreamer.Pipeline;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Recorder pipeline branch base class.
 */
@Slf4j
public abstract class RecorderBranchBase {
    @Getter
    private RecorderCapability capability;
    @Getter(AccessLevel.PROTECTED)
    private GstDao gstCore;
    @Getter(AccessLevel.PROTECTED)
    private Pipeline pipeline;
    private HashMap<Element, Element> teeSrc2Que;
    private HashMap<Pad, Element> teeSrcPad2Tee;
    @Getter(AccessLevel.PROTECTED)
    private Pad.PROBE teeBlockProbe;
    private Lock condLock;
    private Condition padProbeUnlink;
    private AtomicInteger detachCnt;

    /**
     * Create and get the entry pad of audio path.
     *
     * @return audio entry pad
     */
    public abstract Pad getEntryAudioPad();

    /**
     * Create and get the entry pad of video path.
     *
     * @return video entry pad
     */
    public abstract Pad getEntryVideoPad();

    /**
     * Constructor for RecorderBranchBase.
     *
     * @param cap branch capability
     * @param dao GStreamer data access object
     * @param pipeline GStreamer pipeline
     */
    public RecorderBranchBase(RecorderCapability cap, GstDao dao, Pipeline pipeline) {
        this.capability = cap;
        this.gstCore = dao;
        this.pipeline = pipeline;
        this.teeSrc2Que = new HashMap<>();
        this.teeSrcPad2Tee = new HashMap<>();
        this.condLock = new ReentrantLock();
        this.padProbeUnlink = this.condLock.newCondition();
        this.detachCnt = new AtomicInteger(0);

        this.teeBlockProbe = (teePadSrc, info) -> {
            Pad quePadSink = this.gstCore.getPadPeer(teePadSrc);

            if (this.gstCore.isPadLinked(teePadSrc)) {
                this.gstCore.unlinkPad(teePadSrc, quePadSink);
                log.info("queue is detached");
            }

            this.gstCore.sendPadEvent(quePadSink, this.gstCore.newEosEvent());

            if (this.detachCnt.incrementAndGet() == this.teeSrc2Que.size()) {
                this.condLock.lock();
                try {
                    this.padProbeUnlink.signal();
                } finally {
                    this.condLock.unlock();
                }
            }

            return PadProbeReturn.REMOVE;
        };
    }

    /**
     * Link a given Pad to this branch.
     *
     * @param recorderElmSrc an element of recorder is going to link to this branch
     * @param capsToBind selection for linking video or audio pad of this branch
     */
    public void bindPath(@NonNull Element recorderElmSrc, @NonNull RecorderCapability capsToBind)
            throws IllegalArgumentException {
        Pad entryPadSink = null;

        // Only a video or a audio pad can be bound at each request
        switch (capsToBind) {
            case AUDIO_ONLY:
                if (this.capability == RecorderCapability.AUDIO_ONLY
                        || this.capability == RecorderCapability.VIDEO_AUDIO) {
                    entryPadSink = this.getEntryAudioPad();
                } else {
                    log.warn("Not supported capability to bind branch: " + capsToBind);
                }
                break;

            case VIDEO_ONLY:
                if (this.capability == RecorderCapability.VIDEO_ONLY
                        || this.capability == RecorderCapability.VIDEO_AUDIO) {
                    entryPadSink = this.getEntryVideoPad();
                } else {
                    log.warn("Not supported capability to bind branch: " + capsToBind);
                }
                break;

            default:
                log.error("Invalid capability to bind branch: " + capsToBind);
                throw new IllegalArgumentException(
                        "Invalid capability to bind branch: " + capsToBind);
        }

        if (entryPadSink != null) {
            Pad recorderSrcPad = this.gstCore.getElementRequestPad(recorderElmSrc, "src_%u");
            Element queueElm = this.gstCore.newElement("queue");
            Pad quePadSrc = this.gstCore.getElementStaticPad(queueElm, "src");
            Pad quePadSink = this.gstCore.getElementStaticPad(queueElm, "sink");

            this.gstCore.setElement(queueElm, "flush-on-eos", true);
            this.gstCore.setElement(queueElm, "leaky", 2);

            // Link elements
            this.gstCore.addPipelineElements(this.pipeline, queueElm);
            this.gstCore.linkPad(recorderSrcPad, quePadSink);
            this.gstCore.linkPad(quePadSrc, entryPadSink);

            // Add to hash map
            this.teeSrc2Que.put(recorderElmSrc, queueElm);
            this.teeSrcPad2Tee.put(recorderSrcPad, recorderElmSrc);

            this.gstCore.syncElementParentState(queueElm);
        }
    }

    protected void detach() {
        this.detachCnt.set(0);

        for (Map.Entry<Element, Element> queue : this.teeSrc2Que.entrySet()) {
            Element queueElm = queue.getValue();
            Pad quePadSink = this.gstCore.getElementStaticPad(queueElm, "sink");
            Pad teePadSrc = this.gstCore.getPadPeer(quePadSink);
            this.gstCore.addPadProbe(teePadSrc, PadProbeType.IDLE, this.teeBlockProbe);
        }

        log.info("waiting for queues detaching");
        this.condLock.lock();
        try {
            while (this.detachCnt.get() != this.teeSrc2Que.size()) {
                this.padProbeUnlink.await();
            }
        } catch (Exception e) {
            log.error(String.format("detach fails: %s", e.getMessage()));
        } finally {
            this.condLock.unlock();
        }
        log.info("all queues are detached");

        // release tee pads
        for (Map.Entry<Pad, Element> pads : this.teeSrcPad2Tee.entrySet()) {
            Element teeSrc = pads.getValue();
            Pad teePadSrc = pads.getKey();
            this.gstCore.relElementRequestPad(teeSrc, teePadSrc);
        }
        this.teeSrcPad2Tee.clear();
    }

    protected void attach() {
        for (Map.Entry<Element, Element> queue : this.teeSrc2Que.entrySet()) {
            Element que = queue.getValue();
            Element recorderElmSrc = queue.getKey();

            if (!this.gstCore.isPadLinked(this.gstCore.getElementStaticPad(que, "sink"))) {
                Pad newSrcPad = this.gstCore.getElementRequestPad(recorderElmSrc, "src_%u");

                this.teeSrcPad2Tee.put(newSrcPad, recorderElmSrc);

                this.gstCore.syncElementParentState(que);
                this.gstCore.linkPad(newSrcPad, this.gstCore.getElementStaticPad(que, "sink"));
                log.info("teePadSrc is linked with que");
            } else {
                log.warn("tee and que are already linked");
            }
        }
    }

    /**
     * Helper function to create a new muxer by the given type.
     *
     * @param type container type
     * @param isFilePath selection muxer properties for file path or app path
     * @return a muxer element
     * @throws IllegalArgumentException if type is not supported
     */
    protected Element getMuxerFromType(ContainerType type, boolean isFilePath)
            throws IllegalArgumentException {

        Element muxer = null;

        if (ConfigMuxer.CONTAINER_INFO.containsKey(type)) {
            MuxerProperty conf = ConfigMuxer.CONTAINER_INFO.get(type);
            ArrayList<HashMap<String, Object>> propList = new ArrayList<>();

            muxer = this.gstCore.newElement(conf.getGstElmName());

            propList.add(conf.getGeneralProp());
            if (isFilePath) {
                propList.add(conf.getFilePathProp());
            } else {
                propList.add(conf.getAppPathProp());
            }

            for (HashMap<String, Object> properties : propList) {
                for (Map.Entry<String, Object> property : properties.entrySet()) {
                    this.gstCore.setElement(muxer, property.getKey(), property.getValue());
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported muxer container type: " + type);
        }

        return muxer;
    }

    /**
     * Helper function to extension name by the given type.
     *
     * @param type container type
     * @return file extension name
     * @throws IllegalArgumentException if type is not supported
     */
    protected String getFileExtensionFromType(ContainerType type) throws IllegalArgumentException {
        if (ConfigMuxer.CONTAINER_INFO.containsKey(type)) {
            return ConfigMuxer.CONTAINER_INFO.get(type).getFileExt();
        } else {
            throw new IllegalArgumentException("Unsupported extension container type: " + type);
        }
    }
}
