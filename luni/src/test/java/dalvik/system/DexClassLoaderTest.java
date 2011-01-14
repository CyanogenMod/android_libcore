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
    private static final String DEX_NAME = "loading-test.dex";
    private static final File TMP_DIR =
        new File(System.getProperty("java.io.tmpdir"), "loading-test");
    private static final File TMP_JAR = new File(TMP_DIR, JAR_NAME);
    private static final File TMP_DEX = new File(TMP_DIR, DEX_NAME);

    protected void setUp() throws IOException {
        TMP_DIR.mkdirs();

        ClassLoader cl = DexClassLoaderTest.class.getClassLoader();

        InputStream in = cl.getResourceAsStream(PACKAGE_PATH + JAR_NAME);
        FileOutputStream out = new FileOutputStream(TMP_JAR);
        Streams.copy(in, out);
        in.close();
        out.close();

        in = cl.getResourceAsStream(PACKAGE_PATH + DEX_NAME);
        out = new FileOutputStream(TMP_DEX);
        Streams.copy(in, out);
        in.close();
        out.close();
    }

    /**
     * Helper to construct an instance to test.
     *
     * @param useDex whether to use the raw dex file ({@code true}) or
     * a dex-in-jar file ({@code false})
     */
    private static DexClassLoader createInstance(boolean useDex) {
        File file = useDex ? TMP_DEX : TMP_JAR;

        return new DexClassLoader(file.getAbsolutePath(),
                                  TMP_DIR.getAbsolutePath(),
                                  null,
                                  ClassLoader.getSystemClassLoader());
    }

    /**
     * Helper to construct an instance to test, using the jar file as
     * the source, and call a named no-argument static method on a
     * named class.
     *
     * @param useDex whether to use the raw dex file ({@code true}) or
     * a dex-in-jar file ({@code false})
     */
    public static Object createInstanceAndCallStaticMethod(boolean useDex,
            String className, String methodName)
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        DexClassLoader dcl = createInstance(useDex);
        Class c = dcl.loadClass(className);
        Method m = c.getMethod(methodName, (Class[]) null);
        return m.invoke(null, (Object[]) null);
    }

    /*
     * Tests that are parametric with respect to whether to use a jar
     * file or a dex file as the source of the code
     */

    /**
     * Just a trivial test of construction. This one merely makes
     * sure that a valid construction doesn't fail; it doesn't try
     * to verify anything about the constructed instance. (Other
     * tests will do that.)
     */
    public static void test_init(boolean useDex) {
        createInstance(useDex);
    }

    /**
     * Check that a class in the jar file may be used successfully. In this
     * case, a trivial static method is called.
     */
    public static void test_simpleUse(boolean useDex) throws Exception {
        String result = (String)
            createInstanceAndCallStaticMethod(useDex, "test.Test1", "test");

        assertSame("blort", result);
    }

    /**
     * Check that a resource in the jar file is retrievable and contains
     * the expected contents.
     */
    public static void test_getResourceAsStream(boolean useDex)
            throws Exception {
        DexClassLoader dcl = createInstance(useDex);
        InputStream in = dcl.getResourceAsStream("test/Resource1.txt");
        byte[] contents = Streams.readFully(in);
        String s = new String(contents, "UTF-8");

        assertEquals("Muffins are tasty!\n", s);
    }

    /*
     * All the following tests are just pass-throughs to test code
     * that lives inside the loading-test dex/jar file.
     */

    public static void test_callStaticMethod(boolean useDex)
            throws Exception {
        createInstanceAndCallStaticMethod(
            useDex, "test.Test2", "test_callStaticMethod");
    }

    public static void test_getStaticVariable(boolean useDex)
            throws Exception {
        createInstanceAndCallStaticMethod(
            useDex, "test.Test2", "test_getStaticVariable");
    }

    public static void test_callInstanceMethod(boolean useDex)
            throws Exception {
        createInstanceAndCallStaticMethod(
            useDex, "test.Test2", "test_callInstanceMethod");
    }

    public static void test_getInstanceVariable(boolean useDex)
            throws Exception {
        createInstanceAndCallStaticMethod(
            useDex, "test.Test2", "test_getInstanceVariable");
    }

    /*
     * The rest of the file consists of the actual test methods, which
     * are all mostly just calls to the parametrically-defined tests
     * above.
     */

    public void test_jar_init() throws Exception {
        test_init(false);
    }

    public void test_jar_simpleUse() throws Exception {
        test_simpleUse(false);
    }

    public void test_jar_getResourceAsStream() throws Exception {
        test_getResourceAsStream(false);
    }

    public void test_jar_callStaticMethod() throws Exception {
        test_callStaticMethod(false);
    }

    public void test_jar_getStaticVariable() throws Exception {
        test_getStaticVariable(false);
    }

    public void test_jar_callInstanceMethod() throws Exception {
        test_callInstanceMethod(false);
    }

    public void test_jar_getInstanceVariable() throws Exception {
        test_getInstanceVariable(false);
    }

    public void test_dex_init() throws Exception {
        test_init(true);
    }

    public void test_dex_simpleUse() throws Exception {
        test_simpleUse(true);
    }

    /*
     * Note: No getResourceAsStream() test, since the dex file doesn't
     * have any resources.
     */
    // public void test_dex_getResourceAsStream()

    public void test_dex_callStaticMethod() throws Exception {
        test_callStaticMethod(true);
    }

    public void test_dex_getStaticVariable() throws Exception {
        test_getStaticVariable(true);
    }

    public void test_dex_callInstanceMethod() throws Exception {
        test_callInstanceMethod(true);
    }

    public void test_dex_getInstanceVariable() throws Exception {
        test_getInstanceVariable(true);
    }
}
