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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Utility methods for files operations.
 */
public final class FileUtils {

    private FileUtils() {
    }

    /**
     * Creates a new {@link java.io.File} from the {@code parentDir} and {@code name}, but only if
     * the resulting file would exist beneath {@code parentDir}. Useful if {@code name} could
     * contain "/../" or symlinks. The returned object has a canonicalized path.
     *
     * @throws java.io.IOException if the file would not exist beneath {@code parentDir}
     */
    public static File createSubFile(File parentDir, String name) throws IOException {
        // The subFile must exist beneath parentDir. If name contains "/../" this may not be the
        // case so we check.
        File subFile = new File(parentDir, name).getCanonicalFile();
        if (!subFile.getPath().startsWith(parentDir.getCanonicalPath())) {
            throw new IOException(name + " must exist beneath " + parentDir +
                    ". Canonicalized subpath: " + subFile);
        }
        return subFile;
    }

    /**
     * Makes sure a directory exists. If it doesn't exist, it is created. Parent directories are
     * also created as needed. If {@code makeWorldReadable} is {@code true} the directory's default
     * permissions will be set. Even when {@code makeWorldReadable} is {@code true}, only
     * directories explicitly created will have their permissions set; existing directories are
     * untouched.
     *
     * @throws IOException if the directory or one of its parents did not already exist and could
     *     not be created
     */
    public static void ensureDirectoriesExist(File dir, boolean makeWorldReadable)
            throws IOException {
        LinkedList<File> dirs = new LinkedList<>();
        File currentDir = dir;
        do {
            dirs.addFirst(currentDir);
            currentDir = currentDir.getParentFile();
        } while (currentDir != null);

        for (File dirToCheck : dirs) {
            if (!dirToCheck.exists()) {
                if (!dirToCheck.mkdir()) {
                    throw new IOException("Unable to create directory: " + dir);
                }
                if (makeWorldReadable) {
                    makeDirectoryWorldAccessible(dirToCheck);
                }
            } else if (!dirToCheck.isDirectory()) {
                throw new IOException(dirToCheck + " exists but is not a directory");
            }
        }
    }

    public static void makeDirectoryWorldAccessible(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IOException(directory + " must be a directory");
        }
        makeWorldReadable(directory);
        if (!directory.setExecutable(true, false /* ownerOnly */)) {
            throw new IOException("Unable to make " + directory + " world-executable");
        }
    }

    public static void makeWorldReadable(File file) throws IOException {
        if (!file.setReadable(true, false /* ownerOnly */)) {
            throw new IOException("Unable to make " + file + " world-readable");
        }
    }

    /**
     * Calculates the checksum from the contents of a file.
     */
    public static long calculateChecksum(File file) throws IOException {
        final int BUFFER_SIZE = 8196;
        CRC32 crc32 = new CRC32();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = fis.read(buffer)) != -1) {
                crc32.update(buffer, 0, count);
            }
        }
        return crc32.getValue();
    }

    public static void rename(File from, File to) throws IOException {
        ensureFileDoesNotExist(to);
        if (!from.renameTo(to)) {
            throw new IOException("Unable to rename " + from + " to " + to);
        }
    }

    public static void ensureFileDoesNotExist(File file) throws IOException {
        if (file.exists()) {
            if (!file.isFile()) {
                throw new IOException(file + " is not a file");
            }
            doDelete(file);
        }
    }

    public static void doDelete(File file) throws IOException {
        if (!file.delete()) {
            throw new IOException("Unable to delete: " + file);
        }
    }

    public static boolean isSymlink(File file) throws IOException {
        String baseName = file.getName();
        String canonicalPathExceptBaseName =
                new File(file.getParentFile().getCanonicalFile(), baseName).getPath();
        return !file.getCanonicalPath().equals(canonicalPathExceptBaseName);
    }

    public static void deleteRecursive(File toDelete) throws IOException {
        if (toDelete.isDirectory()) {
            for (File file : toDelete.listFiles()) {
                if (file.isDirectory() && !FileUtils.isSymlink(file)) {
                    // The isSymlink() check is important so that we don't delete files in other
                    // directories: only the symlink itself.
                    deleteRecursive(file);
                } else {
                    // Delete symlinks to directories or files.
                    FileUtils.doDelete(file);
                }
            }
            String[] remainingFiles = toDelete.list();
            if (remainingFiles.length != 0) {
                throw new IOException("Unable to delete files: " + Arrays
                        .toString(remainingFiles));
            }
        }
        FileUtils.doDelete(toDelete);
    }

    public static boolean filesExist(File rootDir, String... fileNames) throws IOException {
        for (String fileName : fileNames) {
            File file = new File(rootDir, fileName);
            if (!file.exists()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Read all lines from a UTF-8 encoded file, returning them as a list of strings.
     */
    public static List<String> readLines(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try (BufferedReader fileReader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8));
        ) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = fileReader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        }
    }
}
