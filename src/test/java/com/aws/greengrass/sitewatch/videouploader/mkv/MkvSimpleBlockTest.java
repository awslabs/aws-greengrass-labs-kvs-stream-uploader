package com.aws.greengrass.sitewatch.videouploader.mkv;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class MkvSimpleBlockTest {

    @Test
    public void equals_differentObject_returnFalse() {
        MkvSimpleBlock simpleBlock = new MkvSimpleBlock(
                (short) 0,
                1,
                ByteBuffer.wrap(new byte[]{(byte) 0xA3, (byte) 0x85}),
                ByteBuffer.wrap(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00})
        );
        MkvSimpleBlock simpleBlockWithDifferentTimecode = new MkvSimpleBlock(
                (short) 10,
                1,
                ByteBuffer.wrap(new byte[]{(byte) 0xA3, (byte) 0x85}),
                ByteBuffer.wrap(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00})
        );
        MkvSimpleBlock simpleBlockWithDifferentTrackNumber = new MkvSimpleBlock(
                (short) 0,
                2,
                ByteBuffer.wrap(new byte[]{(byte) 0xA3, (byte) 0x85}),
                ByteBuffer.wrap(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00})
        );

        Assertions.assertNotEquals(simpleBlock, null);
        Assertions.assertNotEquals(simpleBlock, "other");
        Assertions.assertNotEquals(simpleBlock, simpleBlockWithDifferentTimecode);
        Assertions.assertNotEquals(simpleBlock, simpleBlockWithDifferentTrackNumber);
    }

    @Test
    public void equals_sameObject_returnTre() {
        MkvSimpleBlock simpleBlock = new MkvSimpleBlock(
                (short) 0,
                1,
                ByteBuffer.wrap(new byte[]{(byte) 0xA3, (byte) 0x85}),
                ByteBuffer.wrap(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00})
        );
        MkvSimpleBlock sameSimpleBlock = new MkvSimpleBlock(
                (short) 0,
                1,
                ByteBuffer.wrap(new byte[]{(byte) 0xA3, (byte) 0x85}),
                ByteBuffer.wrap(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00})
        );

        Assertions.assertEquals(simpleBlock, sameSimpleBlock);
    }

    @Test
    public void hashCode_differentObject_differentHashCode() {
        MkvSimpleBlock simpleBlock = new MkvSimpleBlock(
                (short) 0,
                1,
                ByteBuffer.wrap(new byte[]{(byte) 0xA3, (byte) 0x85}),
                ByteBuffer.wrap(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00})
        );
        MkvSimpleBlock simpleBlockWithDifferentTimecode = new MkvSimpleBlock(
                (short) 10,
                1,
                ByteBuffer.wrap(new byte[]{(byte) 0xA3, (byte) 0x85}),
                ByteBuffer.wrap(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00})
        );
        MkvSimpleBlock simpleBlockWithDifferentTrackNumber = new MkvSimpleBlock(
                (short) 0,
                2,
                ByteBuffer.wrap(new byte[]{(byte) 0xA3, (byte) 0x85}),
                ByteBuffer.wrap(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00})
        );

        Assertions.assertNotEquals(simpleBlock.hashCode(), simpleBlockWithDifferentTimecode.hashCode());
        Assertions.assertNotEquals(simpleBlock.hashCode(), simpleBlockWithDifferentTrackNumber.hashCode());
    }

    @Test
    public void equals_sameObject_sameHashCode() {
        MkvSimpleBlock simpleBlock = new MkvSimpleBlock(
                (short) 0,
                1,
                ByteBuffer.wrap(new byte[]{(byte) 0xA3, (byte) 0x85}),
                ByteBuffer.wrap(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00})
        );
        MkvSimpleBlock sameSimpleBlock = new MkvSimpleBlock(
                (short) 0,
                1,
                ByteBuffer.wrap(new byte[]{(byte) 0xA3, (byte) 0x85}),
                ByteBuffer.wrap(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00})
        );

        Assertions.assertEquals(simpleBlock.hashCode(), sameSimpleBlock.hashCode());
    }
}
