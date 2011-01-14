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

package dalvik.system;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import libcore.base.Streams;
import junit.framework.TestCase;

/**
 * Tests for the class {@link DexClassLoader}.
 */
public class DexClassLoaderTest extends TestCase {
    private static final String PACKAGE_PATH = "dalvik/system/";
    private static final String JAR_NAME = "loading-test.jar";
    private static final File TMP_DIR =
        new File(System.getProperty("java.io.tmpdir"), "loading-test");
    private static final File TMP_JAR = new File(TMP_DIR, JAR_NAME);

    protected void setUp() throws IOException {
        TMP_DIR.mkdirs();

        ClassLoader cl = DexClassLoaderTest.class.getClassLoader();
        InputStream in = cl.getResourceAsStream(PACKAGE_PATH + JAR_NAME);
        FileOutputStream out = new FileOutputStream(TMP_JAR);

        Streams.copy(in, out);
        in.close();
        out.close();
    }

    /**
     * Helper to construct an instance to test.
     */
    static private DexClassLoader createInstance() {
        return new DexClassLoader(TMP_JAR.getAbsolutePath(),
                                  TMP_DIR.getAbsolutePath(),
                                  null,
                                  ClassLoader.getSystemClassLoader());
    }

    /**
     * Just a trivial test of construction. This one merely makes
     * sure that a valid construction doesn't fail; it doesn't try
     * to verify anything about the constructed instance. (Other
     * tests will do that.)
     */
    public void test_init() {
        createInstance();
    }

    /**
     * Check that a class in the jar file may be used successfully. In this
     * case, a trivial static method is called.
     */
    public void test_simpleUse()
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        DexClassLoader dcl = createInstance();
        Class c = dcl.loadClass("test.Test1");
        Method m = c.getMethod("test", (Class[]) null);
        String result = (String) m.invoke(null, (Object[]) null);

        assertSame("blort", result);
    }

    /**
     * Check that a resource in the jar file is retrievable and contains
     * the expected contents.
     */
    public void test_getResourceAsStream()
            throws IOException, UnsupportedEncodingException {
        DexClassLoader dcl = createInstance();
        InputStream in = dcl.getResourceAsStream("test/Resource1.txt");
        byte[] contents = Streams.readFully(in);
        String s = new String(contents, "UTF-8");

        assertEquals("Muffins are tasty!\n", s);
    }
}
