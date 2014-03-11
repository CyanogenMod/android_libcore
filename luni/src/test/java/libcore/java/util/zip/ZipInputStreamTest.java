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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

import tests.support.resource.Support_Resources;

public final class ZipInputStreamTest extends TestCase {

    public void testShortMessage() throws IOException {
        byte[] data = "Hello World".getBytes("UTF-8");
        byte[] zipped = ZipOutputStreamTest.zip("short", data);
        assertEquals(Arrays.toString(data), Arrays.toString(unzip("short", zipped)));
    }

    public void testLongMessage() throws IOException {
        byte[] data = new byte[1024 * 1024];
        new Random().nextBytes(data);
        assertTrue(Arrays.equals(data, unzip("r", ZipOutputStreamTest.zip("r", data))));
    }

    public void testNullCharset() throws IOException {
        try {
            new ZipInputStream(new ByteArrayInputStream(new byte[1]), null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    // Tests that non-UTF8 encoded zip file entries can be interpreted. Relies on ZipOutputStream.
    public void testNonUtf8Encoding() throws IOException {
        Charset charset = Charset.forName("Cp437");
        String encodingDependentString = "\u00FB";
        assertEncodingDiffers(encodingDependentString, charset, StandardCharsets.US_ASCII,
                StandardCharsets.UTF_8);
        String name = "name" + encodingDependentString;
        String comment = "comment" + encodingDependentString;

        ByteArrayOutputStream bytesOutputStream = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bytesOutputStream, charset);
        ZipEntry writeEntry = new ZipEntry(name);
        writeEntry.setComment(comment);
        out.putNextEntry(writeEntry);
        out.write("FileContentsIrrelevant".getBytes());
        out.closeEntry();
        out.close();

        ByteArrayInputStream bytesInputStream =
                new ByteArrayInputStream(bytesOutputStream.toByteArray());
        ZipInputStream in = new ZipInputStream(bytesInputStream, StandardCharsets.US_ASCII);
        ZipEntry readEntry = in.getNextEntry();
        // Due to the way ZipInputStream works it never returns entry comments.
        assertNull("ZipInputStream must not retrieve comments", readEntry.getComment());
        assertFalse(readEntry.getName().equals(name));
        in.close();

        bytesInputStream = new ByteArrayInputStream(bytesOutputStream.toByteArray());
        in = new ZipInputStream(bytesInputStream, charset);
        readEntry = in.getNextEntry();
        // Because ZipInputStream never reads the central directory it never returns entry
        // comments or the file comment.
        assertNull("ZipInputStream must not retrieve comments", readEntry.getComment());
        assertEquals(name, readEntry.getName());
        in.close();
    }

    // Tests that UTF8 encoded zip file entries can be interpreted when the constructor is provided
    // with a non-UTF-8 encoding. Relies on ZipOutputStream.
    public void testUtf8EncodingOverridesConstructor() throws IOException {
        Charset charset = Charset.forName("Cp437");
        String encodingDependentString = "\u00FB";
        assertEncodingDiffers(encodingDependentString, charset, StandardCharsets.UTF_8);
        String name = "name" + encodingDependentString;
        String comment = "comment" + encodingDependentString;

        ByteArrayOutputStream bytesOutputStream = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bytesOutputStream, StandardCharsets.UTF_8);
        // The entry will be tagged as being UTF-8 encoded.
        ZipEntry writeEntry = new ZipEntry(name);
        writeEntry.setComment(comment);
        out.putNextEntry(writeEntry);
        out.write("FileContentsIrrelevant".getBytes());
        out.closeEntry();
        out.close();

        ByteArrayInputStream bytesInputStream =
                new ByteArrayInputStream(bytesOutputStream.toByteArray());
        ZipInputStream in = new ZipInputStream(bytesInputStream, charset);
        ZipEntry readEntry = in.getNextEntry();
        // Because ZipInputStream never reads the central directory it never returns entry
        // comments or the file comment.
        assertNull("ZipInputStream must not retrieve comments", readEntry.getComment());
        assertNotNull(readEntry);
        assertEquals(name, readEntry.getName());
        in.close();
    }

    /**
     * Asserts the byte encoding for the string is different for all the supplied character
     * sets.
     */
    private void assertEncodingDiffers(String string, Charset... charsets) {
        Set<List<Byte>> encodings = new HashSet<List<Byte>>();
        for (int i = 0; i < charsets.length; i++) {
            List<Byte> byteList = new ArrayList<Byte>();
            for (byte b : string.getBytes(charsets[i])) {
                byteList.add(b);
            }
            assertTrue("Encoding has been seen before", encodings.add(byteList));
        }
    }

    public static byte[] unzip(String name, byte[] bytes) throws IOException {
        ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(bytes));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ZipEntry entry = in.getNextEntry();
        assertEquals(name, entry.getName());

        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }

        assertNull(in.getNextEntry()); // There's only one entry in the Zip files we create.

        in.close();
        return out.toByteArray();
    }

    /**
     * Reference implementation allows reading of empty zip using a {@link ZipInputStream}.
     */
    public void testReadEmpty() throws IOException {
        InputStream emptyZipIn = Support_Resources.getStream("java/util/zip/EmptyArchive.zip");
        ZipInputStream in = new ZipInputStream(emptyZipIn);
        try {
            ZipEntry entry = in.getNextEntry();
            assertNull("An empty zip has no entries", entry);
        } finally {
            in.close();
        }
    }
}
