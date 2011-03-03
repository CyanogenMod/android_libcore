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

package libcore.java.lang;

import junit.framework.TestCase;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;

public class SystemTest extends TestCase {

    public void testLineSeparator() throws Exception {
        try {
            // Before Java 7, the small number of classes that wanted the line separator would
            // use System.getProperty. Now they should use System.lineSeparator instead, and the
            // "line.separator" property has no effect after the VM has started.

            // Test System.lineSeparator directly.
            assertEquals("\n", System.lineSeparator());
            System.setProperty("line.separator", "poop");
            assertEquals("\n", System.lineSeparator());
            assertFalse(System.lineSeparator().equals(System.getProperty("line.separator")));

            // java.io.BufferedWriter --- uses System.lineSeparator on Android but not on RI.
            StringWriter sw = new StringWriter();
            BufferedWriter bw = new BufferedWriter(sw);
            bw.newLine();
            bw.flush();
            assertEquals(System.lineSeparator(), sw.toString());
            assertFalse(System.lineSeparator().equals(System.getProperty("line.separator")));

            // java.io.PrintStream --- uses System.lineSeparator on Android but not on RI.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new PrintStream(baos).println();
            assertEquals(System.lineSeparator(), new String(baos.toByteArray(), "UTF-8"));
            assertFalse(System.lineSeparator().equals(System.getProperty("line.separator")));

            // java.io.PrintWriter --- uses System.lineSeparator on Android but not on RI.
            sw = new StringWriter();
            new PrintWriter(sw).println();
            assertEquals(System.lineSeparator(), sw.toString());
            assertFalse(System.lineSeparator().equals(System.getProperty("line.separator")));

            // java.util.Formatter --- uses System.lineSeparator on both.
            assertEquals(System.lineSeparator(), new Formatter().format("%n").toString());
            assertFalse(System.lineSeparator().equals(System.getProperty("line.separator")));
        } finally {
            System.setProperty("line.separator", "\n");
        }
    }

    public void testArrayCopyTargetNotArray() {
        try {
            System.arraycopy(new char[5], 0, "Hello", 0, 3);
            fail();
        } catch (ArrayStoreException e) {
            assertEquals("source and destination must be arrays, but were "
                    + "[C and Ljava/lang/String;", e.getMessage());
        }
    }

    public void testArrayCopySourceNotArray() {
        try {
            System.arraycopy("Hello", 0, new char[5], 0, 3);
            fail();
        } catch (ArrayStoreException e) {
            assertEquals("source and destination must be arrays, but were "
                    + "Ljava/lang/String; and [C", e.getMessage());
        }
    }

    public void testArrayCopyArrayTypeMismatch() {
        try {
            System.arraycopy(new char[5], 0, new Object[5], 0, 3);
            fail();
        } catch (ArrayStoreException e) {
            assertEquals("source and destination arrays are incompatible: "
                    + "[C and [Ljava/lang/Object;", e.getMessage());
        }
    }

    public void testArrayCopyElementTypeMismatch() {
        try {
            System.arraycopy(new Object[] { null, 5, "hello" }, 0,
                    new Integer[] { 1, 2, 3, null, null }, 0, 3);
            fail();
        } catch (ArrayStoreException e) {
            assertEquals("source[2] of type Ljava/lang/String; cannot be "
                    + "stored in destination array of type [Ljava/lang/Integer;",
                    e.getMessage());
        }
    }

    /**
     * http://b/issue?id=2136462
     */
    public void testBackFromTheDead() {
        try {
            new ConstructionFails();
        } catch (AssertionError expected) {
        }

        for (int i = 0; i < 20; i++) {
            if (ConstructionFails.INSTANCE != null) {
                fail("finalize() called, even though constructor failed!");
            }

            induceGc(i);
        }
    }

    private void induceGc(int rev) {
        System.gc();
        try {
            byte[] b = new byte[1024 << rev];
        } catch (OutOfMemoryError e) {
        }
    }

    static class ConstructionFails {
        private static ConstructionFails INSTANCE;

        ConstructionFails() {
            throw new AssertionError();
        }

        @Override protected void finalize() throws Throwable {
            INSTANCE = this;
            new AssertionError("finalize() called, even though constructor failed!")
                    .printStackTrace();
        }
    }
}
