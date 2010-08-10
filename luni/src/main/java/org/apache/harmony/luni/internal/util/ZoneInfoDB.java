/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.apache.harmony.luni.internal.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;
import libcore.io.IoUtils;

/**
 * A class used to initialize the time zone database.  This implementation uses the
 * 'zoneinfo' database as the source of time zone information.  However, to conserve
 * disk space the data for all time zones are concatenated into a single file, and a
 * second file is used to indicate the starting position of each time zone record.  A
 * third file indicates the version of the zoneinfo database used to generate the data.
 *
 * {@hide}
 */
public final class ZoneInfoDB {
    /**
     * The directory containing the time zone database files.
     */
    private static final String ZONE_DIRECTORY_NAME =
            System.getenv("ANDROID_ROOT") + "/usr/share/zoneinfo/";

    /**
     * The name of the file containing the concatenated time zone records.
     */
    private static final String ZONE_FILE_NAME = ZONE_DIRECTORY_NAME + "zoneinfo.dat";

    /**
     * The name of the file containing the index to each time zone record within
     * the zoneinfo.dat file.
     */
    private static final String INDEX_FILE_NAME = ZONE_DIRECTORY_NAME + "zoneinfo.idx";

    private static final Object LOCK = new Object();

    private static final String VERSION = readVersion();

    private static String[] names;
    private static int[] starts;
    private static int[] lengths;
    private static int[] offsets;
    static {
        readIndex();
    }

    private ZoneInfoDB() {}

    /**
     * Reads the file indicating the database version in use.  If the file is not
     * present or is unreadable, we assume a version of "2007h".
     */
    private static String readVersion() {
        // Zoneinfo version used prior to creation of the zoneinfo.version file.
        String version = "2007h";
        RandomAccessFile versionFile = null;
        try {
            versionFile = new RandomAccessFile(ZONE_DIRECTORY_NAME + "zoneinfo.version", "r");
            byte[] buf = new byte[(int) versionFile.length()];
            versionFile.readFully(buf);
            version = new String(buf, 0, buf.length, Charsets.ISO_8859_1).trim();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            IoUtils.closeQuietly(versionFile);
        }
        return version;
    }

    private static void readIndex() {
        RandomAccessFile indexFile = null;
        try {
            indexFile = new RandomAccessFile(INDEX_FILE_NAME, "r");

            // The database reserves 40 bytes for each name.
            final int SIZEOF_TZNAME = 40;
            // The database uses 32-bit (4 byte) integers.
            final int SIZEOF_TZINT = 4;

            byte[] nameBytes = new byte[SIZEOF_TZNAME];

            int numEntries = (int) (indexFile.length() / (SIZEOF_TZNAME + 3*SIZEOF_TZINT));

            char[] nameChars = new char[numEntries * SIZEOF_TZNAME];
            int[] nameEnd = new int[numEntries];
            int nameOffset = 0;

            starts = new int[numEntries];
            lengths = new int[numEntries];
            offsets = new int[numEntries];

            for (int i = 0; i < numEntries; i++) {
                indexFile.readFully(nameBytes);
                starts[i] = indexFile.readInt();
                lengths[i] = indexFile.readInt();
                offsets[i] = indexFile.readInt();

                // Don't include null chars in the String
                int len = nameBytes.length;
                for (int j = 0; j < len; j++) {
                    if (nameBytes[j] == 0) {
                        break;
                    }
                    nameChars[nameOffset++] = (char) (nameBytes[j] & 0xFF);
                }

                nameEnd[i] = nameOffset;
            }

            String name = new String(nameChars, 0, nameOffset);

            // Assumes the nameChars is all ASCII (so byte offsets == char offsets).
            names = new String[numEntries];
            for (int i = 0; i < numEntries; i++) {
                names[i] = name.substring(i == 0 ? 0 : nameEnd[i - 1], nameEnd[i]);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            IoUtils.closeQuietly(indexFile);
        }
    }

    public static String getVersion() {
        return VERSION;
    }

    public static String[] getAvailableIDs() {
        return (String[]) names.clone();
    }

    public static String[] getAvailableIDs(int rawOffset) {
        List<String> matches = new ArrayList<String>();
        for (int i = 0, end = offsets.length; i < end; i++) {
            if (offsets[i] == rawOffset) {
                matches.add(names[i]);
            }
        }
        return matches.toArray(new String[matches.size()]);
    }

    private static TimeZone readTimeZone(String name) throws IOException {
        FileInputStream fis = null;
        int length = 0;

        File f = new File(ZONE_DIRECTORY_NAME + name);
        if (!f.exists()) {
            fis = new FileInputStream(ZONE_FILE_NAME);
            int i = Arrays.binarySearch(ZoneInfoDB.names, name);

            if (i < 0) {
                return null;
            }

            int start = ZoneInfoDB.starts[i];
            length = ZoneInfoDB.lengths[i];

            fis.skip(start);
        }

        if (fis == null) {
            fis = new FileInputStream(f);
            length = (int)f.length(); // data won't exceed 2G!
        }

        byte[] data = new byte[length];
        int nread = 0;
        while (nread < length) {
            int size = fis.read(data, nread, length - nread);
            if (size > 0) {
                nread += size;
            }
        }

        try {
            fis.close();
        } catch (IOException e3) {
            // probably better to continue than to fail here
            java.util.logging.Logger.global.warning("IOException " + e3 +
                " retrieving time zone data");
            e3.printStackTrace();
        }

        if (data.length < 36) {
            return null;
        }
        if (data[0] != 'T' || data[1] != 'Z' ||
            data[2] != 'i' || data[3] != 'f') {
            return null;
        }

        int ntransition = read4(data, 32);
        int ngmtoff = read4(data, 36);
        int base = 44;

        int[] transitions = new int[ntransition];
        for (int i = 0; i < ntransition; i++) {
            transitions[i] = read4(data, base + 4 * i);
        }
        base += 4 * ntransition;

        byte[] type = new byte[ntransition];
        for (int i = 0; i < ntransition; i++) {
            type[i] = data[base + i];
        }
        base += ntransition;

        int[] gmtoff = new int[ngmtoff];
        byte[] isdst = new byte[ngmtoff];
        byte[] abbrev = new byte[ngmtoff];
        for (int i = 0; i < ngmtoff; i++) {
            gmtoff[i] = read4(data, base + 6 * i);
            isdst[i] = data[base + 6 * i + 4];
            abbrev[i] = data[base + 6 * i + 5];
        }

        base += 6 * ngmtoff;

        return new ZoneInfo(name, transitions, type, gmtoff, isdst, abbrev, data, base);
    }

    private static int read4(byte[] data, int off) {
        return ((data[off    ] & 0xFF) << 24) |
               ((data[off + 1] & 0xFF) << 16) |
               ((data[off + 2] & 0xFF) <<  8) |
               ((data[off + 3] & 0xFF) <<  0);
    }

    public static TimeZone getTimeZone(String id) {
        if (id == null) {
            return null;
        }

        try {
            return readTimeZone(id);
        } catch (IOException ignored) {
            return null;
        }
    }

    public static TimeZone getSystemDefault() {
        synchronized (LOCK) {
            TimezoneGetter tzGetter = TimezoneGetter.getInstance();
            String zoneName = tzGetter != null ? tzGetter.getId() : null;
            return zoneName != null && !zoneName.isEmpty()
                    ? TimeZone.getTimeZone(zoneName.trim())
                    : TimeZone.getTimeZone("localtime"); // use localtime for the simulator
        }
    }
}
