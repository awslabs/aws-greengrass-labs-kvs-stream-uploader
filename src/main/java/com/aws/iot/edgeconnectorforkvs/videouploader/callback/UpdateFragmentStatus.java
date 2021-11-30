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

package com.aws.iot.edgeconnectorforkvs.videouploader.callback;

import com.aws.iot.edgeconnectorforkvs.videouploader.model.VideoFile;

/**
 * This is a functional interface for callback whenever there is any updated KVS fragment or uploaded file.
 */
@FunctionalInterface
public interface UpdateFragmentStatus {

    /**
     * Update status of newly fragment or file.
     *
     * @param lastUpdatedFragment Timecode of the last fragment
     * @param lastUpdatedVideoFile File of the last file
     */
    void update(long lastUpdatedFragment, VideoFile lastUpdatedVideoFile);
}
