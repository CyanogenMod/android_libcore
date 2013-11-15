/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.tests.java.lang;

import junit.framework.TestCase;
import java.io.InputStream;

public class ClassLoaderTest extends TestCase {
    /**
     * java.lang.ClassLoader#getResource(java.lang.String)
     */
    public void test_getResourceLjava_lang_String() {
        // Test for method java.net.URL
        // java.lang.ClassLoader.getResource(java.lang.String)
        java.net.URL u = ClassLoader.getSystemClassLoader().getResource("hyts_Foo.c");
        assertNotNull("Unable to find resource", u);
        java.io.InputStream is = null;
        try {
            is = u.openStream();
            assertNotNull("Resource returned is invalid", is);
            is.close();
        } catch (java.io.IOException e) {
            fail("IOException getting stream for resource : " + e.getMessage());
        }
    }

    /**
     * java.lang.ClassLoader#getResourceAsStream(java.lang.String)
     */
    public void test_getResourceAsStreamLjava_lang_String() {
        // Test for method java.io.InputStream
        // java.lang.ClassLoader.getResourceAsStream(java.lang.String)
        // Need better test...

        java.io.InputStream is = null;
        assertNotNull("Failed to find resource: hyts_Foo.c", (is = ClassLoader
                .getSystemClassLoader().getResourceAsStream("hyts_Foo.c")));
        try {
            is.close();
        } catch (java.io.IOException e) {
            fail("Exception during getResourceAsStream: " + e.toString());
        }
    }

    /**
     * java.lang.ClassLoader#getSystemClassLoader()
     */
    public void test_getSystemClassLoader() {
        // Test for method java.lang.ClassLoader
        // java.lang.ClassLoader.getSystemClassLoader()
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        java.io.InputStream is = cl.getResourceAsStream("hyts_Foo.c");
        assertNotNull("Failed to find resource from system classpath", is);
        try {
            is.close();
        } catch (java.io.IOException e) {
        }

    }

    /**
     * java.lang.ClassLoader#getSystemResource(java.lang.String)
     */
    public void test_getSystemResourceLjava_lang_String() {
        // Test for method java.net.URL
        // java.lang.ClassLoader.getSystemResource(java.lang.String)
        // Need better test...
        assertNotNull("Failed to find resource: hyts_Foo.c", ClassLoader
                .getSystemResource("hyts_Foo.c"));
    }


    //Regression Test for JIRA-2047
    public void test_getResourceAsStream_withSharpChar() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(
                ClassTest.FILENAME);
        assertNotNull(in);
        in.close();
    }
}