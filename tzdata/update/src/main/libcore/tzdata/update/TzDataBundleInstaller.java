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

import android.util.Slog;

import java.io.File;
import java.io.IOException;

/**
 * A bundle-validation / extraction class. Separate from the services code that uses it for easier
 * testing.
 */
public final class TzDataBundleInstaller {

    static final String CURRENT_TZ_DATA_DIR_NAME = "current";
    static final String WORKING_DIR_NAME = "working";
    static final String OLD_TZ_DATA_DIR_NAME = "old";

    private final String logTag;
    private final File installDir;

    public TzDataBundleInstaller(String logTag, File installDir) {
        this.logTag = logTag;
        this.installDir = installDir;
    }

    /**
     * Install the supplied content.
     *
     * <p>Errors during unpacking or installation will throw an {@link IOException}.
     * If the content is invalid this method returns {@code false}.
     * If the installation completed successfully this method returns {@code true}.
     */
    public boolean install(byte[] content) throws IOException {
        File oldTzDataDir = new File(installDir, OLD_TZ_DATA_DIR_NAME);
        if (oldTzDataDir.exists()) {
            FileUtils.deleteRecursive(oldTzDataDir);
        }

        File currentTzDataDir = new File(installDir, CURRENT_TZ_DATA_DIR_NAME);
        File workingDir = new File(installDir, WORKING_DIR_NAME);

        Slog.i(logTag, "Applying time zone update");
        File unpackedContentDir = unpackBundle(content, workingDir);
        try {
            if (!checkBundleFilesExist(unpackedContentDir)) {
                Slog.i(logTag, "Update not applied: Bundle is missing files");
                return false;
            }

            if (verifySystemChecksums(unpackedContentDir)) {
                FileUtils.makeDirectoryWorldAccessible(unpackedContentDir);

                if (currentTzDataDir.exists()) {
                    Slog.i(logTag, "Moving " + currentTzDataDir + " to " + oldTzDataDir);
                    FileUtils.rename(currentTzDataDir, oldTzDataDir);
                }
                Slog.i(logTag, "Moving " + unpackedContentDir + " to " + currentTzDataDir);
                FileUtils.rename(unpackedContentDir, currentTzDataDir);
                Slog.i(logTag, "Update applied: " + currentTzDataDir + " successfully created");
                return true;
            }
            Slog.i(logTag, "Update not applied: System checksum did not match");
            return false;
        } finally {
            deleteBestEffort(oldTzDataDir);
            deleteBestEffort(unpackedContentDir);
        }
    }

    private void deleteBestEffort(File dir) {
        if (dir.exists()) {
            try {
                FileUtils.deleteRecursive(dir);
            } catch (IOException e) {
                // Logged but otherwise ignored.
                Slog.w(logTag, "Unable to delete " + dir, e);
            }
        }
    }

    private File unpackBundle(byte[] content, File targetDir) throws IOException {
        Slog.i(logTag, "Unpacking update content to: " + targetDir);
        ConfigBundle bundle = new ConfigBundle(content);
        bundle.extractTo(targetDir);
        return targetDir;
    }

    private boolean checkBundleFilesExist(File unpackedContentDir) throws IOException {
        Slog.i(logTag, "Verifying bundle contents");
        return FileUtils.filesExist(unpackedContentDir,
                ConfigBundle.TZ_DATA_VERSION_FILE_NAME,
                ConfigBundle.CHECKSUMS_FILE_NAME,
                ConfigBundle.ZONEINFO_FILE_NAME,
                ConfigBundle.ICU_DATA_FILE_NAME);
    }

    private boolean verifySystemChecksums(File unpackedContentDir) throws IOException {
        Slog.i(logTag, "Verifying system file checksums");
        File checksumsFile = new File(unpackedContentDir, ConfigBundle.CHECKSUMS_FILE_NAME);
        for (String line : FileUtils.readLines(checksumsFile)) {
            int delimiterPos = line.indexOf(',');
            if (delimiterPos <= 0 || delimiterPos == line.length() - 1) {
                throw new IOException("Bad checksum entry: " + line);
            }
            long expectedChecksum;
            try {
                expectedChecksum = Long.parseLong(line.substring(0, delimiterPos));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid checksum value: " + line);
            }
            String filePath = line.substring(delimiterPos + 1);
            File file = new File(filePath);
            if (!file.exists()) {
                Slog.i(logTag, "Failed checksum test for file: " + file + ": file not found");
                return false;
            }
            long actualChecksum = FileUtils.calculateChecksum(file);
            if (actualChecksum != expectedChecksum) {
                Slog.i(logTag, "Failed checksum test for file: " + file
                        + ": required=" + expectedChecksum + ", actual=" + actualChecksum);
                return false;
            }
        }
        return true;
    }
}
