package com.aws.greengrass.sitewatch.videouploader.mkv;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MkvRawElementTest {

    @Test
    public void getMkvSize_validInput_returnMkvSize() {
        byte[] zeroSizedArray = new byte[]{(byte) 0x88};
        byte[] result = MkvDataRawElement.getMkvSize(8L);
        Assertions.assertArrayEquals(zeroSizedArray, result);
    }

    @Test
    public void getMkvSize_exceedMaxLimit_returnZeroSizedArray() {
        byte[] zeroSizedArray = new byte[0];
        byte[] result = MkvDataRawElement.getMkvSize(0x01000000_00000000L);
        Assertions.assertArrayEquals(zeroSizedArray, result);
    }
}
