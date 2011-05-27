/*
 * Copyright (C) 2011 The Android Open Source Project
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

public class FileOutputStreamTest extends TestCase {
    public void testFileDescriptorOwnership() throws Exception {
        File tmp = File.createTempFile("FileOutputStreamTest", "tmp");
        FileOutputStream fos1 = new FileOutputStream(tmp);
        FileOutputStream fos2 = new FileOutputStream(fos1.getFD());
        fos2.close();
        // The underlying FileDescriptor shouldn't be closed, so this should succeed.
        fos1.write(1);
        fos1.close();
        try {
            // This should fail.
            fos1.write(1);
            fail();
        } catch (IOException expected) {
        }
    }

    public void testClose() throws Exception {
        FileOutputStream fos = new FileOutputStream(File.createTempFile("FileOutputStreamTest", "tmp"));

        // Closing an already-closed stream is a no-op...
        fos.close();
        // ...as is flushing...
        fos.flush();

        // ...but any explicit write is an error.
        byte[] bytes = "hello".getBytes();
        try {
            fos.write(bytes);
            fail();
        } catch (IOException expected) {
        }
        try {
            fos.write(bytes, 0, 2);
            fail();
        } catch (IOException expected) {
        }
        try {
            fos.write(42);
            fail();
        } catch (IOException expected) {
        }
    }
}
