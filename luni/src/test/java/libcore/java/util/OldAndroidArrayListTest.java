/*
 * Copyright (C) 2008 The Android Open Source Project
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

package libcore.java.util;

import junit.framework.TestCase;
import libcore.io.Streams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * This test case tests several often used functionality of ArrayLists.
 */
public class OldAndroidArrayListTest extends TestCase {

    @SuppressWarnings("unchecked")
    public void testArrayList() throws Exception {
        ArrayList array = new ArrayList();
        assertEquals(0, array.size());
        assertTrue(array.isEmpty());

        array.add(new Integer(0));
        array.add(0, new Integer(1));
        array.add(1, new Integer(2));
        array.add(new Integer(3));
        array.add(new Integer(1));

        assertEquals(5, array.size());
        assertFalse(array.isEmpty());

        assertEquals(1, ((Integer) array.get(0)).intValue());
        assertEquals(2, ((Integer) array.get(1)).intValue());
        assertEquals(0, ((Integer) array.get(2)).intValue());
        assertEquals(3, ((Integer) array.get(3)).intValue());
        assertEquals(1, ((Integer) array.get(4)).intValue());

        assertFalse(array.contains(null));
        assertTrue(array.contains(new Integer(2)));
        assertEquals(0, array.indexOf(new Integer(1)));
        assertEquals(4, array.lastIndexOf(new Integer(1)));
        assertTrue(array.indexOf(new Integer(5)) < 0);
        assertTrue(array.lastIndexOf(new Integer(5)) < 0);

        array.remove(1);
        array.remove(1);

        assertEquals(3, array.size());
        assertFalse(array.isEmpty());
        assertEquals(1, ((Integer) array.get(0)).intValue());
        assertEquals(3, ((Integer) array.get(1)).intValue());
        assertEquals(1, ((Integer) array.get(2)).intValue());

        assertFalse(array.contains(null));
        assertFalse(array.contains(new Integer(2)));
        assertEquals(0, array.indexOf(new Integer(1)));
        assertEquals(2, array.lastIndexOf(new Integer(1)));
        assertTrue(array.indexOf(new Integer(5)) < 0);
        assertTrue(array.lastIndexOf(new Integer(5)) < 0);

        array.clear();

        assertEquals(0, array.size());
        assertTrue(array.isEmpty());
        assertTrue(array.indexOf(new Integer(5)) < 0);
        assertTrue(array.lastIndexOf(new Integer(5)) < 0);

        ArrayList al = new ArrayList();

        assertFalse(al.remove(null));
        assertFalse(al.remove("string"));

        al.add("string");
        al.add(null);

        assertTrue(al.remove(null));
        assertTrue(al.remove("string"));
    }

    public static class JarOpenerThread extends Thread {
        private final JarFile file;
        private final ArrayList<String> entries;
        Exception thrown;

        public JarOpenerThread(JarFile file, ArrayList<String> entries) {
            this.file = file;
            this.entries = entries;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
            while (true) {
                ZipEntry je = file.getEntry(getEntryName());
                // byte[] entry = Streams.readFully(jf.getInputStream(je));
                try {
                    byte[] bytes = Streams.readFully(file.getInputStream(je));
                } catch (Exception e) {
                   throw new RuntimeException(e);
                }
            }
        }

        String getEntryName() {
            Random r = new Random(System.currentTimeMillis());
            return entries.get(r.nextInt(entries.size()));
        }
    }

    public void testGetResource() throws Exception {
        final JarFile jf = new JarFile("/data/local/tmp/Hangouts.jar");
        ArrayList<String> entryNames = new ArrayList<>();
        Enumeration<JarEntry> entries = jf.entries();
        while (entries.hasMoreElements()) {
            entryNames.add(entries.nextElement().getName());
        }
        Thread[] threads = new Thread[100];

        for (int i = 0; i < threads.length; ++i) {
            threads[i] = new JarOpenerThread(jf, entryNames);
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t: threads) {
            t.join();
        }

        for (Thread t : threads) {
            if (((JarOpenerThread) t).thrown != null) {
                throw ((JarOpenerThread) t).thrown;
            }
        }
    }
}

