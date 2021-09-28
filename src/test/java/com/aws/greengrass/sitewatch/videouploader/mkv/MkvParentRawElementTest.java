package com.aws.greengrass.sitewatch.videouploader.mkv;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class MkvParentRawElementTest {

    @Test
    public void equals_sameSettings_returnTrue() {
        MkvParentRawElement parentElement = new MkvParentRawElement(
                ByteBuffer.wrap(new byte[]{(byte) 0xAE, (byte) 0x81})
        );
        MkvParentRawElement sameIdParentElement = new MkvParentRawElement(
                ByteBuffer.wrap(new byte[]{(byte) 0xAE, (byte) 0x81})
        );

        Assertions.assertEquals(parentElement, sameIdParentElement);
    }

    @Test
    public void equals_differentObject_returnFalse() {
        MkvParentRawElement parentElement = new MkvParentRawElement(
                ByteBuffer.wrap(new byte[]{(byte) 0xAE, (byte) 0x81})
        );
        MkvParentRawElement differentIdParentElement = new MkvParentRawElement(
                ByteBuffer.wrap(new byte[]{(byte) 0xD7, (byte) 0x81})
        );

        Assertions.assertNotEquals(parentElement, null);
        Assertions.assertNotEquals(parentElement, "other");
        Assertions.assertNotEquals(parentElement, differentIdParentElement);
    }

    @Test
    public void hashCode_differentObject_returnFalse() {
        MkvParentRawElement parentElement = new MkvParentRawElement(
                ByteBuffer.wrap(new byte[]{(byte) 0xAE, (byte) 0x81})
        );
        MkvParentRawElement differentIdParentElement = new MkvParentRawElement(
                ByteBuffer.wrap(new byte[]{(byte) 0xD7, (byte) 0x81})
        );

        Assertions.assertNotEquals(parentElement.hashCode(), differentIdParentElement.hashCode());
    }
}
