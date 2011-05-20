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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.TestCase;
import libcore.java.util.SerializableTester;

public class URLTest extends TestCase {

    public void testUrlParts() throws Exception {
        URL url = new URL("http://username:password@host:8080/directory/file?query#ref");
        assertEquals("http", url.getProtocol());
        assertEquals("username:password@host:8080", url.getAuthority());
        assertEquals("/directory/file", url.getPath());
        assertEquals("ref", url.getRef());

        assertEquals("username:password", url.getUserInfo());
        assertEquals("host", url.getHost());
        assertEquals(8080, url.getPort());
        assertEquals("/directory/file?query", url.getFile());
        assertEquals("query", url.getQuery());

        assertEquals(80, url.getDefaultPort());
    }
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

    /**
     * Android's URL.equals() works as if the network is down. This is different
     * from the RI, which does potentially slow and inconsistent DNS lookups in
     * URL.equals.
     */
    public void testEqualsDoesNotDoHostnameResolution() throws Exception {
        for (InetAddress inetAddress : InetAddress.getAllByName("localhost")) {
            String address = inetAddress.getHostAddress();
            if (inetAddress instanceof Inet6Address) {
                address = "[" + address + "]";
            }
            URL urlByHostName = new URL("http://localhost/foo?bar=baz#quux");
            URL urlByAddress = new URL("http://" + address + "/foo?bar=baz#quux");
            assertFalse("Expected " + urlByHostName + " to not equal " + urlByAddress,
                    urlByHostName.equals(urlByAddress));
        }
    }

    public void testEqualsCaseMapping() throws Exception {
        assertEquals(new URL("HTTP://localhost/foo?bar=baz#quux"),
                new URL("HTTP://localhost/foo?bar=baz#quux"));
        assertTrue(new URL("http://localhost/foo?bar=baz#quux").equals(
                new URL("http://LOCALHOST/foo?bar=baz#quux")));
        assertFalse(new URL("http://localhost/foo?bar=baz#quux").equals(
                new URL("http://localhost/FOO?bar=baz#quux")));
        assertFalse(new URL("http://localhost/foo?bar=baz#quux").equals(
                new URL("http://localhost/foo?BAR=BAZ#quux")));
        assertFalse(new URL("http://localhost/foo?bar=baz#quux").equals(
                new URL("http://localhost/foo?bar=baz#QUUX")));
    }

    public void testEqualsWithNullHost() throws Exception {
        assertFalse(new URL("file", null, -1, "/a/").equals(new URL("file:/a/")));
        assertFalse(new URL("http", null, 80, "/a/").equals(new URL("http:/a/")));
    }

    public void testUrlSerialization() throws Exception {
        String s = "aced00057372000c6a6176612e6e65742e55524c962537361afce472030006490004706f72744c0"
                + "009617574686f726974797400124c6a6176612f6c616e672f537472696e673b4c000466696c65710"
                + "07e00014c0004686f737471007e00014c000870726f746f636f6c71007e00014c000372656671007"
                + "e00017870ffffffff74000e757365723a7061737340686f73747400102f706174682f66696c653f7"
                + "175657279740004686f7374740004687474707400046861736878";
        URL url = new URL("http://user:pass@host/path/file?query#hash");
        new SerializableTester<URL>(url, s).test();
    }

    /**
     * The serialized form of a URL includes its hash code. But the hash code
     * is not documented. Check that we don't return a deserialized hash code
     * from a deserialized value.
     */
    public void testUrlSerializationWithHashCode() throws Exception {
        String s = "aced00057372000c6a6176612e6e65742e55524c962537361afce47203000749000868617368436"
                + "f6465490004706f72744c0009617574686f726974797400124c6a6176612f6c616e672f537472696"
                + "e673b4c000466696c6571007e00014c0004686f737471007e00014c000870726f746f636f6c71007"
                + "e00014c000372656671007e00017870cdf0efacffffffff74000e757365723a7061737340686f737"
                + "47400102f706174682f66696c653f7175657279740004686f7374740004687474707400046861736"
                + "878";
        final URL url = new URL("http://user:pass@host/path/file?query#hash");
        new SerializableTester<URL>(url, s) {
            @Override protected void verify(URL deserialized) {
                assertEquals(url.hashCode(), deserialized.hashCode());
            }
        }.test();
    }

    public void testOnlySupportedProtocols() {
        try {
            new URL("abcd://host");
            fail();
        } catch (MalformedURLException expected) {
        }
    }

    // TODO: test resolve relative URL
}
