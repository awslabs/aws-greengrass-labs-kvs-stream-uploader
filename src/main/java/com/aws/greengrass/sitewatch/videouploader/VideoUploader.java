/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.aws.greengrass.sitewatch.videouploader;

import lombok.NonNull;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Date;

/**
 * A interface for operating video uploading.
 */
public interface VideoUploader extends Closeable {

    /**
     * Upload all videos that its date is between start time and end time.
     *
     * @param videoUploadingStartTime Video upload start time
     * @param videoUploadingEndTime   Video upload end time
     * @param statusChangedCallBack   A callback for updating status
     * @param uploadCallBack          A callback for task completes or fails
     * @throws IllegalArgumentException The value for this input parameter is invalid
     */
    void uploadHistoricalVideo(@NonNull Date videoUploadingStartTime, @NonNull Date videoUploadingEndTime,
                               Runnable statusChangedCallBack, Runnable uploadCallBack);


    /**
     * Upload a video from {@link InputStream}.
     *
     * @param inputStream             The input stream
     * @param videoUploadingStartTime The start time of the given input stream
     * @param statusChangedCallBack   A callback for updating status
     * @param uploadCallBack          A callback for task completes or fails
     */
    void uploadStream(@NonNull InputStream inputStream, Date videoUploadingStartTime, Runnable statusChangedCallBack,
                      Runnable uploadCallBack);

    /**
     * Closes current task and releases all resources.
     */
    @Override
    void close();
}
