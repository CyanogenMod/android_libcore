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

package libcore.java.net;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.ResponseCache;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import libcore.javax.net.ssl.TestSSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import tests.http.DefaultResponseCache;
import tests.http.MockResponse;
import tests.http.MockWebServer;
import tests.http.RecordedRequest;

public class URLConnectionTest extends junit.framework.TestCase {

    private static final Authenticator SIMPLE_AUTHENTICATOR = new Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication("username", "password".toCharArray());
        }
    };

    private MockWebServer server = new MockWebServer();

    @Override protected void tearDown() throws Exception {
        ResponseCache.setDefault(null);
        Authenticator.setDefault(null);
        server.shutdown();
    }

    public void testRequestHeaders() throws IOException, InterruptedException {
        server.enqueue(new MockResponse());
        server.play();

        HttpURLConnection urlConnection = (HttpURLConnection) server.getUrl("/").openConnection();
        urlConnection.addRequestProperty("D", "e");
        urlConnection.addRequestProperty("D", "f");
        Map<String, List<String>> requestHeaders = urlConnection.getRequestProperties();
        assertEquals(newSet("e", "f"), new HashSet<String>(requestHeaders.get("D")));
        try {
            requestHeaders.put("G", Arrays.asList("h"));
            fail("Modified an unmodifiable view.");
        } catch (UnsupportedOperationException expected) {
        }
        try {
            requestHeaders.get("D").add("i");
            fail("Modified an unmodifiable view.");
        } catch (UnsupportedOperationException expected) {
        }
        try {
            urlConnection.setRequestProperty(null, "j");
            fail();
        } catch (NullPointerException expected) {
        }
        try {
            urlConnection.addRequestProperty(null, "k");
            fail();
        } catch (NullPointerException expected) {
        }
        urlConnection.setRequestProperty("NullValue", null); // should fail silently!
        urlConnection.addRequestProperty("AnotherNullValue", null);  // should fail silently!

        urlConnection.getResponseCode();
        RecordedRequest request = server.takeRequest();
        assertContains(request.getHeaders(), "D: e");
        assertContains(request.getHeaders(), "D: f");
        assertContainsNoneMatching(request.getHeaders(), "NullValue.*");
        assertContainsNoneMatching(request.getHeaders(), "AnotherNullValue.*");
        assertContainsNoneMatching(request.getHeaders(), "G:.*");
        assertContainsNoneMatching(request.getHeaders(), "null:.*");

        try {
            urlConnection.addRequestProperty("N", "o");
            fail("Set header after connect");
        } catch (IllegalStateException expected) {
        }
        try {
            urlConnection.setRequestProperty("P", "q");
            fail("Set header after connect");
        } catch (IllegalStateException expected) {
        }
    }

    public void testResponseHeaders() throws IOException, InterruptedException {
        server.enqueue(new MockResponse()
                .setStatus("HTTP/1.0 200 Fantastic")
                .addHeader("A: b")
                .addHeader("A: c")
                .setChunkedBody("ABCDE\nFGHIJ\nKLMNO\nPQR", 8));
        server.play();

        HttpURLConnection urlConnection = (HttpURLConnection) server.getUrl("/").openConnection();
        assertEquals(200, urlConnection.getResponseCode());
        assertEquals("Fantastic", urlConnection.getResponseMessage());
        Map<String, List<String>> responseHeaders = urlConnection.getHeaderFields();
        assertEquals(newSet("b", "c"), new HashSet<String>(responseHeaders.get("A")));
        try {
            responseHeaders.put("N", Arrays.asList("o"));
            fail("Modified an unmodifiable view.");
        } catch (UnsupportedOperationException expected) {
        }
        try {
            responseHeaders.get("A").add("d");
            fail("Modified an unmodifiable view.");
        } catch (UnsupportedOperationException expected) {
        }
    }

    // Check that if we don't read to the end of a response, the next request on the
    // recycled connection doesn't get the unread tail of the first request's response.
    // http://code.google.com/p/android/issues/detail?id=2939
    public void test_2939() throws Exception {
        MockResponse response = new MockResponse().setChunkedBody("ABCDE\nFGHIJ\nKLMNO\nPQR", 8);

        server.enqueue(response);
        server.enqueue(response);
        server.play();

        assertContent("ABCDE", server.getUrl("/").openConnection(), 5);
        assertContent("ABCDE", server.getUrl("/").openConnection(), 5);
    }

    // Check that we recognize a few basic mime types by extension.
    // http://code.google.com/p/android/issues/detail?id=10100
    public void test_10100() throws Exception {
        assertEquals("image/jpeg", URLConnection.guessContentTypeFromName("someFile.jpg"));
        assertEquals("application/pdf", URLConnection.guessContentTypeFromName("stuff.pdf"));
    }

    public void testConnectionsArePooled() throws Exception {
        MockResponse response = new MockResponse().setBody("ABCDEFGHIJKLMNOPQR");

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

    enum WriteKind { BYTE_BY_BYTE, SMALL_BUFFERS, LARGE_BUFFERS }

    public void test_chunkedUpload_byteByByte() throws Exception {
        doUpload(TransferKind.CHUNKED, WriteKind.BYTE_BY_BYTE);
    }

    public void test_chunkedUpload_smallBuffers() throws Exception {
        doUpload(TransferKind.CHUNKED, WriteKind.SMALL_BUFFERS);
    }

    public void test_chunkedUpload_largeBuffers() throws Exception {
        doUpload(TransferKind.CHUNKED, WriteKind.LARGE_BUFFERS);
    }

    public void test_fixedLengthUpload_byteByByte() throws Exception {
        doUpload(TransferKind.FIXED_LENGTH, WriteKind.BYTE_BY_BYTE);
    }

    public void test_fixedLengthUpload_smallBuffers() throws Exception {
        doUpload(TransferKind.FIXED_LENGTH, WriteKind.SMALL_BUFFERS);
    }

    public void test_fixedLengthUpload_largeBuffers() throws Exception {
        doUpload(TransferKind.FIXED_LENGTH, WriteKind.LARGE_BUFFERS);
    }

    private void doUpload(TransferKind uploadKind, WriteKind writeKind) throws Exception {
        int n = 512*1024;
        server.setBodyLimit(0);
        server.enqueue(new MockResponse());
        server.play();

        HttpURLConnection conn = (HttpURLConnection) server.getUrl("/").openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        if (uploadKind == TransferKind.CHUNKED) {
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
        if (uploadKind == TransferKind.CHUNKED) {
            assertTrue(request.getChunkSizes().size() > 0);
        } else {
            assertTrue(request.getChunkSizes().isEmpty());
        }
    }

    /**
     * Test that response caching is consistent with the RI and the spec.
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html#sec13.4
     */
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
        // (See test_responseCaching_300.)
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
        // (See test_responseCaching_410.)
        for (int i = 411; i <= 418; ++i) {
            assertCached(false, i);
        }
        for (int i = 500; i <= 506; ++i) {
            assertCached(false, i);
        }
    }

    public void test_responseCaching_300() throws Exception {
        // TODO: fix this for android
        assertCached(false, 300);
    }

    public void test_responseCaching_407() throws Exception {
        // This test will fail on Android because we throw if we're not using a proxy.
        // This isn't true of the RI, but it seems like useful debugging behavior.
        assertCached(false, 407);
    }

    public void test_responseCaching_410() throws Exception {
        // the HTTP spec permits caching 410s, but the RI doesn't.
        assertCached(false, 410);
    }

    private void assertCached(boolean shouldPut, int responseCode) throws Exception {
        server = new MockWebServer();
        server.enqueue(new MockResponse()
                .setResponseCode(responseCode)
                .setBody("ABCDE")
                .addHeader("WWW-Authenticate: challenge"));
        server.play();

        DefaultResponseCache responseCache = new DefaultResponseCache();
        ResponseCache.setDefault(responseCache);
        URL url = server.getUrl("/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        assertEquals(responseCode, conn.getResponseCode());

        // exhaust the content stream
        try {
            // TODO: remove special case once testUnauthorizedResponseHandling() is fixed
            if (responseCode != 401) {
                readAscii(conn.getInputStream(), Integer.MAX_VALUE);
            }
        } catch (IOException ignored) {
        }

        Set<URI> expectedCachedUris = shouldPut
                ? Collections.singleton(url.toURI())
                : Collections.<URI>emptySet();
        assertEquals(Integer.toString(responseCode),
                expectedCachedUris, responseCache.getContents().keySet());
        server.shutdown(); // tearDown() isn't sufficient; this test starts multiple servers
    }

    public void testConnectViaHttps() throws IOException, InterruptedException {
        TestSSLContext testSSLContext = TestSSLContext.create();

        server.useHttps(testSSLContext.serverContext.getSocketFactory(), false);
        server.enqueue(new MockResponse().setBody("this response comes via HTTPS"));
        server.play();

        HttpsURLConnection connection = (HttpsURLConnection) server.getUrl("/foo").openConnection();
        connection.setSSLSocketFactory(testSSLContext.clientContext.getSocketFactory());

        assertContent("this response comes via HTTPS", connection);

        RecordedRequest request = server.takeRequest();
        assertEquals("GET /foo HTTP/1.1", request.getRequestLine());
    }

    public void testConnectViaHttpsReusingConnections() throws IOException, InterruptedException {
        TestSSLContext testSSLContext = TestSSLContext.create();

        server.useHttps(testSSLContext.serverContext.getSocketFactory(), false);
        server.enqueue(new MockResponse().setBody("this response comes via HTTPS"));
        server.enqueue(new MockResponse().setBody("another response via HTTPS"));
        server.play();

        // install a custom SSL socket factory so the server can be authorized
        HttpsURLConnection connection = (HttpsURLConnection) server.getUrl("/").openConnection();
        connection.setSSLSocketFactory(testSSLContext.clientContext.getSocketFactory());
        assertContent("this response comes via HTTPS", connection);

        // without an SSL socket factory, the connection should fail
        connection = (HttpsURLConnection) server.getUrl("/").openConnection();
        try {
            readAscii(connection.getInputStream(), Integer.MAX_VALUE);
            fail();
        } catch (SSLException expected) {
        }
    }

    public void testConnectViaHttpsWithSSLFallback() throws IOException, InterruptedException {
        TestSSLContext testSSLContext = TestSSLContext.create();

        server.useHttps(testSSLContext.serverContext.getSocketFactory(), false);
        server.enqueue(new MockResponse().setDisconnectAtStart(true));
        server.enqueue(new MockResponse().setBody("this response comes via SSL"));
        server.play();

        HttpsURLConnection connection = (HttpsURLConnection) server.getUrl("/foo").openConnection();
        connection.setSSLSocketFactory(testSSLContext.clientContext.getSocketFactory());

        assertContent("this response comes via SSL", connection);

        RecordedRequest request = server.takeRequest();
        assertEquals("GET /foo HTTP/1.1", request.getRequestLine());
    }

    public void testConnectViaProxy() throws IOException, InterruptedException {
        MockResponse mockResponse = new MockResponse().setBody("this response comes via a proxy");
        server.enqueue(mockResponse);
        server.play();

        URLConnection connection = new URL("http://android.com/foo").openConnection(
                server.toProxyAddress());
        assertContent("this response comes via a proxy", connection);

        RecordedRequest request = server.takeRequest();
        assertEquals("GET http://android.com/foo HTTP/1.1", request.getRequestLine());
        assertContains(request.getHeaders(), "Host: android.com");
    }

    public void testContentDisagreesWithContentLengthHeader() throws IOException {
        server.enqueue(new MockResponse()
                .setBody("abc\r\nYOU SHOULD NOT SEE THIS")
                .clearHeaders()
                .addHeader("Content-Length: 3"));
        server.play();

        assertContent("abc", server.getUrl("/").openConnection());
    }

    public void testContentDisagreesWithChunkedHeader() throws IOException {
        MockResponse mockResponse = new MockResponse();
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

        server.useHttps(testSSLContext.serverContext.getSocketFactory(), true);
        server.enqueue(new MockResponse().clearHeaders()); // for CONNECT
        server.enqueue(new MockResponse().setBody("this response comes via a secure proxy"));
        server.play();

        URL url = new URL("https://android.com/foo");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection(
                server.toProxyAddress());
        connection.setSSLSocketFactory(testSSLContext.clientContext.getSocketFactory());
        connection.setHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        assertContent("this response comes via a secure proxy", connection);

        RecordedRequest connect = server.takeRequest();
        assertEquals("Connect line failure on proxy",
                "CONNECT android.com:443 HTTP/1.1", connect.getRequestLine());
        assertContains(connect.getHeaders(), "Host: android.com");

        RecordedRequest get = server.takeRequest();
        assertEquals("GET /foo HTTP/1.1", get.getRequestLine());
        assertContains(get.getHeaders(), "Host: android.com");
    }

    public void testResponseCachingAndInputStreamSkipWithFixedLength() throws IOException {
        testResponseCaching(TransferKind.FIXED_LENGTH);
    }

    public void testResponseCachingAndInputStreamSkipWithChunkedEncoding() throws IOException {
        testResponseCaching(TransferKind.CHUNKED);
    }

    public void testResponseCachingAndInputStreamSkipWithNoLengthHeaders() throws IOException {
        testResponseCaching(TransferKind.END_OF_STREAM);
    }

    /**
     * HttpURLConnection.getInputStream().skip(long) causes ResponseCache corruption
     * http://code.google.com/p/android/issues/detail?id=8175
     */
    private void testResponseCaching(TransferKind transferKind) throws IOException {
        MockResponse response = new MockResponse();
        transferKind.setBody(response, "I love puppies but hate spiders", 1);
        server.enqueue(response);
        server.play();

        DefaultResponseCache cache = new DefaultResponseCache();
        ResponseCache.setDefault(cache);

        // Make sure that calling skip() doesn't omit bytes from the cache.
        URLConnection urlConnection = server.getUrl("/").openConnection();
        InputStream in = urlConnection.getInputStream();
        assertEquals("I love ", readAscii(in, "I love ".length()));
        reliableSkip(in, "puppies but hate ".length());
        assertEquals("spiders", readAscii(in, "spiders".length()));
        assertEquals(-1, in.read());
        in.close();
        assertEquals(1, cache.getSuccessCount());
        assertEquals(0, cache.getAbortCount());

        urlConnection = server.getUrl("/").openConnection(); // this response is cached!
        in = urlConnection.getInputStream();
        assertEquals("I love puppies but hate spiders",
                readAscii(in, "I love puppies but hate spiders".length()));
        assertEquals(-1, in.read());
        assertEquals(1, cache.getMissCount());
        assertEquals(1, cache.getHitCount());
        assertEquals(1, cache.getSuccessCount());
        assertEquals(0, cache.getAbortCount());
    }

    public void testResponseCacheRequestHeaders() throws IOException, URISyntaxException {
        server.enqueue(new MockResponse().setBody("ABC"));
        server.play();

        final AtomicReference<Map<String, List<String>>> requestHeadersRef
                = new AtomicReference<Map<String, List<String>>>();
        ResponseCache.setDefault(new ResponseCache() {
            @Override public CacheResponse get(URI uri, String requestMethod,
                    Map<String, List<String>> requestHeaders) throws IOException {
                requestHeadersRef.set(requestHeaders);
                return null;
            }
            @Override public CacheRequest put(URI uri, URLConnection conn) throws IOException {
                return null;
            }
        });

        URL url = server.getUrl("/");
        URLConnection urlConnection = url.openConnection();
        urlConnection.addRequestProperty("A", "android");
        readAscii(urlConnection.getInputStream(), Integer.MAX_VALUE);
        assertEquals(Arrays.asList("android"), requestHeadersRef.get().get("A"));
    }

    private void reliableSkip(InputStream in, int length) throws IOException {
        while (length > 0) {
            length -= in.skip(length);
        }
    }

    /**
     * Reads {@code count} characters from the stream. If the stream is
     * exhausted before {@code count} characters can be read, the remaining
     * characters are returned and the stream is closed.
     */
    private String readAscii(InputStream in, int count) throws IOException {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            int value = in.read();
            if (value == -1) {
                in.close();
                break;
            }
            result.append((char) value);
        }
        return result.toString();
    }

    public void testServerDisconnectsPrematurelyWithContentLengthHeader() throws IOException {
        testServerPrematureDisconnect(TransferKind.FIXED_LENGTH);
    }

    public void testServerDisconnectsPrematurelyWithChunkedEncoding() throws IOException {
        testServerPrematureDisconnect(TransferKind.CHUNKED);
    }

    public void testServerDisconnectsPrematurelyWithNoLengthHeaders() throws IOException {
        /*
         * Intentionally empty. This case doesn't make sense because there's no
         * such thing as a premature disconnect when the disconnect itself
         * indicates the end of the data stream.
         */
    }

    private void testServerPrematureDisconnect(TransferKind transferKind) throws IOException {
        MockResponse response = new MockResponse();
        transferKind.setBody(response, "ABCDE\nFGHIJKLMNOPQRSTUVWXYZ", 16);
        server.enqueue(truncateViolently(response, 16));
        server.enqueue(new MockResponse().setBody("Request #2"));
        server.play();

        DefaultResponseCache cache = new DefaultResponseCache();
        ResponseCache.setDefault(cache);

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                server.getUrl("/").openConnection().getInputStream()));
        assertEquals("ABCDE", reader.readLine());
        try {
            reader.readLine();
            fail("This implementation silently ignored a truncated HTTP body.");
        } catch (IOException expected) {
        }

        assertEquals(1, cache.getAbortCount());
        assertEquals(0, cache.getSuccessCount());
        assertContent("Request #2", server.getUrl("/").openConnection());
        assertEquals(1, cache.getAbortCount());
        assertEquals(1, cache.getSuccessCount());
    }

    public void testClientPrematureDisconnectWithContentLengthHeader() throws IOException {
        testClientPrematureDisconnect(TransferKind.FIXED_LENGTH);
    }

    public void testClientPrematureDisconnectWithChunkedEncoding() throws IOException {
        testClientPrematureDisconnect(TransferKind.CHUNKED);
    }

    public void testClientPrematureDisconnectWithNoLengthHeaders() throws IOException {
        testClientPrematureDisconnect(TransferKind.END_OF_STREAM);
    }

    private void testClientPrematureDisconnect(TransferKind transferKind) throws IOException {
        MockResponse response = new MockResponse();
        transferKind.setBody(response, "ABCDE\nFGHIJKLMNOPQRSTUVWXYZ", 1024);
        server.enqueue(response);
        server.enqueue(new MockResponse().setBody("Request #2"));
        server.play();

        DefaultResponseCache cache = new DefaultResponseCache();
        ResponseCache.setDefault(cache);

        InputStream in = server.getUrl("/").openConnection().getInputStream();
        assertEquals("ABCDE", readAscii(in, 5));
        in.close();
        try {
            in.read();
            fail("Expected an IOException because the stream is closed.");
        } catch (IOException expected) {
        }

        assertEquals(1, cache.getAbortCount());
        assertEquals(0, cache.getSuccessCount());
        assertContent("Request #2", server.getUrl("/").openConnection());
        assertEquals(1, cache.getAbortCount());
        assertEquals(1, cache.getSuccessCount());
    }

    /**
     * Shortens the body of {@code response} but not the corresponding headers.
     * Only useful to test how clients respond to the premature conclusion of
     * the HTTP body.
     */
    private MockResponse truncateViolently(MockResponse response, int numBytesToKeep) {
        response.setDisconnectAtEnd(true);
        List<String> headers = new ArrayList<String>(response.getHeaders());
        response.setBody(Arrays.copyOfRange(response.getBody(), 0, numBytesToKeep));
        response.getHeaders().clear();
        response.getHeaders().addAll(headers);
        return response;
    }

    public void testMarkAndResetWithContentLengthHeader() throws IOException {
        testMarkAndReset(TransferKind.FIXED_LENGTH);
    }

    public void testMarkAndResetWithChunkedEncoding() throws IOException {
        testMarkAndReset(TransferKind.CHUNKED);
    }

    public void testMarkAndResetWithNoLengthHeaders() throws IOException {
        testMarkAndReset(TransferKind.END_OF_STREAM);
    }

    public void testMarkAndReset(TransferKind transferKind) throws IOException {
        MockResponse response = new MockResponse();
        transferKind.setBody(response, "ABCDEFGHIJKLMNOPQRSTUVWXYZ", 1024);
        server.enqueue(response);
        server.play();

        DefaultResponseCache cache = new DefaultResponseCache();
        ResponseCache.setDefault(cache);

        InputStream in = server.getUrl("/").openConnection().getInputStream();
        assertFalse("This implementation claims to support mark().", in.markSupported());
        in.mark(5);
        assertEquals("ABCDE", readAscii(in, 5));
        try {
            in.reset();
            fail();
        } catch (IOException expected) {
        }
        assertEquals("FGHIJKLMNOPQRSTUVWXYZ", readAscii(in, Integer.MAX_VALUE));

        assertContent("ABCDEFGHIJKLMNOPQRSTUVWXYZ", server.getUrl("/").openConnection());
        assertEquals(1, cache.getSuccessCount());
        assertEquals(1, cache.getHitCount());
    }

    /**
     * We've had a bug where we forget the HTTP response when we see response
     * code 401. This causes a new HTTP request to be issued for every call into
     * the URLConnection.
     */
    public void testUnauthorizedResponseHandling() throws IOException {
        MockResponse response = new MockResponse()
                .addHeader("WWW-Authenticate: challenge")
                .setResponseCode(401) // UNAUTHORIZED
                .setBody("Unauthorized");
        server.enqueue(response);
        server.enqueue(response);
        server.enqueue(response);
        server.play();

        URL url = server.getUrl("/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        assertEquals(401, conn.getResponseCode());
        assertEquals(401, conn.getResponseCode());
        assertEquals(401, conn.getResponseCode());
        assertEquals(1, server.getRequestCount());
    }

    public void testNonHexChunkSize() throws IOException {
        server.enqueue(new MockResponse()
                .setBody("5\r\nABCDE\r\nG\r\nFGHIJKLMNOPQRSTU\r\n0\r\n\r\n")
                .clearHeaders()
                .addHeader("Transfer-encoding: chunked"));
        server.play();

        URLConnection connection = server.getUrl("/").openConnection();
        try {
            readAscii(connection.getInputStream(), Integer.MAX_VALUE);
            fail();
        } catch (IOException e) {
        }
    }

    public void testMissingChunkBody() throws IOException {
        server.enqueue(new MockResponse()
                .setBody("5")
                .clearHeaders()
                .addHeader("Transfer-encoding: chunked")
                .setDisconnectAtEnd(true));
        server.play();

        URLConnection connection = server.getUrl("/").openConnection();
        try {
            readAscii(connection.getInputStream(), Integer.MAX_VALUE);
            fail();
        } catch (IOException e) {
        }
    }

    public void testClientConfiguredGzipContentEncoding() throws Exception {
        server.enqueue(new MockResponse()
                .setBody(gzip("ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes("UTF-8")))
                .addHeader("Content-Encoding: gzip"));
        server.play();

        URLConnection connection = server.getUrl("/").openConnection();
        connection.addRequestProperty("Accept-Encoding", "gzip");
        InputStream gunzippedIn = new GZIPInputStream(connection.getInputStream());
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", readAscii(gunzippedIn, Integer.MAX_VALUE));

        RecordedRequest request = server.takeRequest();
        assertContains(request.getHeaders(), "Accept-Encoding: gzip");
    }

    public void testGzipAndConnectionReuseWithFixedLength() throws Exception {
        testClientConfiguredGzipContentEncodingAndConnectionReuse(TransferKind.FIXED_LENGTH);
    }

    public void testGzipAndConnectionReuseWithChunkedEncoding() throws Exception {
        testClientConfiguredGzipContentEncodingAndConnectionReuse(TransferKind.CHUNKED);
    }

    /**
     * Test a bug where gzip input streams weren't exhausting the input stream,
     * which corrupted the request that followed.
     * http://code.google.com/p/android/issues/detail?id=7059
     */
    private void testClientConfiguredGzipContentEncodingAndConnectionReuse(
            TransferKind transferKind) throws Exception {
        MockResponse responseOne = new MockResponse();
        responseOne.addHeader("Content-Encoding: gzip");
        transferKind.setBody(responseOne, gzip("one (gzipped)".getBytes("UTF-8")), 5);
        server.enqueue(responseOne);
        MockResponse responseTwo = new MockResponse();
        transferKind.setBody(responseTwo, "two (identity)", 5);
        server.enqueue(responseTwo);
        server.play();

        URLConnection connection = server.getUrl("/").openConnection();
        connection.addRequestProperty("Accept-Encoding", "gzip");
        InputStream gunzippedIn = new GZIPInputStream(connection.getInputStream());
        assertEquals("one (gzipped)", readAscii(gunzippedIn, Integer.MAX_VALUE));
        assertEquals(0, server.takeRequest().getSequenceNumber());

        connection = server.getUrl("/").openConnection();
        assertEquals("two (identity)", readAscii(connection.getInputStream(), Integer.MAX_VALUE));
        assertEquals(1, server.takeRequest().getSequenceNumber());
    }

    /**
     * Obnoxiously test that the chunk sizes transmitted exactly equal the
     * requested data+chunk header size. Although setChunkedStreamingMode()
     * isn't specific about whether the size applies to the data or the
     * complete chunk, the RI interprets it as a complete chunk.
     */
    public void testSetChunkedStreamingMode() throws IOException, InterruptedException {
        server.enqueue(new MockResponse());
        server.play();

        HttpURLConnection urlConnection = (HttpURLConnection) server.getUrl("/").openConnection();
        urlConnection.setChunkedStreamingMode(8);
        urlConnection.setDoOutput(true);
        OutputStream outputStream = urlConnection.getOutputStream();
        outputStream.write("ABCDEFGHIJKLMNOPQ".getBytes("US-ASCII"));
        assertEquals(200, urlConnection.getResponseCode());

        RecordedRequest request = server.takeRequest();
        assertEquals("ABCDEFGHIJKLMNOPQ", new String(request.getBody(), "US-ASCII"));
        assertEquals(Arrays.asList(3, 3, 3, 3, 3, 2), request.getChunkSizes());
    }

    public void testAuthenticateWithFixedLengthStreaming() throws Exception {
        testAuthenticateWithStreamingPost(StreamingMode.FIXED_LENGTH);
    }

    public void testAuthenticateWithChunkedStreaming() throws Exception {
        testAuthenticateWithStreamingPost(StreamingMode.CHUNKED);
    }

    private void testAuthenticateWithStreamingPost(StreamingMode streamingMode) throws Exception {
        MockResponse pleaseAuthenticate = new MockResponse()
                .setResponseCode(401)
                .addHeader("WWW-Authenticate: Basic realm=\"protected area\"")
                .setBody("Please authenticate.");
        server.enqueue(pleaseAuthenticate);
        server.play();

        Authenticator.setDefault(SIMPLE_AUTHENTICATOR);
        HttpURLConnection connection = (HttpURLConnection) server.getUrl("/").openConnection();
        connection.setDoOutput(true);
        byte[] requestBody = { 'A', 'B', 'C', 'D' };
        if (streamingMode == StreamingMode.FIXED_LENGTH) {
            connection.setFixedLengthStreamingMode(requestBody.length);
        } else if (streamingMode == StreamingMode.CHUNKED) {
            connection.setChunkedStreamingMode(0);
        }
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(requestBody);
        outputStream.close();
        try {
            connection.getInputStream();
            fail();
        } catch (HttpRetryException expected) {
        }

        // no authorization header for the request...
        RecordedRequest request = server.takeRequest();
        assertContainsNoneMatching(request.getHeaders(), "Authorization: Basic .*");
        assertEquals(Arrays.toString(requestBody), Arrays.toString(request.getBody()));
    }

    enum StreamingMode {
        FIXED_LENGTH, CHUNKED
    }

    public void testAuthenticateWithPost() throws Exception {
        MockResponse pleaseAuthenticate = new MockResponse()
                .setResponseCode(401)
                .addHeader("WWW-Authenticate: Basic realm=\"protected area\"")
                .setBody("Please authenticate.");
        // fail auth three times...
        server.enqueue(pleaseAuthenticate);
        server.enqueue(pleaseAuthenticate);
        server.enqueue(pleaseAuthenticate);
        // ...then succeed the fourth time
        server.enqueue(new MockResponse().setBody("Successful auth!"));
        server.play();

        Authenticator.setDefault(SIMPLE_AUTHENTICATOR);
        HttpURLConnection connection = (HttpURLConnection) server.getUrl("/").openConnection();
        connection.setDoOutput(true);
        byte[] requestBody = { 'A', 'B', 'C', 'D' };
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(requestBody);
        outputStream.close();
        assertEquals("Successful auth!", readAscii(connection.getInputStream(), Integer.MAX_VALUE));

        // no authorization header for the first request...
        RecordedRequest request = server.takeRequest();
        assertContainsNoneMatching(request.getHeaders(), "Authorization: Basic .*");

        // ...but the three requests that follow include an authorization header
        for (int i = 0; i < 3; i++) {
            request = server.takeRequest();
            assertEquals("POST / HTTP/1.1", request.getRequestLine());
            assertContains(request.getHeaders(), "Authorization: Basic "
                    + "dXNlcm5hbWU6cGFzc3dvcmQ="); // "dXNl..." == base64("username:password")
            assertEquals(Arrays.toString(requestBody), Arrays.toString(request.getBody()));
        }
    }

    public void testAuthenticateWithGet() throws Exception {
        MockResponse pleaseAuthenticate = new MockResponse()
                .setResponseCode(401)
                .addHeader("WWW-Authenticate: Basic realm=\"protected area\"")
                .setBody("Please authenticate.");
        // fail auth three times...
        server.enqueue(pleaseAuthenticate);
        server.enqueue(pleaseAuthenticate);
        server.enqueue(pleaseAuthenticate);
        // ...then succeed the fourth time
        server.enqueue(new MockResponse().setBody("Successful auth!"));
        server.play();

        Authenticator.setDefault(SIMPLE_AUTHENTICATOR);
        HttpURLConnection connection = (HttpURLConnection) server.getUrl("/").openConnection();
        assertEquals("Successful auth!", readAscii(connection.getInputStream(), Integer.MAX_VALUE));

        // no authorization header for the first request...
        RecordedRequest request = server.takeRequest();
        assertContainsNoneMatching(request.getHeaders(), "Authorization: Basic .*");

        // ...but the three requests that follow requests include an authorization header
        for (int i = 0; i < 3; i++) {
            request = server.takeRequest();
            assertEquals("GET / HTTP/1.1", request.getRequestLine());
            assertContains(request.getHeaders(), "Authorization: Basic "
                    + "dXNlcm5hbWU6cGFzc3dvcmQ="); // "dXNl..." == base64("username:password")
        }
    }

    public void testRedirectedWithChunkedEncoding() throws Exception {
        testRedirected(TransferKind.CHUNKED, true);
    }

    public void testRedirectedWithContentLengthHeader() throws Exception {
        testRedirected(TransferKind.FIXED_LENGTH, true);
    }

    public void testRedirectedWithNoLengthHeaders() throws Exception {
        testRedirected(TransferKind.END_OF_STREAM, false);
    }

    private void testRedirected(TransferKind transferKind, boolean reuse) throws Exception {
        MockResponse response = new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
                .addHeader("Location: /foo");
        transferKind.setBody(response, "This page has moved!", 10);
        server.enqueue(response);
        server.enqueue(new MockResponse().setBody("This is the new location!"));
        server.play();

        URLConnection connection = server.getUrl("/").openConnection();
        assertEquals("This is the new location!",
                readAscii(connection.getInputStream(), Integer.MAX_VALUE));

        RecordedRequest first = server.takeRequest();
        assertEquals("GET / HTTP/1.1", first.getRequestLine());
        RecordedRequest retry = server.takeRequest();
        assertEquals("GET /foo HTTP/1.1", retry.getRequestLine());
        if (reuse) {
            assertEquals("Expected connection reuse", 1, retry.getSequenceNumber());
        }
    }

    public void testRedirectedOnHttps() throws IOException, InterruptedException {
        TestSSLContext testSSLContext = TestSSLContext.create();
        server.useHttps(testSSLContext.serverContext.getSocketFactory(), false);
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
                .addHeader("Location: /foo")
                .setBody("This page has moved!"));
        server.enqueue(new MockResponse().setBody("This is the new location!"));
        server.play();

        HttpsURLConnection connection = (HttpsURLConnection) server.getUrl("/").openConnection();
        connection.setSSLSocketFactory(testSSLContext.clientContext.getSocketFactory());
        assertEquals("This is the new location!",
                readAscii(connection.getInputStream(), Integer.MAX_VALUE));

        RecordedRequest first = server.takeRequest();
        assertEquals("GET / HTTP/1.1", first.getRequestLine());
        RecordedRequest retry = server.takeRequest();
        assertEquals("GET /foo HTTP/1.1", retry.getRequestLine());
        assertEquals("Expected connection reuse", 1, retry.getSequenceNumber());
    }

    public void testNotRedirectedFromHttpsToHttp() throws IOException, InterruptedException {
        TestSSLContext testSSLContext = TestSSLContext.create();
        server.useHttps(testSSLContext.serverContext.getSocketFactory(), false);
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
                .addHeader("Location: http://anyhost/foo")
                .setBody("This page has moved!"));
        server.play();

        HttpsURLConnection connection = (HttpsURLConnection) server.getUrl("/").openConnection();
        connection.setSSLSocketFactory(testSSLContext.clientContext.getSocketFactory());
        assertEquals("This page has moved!",
                readAscii(connection.getInputStream(), Integer.MAX_VALUE));
    }

    public void testNotRedirectedFromHttpToHttps() throws IOException, InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
                .addHeader("Location: https://anyhost/foo")
                .setBody("This page has moved!"));
        server.play();

        HttpURLConnection connection = (HttpURLConnection) server.getUrl("/").openConnection();
        assertEquals("This page has moved!",
                readAscii(connection.getInputStream(), Integer.MAX_VALUE));
    }

    public void testRedirectToAnotherOriginServer() throws Exception {
        MockWebServer server2 = new MockWebServer();
        server2.enqueue(new MockResponse().setBody("This is the 2nd server!"));
        server2.play();

        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
                .addHeader("Location: " + server2.getUrl("/").toString())
                .setBody("This page has moved!"));
        server.enqueue(new MockResponse().setBody("This is the first server again!"));
        server.play();

        URLConnection connection = server.getUrl("/").openConnection();
        assertEquals("This is the 2nd server!",
                readAscii(connection.getInputStream(), Integer.MAX_VALUE));
        assertEquals(server2.getUrl("/"), connection.getURL());

        // make sure the first server was careful to recycle the connection
        assertEquals("This is the first server again!",
                readAscii(server.getUrl("/").openStream(), Integer.MAX_VALUE));

        RecordedRequest first = server.takeRequest();
        assertContains(first.getHeaders(), "Host: localhost:" + server.getPort());
        RecordedRequest second = server2.takeRequest();
        assertContains(second.getHeaders(), "Host: localhost:" + server2.getPort());
        RecordedRequest third = server.takeRequest();
        assertEquals("Expected connection reuse", 1, third.getSequenceNumber());

        server2.shutdown();
    }

    public void testHttpsWithCustomTrustManager() throws Exception {
        RecordingHostnameVerifier hostnameVerifier = new RecordingHostnameVerifier();
        RecordingTrustManager trustManager = new RecordingTrustManager();
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[] { trustManager }, new java.security.SecureRandom());

        HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        SSLSocketFactory defaultSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        try {
            TestSSLContext testSSLContext = TestSSLContext.create();
            server.useHttps(testSSLContext.serverContext.getSocketFactory(), false);
            server.enqueue(new MockResponse().setBody("ABC"));
            server.enqueue(new MockResponse().setBody("DEF"));
            server.enqueue(new MockResponse().setBody("GHI"));
            server.play();

            URL url = server.getUrl("/");
            assertEquals("ABC", readAscii(url.openStream(), Integer.MAX_VALUE));
            assertEquals("DEF", readAscii(url.openStream(), Integer.MAX_VALUE));
            assertEquals("GHI", readAscii(url.openStream(), Integer.MAX_VALUE));

            assertEquals(Arrays.asList("verify localhost"), hostnameVerifier.calls);
            assertEquals(Arrays.asList("checkServerTrusted ["
                                       + "CN=localhost 1, "
                                       + "CN=Test Intermediate Certificate Authority 1, "
                                       + "CN=Test Root Certificate Authority 1"
                                       + "] RSA"),
                    trustManager.calls);
        } finally {
            HttpsURLConnection.setDefaultHostnameVerifier(defaultHostnameVerifier);
            HttpsURLConnection.setDefaultSSLSocketFactory(defaultSSLSocketFactory);
        }
    }

    public void testConnectTimeouts() throws IOException {
        // 10.0.0.0 is non-routable and will time out on every network
        URLConnection urlConnection = new URL("http://10.0.0.0/").openConnection();
        urlConnection.setConnectTimeout(1000);
        try {
            urlConnection.getInputStream();
            fail();
        } catch (SocketTimeoutException expected) {
        }
    }

    public void testReadTimeouts() throws IOException {
        /*
         * This relies on the fact that MockWebServer doesn't close the
         * connection after a response has been sent. This causes the client to
         * try to read more bytes than are sent, which results in a timeout.
         */
        MockResponse timeout = new MockResponse()
                .setBody("ABC")
                .clearHeaders()
                .addHeader("Content-Length: 4");
        server.enqueue(timeout);
        server.play();

        URLConnection urlConnection = server.getUrl("/").openConnection();
        urlConnection.setReadTimeout(1000);
        InputStream in = urlConnection.getInputStream();
        assertEquals('A', in.read());
        assertEquals('B', in.read());
        assertEquals('C', in.read());
        try {
            in.read(); // if Content-Length was accurate, this would return -1 immediately
            fail();
        } catch (SocketTimeoutException expected) {
        }
    }

    /**
     * Encodes the response body using GZIP and adds the corresponding header.
     */
    public byte[] gzip(byte[] bytes) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        OutputStream gzippedOut = new GZIPOutputStream(bytesOut);
        gzippedOut.write(bytes);
        gzippedOut.close();
        return bytesOut.toByteArray();
    }

    /**
     * Reads at most {@code limit} characters from {@code in} and asserts that
     * content equals {@code expected}.
     */
    private void assertContent(String expected, URLConnection connection, int limit)
            throws IOException {
        assertEquals(expected, readAscii(connection.getInputStream(), limit));
        ((HttpURLConnection) connection).disconnect();
    }

    private void assertContent(String expected, URLConnection connection) throws IOException {
        assertContent(expected, connection, Integer.MAX_VALUE);
    }

    private void assertContains(List<String> headers, String header) {
        assertTrue(headers.toString(), headers.contains(header));
    }

    private void assertContainsNoneMatching(List<String> headers, String pattern) {
        for (String header : headers) {
            if (header.matches(pattern)) {
                fail("Header " + header + " matches " + pattern);
            }
        }
    }

    private Set<String> newSet(String... elements) {
        return new HashSet<String>(Arrays.asList(elements));
    }

    enum TransferKind {
        CHUNKED() {
            @Override void setBody(MockResponse response, byte[] content, int chunkSize)
                    throws IOException {
                response.setChunkedBody(content, chunkSize);
            }
        },
        FIXED_LENGTH() {
            @Override void setBody(MockResponse response, byte[] content, int chunkSize) {
                response.setBody(content);
            }
        },
        END_OF_STREAM() {
            @Override void setBody(MockResponse response, byte[] content, int chunkSize) {
                response.setBody(content);
                response.setDisconnectAtEnd(true);
                for (Iterator<String> h = response.getHeaders().iterator(); h.hasNext(); ) {
                    if (h.next().startsWith("Content-Length:")) {
                        h.remove();
                        break;
                    }
                }
            }
        };

        abstract void setBody(MockResponse response, byte[] content, int chunkSize)
                throws IOException;

        void setBody(MockResponse response, String content, int chunkSize) throws IOException {
            setBody(response, content.getBytes("UTF-8"), chunkSize);
        }
    }

    private static class RecordingTrustManager implements X509TrustManager {
        private final List<String> calls = new ArrayList<String>();

        public X509Certificate[] getAcceptedIssuers() {
            calls.add("getAcceptedIssuers");
            return new X509Certificate[] {};
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            calls.add("checkClientTrusted " + certificatesToString(chain) + " " + authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            calls.add("checkServerTrusted " + certificatesToString(chain) + " " + authType);
        }

        private String certificatesToString(X509Certificate[] certificates) {
            List<String> result = new ArrayList<String>();
            for (X509Certificate certificate : certificates) {
                result.add(certificate.getSubjectDN() + " " + certificate.getSerialNumber());
            }
            return result.toString();
        }
    }

    private static class RecordingHostnameVerifier implements HostnameVerifier {
        private final List<String> calls = new ArrayList<String>();

        public boolean verify(String hostname, SSLSession session) {
            calls.add("verify " + hostname);
            return true;
        }
    }
}
