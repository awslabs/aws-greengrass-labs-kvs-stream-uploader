/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aws.iot.edgeconnectorforkvs.util;

import lombok.Synchronized;

public final class Constants {


    private Constants() {
    };

    public static final int UPLOADER_WAIT_FOR_ACKS_DELAY_MILLI_SECONDS = 3 * 1000; // 3 seconds

    public static final String VIDEO_FILENAME_PREFIX = "video_";
    public static final String VIDEO_FILENAME_POSTFIX = ".mkv";
    public static final String VIDEO_FILENAME_UPLOADED_POSTFIX = "_uploaded.mkv";

    private static final Object SYNCLOCK = new Object[0];
    private static boolean fatalStatus;

    @Synchronized("SYNCLOCK")
    public static boolean getFatalStatus() {
        return fatalStatus;
    }

    @Synchronized("SYNCLOCK")
    public static void setFatalStatus(boolean to) {
        fatalStatus = to;
    }

    // To be used for debugging
    public static String getCallingFunctionName(int lvl) {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        StackTraceElement e = stacktrace[lvl + 1];
        String methodName = e.getMethodName();
        return methodName;
    }
}
