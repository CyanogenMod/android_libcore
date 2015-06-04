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
 * limitations under the License.
 */

package libcore.io;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.UnknownServiceException;
import java.util.Arrays;

import tests.support.resource.Support_Resources;

public class ClassPathURLStreamHandlerTest extends TestCase {

    // A well formed jar file with 6 entries.
    private static final String JAR = "ClassPathURLStreamHandlerTest.jar";
    private static final String ENTRY_IN_ROOT = "root.txt";
    private static final String ENTRY_IN_SUBDIR = "foo/bar/baz.txt";
    private static final String ENTRY_WITH_SPACES_ENCODED = "file%20with%20spaces.txt";
    private static final String ENTRY_WITH_SPACES_UNENCODED = "file with spaces.txt";
    private static final String ENTRY_THAT_NEEDS_ESCAPING = "file_with_percent20_%20.txt";
    private static final String ENTRY_WITH_RELATIVE_PATH = "foo/../foo/bar/baz.txt";
    private static final String MISSING_ENTRY = "Wrong.resource";

    private File jarFile;

    @Override
    protected void setUp() {
        File resources = Support_Resources.createTempFolder();
        Support_Resources.copyFile(resources, null, JAR);
        jarFile = new File(resources, JAR);
    }

    public void testConstructor() throws Exception {
        try {
            ClassPathURLStreamHandler streamHandler = new ClassPathURLStreamHandler("Missing.file");
            fail("Should throw IOException");
        } catch (IOException expected) {
        }

        String fileName = jarFile.getCanonicalPath();
        ClassPathURLStreamHandler streamHandler = new ClassPathURLStreamHandler(fileName);
        streamHandler.close();
    }

    public void testGetEntryOrNull() throws Exception {
        String fileName = jarFile.getCanonicalPath();
        ClassPathURLStreamHandler streamHandler = new ClassPathURLStreamHandler(fileName);

        assertNotNull(streamHandler.getEntryUrlOrNull(ENTRY_IN_ROOT));
        assertNotNull(streamHandler.getEntryUrlOrNull(ENTRY_IN_SUBDIR));
        assertNotNull(streamHandler.getEntryUrlOrNull(ENTRY_WITH_SPACES_UNENCODED));
        assertNotNull(streamHandler.getEntryUrlOrNull(ENTRY_THAT_NEEDS_ESCAPING));

        // getEntryOrNull() performs an exact match on the entry name.
        assertNull(streamHandler.getEntryUrlOrNull(MISSING_ENTRY));
        assertNull(streamHandler.getEntryUrlOrNull("/" + ENTRY_IN_ROOT));
        assertNull(streamHandler.getEntryUrlOrNull("/" + ENTRY_IN_SUBDIR));
        assertNull(streamHandler.getEntryUrlOrNull(ENTRY_WITH_SPACES_ENCODED));
        assertNull(streamHandler.getEntryUrlOrNull(ENTRY_WITH_RELATIVE_PATH));
        streamHandler.close();
    }

    public void testOpenConnection() throws Exception {
        String fileName = jarFile.getCanonicalPath();
        ClassPathURLStreamHandler streamHandler = new ClassPathURLStreamHandler(fileName);

        assertOpenConnectionOk(jarFile, ENTRY_IN_ROOT, streamHandler);
        assertOpenConnectionOk(jarFile, ENTRY_IN_SUBDIR, streamHandler);
        assertOpenConnectionOk(jarFile, ENTRY_WITH_SPACES_ENCODED, streamHandler);
        assertOpenConnectionOk(jarFile, ENTRY_WITH_SPACES_UNENCODED, streamHandler);

        assertOpenConnectionConnectFails(jarFile, ENTRY_WITH_RELATIVE_PATH, streamHandler);
        assertOpenConnectionConnectFails(jarFile, MISSING_ENTRY, streamHandler);
        assertOpenConnectionConnectFails(jarFile, ENTRY_THAT_NEEDS_ESCAPING, streamHandler);

        streamHandler.close();
    }

    private void assertOpenConnectionConnectFails(
            File jarFile, String entryName, URLStreamHandler streamHandler) throws IOException {

        URL standardUrl = createJarUrl(jarFile, entryName, null /* use default stream handler */);
        try {
            standardUrl.openConnection().connect();
            fail();
        } catch (FileNotFoundException expected) {
        }

        URL actualUrl = createJarUrl(jarFile, entryName, streamHandler);
        try {
            actualUrl.openConnection().connect();
            fail();
        } catch (FileNotFoundException expected) {
        }
    }

    private static void assertOpenConnectionOk(File jarFile, String entryName,
            ClassPathURLStreamHandler streamHandler) throws IOException {
        URL standardUrl = createJarUrl(jarFile, entryName, null /* use default stream handler */);
        URLConnection standardUrlConnection = standardUrl.openConnection();
        assertNotNull(standardUrlConnection);

        URL actualUrl = createJarUrl(jarFile, entryName, streamHandler);
        URLConnection actualUrlConnection = actualUrl.openConnection();
        assertNotNull(actualUrlConnection);
        assertBehaviorSame(standardUrlConnection, actualUrlConnection);
    }

    private static void assertBehaviorSame(URLConnection standardURLConnection,
            URLConnection actualUrlConnection) throws IOException {

        JarURLConnection standardJarUrlConnection = (JarURLConnection) standardURLConnection;
        JarURLConnection actualJarUrlConnection = (JarURLConnection) actualUrlConnection;

        byte[] actualBytes = Streams.readFully(actualJarUrlConnection.getInputStream());
        byte[] standardBytes = Streams.readFully(standardJarUrlConnection.getInputStream());
        assertEquals(Arrays.toString(standardBytes), Arrays.toString(actualBytes));

        try {
            actualJarUrlConnection.getOutputStream();
            fail();
        } catch (UnknownServiceException expected) {
        }

        assertEquals(
                standardJarUrlConnection.getJarFile().getName(),
                actualJarUrlConnection.getJarFile().getName());

        assertEquals(
                standardJarUrlConnection.getJarEntry().getName(),
                actualJarUrlConnection.getJarEntry().getName());

        assertEquals(
                standardJarUrlConnection.getJarFileURL(),
                actualJarUrlConnection.getJarFileURL());

        assertEquals(
                standardJarUrlConnection.getContentType(),
                actualJarUrlConnection.getContentType());

        assertEquals(
                standardJarUrlConnection.getContentLength(),
                actualJarUrlConnection.getContentLength());
    }

    private static URL createJarUrl(File jarFile, String entryName, URLStreamHandler streamHandler)
            throws MalformedURLException {
        return new URL("jar", null, -1, jarFile.toURI() + "!/" + entryName, streamHandler);
    }

}
