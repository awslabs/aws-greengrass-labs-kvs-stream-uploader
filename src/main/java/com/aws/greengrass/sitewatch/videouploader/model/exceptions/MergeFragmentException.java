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

package com.aws.greengrass.sitewatch.videouploader.model.exceptions;

/**
 * An exception for failures from merging 2 MKV files.
 */
public class MergeFragmentException extends RuntimeException {

    /**
     * This exception is thrown when 2 MKV files could not be merged into 1 MKV file.
     *
     * @param message The failure message
     */
    public MergeFragmentException(String message) {
        super(message);
    }

    /**
     * This exception is thrown when 2 MKV files could not be merged into 1 MKV file.
     *
     * @param message The failure message
     * @param cause   The failure cause
     */
    public MergeFragmentException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * This exception is thrown when 2 MKV files could not be merged into 1 MKV file.
     *
     * @param cause The failure cause
     */
    public MergeFragmentException(Throwable cause) {
        super(cause);
    }
}
