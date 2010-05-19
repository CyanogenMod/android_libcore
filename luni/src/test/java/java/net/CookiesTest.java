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

package java.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import tests.http.MockResponse;
import tests.http.MockWebServer;
import tests.http.RecordedRequest;

public class CookiesTest extends TestCase {

    /**
     * The Netscape cookie spec is officially obsolete, but widely used in practice. It's awkward to
     * parse because the expires header includes a comma which is supposed to only be used for
     * listing multiple values of a header on a single line. The use of an explicit date is also
     * unloved because it is vulnerable to clock skew.
     */
    public void testNetscapeResponse() throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        MockWebServer server = new MockWebServer();
        server.play();

        server.enqueue(new MockResponse().addHeader("Set-Cookie: a=apple; "
                + "expires=Fri, 31-Dec-9999 23:59:59 GMT; "
                + "path=/path; "
                + "domain=localhost; "
                + "secure"));
        get(server);

        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        assertEquals(1, cookies.size());
        HttpCookie cookie = cookies.get(0);
        assertEquals("a", cookie.getName());
        assertEquals("apple", cookie.getValue());
        assertEquals(null, cookie.getComment());
        assertEquals(null, cookie.getCommentURL());
        assertEquals(false, cookie.getDiscard());
        assertEquals("localhost", cookie.getDomain());
        assertTrue(cookie.getMaxAge() > 100000000000L);
        assertEquals("/path", cookie.getPath());
        assertEquals(true, cookie.getSecure());
        assertEquals(0, cookie.getVersion());
    }

    /**
     * RFC 2109 formalizes the Netscape cookie spec. It replaces the {@code expires} attribute with
     * {@code Max-Age} and adds {@code Comment} and {@code Version} attributes.
     */
    public void testRfc2109Response() throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        MockWebServer server = new MockWebServer();
        server.play();

        server.enqueue(new MockResponse().addHeader("Set-Cookie: a=apple; "
                + "Comment=this cookie is delicious; "
                + "Domain=localhost; "
                + "Max-Age=60; "
                + "Path=/path; "
                + "Secure; "
                + "Version=1"));
        get(server);

        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        assertEquals(1, cookies.size());
        HttpCookie cookie = cookies.get(0);
        assertEquals("a", cookie.getName());
        assertEquals("apple", cookie.getValue());
        assertEquals("this cookie is delicious", cookie.getComment());
        assertEquals(null, cookie.getCommentURL());
        assertEquals(false, cookie.getDiscard());
        assertEquals("localhost", cookie.getDomain());
        assertEquals(60, cookie.getMaxAge());
        assertEquals("/path", cookie.getPath());
        assertEquals(true, cookie.getSecure());
        assertEquals(1, cookie.getVersion());
    }

    /**
     * RFC 2965 refines RFC 2109. It adds {@code Discard}, {@code Port}, and {@code CommentURL}
     * attributes and renames the header from {@code Set-Cookie} to {@code Set-Cookie2}.
     */
    public void testRfc2965Response() throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        MockWebServer server = new MockWebServer();
        server.play();

        server.enqueue(new MockResponse().addHeader("Set-Cookie2: a=apple; "
                + "Comment=this cookie is delicious; "
                + "CommentURL=\"http://google.com/\"; "
                + "Discard; "
                + "Domain=localhost; "
                + "Max-Age=60; "
                + "Path=/path; "
                + "Port=\"80,443," + server.getPort() + "\"; "
                + "Secure; "
                + "Version=1"));
        get(server);

        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        assertEquals(1, cookies.size());
        HttpCookie cookie = cookies.get(0);
        assertEquals("a", cookie.getName());
        assertEquals("apple", cookie.getValue());
        assertEquals("this cookie is delicious", cookie.getComment());
        assertEquals("http://google.com/", cookie.getCommentURL());
        assertEquals(true, cookie.getDiscard());
        assertEquals("localhost", cookie.getDomain());
        assertEquals(60, cookie.getMaxAge());
        assertEquals("/path", cookie.getPath());
        assertEquals("80,443," + server.getPort(), cookie.getPortlist());
        assertEquals(true, cookie.getSecure());
        assertEquals(1, cookie.getVersion());
    }

    public void testResponseWithMultipleCookieHeaderLines() throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        MockWebServer server = new MockWebServer();
        server.play();

        server.enqueue(new MockResponse()
                .addHeader("Set-Cookie: a=apple")
                .addHeader("Set-Cookie: b=banana"));
        get(server);

        List<HttpCookie> cookies = sortedCopy(cookieManager.getCookieStore().getCookies());
        assertEquals(2, cookies.size());
        HttpCookie cookieA = cookies.get(0);
        assertEquals("a", cookieA.getName());
        assertEquals("apple", cookieA.getValue());
        HttpCookie cookieB = cookies.get(1);
        assertEquals("b", cookieB.getName());
        assertEquals("banana", cookieB.getValue());
    }

    public void testResponseWithMultipleCookiesInOneHeaderLine() throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        MockWebServer server = new MockWebServer();
        server.play();

        server.enqueue(new MockResponse().addHeader("Set-Cookie: "
                + "a=apple, "
                + "b=banana"));
        get(server);

        List<HttpCookie> cookies = sortedCopy(cookieManager.getCookieStore().getCookies());
        assertEquals(2, cookies.size());
        HttpCookie cookieA = cookies.get(0);
        assertEquals("a", cookieA.getName());
        assertEquals("apple", cookieA.getValue());
        HttpCookie cookieB = cookies.get(1);
        assertEquals("b", cookieB.getName());
        assertEquals("banana", cookieB.getValue());
    }

    public void testResponseWithMultipleNetscapeCookiesInOneHeaderLine() throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        MockWebServer server = new MockWebServer();
        server.play();

        server.enqueue(new MockResponse().addHeader("Set-Cookie: "
                + "a=apple; expires=Fri, 31-Dec-9999 23:59:59 GMT; "
                + "b=banana; expires=Fri, 31-Dec-9999 23:59:59 GMT"));
        get(server);

        List<HttpCookie> cookies = sortedCopy(cookieManager.getCookieStore().getCookies());
        assertEquals(2, cookies.size());
        HttpCookie cookieA = cookies.get(0);
        assertEquals("a", cookieA.getName());
        assertEquals("apple", cookieA.getValue());
        HttpCookie cookieB = cookies.get(1);
        assertEquals("b", cookieB.getName());
        assertEquals("banana", cookieB.getValue());
    }

    public void testSendingCookiesFromStore() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse());
        server.play();

        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpCookie cookieA = new HttpCookie("a", "apple");
        cookieA.setDomain("localhost");
        cookieA.setPath("/");
        cookieManager.getCookieStore().add(server.getUrl("/").toURI(), cookieA);
        HttpCookie cookieB = new HttpCookie("b", "banana");
        cookieB.setDomain("localhost");
        cookieB.setPath("/");
        cookieManager.getCookieStore().add(server.getUrl("/").toURI(), cookieB);
        CookieHandler.setDefault(cookieManager);

        get(server);
        RecordedRequest request = server.takeRequest();

        List<String> receivedHeaders = request.getHeaders();
        assertContains(receivedHeaders, "Cookie: $Version=\"1\"; "
                + "a=\"apple\";$Path=\"/\";$Domain=\"localhost\"; "
                + "b=\"banana\";$Path=\"/\";$Domain=\"localhost\"");
    }

    /**
     * Test which headers show up where. The cookie manager should be notified of both
     * user-specified and derived headers like {@code Content-Length}. Headers named {@code Cookie}
     * or {@code Cookie2} that are returned by the cookie manager should show up in the request and
     * in {@code getRequestProperties}.
     */
    public void testHeadersSentToCookieHandler() throws IOException, InterruptedException {
        final Map<String, List<String>> cookieHandlerHeaders = new HashMap<String, List<String>>();
        CookieHandler.setDefault(new CookieManager() {
            @Override public Map<String, List<String>> get(URI uri,
                    Map<String, List<String>> requestHeaders) throws IOException {
                cookieHandlerHeaders.putAll(requestHeaders);
                Map<String, List<String>> result = new HashMap<String, List<String>>();
                result.put("Cookie", Collections.singletonList("Bar=bar"));
                result.put("Cookie2", Collections.singletonList("Baz=baz"));
                result.put("Quux", Collections.singletonList("quux"));
                return result;
            }
        });
        MockWebServer server = new MockWebServer();
        server.play();

        server.enqueue(new MockResponse());
        HttpURLConnection connection = (HttpURLConnection) server.getUrl("/").openConnection();
        assertEquals(Collections.<String, List<String>>emptyMap(),
                connection.getRequestProperties());

        connection.setRequestProperty("Foo", "foo");
        connection.setDoOutput(true);
        connection.getOutputStream().write(5);
        connection.getOutputStream().close();
        connection.getInputStream().close();

        RecordedRequest request = server.takeRequest();

        assertContainsAll(cookieHandlerHeaders.keySet(), "Foo");
        assertContainsAll(cookieHandlerHeaders.keySet(),
                "Content-Type", "Content-Length", "User-Agent", "Connection", "Host");
        assertFalse(cookieHandlerHeaders.containsKey("Cookie"));

        /*
         * The API specifies that calling getRequestProperties() on a connected instance should fail
         * with an IllegalStateException, but the RI violates the spec and returns a valid map.
         * http://www.mail-archive.com/net-dev@openjdk.java.net/msg01768.html
         */
        try {
            assertContainsAll(connection.getRequestProperties().keySet(), "Foo");
            assertContainsAll(connection.getRequestProperties().keySet(),
                    "Content-Type", "Content-Length", "User-Agent", "Connection", "Host");
            assertContainsAll(connection.getRequestProperties().keySet(), "Cookie", "Cookie2");
            assertFalse(connection.getRequestProperties().containsKey("Quux"));
        } catch (IllegalStateException expected) {
        }

        assertContainsAll(request.getHeaders(), "Foo: foo", "Cookie: Bar=bar", "Cookie2: Baz=baz");
        assertFalse(request.getHeaders().contains("Quux: quux"));
    }

    public void testCookiesSentIgnoresCase() throws Exception {
        CookieHandler.setDefault(new CookieManager() {
            @Override public Map<String, List<String>> get(URI uri,
                    Map<String, List<String>> requestHeaders) throws IOException {
                Map<String, List<String>> result = new HashMap<String, List<String>>();
                result.put("COOKIE", Collections.singletonList("Bar=bar"));
                result.put("cooKIE2", Collections.singletonList("Baz=baz"));
                return result;
            }
        });
        MockWebServer server = new MockWebServer();
        server.play();

        server.enqueue(new MockResponse());
        get(server);

        RecordedRequest request = server.takeRequest();
        assertContainsAll(request.getHeaders(), "COOKIE: Bar=bar", "cooKIE2: Baz=baz");
        assertFalse(request.getHeaders().contains("Quux: quux"));
    }

    private void assertContains(Collection<String> collection, String element) {
        for (String c : collection) {
            if (c.equalsIgnoreCase(element)) {
                return;
            }
        }
        fail("No " + element + " in " + collection);
    }

    private void assertContainsAll(Collection<String> collection, String... toFind) {
        for (String s : toFind) {
            assertContains(collection, s);
        }
    }

    private List<HttpCookie> sortedCopy(List<HttpCookie> cookies) {
        List<HttpCookie> result = new ArrayList<HttpCookie>(cookies);
        Collections.sort(result, new Comparator<HttpCookie>() {
            public int compare(HttpCookie a, HttpCookie b) {
                return a.getName().compareTo(b.getName());
            }
        });
        return result;
    }

    private Map<String,List<String>> get(MockWebServer server) throws Exception {
        URLConnection connection = server.getUrl("/").openConnection();
        Map<String, List<String>> headers = connection.getHeaderFields();
        connection.getInputStream().close();
        return headers;
    }
}
