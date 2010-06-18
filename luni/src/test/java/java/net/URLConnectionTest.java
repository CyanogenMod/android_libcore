/*
 * Copyright (C) 2009 The Android Open Source Project
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TestSSLContext;
import tests.http.MockResponse;
import tests.http.MockWebServer;
import tests.http.RecordedRequest;

public class URLConnectionTest extends junit.framework.TestCase {

    // Check that if we don't read to the end of a response, the next request on the
    // recycled connection doesn't get the unread tail of the first request's response.
    // http://code.google.com/p/android/issues/detail?id=2939
    public void test_2939() throws Exception {
        MockResponse response = new MockResponse().setChunkedBody("ABCDE\nFGHIJ\nKLMNO\nPQR", 8);

        MockWebServer server = new MockWebServer();
        server.enqueue(response);
        server.enqueue(response);
        server.play();

        assertContent("ABCDE", server.getUrl("/").openConnection(), 5);
        assertEquals(0, server.takeRequest().getSequenceNumber());
        assertContent("ABCDE", server.getUrl("/").openConnection(), 5);
        assertEquals(1, server.takeRequest().getSequenceNumber());
    }

    public void testConnectionsArePooled() throws Exception {
        MockResponse response = new MockResponse().setBody("ABCDEFGHIJKLMNOPQR");

        MockWebServer server = new MockWebServer();
        server.enqueue(response);
        server.enqueue(response);
        server.enqueue(response);
        server.play();

        assertContent("ABCDEFGHIJKLMNOPQR", server.getUrl("/").openConnection());
        assertEquals(0, server.takeRequest().getSequenceNumber());
        assertContent("ABCDEFGHIJKLMNOPQR", server.getUrl("/").openConnection());
        assertEquals(1, server.takeRequest().getSequenceNumber());
        assertContent("ABCDEFGHIJKLMNOPQR", server.getUrl("/").openConnection());
        assertEquals(2, server.takeRequest().getSequenceNumber());
    }

    public void testChunkedConnectionsArePooled() throws Exception {
        MockResponse response = new MockResponse().setChunkedBody("ABCDEFGHIJKLMNOPQR", 5);

        MockWebServer server = new MockWebServer();
        server.enqueue(response);
        server.enqueue(response);
        server.enqueue(response);
        server.play();

        assertContent("ABCDEFGHIJKLMNOPQR", server.getUrl("/").openConnection());
        assertEquals(0, server.takeRequest().getSequenceNumber());
        assertContent("ABCDEFGHIJKLMNOPQR", server.getUrl("/").openConnection());
        assertEquals(1, server.takeRequest().getSequenceNumber());
        assertContent("ABCDEFGHIJKLMNOPQR", server.getUrl("/").openConnection());
        assertEquals(2, server.takeRequest().getSequenceNumber());
    }

    enum UploadKind { CHUNKED, FIXED_LENGTH }
    enum WriteKind { BYTE_BY_BYTE, SMALL_BUFFERS, LARGE_BUFFERS }

    public void test_chunkedUpload_byteByByte() throws Exception {
        doUpload(UploadKind.CHUNKED, WriteKind.BYTE_BY_BYTE);
    }

    public void test_chunkedUpload_smallBuffers() throws Exception {
        doUpload(UploadKind.CHUNKED, WriteKind.SMALL_BUFFERS);
    }

    public void test_chunkedUpload_largeBuffers() throws Exception {
        doUpload(UploadKind.CHUNKED, WriteKind.LARGE_BUFFERS);
    }

    public void test_fixedLengthUpload_byteByByte() throws Exception {
        doUpload(UploadKind.FIXED_LENGTH, WriteKind.BYTE_BY_BYTE);
    }

    public void test_fixedLengthUpload_smallBuffers() throws Exception {
        doUpload(UploadKind.FIXED_LENGTH, WriteKind.SMALL_BUFFERS);
    }

    public void test_fixedLengthUpload_largeBuffers() throws Exception {
        doUpload(UploadKind.FIXED_LENGTH, WriteKind.LARGE_BUFFERS);
    }

    private void doUpload(UploadKind uploadKind, WriteKind writeKind) throws Exception {
        int n = 512*1024;
        MockWebServer server = new MockWebServer();
        server.setBodyLimit(0);
        server.enqueue(new MockResponse());
        server.play();

        HttpURLConnection conn = (HttpURLConnection) server.getUrl("/").openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        if (uploadKind == UploadKind.CHUNKED) {
            conn.setChunkedStreamingMode(-1);
        } else {
            conn.setFixedLengthStreamingMode(n);
        }
        OutputStream out = conn.getOutputStream();
        if (writeKind == WriteKind.BYTE_BY_BYTE) {
            for (int i = 0; i < n; ++i) {
                out.write('x');
            }
        } else {
            byte[] buf = new byte[writeKind == WriteKind.SMALL_BUFFERS ? 256 : 64*1024];
            Arrays.fill(buf, (byte) 'x');
            for (int i = 0; i < n; i += buf.length) {
                out.write(buf, 0, Math.min(buf.length, n - i));
            }
        }
        out.close();
        assertEquals(200, conn.getResponseCode());
        RecordedRequest request = server.takeRequest();
        assertEquals(n, request.getBodySize());
        if (uploadKind == UploadKind.CHUNKED) {
            assertTrue(request.getChunkSizes().size() > 0);
        } else {
            assertTrue(request.getChunkSizes().isEmpty());
        }
    }

    public void test_responseCaching() throws Exception {
        // Test each documented HTTP/1.1 code, plus the first unused value in each range.
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html

        // We can't test 100 because it's not really a response.
        // assertCached(false, 100);
        assertCached(false, 101);
        assertCached(false, 102);
        assertCached(true,  200);
        assertCached(false, 201);
        assertCached(false, 202);
        assertCached(true,  203);
        assertCached(false, 204);
        assertCached(false, 205);
        assertCached(true,  206);
        assertCached(false, 207);
        assertCached(true,  301);
        for (int i = 302; i <= 308; ++i) {
            assertCached(false, i);
        }
        for (int i = 400; i <= 406; ++i) {
            assertCached(false, i);
        }
        // (See test_responseCaching_407.)
        assertCached(false, 408);
        assertCached(false, 409);
        assertCached(true,  410);
        for (int i = 411; i <= 418; ++i) {
            assertCached(false, i);
        }
        for (int i = 500; i <= 506; ++i) {
            assertCached(false, i);
        }
    }

    public void test_responseCaching_407() throws Exception {
        // This test will fail on Android because we throw if we're not using a proxy.
        // This isn't true of the RI, but it seems like useful debugging behavior.
        assertCached(false, 407);
    }

    private void assertCached(boolean shouldPut, int responseCode) throws Exception {
        class MyResponseCache extends ResponseCache {
            public boolean didPut;
            public CacheResponse get(URI uri, String requestMethod,
                    Map<String, List<String>> requestHeaders) throws IOException {
                return null;
            }
            public CacheRequest put(URI uri, URLConnection conn) throws IOException {
                didPut = true;
                return null;
            }
        }
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse()
                .setResponseCode(responseCode)
                .addHeader("WWW-Authenticate: challenge"));
        server.play();

        MyResponseCache cache = new MyResponseCache();
        ResponseCache.setDefault(cache);
        HttpURLConnection conn = (HttpURLConnection) server.getUrl("/").openConnection();
        assertEquals(responseCode, conn.getResponseCode());
        assertEquals(Integer.toString(responseCode), shouldPut, cache.didPut);

    }

    public void testConnectViaHttps() throws IOException, InterruptedException {
        TestSSLContext testSSLContext = TestSSLContext.create();

        MockWebServer server = new MockWebServer();
        server.useHttps(testSSLContext.sslContext.getSocketFactory(), false);
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("this response comes via HTTPS"));
        server.play();

        URL url = new URL("https://localhost:" + server.getPort() + "/foo");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(testSSLContext.sslContext.getSocketFactory());

        assertContent("this response comes via HTTPS", connection);

        RecordedRequest request = server.takeRequest();
        assertEquals("GET /foo HTTP/1.1", request.getRequestLine());
    }

    public void testConnectViaProxy() throws IOException, InterruptedException {
        MockWebServer proxy = new MockWebServer();
        MockResponse mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody("this response comes via a proxy");
        proxy.enqueue(mockResponse);
        proxy.play();

        URLConnection connection = new URL("http://android.com/foo").openConnection(
                proxy.toProxyAddress());
        assertContent("this response comes via a proxy", connection);

        RecordedRequest request = proxy.takeRequest();
        assertEquals("GET http://android.com/foo HTTP/1.1", request.getRequestLine());
        assertContains(request.getHeaders(), "Host: android.com");
    }

    public void testContentDisagreesWithContentLengthHeader() throws IOException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("abc\r\nYOU SHOULD NOT SEE THIS")
                .clearHeaders()
                .addHeader("Content-Length: 3"));
        server.play();

        assertContent("abc", server.getUrl("/").openConnection());
    }

    public void testContentDisagreesWithChunkedHeader() throws IOException {
        MockWebServer server = new MockWebServer();
        MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(200);
        mockResponse.setChunkedBody("abc", 3);
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        bytesOut.write(mockResponse.getBody());
        bytesOut.write("\r\nYOU SHOULD NOT SEE THIS".getBytes());
        mockResponse.setBody(bytesOut.toByteArray());
        mockResponse.clearHeaders();
        mockResponse.addHeader("Transfer-encoding: chunked");

        server.enqueue(mockResponse);
        server.play();

        assertContent("abc", server.getUrl("/").openConnection());
    }

    public void testConnectViaHttpProxyToHttps() throws IOException, InterruptedException {
        TestSSLContext testSSLContext = TestSSLContext.create();

        MockWebServer proxy = new MockWebServer();
        proxy.useHttps(testSSLContext.sslContext.getSocketFactory(), true);
        proxy.enqueue(new MockResponse().setResponseCode(200).clearHeaders()); // for CONNECT
        proxy.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("this response comes via a secure proxy"));
        proxy.play();

        URL url = new URL("https://android.com/foo");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection(
                proxy.toProxyAddress());
        connection.setSSLSocketFactory(testSSLContext.sslContext.getSocketFactory());
        connection.setHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        assertContent("this response comes via a secure proxy", connection);

        RecordedRequest connect = proxy.takeRequest();
        assertEquals("Connect line failure on proxy",
                "CONNECT android.com:443 HTTP/1.1", connect.getRequestLine());
        assertContains(connect.getHeaders(), "Host: android.com");

        RecordedRequest get = proxy.takeRequest();
        assertEquals("GET /foo HTTP/1.1", get.getRequestLine());
        assertContains(get.getHeaders(), "Host: android.com");
    }

    /**
     * Reads at most {@code limit} characters from {@code in} and asserts that
     * content equals {@code expected}.
     */
    private void assertContent(String expected, URLConnection connection, int limit)
            throws IOException {
        InputStreamReader reader = new InputStreamReader(connection.getInputStream());
        StringWriter writer = new StringWriter();
        char[] buffer = new char[1024];
        int count;
        while (limit > 0
                && (count = reader.read(buffer, 0, Math.min(limit, buffer.length))) != -1) {
            writer.write(buffer, 0, count);
            limit -= count;
        }
        assertEquals(expected, writer.toString());
        reader.close();
        ((HttpURLConnection) connection).disconnect();
    }

    private void assertContent(String expected, URLConnection connection) throws IOException {
        assertContent(expected, connection, Integer.MAX_VALUE);
    }

    private void assertContains(List<String> headers, String header) {
        assertTrue(headers.toString(), headers.contains(header));
    }
}
