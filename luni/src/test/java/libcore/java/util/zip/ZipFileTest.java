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

package libcore.java.util.zip;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import junit.framework.TestCase;
import libcore.io.IoUtils;

public final class ZipFileTest extends TestCase {

    /**
     * Exercise Inflater's ability to refill the zlib's input buffer. As of this
     * writing, this buffer's max size is 64KiB compressed bytes. We'll write a
     * full megabyte of uncompressed data, which should be sufficient to exhaust
     * the buffer. http://b/issue?id=2734751
     */
    public void testInflatingFilesRequiringZipRefill() throws IOException {
        int originalSize = 1024 * 1024;
        byte[] readBuffer = new byte[8192];
        ZipFile zipFile = new ZipFile(createZipFile(originalSize));
        for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements(); ) {
            ZipEntry zipEntry = e.nextElement();
            assertTrue("This test needs >64 KiB of compressed data to exercise Inflater",
                    zipEntry.getCompressedSize() > (64 * 1024));
            InputStream is = zipFile.getInputStream(zipEntry);
            while (is.read(readBuffer, 0, readBuffer.length) != -1) {}
            is.close();
        }
        zipFile.close();
    }

    private static void replaceBytes(byte[] original, byte[] replacement, byte[] buffer) {
        // Gotcha here: original and replacement must be the same length
        assertEquals(original.length, replacement.length);
        boolean found;
        for(int i=0; i < buffer.length - original.length; i++) {
            found = false;
            if (buffer[i] == original[0]) {
                found = true;
                for (int j=0; j < original.length; j++) {
                    if (buffer[i+j] != original[j]) {
                        found = false;
                        break;
                    }
                }
            }
            if (found) {
                for (int j=0; j < original.length; j++) {
                    buffer[i+j] = replacement[j];
                }
            }
        }
    }

    /**
     * Make sure we don't fail silently for duplicate entries.
     * b/8219321
     */
    public void testDuplicateEntries() throws IOException {
        String entryName = "test_file_name1";
        String tmpName = "test_file_name2";

        // create the template data
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bytesOut);
        ZipEntry ze1 = new ZipEntry(tmpName);
        out.putNextEntry(ze1);
        out.closeEntry();
        ZipEntry ze2 = new ZipEntry(entryName);
        out.putNextEntry(ze2);
        out.closeEntry();
        out.close();

        // replace the bytes we don't like
        byte[] buf = bytesOut.toByteArray();
        replaceBytes(tmpName.getBytes(), entryName.getBytes(), buf);

        // write the result to a file
        File badZip = File.createTempFile("badzip", "zip");
        badZip.deleteOnExit();
        FileOutputStream outstream = new FileOutputStream(badZip);
        outstream.write(buf);
        outstream.close();

        // see if we can still handle it
        try {
            ZipFile bad = new ZipFile(badZip);
            fail();
        } catch (ZipException expected) {
        }
    }

    public void testInflatingStreamsRequiringZipRefill() throws IOException {
        int originalSize = 1024 * 1024;
        byte[] readBuffer = new byte[8192];
        ZipInputStream in = new ZipInputStream(new FileInputStream(createZipFile(originalSize)));
        while (in.getNextEntry() != null) {
            while (in.read(readBuffer, 0, readBuffer.length) != -1) {}
        }
        in.close();
    }

    /**
     * Compresses a single random file into a .zip archive.
     */
    private File createZipFile(int uncompressedSize) throws IOException {
        File result = File.createTempFile("ZipFileTest", "zip");
        result.deleteOnExit();

        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(result));
        ZipEntry entry = new ZipEntry("random");
        out.putNextEntry(entry);

        byte[] writeBuffer = new byte[8192];
        Random random = new Random();
        for (int i = 0; i < uncompressedSize; i += writeBuffer.length) {
            random.nextBytes(writeBuffer);
            out.write(writeBuffer, 0, Math.min(writeBuffer.length, uncompressedSize - i));
        }

        out.closeEntry();
        out.close();
        return result;
      }

      public void testHugeZipFile() throws IOException {
          int expectedEntryCount = 64*1024 - 1;
          File f = createHugeZipFile(expectedEntryCount);
          ZipFile zipFile = new ZipFile(f);
          int entryCount = 0;
          for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements(); ) {
              ZipEntry zipEntry = e.nextElement();
              ++entryCount;
          }
          assertEquals(expectedEntryCount, entryCount);
          zipFile.close();
      }

      public void testZip64Support() throws IOException {
          try {
              createHugeZipFile(64*1024);
              fail(); // Make this test more like testHugeZipFile when we have Zip64 support.
          } catch (ZipException expected) {
          }
      }

      /**
       * Compresses the given number of empty files into a .zip archive.
       */
      private File createHugeZipFile(int count) throws IOException {
          File result = File.createTempFile("ZipFileTest", "zip");
          result.deleteOnExit();

          ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(result)));
          for (int i = 0; i < count; ++i) {
              ZipEntry entry = new ZipEntry(Integer.toHexString(i));
              out.putNextEntry(entry);
              out.closeEntry();
          }

          out.close();
          return result;
      }
}
