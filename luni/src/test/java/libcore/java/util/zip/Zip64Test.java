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

package libcore.java.util.zip;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Zip64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

public class Zip64Test extends TestCase {

    // We shouldn't attempt to look inside the extended info if we have valid fields
    // in the regular file header / central directory entry.
    public void testParseZip64ExtendedInfo_noFieldsPresent() throws Exception {
        ZipEntry ze = createZipEntry(null, 100, 200, ZipEntry.STORED, 300);
        Zip64.parseZip64ExtendedInfo(ze, false /* fromCentralDirectory */);
        Zip64.parseZip64ExtendedInfo(ze, true /* fromCentralDirectory */);
    }

    // We *should* attempt to look in the extended info if the local file header / central
    // directory entry don't have the correct values.
    public void testParseZip64ExtendedInfo_missingExtendedInfo() throws Exception {
        ZipEntry ze = createZipEntry(null, Zip64.MAX_ZIP_ENTRY_AND_ARCHIVE_SIZE,
                Zip64.MAX_ZIP_ENTRY_AND_ARCHIVE_SIZE, ZipEntry.STORED, 300);
        try {
            Zip64.parseZip64ExtendedInfo(ze, false /* fromCentralDirectory */);
            fail();
        } catch (ZipException expected) {
        }

        try {
            Zip64.parseZip64ExtendedInfo(ze, true /* fromCentralDirectory */);
            fail();
        } catch (ZipException expected) {
        }
    }

    // Test the case where the compressed / uncompressed sizes are in the extended info
    // but the header offset isn't.
    public void testParseZip64ExtendedInfo_partialInfo() throws Exception {
        byte[] extras = new byte[20];
        ByteBuffer buf = ByteBuffer.wrap(extras);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) 0x0001);
        buf.putShort((short) 16);
        buf.putLong(50);
        buf.putLong(100);

        ZipEntry ze = createZipEntry(extras, Zip64.MAX_ZIP_ENTRY_AND_ARCHIVE_SIZE,
                Zip64.MAX_ZIP_ENTRY_AND_ARCHIVE_SIZE, ZipEntry.STORED, 300);

        Zip64.parseZip64ExtendedInfo(ze, false /*fromCentralDirectory */);
        assertEquals(50, ze.getSize());
        assertEquals(100, ze.getCompressedSize());

        ze = createZipEntry(extras, Zip64.MAX_ZIP_ENTRY_AND_ARCHIVE_SIZE,
                Zip64.MAX_ZIP_ENTRY_AND_ARCHIVE_SIZE, ZipEntry.STORED, 300);
        Zip64.parseZip64ExtendedInfo(ze, true /*fromCentralDirectory */);
        assertEquals(50, ze.getSize());
        assertEquals(100, ze.getCompressedSize());
    }

    public void testInsertZip64ExtendedInfo() throws Exception {
        ZipEntry ze = createZipEntry(null, Zip64.MAX_ZIP_ENTRY_AND_ARCHIVE_SIZE + 300,
                Zip64.MAX_ZIP_ENTRY_AND_ARCHIVE_SIZE + 500, ZipEntry.STORED, 300);
        Zip64.insertZip64ExtendedInfoToExtras(ze);

        assertNotNull(ze.getExtra());
        ByteBuffer buf = ByteBuffer.wrap(ze.getExtra());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x0001, buf.getShort());
        assertEquals(24, buf.getShort());
        assertEquals(Zip64.MAX_ZIP_ENTRY_AND_ARCHIVE_SIZE + 300, buf.getLong());
        assertEquals(Zip64.MAX_ZIP_ENTRY_AND_ARCHIVE_SIZE + 500, buf.getLong());
    }

    private static ZipEntry createZipEntry(byte[] extras, long size, long compressedSize,
                                           int compressionMethod, long headerOffset) {
        return new ZipEntry("name", "comment", 42 /* crc */, compressedSize, size,
                compressionMethod, 42 /* time */, 42 /* modDate */, extras, headerOffset,
                42 /* data offset */);
    }
}
