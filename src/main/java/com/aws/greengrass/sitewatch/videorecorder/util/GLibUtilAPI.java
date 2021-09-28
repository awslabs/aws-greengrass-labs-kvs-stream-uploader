package com.aws.greengrass.sitewatch.videorecorder.util;

import java.util.HashMap;
import org.freedesktop.gstreamer.lowlevel.GNative;
import org.freedesktop.gstreamer.lowlevel.GTypeMapper;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * GLib utility.
 */
@SuppressWarnings("MethodName")
interface GLibUtilAPI extends Library {
    GLibUtilAPI GLIB_API =
            GNative.loadLibrary("glib-2.0", GLibUtilAPI.class, new HashMap<String, Object>() {
                {
                    put(Library.OPTION_TYPE_MAPPER, new GTypeMapper());
                }
            });

    Pointer g_strdup(String str);
}
