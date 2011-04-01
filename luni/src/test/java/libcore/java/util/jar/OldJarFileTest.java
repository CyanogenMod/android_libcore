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
package libcore.java.util.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Permission;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import junit.framework.TestCase;
import tests.support.resource.Support_Resources;

public class OldJarFileTest extends TestCase {

    private final String jarName = "hyts_patch.jar"; // a 'normal' jar file
    private final String entryName = "foo/bar/A.class";
    private File resources;

    // custom security manager
    SecurityManager sm = new SecurityManager() {
        final String forbidenPermissionName = "user.dir";

        public void checkPermission(Permission perm) {
            if (perm.getName().equals(forbidenPermissionName)) {
                throw new SecurityException();
            }
        }
    };

    @Override
    protected void setUp() {
        resources = Support_Resources.createTempFolder();
    }

    /**
     * java.util.jar.JarFile#JarFile(java.io.File)
     */
    public void test_ConstructorLjava_io_File() throws IOException {
        try {
            new JarFile(new File("Wrong.file"));
            fail("Should throw IOException");
        } catch (IOException e) {
            // expected
        }
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new JarFile(new File("tmp.jar"));
            fail("Should throw SecurityException");
        } catch (IOException e) {
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }

        Support_Resources.copyFile(resources, null, jarName);
        new JarFile(new File(resources, jarName));
    }

    /**
     * java.util.jar.JarFile#JarFile(java.lang.String)
     */
    public void test_ConstructorLjava_lang_String() throws IOException {
        try {
            new JarFile("Wrong.file");
            fail("Should throw IOException");
        } catch (IOException e) {
            // expected
        }
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new JarFile("tmp.jar");
            fail("Should throw SecurityException");
        } catch (IOException e) {
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }

        Support_Resources.copyFile(resources, null, jarName);
        String fileName = (new File(resources, jarName)).getCanonicalPath();
        new JarFile(fileName);
    }

    /**
     * java.util.jar.JarFile#JarFile(java.lang.String, boolean)
     */
    public void test_ConstructorLjava_lang_StringZ() throws IOException {
        try {
            new JarFile("Wrong.file", false);
            fail("Should throw IOException");
        } catch (IOException e) {
            // expected
        }
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new JarFile("tmp.jar", true);
            fail("Should throw SecurityException");
        } catch (IOException e) {
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }

        Support_Resources.copyFile(resources, null, jarName);
        String fileName = (new File(resources, jarName)).getCanonicalPath();
        new JarFile(fileName, true);
    }

    /**
     * java.util.jar.JarFile#JarFile(java.io.File, boolean)
     */
    public void test_ConstructorLjava_io_FileZ() throws IOException {
        try {
            new JarFile(new File("Wrong.file"), true);
            fail("Should throw IOException");
        } catch (IOException e) {
            // expected
        }
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new JarFile(new File("tmp.jar"), false);
            fail("Should throw SecurityException");
        } catch (IOException e) {
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }

        Support_Resources.copyFile(resources, null, jarName);
        new JarFile(new File(resources, jarName), false);
    }

    /**
     * java.util.jar.JarFile#JarFile(java.io.File, boolean, int)
     */
    public void test_ConstructorLjava_io_FileZI() {
        try {
            new JarFile(new File("Wrong.file"), true,
                    ZipFile.OPEN_READ);
            fail("Should throw IOException");
        } catch (IOException e) {
            // expected
        }
        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager(sm);
        try {
            new JarFile(new File("tmp.jar"), false,
                    ZipFile.OPEN_READ);
            fail("Should throw SecurityException");
        } catch (IOException e) {
            fail("Should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        } finally {
            System.setSecurityManager(oldSm);
        }

        try {
            Support_Resources.copyFile(resources, null, jarName);
            new JarFile(new File(resources, jarName), false,
                    ZipFile.OPEN_READ);
        } catch (IOException e) {
            fail("Should not throw IOException");
        }

        try {
            Support_Resources.copyFile(resources, null, jarName);
            new JarFile(new File(resources, jarName), false,
                    ZipFile.OPEN_READ | ZipFile.OPEN_DELETE + 33);
            fail("Should throw IllegalArgumentException");
        } catch (IOException e) {
            fail("Should not throw IOException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }



    public void test_close() throws IOException {
        String modifiedJarName = "Modified_SF_EntryAttributes.jar";
        Support_Resources.copyFile(resources, null, modifiedJarName);
        JarFile jarFile = new JarFile(new File(resources, modifiedJarName), true);
        jarFile.entries();

        jarFile.close();
        jarFile.close();

        // Can not check IOException
    }

    /**
     * @throws IOException
     * java.util.jar.JarFile#getInputStream(java.util.zip.ZipEntry)
     */
    public void test_getInputStreamLjava_util_jar_JarEntry() throws IOException {
        File localFile = null;
        try {
            Support_Resources.copyFile(resources, null, jarName);
            localFile = new File(resources, jarName);
        } catch (Exception e) {
            fail("Failed to create local file: " + e);
        }

        byte[] b = new byte[1024];
        try {
            JarFile jf = new JarFile(localFile);
            java.io.InputStream is = jf.getInputStream(jf.getEntry(entryName));
            // BEGIN android-removed
            // jf.close();
            // END android-removed
            assertTrue("Returned invalid stream", is.available() > 0);
            int r = is.read(b, 0, 1024);
            is.close();
            StringBuffer sb = new StringBuffer(r);
            for (int i = 0; i < r; i++) {
                sb.append((char) (b[i] & 0xff));
            }
            String contents = sb.toString();
            assertTrue("Incorrect stream read", contents.indexOf("bar") > 0);
            // BEGIN android-added
            jf.close();
            // END android-added
        } catch (Exception e) {
            fail("Exception during test: " + e.toString());
        }

        try {
            JarFile jf = new JarFile(localFile);
            InputStream in = jf.getInputStream(new JarEntry("invalid"));
            assertNull("Got stream for non-existent entry", in);
        } catch (Exception e) {
            fail("Exception during test 2: " + e);
        }

        try {
            Support_Resources.copyFile(resources, null, jarName);
            File signedFile = new File(resources, jarName);
            JarFile jf = new JarFile(signedFile);
            JarEntry jre = new JarEntry("foo/bar/A.class");
            jf.getInputStream(jre);
            // InputStream returned in any way, exception can be thrown in case
            // of reading from this stream only.
            // fail("Should throw ZipException");
        } catch (ZipException ee) {
            // expected
        }

        try {
            Support_Resources.copyFile(resources, null, jarName);
            File signedFile = new File(resources, jarName);
            JarFile jf = new JarFile(signedFile);
            JarEntry jre = new JarEntry("foo/bar/A.class");
            jf.close();
            jf.getInputStream(jre);
            // InputStream returned in any way, exception can be thrown in case
            // of reading from this stream only.
            // The same for IOException
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException ee) {
            // expected
        }
    }
}
