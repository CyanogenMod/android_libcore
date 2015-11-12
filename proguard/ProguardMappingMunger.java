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
 * limitations under the License
 */

package proguard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class ProguardMappingMunger {

    private static final Map<String, String> PACKAGE_NAME_MAPPING = new HashMap<>();

    static {
        PACKAGE_NAME_MAPPING.put("sun.nio.ch", "xx001");
        PACKAGE_NAME_MAPPING.put("sun.nio.fs", "xx002");
        PACKAGE_NAME_MAPPING.put("sun.misc", "xx003");
        PACKAGE_NAME_MAPPING.put("sun.net.spi", "xx004");
    }

    private static final Set<String> PROBLEMATIC_CLASSES = new HashSet<>();

    static {
        PROBLEMATIC_CLASSES.add("sun.nio.ch.NativeThread");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.UnixAsynchronousSocketChannelImpl");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.FileDispatcherImpl");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.UnixAsynchronousServerSocketChannelImpl");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.ServerSocketChannelImpl");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.DatagramChannelImpl");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.EPollPort");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.IOUtil");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.KQueuePort");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.InheritedChannel");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.DatagramDispatcher");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.EPoll");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.SolarisEventPort");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.FileKey");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.FileChannelImpl");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.SocketChannelImpl");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.DevPollArrayWrapper");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.EPollArrayWrapper");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.PollArrayWrapper");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.KQueue");
        PROBLEMATIC_CLASSES.add("sun.nio.ch.Net");
        PROBLEMATIC_CLASSES.add("sun.nio.fs.UnixNativeDispatcher");
        PROBLEMATIC_CLASSES.add("sun.nio.fs.UnixCopyFile");
        PROBLEMATIC_CLASSES.add("sun.misc.Unsafe");
        PROBLEMATIC_CLASSES.add("sun.misc.Version");
        PROBLEMATIC_CLASSES.add("sun.net.spi.DefaultProxySelector");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage ProguardMappingMunger <infile> <outfile>");
            return;
        }

        process(args[0], args[1]);
        System.out.println("Done..");
    }

    private static String[] parseMapping(String line) {
        String trimmed = line.trim();
        int separator = trimmed.indexOf("->");
        String from = trimmed.substring(0, separator).trim();
        String to = trimmed.substring(separator + 2).trim();
        if (to.lastIndexOf(':') == (to.length() - 1)) {
            to = to.substring(0, (to.length() - 1));
        }

        return new String[] { from, to };
    }

    private static String getName(String methodOrFieldName) {
        if (methodOrFieldName.indexOf('(') != -1) {
            String nameAndReturnType = methodOrFieldName.substring(0, methodOrFieldName.indexOf('('));
            return nameAndReturnType.substring(nameAndReturnType.lastIndexOf(' ')).trim();
        } else {
            return methodOrFieldName.substring(methodOrFieldName.lastIndexOf(' ')).trim();
        }
    }

    private static void process(String infile, String outfile) throws Exception {
        Reader reader = new BufferedReader(new FileReader(infile));
        Writer writer = new BufferedWriter(new FileWriter(outfile));
        Scanner scanner = new Scanner(reader);
        boolean processingProblematicClass = false;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            // Lines starting with spaces are method transformations,
            // Lines starting without spaces are class transformations.
            String[] mapping = parseMapping(line);

            if (line.charAt(0) != ' ') {
                String className = mapping[0];
                String transform = mapping[1];

                if (PROBLEMATIC_CLASSES.contains(className)) {
                    processingProblematicClass = true;
                } else {
                    processingProblematicClass = false;
                }

                if (!className.equals("sun.misc.Unsafe")) {
                    for (Map.Entry<String, String> entries : PACKAGE_NAME_MAPPING.entrySet()) {
                        if (transform.startsWith(entries.getKey())) {
                            String nonQualifiedClassName = processingProblematicClass ?
                                    className.substring(entries.getKey().length()) :
                                    transform.substring(entries.getKey().length());
                            transform = entries.getValue() + nonQualifiedClassName;
                            break;
                        }
                    }
                }

                writer.write(className + " -> " + transform + ":\n");
            } else {
                String methodOrFieldName = mapping[0];
                String transform = mapping[1];
                writer.write("    " + methodOrFieldName + " -> " +
                        (processingProblematicClass ? getName(methodOrFieldName) : transform) + "\n");
            }
        }
    }
}
