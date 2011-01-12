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

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import libcore.base.Streams;
import junit.framework.TestCase;

/**
 * Tests for the class {@link DexClassLoader}.
 */
@TestTargetClass(DexClassLoader.class)
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
     * Just a trivial test of construction.
     */
    public void test_init() {
        DexClassLoader dcl = new DexClassLoader(
                TMP_JAR.getAbsolutePath(),
                TMP_DIR.getAbsolutePath(),
                null,
                ClassLoader.getSystemClassLoader());
    }
}
