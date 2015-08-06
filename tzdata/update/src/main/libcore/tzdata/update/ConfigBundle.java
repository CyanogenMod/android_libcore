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
package libcore.tzdata.update;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A configuration bundle. This is a thin wrapper around some in-memory bytes representing a zip
 * archive and logic for its safe extraction.
 */
public final class ConfigBundle {

    /** The name of the file inside the bundle containing the TZ data version. */
    public static final String TZ_DATA_VERSION_FILE_NAME = "tzdata_version";

    /** The name of the file inside the bundle containing the expected device checksums. */
    public static final String CHECKSUMS_FILE_NAME = "checksums";

    /** The name of the file inside the bundle containing bionic/libcore TZ data. */
    public static final String ZONEINFO_FILE_NAME = "tzdata";

    /** The name of the file inside the bundle containing ICU TZ data. */
    public static final String ICU_DATA_FILE_NAME = "icu/icu_tzdata.dat";

    private static final int BUFFER_SIZE = 8192;

    private final byte[] bytes;

    public ConfigBundle(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBundleBytes() {
        return bytes;
    }

    public void extractTo(File targetDir) throws IOException {
        extractZipSafely(new ByteArrayInputStream(bytes), targetDir, true /* makeWorldReadable */);
    }

    /** Visible for testing */
    static void extractZipSafely(InputStream is, File targetDir, boolean makeWorldReadable)
            throws IOException {

        // Create the extraction dir, if needed.
        FileUtils.ensureDirectoriesExist(targetDir, makeWorldReadable);

        try (ZipInputStream zipInputStream = new ZipInputStream(is)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                // Validate the entry name: make sure the unpacked file will exist beneath the
                // targetDir.
                String name = entry.getName();
                // Note, we assume that nothing will quickly insert a symlink after createSubFile()
                // that might invalidate the guarantees about name existing beneath targetDir.
                File entryFile = FileUtils.createSubFile(targetDir, name);

                if (entry.isDirectory()) {
                    FileUtils.ensureDirectoriesExist(entryFile, makeWorldReadable);
                } else {
                    // Create the path if there was no directory entry.
                    if (!entryFile.getParentFile().exists()) {
                        FileUtils.ensureDirectoriesExist(
                                entryFile.getParentFile(), makeWorldReadable);
                    }

                    try (FileOutputStream fos = new FileOutputStream(entryFile)) {
                        int count;
                        while ((count = zipInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, count);
                        }
                        // sync to disk
                        fos.getFD().sync();
                    }
                    // mark entryFile -rw-r--r--
                    if (makeWorldReadable) {
                        FileUtils.makeWorldReadable(entryFile);
                    }
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfigBundle that = (ConfigBundle) o;

        if (!Arrays.equals(bytes, that.bytes)) {
            return false;
        }

        return true;
    }

}
