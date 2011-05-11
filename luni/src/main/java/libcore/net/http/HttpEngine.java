/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package libcore.net.http;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import libcore.io.IoUtils;
import libcore.io.Streams;
import libcore.util.EmptyArray;

/**
 * Handles a single HTTP request/response pair. Each HTTP engine follows this
 * lifecycle:
 * <ol>
 *     <li>It is created.
 *     <li>The HTTP request message is sent with sendRequest(). Once the request
 *         is sent it is an error to modify the request headers. After
 *         sendRequest() has been called the request body can be written to if
 *         it exists.
 *     <li>The HTTP response message is read with readResponse(). After the
 *         response has been read the response headers and body can be read.
 *         All responses have a response body input stream, though in some
 *         instances this stream is empty.
 * </ol>
 *
 * <p>The request and response may be served by the HTTP response cache, by the
 * network, or by both in the event of a conditional GET.
 *
 * <p>This class may hold a socket connection that needs to be released or
 * recycled. By default, this socket connection is held when the last byte of
 * the response is consumed. To release the connection when it is no longer
 * required, use {@link #automaticallyReleaseConnectionToPool()}.
 */
public class HttpEngine {
    private static final CacheResponse BAD_GATEWAY_RESPONSE = new CacheResponse() {
        @Override public Map<String, List<String>> getHeaders() throws IOException {
            Map<String, List<String>> result = new HashMap<String, List<String>>();
            result.put(null, Collections.singletonList("HTTP/1.1 502 Bad Gateway"));
            return result;
        }
        @Override public InputStream getBody() throws IOException {
            return new ByteArrayInputStream(EmptyArray.BYTE);
        }
    };

    /**
     * The maximum number of bytes to buffer when sending headers and a request
     * body. When the headers and body can be sent in a single write, the
     * request completes sooner. In one WiFi benchmark, using a large enough
     * buffer sped up some uploads by half.
     */
    private static final int MAX_REQUEST_BUFFER_LENGTH = 32768;

    public static final int DEFAULT_CHUNK_LENGTH = 1024;

    public static final String OPTIONS = "OPTIONS";
    public static final String GET = "GET";
    public static final String HEAD = "HEAD";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String TRACE = "TRACE";
    public static final String CONNECT = "CONNECT";

    public static final int HTTP_CONTINUE = 100;

    /**
     * HTTP 1.1 doesn't specify how many redirects to follow, but HTTP/1.0
     * recommended 5. http://www.w3.org/Protocols/HTTP/1.0/spec.html#Code3xx
     */
    public static final int MAX_REDIRECTS = 5;

    protected final HttpURLConnectionImpl policy;

    protected final String method;

    private ResponseSource responseSource;

    protected HttpConnection connection;
    private InputStream socketIn;
    private OutputStream socketOut;

    /**
     * This stream buffers the request headers and the request body when their
     * combined size is less than MAX_REQUEST_BUFFER_LENGTH. By combining them
     * we can save socket writes, which in turn saves a packet transmission.
     * This is socketOut if the request size is large or unknown.
     */
    private OutputStream requestOut;
    private AbstractHttpOutputStream requestBodyOut;

    private InputStream responseBodyIn;

    private final ResponseCache responseCache = ResponseCache.getDefault();
    private CacheResponse cacheResponse;
    private CacheRequest cacheRequest;

    /** The time when the request headers were written, or -1 if they haven't been written yet. */
    private long sentRequestMillis = -1;

    /**
     * True if this client added an "Accept-Encoding: gzip" header field and is
     * therefore responsible for also decompressing the transfer stream.
     */
    private boolean transparentGzip;

    boolean sendChunked;

    /**
     * The version this client will use. Either 0 for HTTP/1.0, or 1 for
     * HTTP/1.1. Upon receiving a non-HTTP/1.1 response, this client
     * automatically sets its version to HTTP/1.0.
     */
    // TODO: is HTTP minor version tracked across HttpEngines?
    private int httpMinorVersion = 1; // Assume HTTP/1.1

    private final URI uri;

    private final RawHeaders rawRequestHeaders;

    /** Null until a response is received from the network or the cache */
    private RawHeaders rawResponseHeaders;

    /*
     * The cache response currently being validated on a conditional get. Null
     * if the cached response doesn't exist or doesn't need validation. If the
     * conditional get succeeds, these will be used for the response headers and
     * body. If it fails, these be closed and set to null.
     */
    private ResponseHeaders responseHeadersToValidate;
    private InputStream responseBodyToValidate;

    /**
     * True if the socket connection should be released to the connection pool
     * when the response has been fully read.
     */
    private boolean automaticallyReleaseConnectionToPool;

    /** True if the socket connection is no longer needed by this engine. */
    private boolean released;

    /**
     * @param connection the connection used for an intermediate response
     *     immediately prior to this request/response pair, such as a same-host
     *     redirect. This engine assumes ownership of the connection and must
     *     release it when it is unneeded.
     */
    public HttpEngine(HttpURLConnectionImpl policy, String method, RawHeaders requestHeaders,
            HttpConnection connection, RetryableOutputStream requestBodyOut) throws IOException {
        this.policy = policy;
        this.method = method;
        this.rawRequestHeaders = new RawHeaders(requestHeaders);
        this.connection = connection;
        this.requestBodyOut = requestBodyOut;

        try {
            uri = policy.getURL().toURILenient();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    /**
     * Figures out what the response source will be, and opens a socket to that
     * source if necessary. Prepares the request headers and gets ready to start
     * writing the request body if it exists.
     */
    public final void sendRequest() throws IOException {
        if (responseSource != null) {
            return;
        }

        prepareRawRequestHeaders();
        RequestHeaders cacheRequestHeaders = new RequestHeaders(uri, rawRequestHeaders);
        initResponseSource(cacheRequestHeaders);

        /*
         * The raw response source may require the network, but the request
         * headers may forbid network use. In that case, dispose of the network
         * response and use a BAD_GATEWAY response instead.
         */
        if (cacheRequestHeaders.onlyIfCached && responseSource.requiresConnection()) {
            if (responseSource == ResponseSource.CONDITIONAL_CACHE) {
                this.responseHeadersToValidate = null;
                IoUtils.closeQuietly(responseBodyToValidate);
                this.responseBodyToValidate = null;
            }
            this.responseSource = ResponseSource.CACHE;
            this.cacheResponse = BAD_GATEWAY_RESPONSE;
            setResponse(RawHeaders.fromMultimap(cacheResponse.getHeaders()),
                    cacheResponse.getBody());
        }

        if (responseSource.requiresConnection()) {
            sendSocketRequest();
        } else if (connection != null) {
            HttpConnectionPool.INSTANCE.recycle(connection);
            connection = null;
        }
    }

    /**
     * Initialize the source for this response. It may be corrected later if the
     * request headers forbids network use.
     */
    private void initResponseSource(RequestHeaders cacheRequestHeaders) throws IOException {
        responseSource = ResponseSource.NETWORK;
        if (!policy.getUseCaches() || responseCache == null) {
            return;
        }

        CacheResponse candidate = responseCache.get(uri, method, rawRequestHeaders.toMultimap());
        if (candidate == null) {
            return;
        }

        Map<String, List<String>> responseHeaders = candidate.getHeaders();
        InputStream cacheBodyIn = candidate.getBody();
        if (!acceptCacheResponseType(candidate) || responseHeaders == null || cacheBodyIn == null) {
            IoUtils.closeQuietly(cacheBodyIn);
            return;
        }

        RawHeaders headers = RawHeaders.fromMultimap(responseHeaders);
        ResponseHeaders cacheResponseHeaders = new ResponseHeaders(uri, headers);
        long now = System.currentTimeMillis();
        this.responseSource = cacheResponseHeaders.chooseResponseSource(now, cacheRequestHeaders);
        if (responseSource == ResponseSource.CACHE) {
            this.cacheResponse = candidate;
            setResponse(headers, cacheBodyIn);
        } else if (responseSource == ResponseSource.CONDITIONAL_CACHE) {
            this.cacheResponse = candidate;
            this.responseHeadersToValidate = cacheResponseHeaders;
            this.responseBodyToValidate = cacheBodyIn;
        } else if (responseSource == ResponseSource.NETWORK) {
            IoUtils.closeQuietly(cacheBodyIn);
        } else {
            throw new AssertionError();
        }
    }

    private void sendSocketRequest() throws IOException {
        if (connection == null) {
            connect();
        }

        if (socketOut != null || requestOut != null || socketIn != null) {
            throw new IllegalStateException();
        }

        socketOut = connection.getOutputStream();
        requestOut = socketOut;
        socketIn = connection.getInputStream();

        if (hasRequestBody()) {
            initRequestBodyOut();
        }
    }

    /**
     * Connect to the origin server either directly or via a proxy.
     */
    protected void connect() throws IOException {
        if (connection == null) {
            connection = openSocketConnection();
        }
    }

    protected final HttpConnection openSocketConnection() throws IOException {
        HttpConnection result = HttpConnection.connect(
                uri, policy.getProxy(), requiresTunnel(), policy.getConnectTimeout());
        Proxy proxy = result.getAddress().getProxy();
        if (proxy != null) {
            policy.setProxy(proxy);
        }
        result.setSoTimeout(policy.getReadTimeout());
        return result;
    }

    protected void initRequestBodyOut() throws IOException {
        int contentLength = -1;
        String contentLengthString = rawRequestHeaders.get("Content-Length");
        if (contentLengthString != null) {
            contentLength = Integer.parseInt(contentLengthString);
        }

        String encoding = rawRequestHeaders.get("Transfer-Encoding");
        int chunkLength = policy.getChunkLength();
        if (chunkLength > 0 || "chunked".equalsIgnoreCase(encoding)) {
            sendChunked = true;
            contentLength = -1;
            if (chunkLength == -1) {
                chunkLength = DEFAULT_CHUNK_LENGTH;
            }
        }

        if (socketOut == null) {
            throw new IllegalStateException("No socket to write to; was a POST cached?");
        }

        if (httpMinorVersion == 0) {
            sendChunked = false;
        }

        int fixedContentLength = policy.getFixedContentLength();
        if (requestBodyOut != null) {
            // request body was already initialized by the predecessor HTTP engine
        } else if (fixedContentLength != -1) {
            writeRequestHeaders(fixedContentLength);
            requestBodyOut = new FixedLengthOutputStream(requestOut, fixedContentLength);
        } else if (sendChunked) {
            writeRequestHeaders(-1);
            requestBodyOut = new ChunkedOutputStream(requestOut, chunkLength);
        } else if (contentLength != -1) {
            writeRequestHeaders(contentLength);
            requestBodyOut = new RetryableOutputStream(contentLength);
        } else {
            requestBodyOut = new RetryableOutputStream();
        }
    }

    /**
     * @param body the response body, or null if it doesn't exist or isn't
     *     available.
     */
    private void setResponse(RawHeaders headers, InputStream body) throws IOException {
        if (this.responseBodyIn != null) {
            throw new IllegalStateException();
        }
        this.rawResponseHeaders = headers;
        this.httpMinorVersion = rawResponseHeaders.getHttpMinorVersion();
        if (body != null) {
            initContentStream(body);
        }
    }

    private boolean hasRequestBody() {
        return method == POST || method == PUT;
    }

    /**
     * Returns the request body or null if this request doesn't have a body.
     */
    public final OutputStream getRequestBody() {
        if (responseSource == null) {
            throw new IllegalStateException();
        }
        return requestBodyOut;
    }

    public final boolean hasResponse() {
        return rawResponseHeaders != null;
    }

    public final RawHeaders getRequestHeaders() {
        return rawRequestHeaders;
    }

    public final RawHeaders getResponseHeaders() {
        if (rawResponseHeaders == null) {
            throw new IllegalStateException();
        }
        return rawResponseHeaders;
    }

    public final InputStream getResponseBody() {
        if (rawResponseHeaders == null) {
            throw new IllegalStateException();
        }
        return responseBodyIn;
    }

    public final CacheResponse getCacheResponse() {
        if (rawResponseHeaders == null) {
            throw new IllegalStateException();
        }
        return cacheResponse;
    }

    public final HttpConnection getConnection() {
        return connection;
    }

    /**
     * Returns true if {@code cacheResponse} is of the right type. This
     * condition is necessary but not sufficient for the cached response to
     * be used.
     */
    protected boolean acceptCacheResponseType(CacheResponse cacheResponse) {
        return true;
    }

    private void maybeCache() throws IOException {
        // Are we caching at all?
        if (!policy.getUseCaches() || responseCache == null) {
            return;
        }

        // Should we cache this response for this request?
        RequestHeaders requestCacheHeaders = new RequestHeaders(uri, rawRequestHeaders);
        ResponseHeaders responseCacheHeaders = new ResponseHeaders(uri, rawResponseHeaders);
        if (!responseCacheHeaders.isCacheable(requestCacheHeaders)) {
            return;
        }

        // Offer this request to the cache.
        cacheRequest = responseCache.put(uri, getHttpConnectionToCache());
    }

    protected HttpURLConnection getHttpConnectionToCache() {
        return policy;
    }

    /**
     * Cause the socket connection to be released to the connection pool when
     * it is no longer needed. If it is already unneeded, it will be pooled
     * immediately.
     */
    public final void automaticallyReleaseConnectionToPool() {
        automaticallyReleaseConnectionToPool = true;
        if (connection != null && released) {
            HttpConnectionPool.INSTANCE.recycle(connection);
            connection = null;
        }
    }

    /**
     * Releases this connection so that it may be either reused or closed.
     */
    public final void releaseSocket(boolean reusable) {
        if (released || connection == null) {
            return;
        }
        released = true;

        // We cannot reuse sockets that have incomplete output.
        if (requestBodyOut != null && !requestBodyOut.closed) {
            reusable = false;
        }

        // If the headers specify that the connection shouldn't be reused, don't reuse it.
        if (hasConnectionCloseHeaders()) {
            reusable = false;
        }

        if (responseBodyIn instanceof UnknownLengthHttpInputStream) {
            reusable = false;
        }

        if (reusable && responseBodyIn != null) {
            // We must discard the response body before the connection can be reused.
            try {
                Streams.skipAll(responseBodyIn);
            } catch (IOException e) {
                reusable = false;
            }
        }

        if (!reusable) {
            connection.closeSocketAndStreams();
            connection = null;
        } else if (automaticallyReleaseConnectionToPool) {
            HttpConnectionPool.INSTANCE.recycle(connection);
            connection = null;
        }
    }

    private void initContentStream(InputStream transferStream) throws IOException {
        if (transparentGzip
                && "gzip".equalsIgnoreCase(rawResponseHeaders.get("Content-Encoding"))) {
            /*
             * If the response was transparently gzipped, remove the gzip header field
             * so clients don't double decompress. http://b/3009828
             */
            rawResponseHeaders.removeAll("Content-Encoding");
            responseBodyIn = new GZIPInputStream(transferStream);
        } else {
            responseBodyIn = transferStream;
        }
    }

    private InputStream getTransferStream() throws IOException {
        if (!hasResponseBody()) {
            return new FixedLengthInputStream(socketIn, cacheRequest, this, 0);
        }

        if ("chunked".equalsIgnoreCase(rawResponseHeaders.get("Transfer-Encoding"))) {
            return new ChunkedInputStream(socketIn, cacheRequest, this);
        }

        String contentLength = rawResponseHeaders.get("Content-Length");
        if (contentLength != null) {
            try {
                int length = Integer.parseInt(contentLength);
                return new FixedLengthInputStream(socketIn, cacheRequest, this, length);
            } catch (NumberFormatException ignored) {
            }
        }

        /*
         * Wrap the input stream from the HttpConnection (rather than
         * just returning "socketIn" directly here), so that we can control
         * its use after the reference escapes.
         */
        return new UnknownLengthHttpInputStream(socketIn, cacheRequest, this);
    }

    private void readResponseHeaders() throws IOException {
        RawHeaders headers;
        do {
            headers = new RawHeaders();
            headers.setStatusLine(Streams.readAsciiLine(socketIn));
            readHeaders(headers);
            setResponse(headers, null);
        } while (headers.getResponseCode() == HTTP_CONTINUE);
    }

    /**
     * Returns true if the response must have a (possibly 0-length) body.
     * See RFC 2616 section 4.3.
     */
    public final boolean hasResponseBody() {
        int responseCode = rawResponseHeaders.getResponseCode();
        if (method != HEAD
                && method != CONNECT
                && (responseCode < HTTP_CONTINUE || responseCode >= 200)
                && responseCode != HttpURLConnectionImpl.HTTP_NO_CONTENT
                && responseCode != HttpURLConnectionImpl.HTTP_NOT_MODIFIED) {
            return true;
        }

        /*
         * If the Content-Length or Transfer-Encoding headers disagree with the
         * response code, the response is malformed. For best compatibility, we
         * honor the headers.
         */
        String contentLength = rawResponseHeaders.get("Content-Length");
        if (contentLength != null && Integer.parseInt(contentLength) > 0) {
            return true;
        }
        if ("chunked".equalsIgnoreCase(rawResponseHeaders.get("Transfer-Encoding"))) {
            return true;
        }

        return false;
    }

    /**
     * Trailers are headers included after the last chunk of a response encoded
     * with chunked encoding.
     */
    final void readTrailers() throws IOException {
        readHeaders(rawResponseHeaders);
    }

    private void readHeaders(RawHeaders headers) throws IOException {
        // parse the result headers until the first blank line
        String line;
        while (!(line = Streams.readAsciiLine(socketIn)).isEmpty()) {
            headers.addLine(line);
        }

        CookieHandler cookieHandler = CookieHandler.getDefault();
        if (cookieHandler != null) {
            cookieHandler.put(uri, headers.toMultimap());
        }
    }

    /**
     * Prepares the HTTP headers and sends them to the server.
     *
     * <p>For streaming requests with a body, headers must be prepared
     * <strong>before</strong> the output stream has been written to. Otherwise
     * the body would need to be buffered!
     *
     * <p>For non-streaming requests with a body, headers must be prepared
     * <strong>after</strong> the output stream has been written to and closed.
     * This ensures that the {@code Content-Length} header field receives the
     * proper value.
     *
     * @param contentLength the number of bytes in the request body, or -1 if
     *      the request body length is unknown.
     */
    private void writeRequestHeaders(int contentLength) throws IOException {
        if (sentRequestMillis != -1) {
            throw new IllegalStateException();
        }

        RawHeaders headersToSend = getNetworkRequestHeaders();
        byte[] bytes = headersToSend.toHeaderString().getBytes(Charsets.ISO_8859_1);

        if (contentLength != -1 && bytes.length + contentLength <= MAX_REQUEST_BUFFER_LENGTH) {
            requestOut = new BufferedOutputStream(socketOut, bytes.length + contentLength);
        }

        sentRequestMillis = System.currentTimeMillis();
        requestOut.write(bytes);
    }

    /**
     * Returns the headers to send on a network request.
     *
     * <p>This adds the content length and content-type headers, which are
     * neither needed nor known when querying the response cache.
     *
     * <p>It updates the status line, which may need to be fully qualified if
     * the connection is using a proxy.
     */
    protected RawHeaders getNetworkRequestHeaders() throws IOException {
        rawRequestHeaders.setStatusLine(getRequestLine());

        int fixedContentLength = policy.getFixedContentLength();
        if (fixedContentLength != -1) {
            rawRequestHeaders.addIfAbsent("Content-Length", Integer.toString(fixedContentLength));
        } else if (sendChunked) {
            rawRequestHeaders.addIfAbsent("Transfer-Encoding", "chunked");
        } else if (requestBodyOut instanceof RetryableOutputStream) {
            int size = ((RetryableOutputStream) requestBodyOut).contentLength();
            rawRequestHeaders.addIfAbsent("Content-Length", Integer.toString(size));
        }

        return rawRequestHeaders;
    }

    /**
     * Populates requestHeaders with defaults and cookies.
     *
     * <p>This client doesn't specify a default {@code Accept} header because it
     * doesn't know what content types the application is interested in.
     */
    private void prepareRawRequestHeaders() throws IOException {
        rawRequestHeaders.setStatusLine(getRequestLine());

        if (rawRequestHeaders.get("User-Agent") == null) {
            rawRequestHeaders.add("User-Agent", getDefaultUserAgent());
        }

        if (rawRequestHeaders.get("Host") == null) {
            rawRequestHeaders.add("Host", getOriginAddress(policy.getURL()));
        }

        if (httpMinorVersion > 0) {
            rawRequestHeaders.addIfAbsent("Connection", "Keep-Alive");
        }

        if (rawRequestHeaders.get("Accept-Encoding") == null) {
            transparentGzip = true;
            rawRequestHeaders.add("Accept-Encoding", "gzip");
        }

        if (hasRequestBody()) {
            rawRequestHeaders.addIfAbsent("Content-Type", "application/x-www-form-urlencoded");
        }

        long ifModifiedSince = policy.getIfModifiedSince();
        if (ifModifiedSince != 0) {
            rawRequestHeaders.add("If-Modified-Since", HttpDate.format(new Date(ifModifiedSince)));
        }

        CookieHandler cookieHandler = CookieHandler.getDefault();
        if (cookieHandler != null) {
            Map<String, List<String>> allCookieHeaders
                    = cookieHandler.get(uri, rawRequestHeaders.toMultimap());
            for (Map.Entry<String, List<String>> entry : allCookieHeaders.entrySet()) {
                String key = entry.getKey();
                if ("Cookie".equalsIgnoreCase(key) || "Cookie2".equalsIgnoreCase(key)) {
                    rawRequestHeaders.addAll(key, entry.getValue());
                }
            }
        }
    }

    private String getRequestLine() {
        String protocol = (httpMinorVersion == 0) ? "HTTP/1.0" : "HTTP/1.1";
        return method + " " + requestString() + " " + protocol;
    }

    private String requestString() {
        URL url = policy.getURL();
        if (includeAuthorityInRequestLine()) {
            return url.toString();
        } else {
            String fileOnly = url.getFile();
            if (fileOnly == null || fileOnly.isEmpty()) {
                fileOnly = "/";
            }
            return fileOnly;
        }
    }

    /**
     * Returns true if the request line should contain the full URL with host
     * and port (like "GET http://android.com/foo HTTP/1.1") or only the path
     * (like "GET /foo HTTP/1.1").
     *
     * <p>This is non-final because for HTTPS it's never necessary to supply the
     * full URL, even if a proxy is in use.
     */
    protected boolean includeAuthorityInRequestLine() {
        return policy.usingProxy();
    }

    protected final String getDefaultUserAgent() {
        String agent = System.getProperty("http.agent");
        return agent != null ? agent : ("Java" + System.getProperty("java.version"));
    }

    private boolean hasConnectionCloseHeaders() {
        return (rawResponseHeaders != null
                && "close".equalsIgnoreCase(rawResponseHeaders.get("Connection")))
                || ("close".equalsIgnoreCase(rawRequestHeaders.get("Connection")));
    }

    protected final String getOriginAddress(URL url) {
        int port = url.getPort();
        String result = url.getHost();
        if (port > 0 && port != policy.getDefaultPort()) {
            result = result + ":" + port;
        }
        return result;
    }

    protected boolean requiresTunnel() {
        return false;
    }

    /**
     * Flushes the remaining request header and body, parses the HTTP response
     * headers and starts reading the HTTP response body if it exists.
     */
    public final void readResponse() throws IOException {
        if (hasResponse()) {
            return;
        }

        if (responseSource == null) {
            throw new IllegalStateException("readResponse() without sendRequest()");
        }

        if (!responseSource.requiresConnection()) {
            return;
        }

        if (sentRequestMillis == -1) {
            int contentLength = requestBodyOut instanceof RetryableOutputStream
                    ? ((RetryableOutputStream) requestBodyOut).contentLength()
                    : -1;
            writeRequestHeaders(contentLength);
        }

        if (requestBodyOut != null) {
            requestBodyOut.close();
            if (requestBodyOut instanceof RetryableOutputStream) {
                ((RetryableOutputStream) requestBodyOut).writeToSocket(requestOut);
            }
        }

        requestOut.flush();
        requestOut = socketOut;

        readResponseHeaders();
        rawResponseHeaders.add(ResponseHeaders.SENT_MILLIS, Long.toString(sentRequestMillis));
        rawResponseHeaders.add(ResponseHeaders.RECEIVED_MILLIS,
                Long.toString(System.currentTimeMillis()));

        if (responseSource == ResponseSource.CONDITIONAL_CACHE) {
            if (responseHeadersToValidate.validate(new ResponseHeaders(uri, rawResponseHeaders))) {
                // discard the network response
                releaseSocket(true);

                // use the cache response
                setResponse(responseHeadersToValidate.headers, responseBodyToValidate);
                responseBodyToValidate = null;
                return;
            } else {
                IoUtils.closeQuietly(responseBodyToValidate);
                responseBodyToValidate = null;
                responseHeadersToValidate = null;
            }
        }

        if (hasResponseBody()) {
            maybeCache(); // reentrant. this calls into user code which may call back into this!
        }

        initContentStream(getTransferStream());
    }
}
