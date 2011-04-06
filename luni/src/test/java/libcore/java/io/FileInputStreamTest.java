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

package libcore.java.io;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import junit.framework.TestCase;
import libcore.io.IoUtils;
import libcore.io.Libcore;

public final class FileInputStreamTest extends TestCase {
    private static final int TOTAL_SIZE = 1024;
    private static final int SKIP_SIZE = 100;

    private static class DataFeeder extends Thread {
        private FileDescriptor mOutFd;

        public DataFeeder(FileDescriptor fd) {
            mOutFd = fd;
        }

        @Override
        public void run() {
            try {
                FileOutputStream fos = new FileOutputStream(mOutFd);
                try {
                    byte[] buffer = new byte[TOTAL_SIZE];
                    for (int i = 0; i < buffer.length; ++i) {
                        buffer[i] = (byte) i;
                    }
                    fos.write(buffer);
                } finally {
                    IoUtils.closeQuietly(fos);
                    IoUtils.close(mOutFd);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void verifyData(FileInputStream is, int start, int count) throws IOException {
        byte buffer[] = new byte[count];
        assertEquals(count, is.read(buffer));
        for (int i = 0; i < count; ++i) {
            assertEquals((byte) (i + start), buffer[i]);
        }
    }

    public void testSkipInPipes() throws Exception {
        FileDescriptor[] pipe = Libcore.os.pipe();
        DataFeeder feeder = new DataFeeder(pipe[1]);
        try {
            feeder.start();
            FileInputStream fis = new FileInputStream(pipe[0]);
            fis.skip(SKIP_SIZE);
            verifyData(fis, SKIP_SIZE, TOTAL_SIZE - SKIP_SIZE);
            assertEquals(-1, fis.read());
            feeder.join(1000);
            assertFalse(feeder.isAlive());
        } finally {
            IoUtils.closeQuietly(pipe[0]);
        }
    }

    public void testDirectories() throws Exception {
        try {
            new FileInputStream(".");
            fail();
        } catch (FileNotFoundException expected) {
        }
    }

    public void testFileDescriptorOwnership() throws Exception {
        File tmp = File.createTempFile("FileOutputStreamTest", "tmp");
        FileOutputStream fos = new FileOutputStream(tmp);
        fos.write(1);
        fos.write(1);
        fos.close();

        FileInputStream fis1 = new FileInputStream(tmp);
        FileInputStream fis2 = new FileInputStream(fis1.getFD());
        fis2.close();
        // The underlying FileDescriptor shouldn't be closed, so this should succeed.
        assertFalse(fis1.read() == -1);
        fis1.close();
        try {
            // This should fail.
            fis1.read();
            fail();
        } catch (IOException expected) {
        }
    }
}
