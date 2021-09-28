package com.aws.greengrass.sitewatch.videorecorder.callback;

import com.aws.greengrass.sitewatch.videorecorder.base.VideoRecorderBase;
import com.aws.greengrass.sitewatch.videorecorder.model.RecorderStatus;

/**
 * Callback for receiving status changes.
 */
public interface StatusCallback {

    /**
     * Callback for receiving status changes.
     *
     * @param recorder current recorder instance
     * @param status current recording or uploading statuses of the recorder
     * @param description detailed descriptions
     */
    void notifyStatus(VideoRecorderBase recorder, RecorderStatus status, String description);
}
