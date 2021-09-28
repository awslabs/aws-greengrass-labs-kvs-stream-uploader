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

import com.aws.iot.iotlab.streamuploader.model.MappedConfig;
import com.aws.iot.iotlab.streamuploader.model.SingleConfig;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Main class used to run Stream Uploader Control and load config
 */
@Slf4j
public class StreamUploaderMain {
    private static MappedConfig mappedConfig;
    private static FileReader fileReader;
    private static Gson gson = new Gson();

    /**
     * Main function for stream uploader
     * @param args
     */
    public static void main(String[] args) {
        try {
            String path = new File(StreamUploaderMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            fileReader = new FileReader(path + "/streamuploaderconfig.json");
            mappedConfig = gson.fromJson(fileReader, MappedConfig.class);
        } catch (Exception e) {
            log.error("Fail to parse configuration file");
            return;
        }

        for (SingleConfig singleConfig: mappedConfig.getConfigList()) {
            StreamConfig streamConfig = new StreamConfig(singleConfig);
            new Thread(new StreamUploaderControl(streamConfig)).start();
        }

        try {
            fileReader.close();
        } catch (IOException e) {
            log.error("Fail to close fileReader");
        }
    }
}
