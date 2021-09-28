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

package com.aws.greengrass.sitewatch.videorecorder.util;

import com.aws.greengrass.sitewatch.videorecorder.model.RecorderCapability;

/**
 * Recorder configurations.
 */
public final class Config {
    /**
     * Default GStreamer Version major version.
     */
    static final int GST_VER_MAJOR = 1;
    /**
     * Default GStreamer Version minor version.
     */
    static final int GST_VER_MINOR = 18;
    /**
     * Default GStreamer Version micro version.
     */
    static final int GST_VER_MICRO = 4;
    /**
     * Default GStreamer Version nano version.
     */
    static final int GST_VER_NANO = 0;

    /**
     * Default file rotation period in NS.
     */
    public static final long DEFAULT_FILE_ROTATION_IN_NS = 60_000_000_000L;

    /**
     * Recorder pipeline file branch names.
     */
    public static final String FILE_PATH = "branchFilePath";
    /**
     * Recorder pipeline app callback branch names.
     */
    public static final String CALLBACK_PATH = "branchCallbackPath";
    /**
     * Recorder pipeline app output stream branch names.
     */
    public static final String OSTREAM_PATH = "branchOutputStreamPath";

    /**
     * Default recorder file branch capability.
     */
    public static final RecorderCapability FILE_PATH_CAPABILITY = RecorderCapability.VIDEO_AUDIO;
    /**
     * Default recorder app branch capability.
     */
    public static final RecorderCapability APP_PATH_CAPABILITY = RecorderCapability.VIDEO_AUDIO;

    private Config() {}
}
