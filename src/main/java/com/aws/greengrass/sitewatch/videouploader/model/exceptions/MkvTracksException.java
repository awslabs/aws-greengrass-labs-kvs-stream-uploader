package com.aws.greengrass.sitewatch.videouploader.model.exceptions;

/**
 * An exception for parsing MKV tracks.
 */
public class MkvTracksException extends RuntimeException {
    /**
     * This exception is thrown it fails to parse MKV tracks.
     *
     * @param message The failure message
     */
    public MkvTracksException(String message) {
        super(message);
    }

    /**
     * This exception is thrown it fails to parse MKV tracks.
     *
     * @param message The failure message
     * @param cause   The failure cause
     */
    public MkvTracksException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * This exception is thrown it fails to parse MKV tracks.
     *
     * @param cause The failure cause
     */
    public MkvTracksException(Throwable cause) {
        super(cause);
    }
}
