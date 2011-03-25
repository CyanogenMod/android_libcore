/*
 * Copyright (C) 2010 The Android Open Source Project
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

package dalvik.system.profiler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Run on device with:
 * adb shell dalvikvm 'dalvik.system.SamplingProfiler\$HprofBinaryToAscii'
 *
 * Run on host with:
 * java -classpath out/target/common/obj/JAVA_LIBRARIES/core_intermediates/classes.jar
 */
public final class HprofBinaryToAscii {

    public static void main(String[] args) {
        System.exit(convert(args) ? 0 : 1);
    }

    public static boolean convert(String[] args) {

        if (args.length != 1) {
            usage("binary hprof file argument expected");
            return false;
        }
        File file = new File(args[0]);
        if (!file.exists()) {
            usage("file " + file + " does not exist");
            return false;
        }

        HprofData hprofData;
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            BinaryHprofReader reader = new BinaryHprofReader(inputStream);
            reader.setStrict(false);
            reader.read();
            hprofData = reader.getHprofData();
        } catch (IOException e) {
            System.out.println("Problem reading binary hprof data from "
                               + file + ": " + e.getMessage());
            return false;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
        try {
            HprofWriter writer = new AsciiHprofWriter(hprofData, System.out);
            writer.write();
        } catch (IOException e) {
            System.out.println("Problem writing ASCII hprof data: " + e.getMessage());
            return false;
        }
        return true;
    }

    private static void usage(String error) {
        System.out.print("ERROR: ");
        System.out.println(error);
        System.out.println();
        System.out.println("usage: HprofBinaryToAscii <binary-hprof-file>");
        System.out.println();
        System.out.println("Reads a binary hprof file and print it in ASCII format");
    }
}
