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

package com.aws.greengrass.sitewatch.videorecorder.util;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

/**
 * Codec configurations.
 */
public final class ConfigCodec {
    /**
     * Encode to parse element.
     */
    public static final Map<String, String> PARSE_INFO;
    static {
        Hashtable<String, String> tmp = new Hashtable<>();

        // video
        tmp.put("H264", "h264parse");
        tmp.put("H265", "h265parse");
        // audio
        tmp.put("MPEG4-GENERIC", "aacparse");
        tmp.put("MP4A-LATM", "aacparse");
        tmp.put("OPUS", "opusparse");
        tmp.put("PCMA", "rawaudioparse");
        tmp.put("PCMU", "rawaudioparse");

        PARSE_INFO = Collections.unmodifiableMap(tmp);
    }

    private ConfigCodec() {}
}
