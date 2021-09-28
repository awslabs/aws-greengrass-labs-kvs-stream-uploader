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

package com.aws.iot.iotlab.streamuploader;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;

public class StreamUploaderMainTest {
    @Test
    public void mainFailureTest() {
        try {
            StreamUploaderMain.main(new String[0]);
        } catch (Exception e) {
            Assertions.fail("Main function fail to invoke");
        }
    }

    @Test
    public void mainTest() {
        StreamUploaderMain streamUploaderMain = new StreamUploaderMain();
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("streamuploaderconfig.json");
        try {
            String path = new File(StreamUploaderMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            File configFile = new File(path + "/streamuploaderconfig.json");
            FileUtils.copyInputStreamToFile(inputStream, configFile);
        } catch (Exception e) {
            Assertions.fail("Fail to write config file for test");
        }

        try {
            StreamUploaderMain.main(new String[0]);
        } catch (Exception e) {
            Assertions.fail("Main function fail to invoke");
        }

        try {
            String path = new File(StreamUploaderMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            File configFile = new File(path + "/streamuploaderconfig.json");
            configFile.delete();
        } catch (Exception e) {
            Assertions.fail("Fail to clear config file for test");
        }
    }
}
