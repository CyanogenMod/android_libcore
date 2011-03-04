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

package libcore.java.net;

import java.net.URL;
import junit.framework.TestCase;

public class URLTest extends TestCase {
    // http://code.google.com/p/android/issues/detail?id=12724
    public void testExplicitPort() throws Exception {
        URL url = new URL("http://www.google.com:80/example?language[id]=2");
        assertEquals("www.google.com", url.getHost());
        assertEquals(80, url.getPort());
    }

    public void testHostWithSlashInFragment() throws Exception {
        URL url = new URL("http://www.google.com#foo/bar");
        assertEquals("www.google.com", url.getHost());
        assertEquals("foo/bar", url.getRef());
        assertEquals(-1, url.getPort());
    }

    public void testHostWithColonAndSlashInFragment() throws Exception {
        URL url = new URL("http://www.google.com#foo:bar/baz");
        assertEquals("www.google.com", url.getHost());
        assertEquals("foo:bar/baz", url.getRef());
        assertEquals(-1, url.getPort());
    }

    public void testEqualsWithNullHost() throws Exception {
        assertFalse(new URL("file", null, -1, "/a/").equals(new URL("file:/a/")));
        assertFalse(new URL("http", null, 80, "/a/").equals(new URL("http:/a/")));
    }
}
