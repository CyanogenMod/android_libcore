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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import libcore.tzdata.update.ConfigBundle;

/**
 * A class for creating a {@link ConfigBundle} containing timezone update data.
 */
public final class TzDataBundleBuilder {

    private String tzDataVersion;
    private StringBuilder checksumsFileContent = new StringBuilder();
    private File zoneInfoFile;
    private File icuTzDataFile;

    public TzDataBundleBuilder setTzDataVersion(String tzDataVersion) {
        this.tzDataVersion = tzDataVersion;
        return this;
    }

    public TzDataBundleBuilder addChecksum(String fileName, long checksum) {
        checksumsFileContent.append(Long.toString(checksum))
                .append(',')
                .append(fileName)
                .append('\n');
        return this;
    }

    public TzDataBundleBuilder addBionicTzData(File zoneInfoFile) {
        this.zoneInfoFile = zoneInfoFile;
        return this;
    }

    public TzDataBundleBuilder addIcuTzData(File icuTzDataFile) {
        this.icuTzDataFile = icuTzDataFile;
        return this;
    }

    /**
     * Builds a {@link libcore.tzdata.update.ConfigBundle}.
     */
    public ConfigBundle build() throws IOException {
        if (tzDataVersion == null) {
            throw new IllegalStateException("Missing tzDataVersion");
        }
        if (zoneInfoFile == null) {
            throw new IllegalStateException("Missing zoneInfo file");
        }

        return buildUnvalidated();
    }

    // For use in tests.
    public TzDataBundleBuilder clearChecksumEntries() {
        checksumsFileContent.setLength(0);
        return this;
    }

    // For use in tests.
    public TzDataBundleBuilder clearBionicTzData() {
        this.zoneInfoFile = null;
        return this;
    }

    /**
     * For use in tests. Use {@link #build()}.
     */
    public ConfigBundle buildUnvalidated() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            addZipEntry(zos, ConfigBundle.CHECKSUMS_FILE_NAME,
                    checksumsFileContent.toString().getBytes(StandardCharsets.UTF_8));
            if (tzDataVersion != null) {
                addZipEntry(zos, ConfigBundle.TZ_DATA_VERSION_FILE_NAME,
                        tzDataVersion.getBytes(StandardCharsets.UTF_8));
            }
            if (zoneInfoFile != null) {
                addZipEntry(zos, ConfigBundle.ZONEINFO_FILE_NAME,
                        readFileAsByteArray(zoneInfoFile));
            }
            if (icuTzDataFile != null) {
                addZipEntry(zos, ConfigBundle.ICU_DATA_FILE_NAME,
                        readFileAsByteArray(icuTzDataFile));
            }
        }
        return new ConfigBundle(baos.toByteArray());
    }

    private static void addZipEntry(ZipOutputStream zos, String name, byte[] content)
            throws IOException {
        ZipEntry zipEntry = new ZipEntry(name);
        zipEntry.setSize(content.length);
        zos.putNextEntry(zipEntry);
        zos.write(content);
        zos.closeEntry();
    }

    /**
     * Returns the contents of 'path' as a byte array.
     */
    public static byte[] readFileAsByteArray(File file) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (FileInputStream  fis = new FileInputStream(file)) {
            int count;
            while ((count = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, count);
            }
        }
        return baos.toByteArray();
    }
}

