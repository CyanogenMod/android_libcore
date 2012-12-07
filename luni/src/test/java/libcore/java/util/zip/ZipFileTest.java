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
import java.util.zip.CRC32;
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
        ZipFile zipFile = new ZipFile(createZipFile(1, originalSize));
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
        ZipInputStream in = new ZipInputStream(new FileInputStream(createZipFile(1, originalSize)));
        while (in.getNextEntry() != null) {
            while (in.read(readBuffer, 0, readBuffer.length) != -1) {}
        }
        in.close();
    }

    public void testZipFileWithLotsOfEntries() throws IOException {
        int expectedEntryCount = 64*1024 - 1;
        File f = createZipFile(expectedEntryCount, 0);
        ZipFile zipFile = new ZipFile(f);
        int entryCount = 0;
        for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements(); ) {
            ZipEntry zipEntry = e.nextElement();
            ++entryCount;
        }
        assertEquals(expectedEntryCount, entryCount);
        zipFile.close();
    }

    // http://code.google.com/p/android/issues/detail?id=36187
    public void testZipFileLargerThan2GiB() throws IOException {
        if (false) { // TODO: this test requires too much time and too much disk space!
            File f = createZipFile(1024, 3*1024*1024);
            ZipFile zipFile = new ZipFile(f);
            int entryCount = 0;
            for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements(); ) {
                ZipEntry zipEntry = e.nextElement();
                ++entryCount;
            }
            assertEquals(1024, entryCount);
            zipFile.close();
        }
    }

    public void testZip64Support() throws IOException {
        try {
            createZipFile(64*1024, 0);
            fail(); // Make this test more like testHugeZipFile when we have Zip64 support.
        } catch (ZipException expected) {
        }
    }

    /**
     * Compresses the given number of files, each of the given size, into a .zip archive.
     */
    private File createZipFile(int entryCount, int entrySize) throws IOException {
        File result = createTemporaryZipFile();

        byte[] writeBuffer = new byte[8192];
        Random random = new Random();

        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(result)));
        for (int entry = 0; entry < entryCount; ++entry) {
            ZipEntry ze = new ZipEntry(Integer.toHexString(entry));
            out.putNextEntry(ze);

            for (int i = 0; i < entrySize; i += writeBuffer.length) {
                random.nextBytes(writeBuffer);
                int byteCount = Math.min(writeBuffer.length, entrySize - i);
                out.write(writeBuffer, 0, byteCount);
            }

            out.closeEntry();
        }

        out.close();
        return result;
    }

    private File createTemporaryZipFile() throws IOException {
        File result = File.createTempFile("ZipFileTest", "zip");
        result.deleteOnExit();
        return result;
    }

    private ZipOutputStream createZipOutputStream(File f) throws IOException {
        return new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
    }

    public void testSTORED() throws IOException {
        ZipOutputStream out = createZipOutputStream(createTemporaryZipFile());
        CRC32 crc = new CRC32();

        // Missing CRC, size, and compressed size => failure.
        try {
            ZipEntry ze = new ZipEntry("a");
            ze.setMethod(ZipEntry.STORED);
            out.putNextEntry(ze);
            fail();
        } catch (ZipException expected) {
        }

        // Missing CRC and compressed size => failure.
        try {
            ZipEntry ze = new ZipEntry("a");
            ze.setMethod(ZipEntry.STORED);
            ze.setSize(0);
            out.putNextEntry(ze);
            fail();
        } catch (ZipException expected) {
        }

        // Missing CRC and size => failure.
        try {
            ZipEntry ze = new ZipEntry("a");
            ze.setMethod(ZipEntry.STORED);
            ze.setSize(0);
            ze.setCompressedSize(0);
            out.putNextEntry(ze);
            fail();
        } catch (ZipException expected) {
        }

        // Missing size and compressed size => failure.
        try {
            ZipEntry ze = new ZipEntry("a");
            ze.setMethod(ZipEntry.STORED);
            ze.setCrc(crc.getValue());
            out.putNextEntry(ze);
            fail();
        } catch (ZipException expected) {
        }

        // Missing size is copied from compressed size.
        {
            ZipEntry ze = new ZipEntry("okay1");
            ze.setMethod(ZipEntry.STORED);
            ze.setCrc(crc.getValue());

            assertEquals(-1, ze.getSize());
            assertEquals(-1, ze.getCompressedSize());

            ze.setCompressedSize(0);

            assertEquals(-1, ze.getSize());
            assertEquals(0, ze.getCompressedSize());

            out.putNextEntry(ze);

            assertEquals(0, ze.getSize());
            assertEquals(0, ze.getCompressedSize());
        }

        // Missing compressed size is copied from size.
        {
            ZipEntry ze = new ZipEntry("okay2");
            ze.setMethod(ZipEntry.STORED);
            ze.setCrc(crc.getValue());

            assertEquals(-1, ze.getSize());
            assertEquals(-1, ze.getCompressedSize());

            ze.setSize(0);

            assertEquals(0, ze.getSize());
            assertEquals(-1, ze.getCompressedSize());

            out.putNextEntry(ze);

            assertEquals(0, ze.getSize());
            assertEquals(0, ze.getCompressedSize());
        }

        // Mismatched size and compressed size => failure.
        try {
            ZipEntry ze = new ZipEntry("a");
            ze.setMethod(ZipEntry.STORED);
            ze.setCrc(crc.getValue());
            ze.setCompressedSize(1);
            ze.setSize(0);
            out.putNextEntry(ze);
            fail();
        } catch (ZipException expected) {
        }

        // Everything present => success.
        ZipEntry ze = new ZipEntry("okay");
        ze.setMethod(ZipEntry.STORED);
        ze.setCrc(crc.getValue());
        ze.setSize(0);
        ze.setCompressedSize(0);
        out.putNextEntry(ze);

        out.close();
    }

    private String makeString(int count, String ch) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; ++i) {
            sb.append(ch);
        }
        return sb.toString();
    }

    public void testComment() throws Exception {
        String expectedFileComment = "1 \u0666 2";
        String expectedEntryComment = "a \u0666 b";

        File file = createTemporaryZipFile();
        ZipOutputStream out = createZipOutputStream(file);

        // Is file comment length checking done on bytes or characters? (Should be bytes.)
        out.setComment(null);
        out.setComment(makeString(0xffff, "a"));
        try {
            out.setComment(makeString(0xffff + 1, "a"));
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            out.setComment(makeString(0xffff, "\u0666"));
            fail();
        } catch (IllegalArgumentException expected) {
        }

        ZipEntry ze = new ZipEntry("a");

        // Is entry comment length checking done on bytes or characters? (Should be bytes.)
        ze.setComment(null);
        ze.setComment(makeString(0xffff, "a"));
        try {
            ze.setComment(makeString(0xffff + 1, "a"));
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            ze.setComment(makeString(0xffff, "\u0666"));
            fail();
        } catch (IllegalArgumentException expected) {
        }

        ze.setComment(expectedEntryComment);
        out.putNextEntry(ze);
        out.closeEntry();

        out.setComment(expectedFileComment);
        out.close();

        ZipFile zipFile = new ZipFile(file);
        // TODO: there's currently no API for reading the file comment --- strings(1) the file?
        assertEquals(expectedEntryComment, zipFile.getEntry("a").getComment());
        zipFile.close();
    }

    public void testNameLengthChecks() throws IOException {
        // Is entry name length checking done on bytes or characters?
        // Really it should be bytes, but the RI only checks characters at construction time.
        // Android does the same, because it's cheap...
        try {
            new ZipEntry((String) null);
            fail();
        } catch (NullPointerException expected) {
        }
        new ZipEntry(makeString(0xffff, "a"));
        try {
            new ZipEntry(makeString(0xffff + 1, "a"));
            fail();
        } catch (IllegalArgumentException expected) {
        }

        // ...but Android won't let you create a zip file with a truncated name.
        ZipOutputStream out = createZipOutputStream(createTemporaryZipFile());
        ZipEntry ze = new ZipEntry(makeString(0xffff, "\u0666"));
        try {
            out.putNextEntry(ze);
            fail(); // The RI fails this test; it just checks the character count at construction time.
        } catch (IllegalArgumentException expected) {
        }
        out.closeEntry();
        out.putNextEntry(new ZipEntry("okay")); // ZipOutputStream.close throws if you add nothing!
        out.close();
    }

    public void testCrc() throws IOException {
        ZipEntry ze = new ZipEntry("test");
        ze.setMethod(ZipEntry.STORED);
        ze.setSize(4);

        // setCrc takes a long, not an int, so -1 isn't a valid CRC32 (because it's 64 bits).
        try {
            ze.setCrc(-1);
        } catch (IllegalArgumentException expected) {
        }

        // You can set the CRC32 to 0xffffffff if you're slightly more careful though...
        ze.setCrc(0xffffffffL);
        assertEquals(0xffffffffL, ze.getCrc());

        // And it actually works, even though we use -1L to mean "no CRC set"...
        ZipOutputStream out = createZipOutputStream(createTemporaryZipFile());
        out.putNextEntry(ze);
        out.write(-1);
        out.write(-1);
        out.write(-1);
        out.write(-1);
        out.closeEntry();
        out.close();
    }
}
