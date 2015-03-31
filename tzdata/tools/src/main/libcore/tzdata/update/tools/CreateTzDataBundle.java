/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package libcore.tzdata.update.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Properties;
import libcore.tzdata.update.ConfigBundle;
import libcore.tzdata.update.FileUtils;

/**
 * A command-line tool for creating a TZ data update bundle.
 *
 * Args:
 * tzdata.properties file - the file describing the bundle (see template file in tzdata/tools)
 * output file - the name of the file to be generated
 */
public class CreateTzDataBundle {

    private CreateTzDataBundle() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            printUsage();
            System.exit(1);
        }
        File f = new File(args[0]);
        if (!f.exists()) {
            System.err.println("Properties file " + f + " not found");
            printUsage();
            System.exit(2);
        }
        Properties p = loadProperties(f);
        TzDataBundleBuilder builder = new TzDataBundleBuilder()
                .setTzDataVersion(getMandatoryProperty(p, "tzdata.version"))
                .addBionicTzData(getMandatoryPropertyFile(p, "bionic.file"))
                .addIcuTzData(getMandatoryPropertyFile(p, "icu.file"));

        int i = 1;
        while (true) {
            String localFileNameProperty = "checksum.file.local." + i;
            String localFileName = p.getProperty(localFileNameProperty);
            String onDeviceFileNameProperty = "checksum.file.ondevice." + i;
            String onDeviceFileName = p.getProperty(onDeviceFileNameProperty);
            boolean foundLocalFileNameProperty = localFileName != null;
            boolean foundOnDeviceFileNameProperty = onDeviceFileName != null;
            if (!foundLocalFileNameProperty && !foundOnDeviceFileNameProperty) {
                break;
            } else if (foundLocalFileNameProperty != foundOnDeviceFileNameProperty) {
                System.out.println("Properties file must specify both, or neither of: "
                        + localFileNameProperty + " and " + onDeviceFileNameProperty);
                System.exit(5);
            }

            long checksum = FileUtils.calculateChecksum(new File(localFileName));
            builder.addChecksum(onDeviceFileName, checksum);
            i++;
        }
        if (i == 1) {
            // For safety we enforce >= 1 checksum entry. The installer does not require it.
            System.out.println("There must be at least one checksum file");
            System.exit(6);
        }
        System.out.println("Update contains checksums for " + (i-1) + " files");

        ConfigBundle bundle = builder.build();
        File outputFile = new File(args[1]);
        try (OutputStream os = new FileOutputStream(outputFile)) {
            os.write(bundle.getBundleBytes());
        }
        System.out.println("Wrote: " + outputFile);
    }

    private static File getMandatoryPropertyFile(Properties p, String propertyName) {
        String fileName = getMandatoryProperty(p, propertyName);
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println(
                    "Missing file: " + file + " for property " + propertyName + " does not exist.");
            printUsage();
            System.exit(4);
        }
        return file;
    }

    private static String getMandatoryProperty(Properties p, String propertyName) {
        String value = p.getProperty(propertyName);
        if (value == null) {
            System.out.println("Missing property: " + propertyName);
            printUsage();
            System.exit(3);
        }
        return value;
    }

    private static Properties loadProperties(File f) throws IOException {
        Properties p = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(f))) {
            p.load(reader);
        }
        return p;
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("\t" + CreateTzDataBundle.class.getName() +
                " <tzupdate.properties file> <output file>");
    }
}
