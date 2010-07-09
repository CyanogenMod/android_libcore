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

package org.apache.harmony.luni.internal.net.www.protocol.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.CookieHandler;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ResponseCache;
import java.net.SocketPermission;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.Charsets;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.apache.harmony.luni.util.Base64;
import org.apache.harmony.luni.util.PriviAction;

/**
 * This subclass extends <code>HttpURLConnection</code> which in turns extends
 * <code>URLConnection</code> This is the actual class that "does the work",
 * such as connecting, sending request and getting the content from the remote
 * server.
 */
public class HttpURLConnectionImpl extends HttpURLConnection {
    public static final String OPTIONS = "OPTIONS";
    public static final String GET = "GET";
    public static final String HEAD = "HEAD";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String TRACE = "TRACE";
    public static final String CONNECT = "CONNECT";

    public static final int HTTP_CONTINUE = 100;

    public static final int MAX_REDIRECTS = 4;

    /**
     * The subset of HTTP methods that the user may select via {@link #setRequestMethod}.
     */
    public static String PERMITTED_USER_METHODS[] = {
            OPTIONS,
            GET,
            HEAD,
            POST,
            PUT,
            DELETE,
            TRACE
            // Note: we don't allow users to specify "CONNECT"
    };

    private final int defaultPort;

    /**
     * The version this client will use. Either 0 for HTTP/1.0, or 1 for
     * HTTP/1.1. Upon receiving a non-HTTP/1.1 response, this client
     * automatically sets its version to HTTP/1.0.
     */
    private int httpVersion = 1; // Assume HTTP/1.1

    protected HttpConnection connection;

    private InputStream is;

    private InputStream uis;

    private OutputStream socketOut;

    private ResponseCache responseCache;

    private CacheResponse cacheResponse;

    private CacheRequest cacheRequest;

    private boolean hasTriedCache;

    private AbstractHttpOutputStream os;

    private boolean sentRequestHeaders;

    boolean sendChunked;

    private String proxyName;

    private int hostPort = -1;

    private String hostName;

    private InetAddress hostAddress;

    // proxy which is used to make the connection.
    private Proxy proxy;

    // the destination URI
    private URI uri;

    // default request header
    private static Header defaultReqHeader = new Header();

    // request header that will be sent to the server
    private Header reqHeader;

    // response header received from the server
    private Header resHeader;

    private int redirectionCount;

    /**
     * Creates an instance of the <code>HttpURLConnection</code>
     *
     * @param url
     *            URL The URL this connection is connecting
     * @param port
     *            int The default connection port
     */
    protected HttpURLConnectionImpl(URL url, int port) {
        super(url);
        defaultPort = port;
        reqHeader = (Header) defaultReqHeader.clone();

        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            // do nothing.
        }
        responseCache = AccessController
                .doPrivileged(new PrivilegedAction<ResponseCache>() {
                    public ResponseCache run() {
                        return ResponseCache.getDefault();
                    }
                });
    }

    /**
     * Creates an instance of the <code>HttpURLConnection</code>
     *
     * @param url
     *            URL The URL this connection is connecting
     * @param port
     *            int The default connection port
     * @param proxy
     *            Proxy The proxy which is used to make the connection
     */
    protected HttpURLConnectionImpl(URL url, int port, Proxy proxy) {
        this(url, port);
        this.proxy = proxy;
    }

    /**
     * Establishes the connection to the remote HTTP server
     *
     * Any methods that requires a valid connection to the resource will call
     * this method implicitly. After the connection is established,
     * <code>connected</code> is set to true.
     *
     *
     * @see #connected
     * @see java.io.IOException
     * @see URLStreamHandler
     */
    @Override
    public void connect() throws IOException {
        if (connected) {
            return;
        }
        if (getFromCache()) {
            return;
        }
        // BEGIN android-changed
        // url.toURI(); throws an URISyntaxException if the url contains
        // illegal characters in e.g. the query.
        // Since the query is not needed for proxy selection, we just create an
        // URI that only contains the necessary information.
        try {
            uri = new URI(url.getProtocol(),
                          null,
                          url.getHost(),
                          url.getPort(),
                          url.getPath(),
                          null,
                          null);
        } catch (URISyntaxException e1) {
            throw new IOException(e1.getMessage());
        }
        // END android-changed
        // socket to be used for connection
        connection = null;
        // try to determine: to use the proxy or not
        if (proxy != null) {
            // try to make the connection to the proxy
            // specified in constructor.
            // IOException will be thrown in the case of failure
            connection = getHTTPConnection(proxy);
        } else {
            // Use system-wide ProxySelect to select proxy list,
            // then try to connect via elements in the proxy list.
            ProxySelector selector = ProxySelector.getDefault();
            List<Proxy> proxyList = selector.select(uri);
            if (proxyList != null) {
                for (Proxy selectedProxy : proxyList) {
                    if (selectedProxy.type() == Proxy.Type.DIRECT) {
                        // the same as NO_PROXY
                        continue;
                    }
                    try {
                        connection = getHTTPConnection(selectedProxy);
                        proxy = selectedProxy;
                        break; // connected
                    } catch (IOException e) {
                        // failed to connect, tell it to the selector
                        selector.connectFailed(uri, selectedProxy.address(), e);
                    }
                }
            }
        }
        if (connection == null) {
            // make direct connection
            connection = getHTTPConnection(null);
        }
        connection.setSoTimeout(getReadTimeout());
        setUpTransportIO(connection);
        connected = true;
    }

    /**
     * Returns connected socket to be used for this HTTP connection.
     */
    protected HttpConnection getHTTPConnection(Proxy proxy) throws IOException {
        HttpConnection.Address address;
        if (proxy == null || proxy.type() == Proxy.Type.DIRECT) {
            this.proxy = null; // not using proxy
            address = new HttpConnection.Address(uri);
        } else {
            address = new HttpConnection.Address(uri, proxy, requiresTunnel());
        }
        return HttpConnectionPool.INSTANCE.get(address, getConnectTimeout());
    }

    /**
     * Sets up the data streams used to send request[s] and read response[s].
     *
     * @param connection
     *            HttpConnection to be used
     */
    protected void setUpTransportIO(HttpConnection connection) throws IOException {
        socketOut = connection.getOutputStream();
        is = connection.getInputStream();
    }

    /**
     * Returns true if the input streams are prepared to return data from the
     * cache.
     */
    private boolean getFromCache() throws IOException {
        if (!useCaches || responseCache == null || hasTriedCache) {
            return (hasTriedCache && is != null);
        }

        hasTriedCache = true;
        if (resHeader == null) {
            resHeader = new Header();
        }
        cacheResponse = responseCache.get(uri, method, resHeader.getFieldMap());
        if (cacheResponse == null) {
            return is != null;
        }
        Map<String, List<String>> headersMap = cacheResponse.getHeaders();
        if (headersMap != null) {
            resHeader = new Header(headersMap);
        }
        is = uis = cacheResponse.getBody();
        return is != null;
    }

    private void maybeCache() throws IOException {
        // Are we caching at all?
        if (!useCaches || responseCache == null) {
            return;
        }
        // Should we cache this particular response code?
        // TODO: cache response code 300 HTTP_MULT_CHOICE ?
        if (responseCode != HTTP_OK && responseCode != HTTP_NOT_AUTHORITATIVE &&
                responseCode != HTTP_PARTIAL && responseCode != HTTP_MOVED_PERM &&
                responseCode != HTTP_GONE) {
            return;
        }
        // Offer this request to the cache.
        cacheRequest = responseCache.put(uri, this);
    }

    /**
     * Closes the connection with the HTTP server
     *
     *
     * @see URLConnection#connect()
     */
    @Override
    public void disconnect() {
        releaseSocket(true);
    }

    /**
     * Releases this connection so that it may be either closed or reused.
     *
     * @param closeSocket true if the socket must not be recycled.
     */
    protected synchronized void releaseSocket(boolean closeSocket) {
        if (connection != null) {
            if (closeSocket || ((os != null) && !os.closed)) {
                /*
                 * In addition to closing the socket if explicitly
                 * requested to do so, we also close it if there was
                 * an output stream associated with the request and it
                 * wasn't cleanly closed.
                 */
                connection.closeSocketAndStreams();
            } else {
                HttpConnectionPool.INSTANCE.recycle(connection);
            }
            connection = null;
        }

        /*
         * Clear "is" and "socketOut" to ensure that no further I/O attempts
         * from this instance make their way to the underlying
         * connection (which may get recycled).
         */
        is = null;
        socketOut = null;
    }

    /**
     * Discard all state initialized from the HTTP response including response
     * code, message, headers and body.
     */
    protected void discardResponse() {
        responseCode = -1;
        responseMessage = null;
        resHeader = null;
        uis = null;
    }

    protected void endRequest() throws IOException {
        if (os != null) {
            os.close();
        }
        sentRequestHeaders = false;
    }

    /**
     * Returns an input stream from the server in the case of error such as the
     * requested file (txt, htm, html) is not found on the remote server.
     * <p>
     * If the content type is not what stated above,
     * <code>FileNotFoundException</code> is thrown.
     *
     * @return InputStream the error input stream returned by the server.
     */
    @Override
    public InputStream getErrorStream() {
        if (connected && method != HEAD && responseCode >= HTTP_BAD_REQUEST) {
            return uis;
        }
        return null;
    }

    /**
     * Returns the value of the field at position <code>pos<code>.
     * Returns <code>null</code> if there is fewer than <code>pos</code> fields
     * in the response header.
     *
     * @return java.lang.String     The value of the field
     * @param pos int               the position of the field from the top
     *
     * @see         #getHeaderField(String)
     * @see         #getHeaderFieldKey
     */
    @Override
    public String getHeaderField(int pos) {
        try {
            getInputStream();
        } catch (IOException e) {
            // ignore
        }
        if (null == resHeader) {
            return null;
        }
        return resHeader.get(pos);
    }

    /**
     * Returns the value of the field corresponding to the <code>key</code>
     * Returns <code>null</code> if there is no such field.
     *
     * If there are multiple fields with that key, the last field value is
     * returned.
     *
     * @return java.lang.String The value of the header field
     * @param key
     *            java.lang.String the name of the header field
     *
     * @see #getHeaderField(int)
     * @see #getHeaderFieldKey
     */
    @Override
    public String getHeaderField(String key) {
        try {
            getInputStream();
        } catch (IOException e) {
            // ignore
        }
        if (null == resHeader) {
            return null;
        }
        return resHeader.get(key);
    }

    @Override
    public String getHeaderFieldKey(int pos) {
        try {
            getInputStream();
        } catch (IOException e) {
            // ignore
        }
        if (null == resHeader) {
            return null;
        }
        return resHeader.getKey(pos);
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        try {
            retrieveResponse();
        } catch (IOException ignored) {
        }
        return resHeader != null ? resHeader.getFieldMap() : null;
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        if (connected) {
            throw new IllegalStateException("Cannot access request header fields after connection is set");
        }
        return reqHeader.getFieldMap();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (!doInput) {
            throw new ProtocolException("This protocol does not support input");
        }

        retrieveResponse();

        /*
         * if the requested file does not exist, throw an exception formerly the
         * Error page from the server was returned if the requested file was
         * text/html this has changed to return FileNotFoundException for all
         * file types
         */
        if (responseCode >= HTTP_BAD_REQUEST) {
            throw new FileNotFoundException(url.toString());
        }

        if (uis == null) {
            // probably a reentrant call, such as from ResponseCache.put()
            throw new IllegalStateException(
                    "getInputStream() is not available. Is this a reentrant call?");
        }

        return uis;
    }

    private InputStream initContentStream() throws IOException {
        if (!hasResponseBody()) {
            return uis = new FixedLengthInputStream(is, cacheRequest, this, 0);
        }

        String encoding = resHeader.get("Transfer-Encoding");
        if (encoding != null && encoding.toLowerCase().equals("chunked")) {
            return uis = new ChunkedInputStream(is, cacheRequest, this);
        }

        String sLength = resHeader.get("Content-Length");
        if (sLength != null) {
            try {
                int length = Integer.parseInt(sLength);
                return uis = new FixedLengthInputStream(is, cacheRequest, this, length);
            } catch (NumberFormatException ignored) {
            }
        }

        /*
         * Wrap the input stream from the HttpConnection (rather than
         * just returning "is" directly here), so that we can control
         * its use after the reference escapes.
         */
        return uis = new UnknownLengthHttpInputStream(is, cacheRequest, this);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (!doOutput) {
            throw new ProtocolException("Does not support output");
        }

        // you can't write after you read
        if (sentRequestHeaders) {
            // TODO: just return 'os' if that's non-null?
            throw new ProtocolException(
                    "OutputStream unavailable because request headers have already been sent!");
        }

        if (os != null) {
            return os;
        }

        // they are requesting a stream to write to. This implies a POST method
        if (method == GET) {
            method = POST;
        }

        // If the request method is neither PUT or POST, then you're not writing
        if (method != PUT && method != POST) {
            throw new ProtocolException(method + " does not support writing");
        }

        int contentLength = -1;
        String contentLengthString = reqHeader.get("Content-Length");
        if (contentLengthString != null) {
            contentLength = Integer.parseInt(contentLengthString);
        }

        String encoding = reqHeader.get("Transfer-Encoding");
        if (chunkLength > 0 || "chunked".equalsIgnoreCase(encoding)) {
            sendChunked = true;
            contentLength = -1;
        }

        if (!connected) {
            // connect and see if there is cache available.
            connect();
        }
        if (socketOut == null) {
             // TODO: what should we do if a cached response exists?
            throw new IOException("No socket to write to; was a POST cached?");
        }

        if (httpVersion == 0) {
            sendChunked = false;
        }

        if (fixedContentLength != -1) {
            writeRequestHeaders(socketOut);
            os = new FixedLengthOutputStream(socketOut, fixedContentLength);
        } else if (sendChunked) {
            writeRequestHeaders(socketOut);
            os = new ChunkedOutputStream(socketOut, chunkLength);
        } else if (contentLength != -1) {
            os = new RetryableOutputStream(contentLength);
        } else {
            os = new RetryableOutputStream();
        }
        return os;
    }

    @Override
    public Permission getPermission() throws IOException {
        return new SocketPermission(getHostName() + ":" + getHostPort(), "connect, resolve");
    }

    @Override
    public String getRequestProperty(String field) {
        if (null == field) {
            return null;
        }
        return reqHeader.get(field);
    }

    /**
     * Returns the characters up to but not including the next "\r\n", "\n", or
     * the end of the stream, consuming the end of line delimiter.
     */
    static String readLine(InputStream is) throws IOException {
        StringBuilder result = new StringBuilder(80);
        while (true) {
            int c = is.read();
            if (c == -1 || c == '\n') {
                break;
            }

            result.append((char) c);
        }
        int length = result.length();
        if (length > 0 && result.charAt(length - 1) == '\r') {
            result.setLength(length - 1);
        }
        return result.toString();
    }

    protected String requestString() {
        if (usingProxy() || proxyName != null) {
            return url.toString();
        }
        String file = url.getFile();
        if (file == null || file.length() == 0) {
            file = "/";
        }
        return file;
    }

    private void readResponseHeaders() throws IOException {
        do {
            responseCode = -1;
            responseMessage = null;
            resHeader = new Header();
            resHeader.setStatusLine(readLine(is).trim());
            readHeaders();
        } while (parseResponseCode() == HTTP_CONTINUE);
    }

    /**
     * Returns true if the response must have a (possibly 0-length) body.
     * See RFC 2616 section 4.3.
     */
    private boolean hasResponseBody() {
        return method != HEAD
                && method != CONNECT
                && (responseCode < HTTP_CONTINUE || responseCode >= 200)
                && responseCode != HTTP_NO_CONTENT
                && responseCode != HTTP_NOT_MODIFIED;
    }

    @Override
    public int getResponseCode() throws IOException {
        retrieveResponse();
        return responseCode;
    }

    private int parseResponseCode() {
        // Response Code Sample : "HTTP/1.0 200 OK"
        String response = resHeader.getStatusLine();
        if (response == null || !response.startsWith("HTTP/")) {
            return -1;
        }
        response = response.trim();
        int mark = response.indexOf(" ") + 1;
        if (mark == 0) {
            return -1;
        }
        if (response.charAt(mark - 2) != '1') {
            httpVersion = 0;
        }
        int last = mark + 3;
        if (last > response.length()) {
            last = response.length();
        }
        responseCode = Integer.parseInt(response.substring(mark, last));
        if (last + 1 <= response.length()) {
            responseMessage = response.substring(last + 1);
        }
        return responseCode;
    }

    void readHeaders() throws IOException {
        // parse the result headers until the first blank line
        String line;
        while ((line = readLine(is)).length() > 1) {
            // Header parsing
            int index = line.indexOf(":");
            if (index == -1) {
                resHeader.add("", line.trim());
            } else {
                resHeader.add(line.substring(0, index), line.substring(index + 1).trim());
            }
        }

        CookieHandler cookieHandler = CookieHandler.getDefault();
        if (cookieHandler != null) {
            cookieHandler.put(uri, resHeader.getFieldMap());
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
     * This ensures that the {@code Content-Length} header receives the proper
     * value.
     */
    private void writeRequestHeaders(OutputStream out) throws IOException {
        prepareRequestHeaders();

        StringBuilder result = new StringBuilder(256);
        result.append(reqHeader.getStatusLine()).append("\r\n");
        for (int i = 0; i < reqHeader.length(); i++) {
            String key = reqHeader.getKey(i);
            String value = reqHeader.get(i);
            if (key != null) {
                result.append(key).append(": ").append(value).append("\r\n");
            }
        }
        result.append("\r\n");
        out.write(result.toString().getBytes(Charsets.ISO_8859_1));
        sentRequestHeaders = true;
    }

    /**
     * Populates reqHeader with the HTTP headers to be sent. Header values are
     * derived from the request itself and the cookie manager.
     *
     * <p>This client doesn't specify a default {@code Accept} header because it
     * doesn't know what content types the application is interested in.
     */
    private void prepareRequestHeaders() throws IOException {
        String protocol = (httpVersion == 0) ? "HTTP/1.0" : "HTTP/1.1";
        reqHeader.setStatusLine(method + " " + requestString() + " " + protocol);

        if (reqHeader.get("User-Agent") == null) {
            String agent = getSystemProperty("http.agent");
            if (agent == null) {
                agent = "Java" + getSystemProperty("java.version");
            }
            reqHeader.add("User-Agent", agent);
        }

        if (reqHeader.get("Host") == null) {
            int port = url.getPort();
            String host = (port > 0 && port != defaultPort)
                    ? url.getHost() + ":" + port
                    : url.getHost();
            reqHeader.add("Host", host);
        }

        if (httpVersion > 0) {
            reqHeader.addIfAbsent("Connection", "Keep-Alive");
        }

        if (fixedContentLength != -1) {
            reqHeader.addIfAbsent("Content-Length", Integer.toString(fixedContentLength));
        } else if (sendChunked) {
            reqHeader.addIfAbsent("Transfer-Encoding", "chunked");
        } else if (os instanceof RetryableOutputStream) {
            int size = ((RetryableOutputStream) os).contentLength();
            reqHeader.addIfAbsent("Content-Length", Integer.toString(size));
        }

        if (os != null) {
            reqHeader.addIfAbsent("Content-Type", "application/x-www-form-urlencoded");
        }

        CookieHandler cookieHandler = CookieHandler.getDefault();
        if (cookieHandler != null) {
            Map<String, List<String>> allCookieHeaders
                    = cookieHandler.get(uri, reqHeader.getFieldMap());
            for (Map.Entry<String, List<String>> entry : allCookieHeaders.entrySet()) {
                String key = entry.getKey();
                if ("Cookie".equalsIgnoreCase(key) || "Cookie2".equalsIgnoreCase(key)) {
                    reqHeader.addAll(key, entry.getValue());
                }
            }
        }
    }

    /**
     * A slightly different implementation from this parent's
     * <code>setIfModifiedSince()</code> Since this HTTP impl supports
     * IfModifiedSince as one of the header field, the request header is updated
     * with the new value.
     *
     *
     * @param newValue
     *            the number of millisecond since epoch
     *
     * @throws IllegalStateException
     *             if already connected.
     */
    @Override
    public void setIfModifiedSince(long newValue) {
        super.setIfModifiedSince(newValue);
        // convert from millisecond since epoch to date string
        SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date(newValue));
        reqHeader.add("If-Modified-Since", date);
    }

    @Override
    public void setRequestProperty(String field, String newValue) {
        if (connected) {
            throw new IllegalStateException("Cannot set method after connection is made");
        }
        if (field == null) {
            throw new NullPointerException();
        }
        reqHeader.set(field, newValue);
    }

    @Override
    public void addRequestProperty(String field, String value) {
        if (connected) {
            throw new IllegalAccessError("Cannot set method after connection is made");
        }
        if (field == null) {
            throw new NullPointerException();
        }
        reqHeader.add(field, value);
    }

    /**
     * Get the connection port. This is either the URL's port or the proxy port
     * if a proxy port has been set.
     */
    private int getHostPort() {
        if (hostPort < 0) {
            // the value was not set yet
            if (proxy != null) {
                hostPort = ((InetSocketAddress) proxy.address()).getPort();
            } else {
                hostPort = url.getPort();
            }
            if (hostPort < 0) {
                hostPort = defaultPort;
            }
        }
        return hostPort;
    }

    /**
     * Get the InetAddress of the connection machine. This is either the address
     * given in the URL or the address of the proxy server.
     */
    private InetAddress getHostAddress() throws IOException {
        if (hostAddress == null) {
            // the value was not set yet
            if (proxy != null && proxy.type() != Proxy.Type.DIRECT) {
                hostAddress = ((InetSocketAddress) proxy.address())
                        .getAddress();
            } else {
                hostAddress = InetAddress.getByName(url.getHost());
            }
        }
        return hostAddress;
    }

    /**
     * Get the hostname of the connection machine. This is either the name given
     * in the URL or the name of the proxy server.
     */
    private String getHostName() {
        if (hostName == null) {
            // the value was not set yet
            if (proxy != null) {
                hostName = ((InetSocketAddress) proxy.address()).getHostName();
            } else {
                hostName = url.getHost();
            }
        }
        return hostName;
    }

    private String getSystemProperty(final String property) {
        return AccessController.doPrivileged(new PriviAction<String>(property));
    }

    @Override
    public boolean usingProxy() {
        return (proxy != null && proxy.type() != Proxy.Type.DIRECT);
    }

    protected boolean requiresTunnel() {
        return false;
    }

    /**
     * Aggressively tries to get the final HTTP response, potentially making
     * many HTTP requests in the process in order to cope with redirects and
     * authentication.
     */
    protected final void retrieveResponse() throws IOException {
        if (resHeader != null) {
            return;
        }

        redirectionCount = 0;
        while (true) {
            connect();

            // if we can get a response from the cache, we're done
            if (cacheResponse != null) {
                // TODO: how does this interact with redirects? Consider processing the headers so
                // that a redirect is never returned.
                return;
            }

            if (!sentRequestHeaders) {
                writeRequestHeaders(socketOut);
            }

            if (os != null) {
                os.close();
                if (os instanceof RetryableOutputStream) {
                    ((RetryableOutputStream) os).writeToSocket(socketOut);
                }
            }

            socketOut.flush();

            readResponseHeaders();

            if (hasResponseBody()) {
                maybeCache(); // reentrant. this calls into user code which may call back into this!
            }

            initContentStream();

            if (processResponseHeaders()) {
                return;
            }

            /*
             * The first request wasn't sufficient. Prepare for another...
             */

            // TODO: read the response body so we don't have to create another socket connection?
            uis.close();
            uis = null;
            endRequest();
            releaseSocket(true);
            connected = false;

            if (os != null && !(os instanceof RetryableOutputStream)) {
                throw new HttpRetryException("Cannot retry streamed HTTP body", responseCode);
            }
        }
    }

    /**
     * Returns true if this is a final response; otherwise the current response
     * is an intermediate result and another request should be sent.
     */
    private boolean processResponseHeaders() throws IOException {
        switch (responseCode) {
            case HTTP_PROXY_AUTH: // proxy authorization failed ?
                if (!usingProxy()) {
                    throw new IOException(
                            "Received HTTP_PROXY_AUTH (407) code while not using proxy");
                }
                return processAuthHeader("Proxy-Authenticate", "Proxy-Authorization");

            case HTTP_UNAUTHORIZED: // HTTP authorization failed ?
                return processAuthHeader("WWW-Authenticate", "Authorization");

            case HTTP_MULT_CHOICE:
            case HTTP_MOVED_PERM:
            case HTTP_MOVED_TEMP:
            case HTTP_SEE_OTHER:
            case HTTP_USE_PROXY:

                /*
                 * See if there is a server redirect to the URL, but only handle 1
                 * level of URL redirection from the server to avoid being caught in
                 * an infinite loop
                 */

                if (!getInstanceFollowRedirects()) {
                    return true;
                }
                if (os != null) {
                    return true; // TODO: we could follow redirects for retryable output streams...
                }
                if (++redirectionCount > MAX_REDIRECTS) {
                    throw new ProtocolException("Too many redirects");
                }
                String location = getHeaderField("Location");
                if (location == null) {
                    return true;
                }
                if (responseCode == HTTP_USE_PROXY) {
                    int start = 0;
                    if (location.startsWith(url.getProtocol() + ':')) {
                        start = url.getProtocol().length() + 1;
                    }
                    if (location.startsWith("//", start)) {
                        start += 2;
                    }
                    setProxy(location.substring(start));
                } else {
                    url = new URL(url, location);
                    hostName = url.getHost();
                    // reset the port
                    hostPort = -1;
                }
                return false;

            default:
                return true;
        }
    }

    /**
     * Returns true if authorization failed permanently; false if we should
     * retry with new credentials.
     */
    private boolean processAuthHeader(String responseHeader, String followUpRequestHeader)
            throws IOException {
        // keep asking for username/password until authorized
        String challenge = resHeader.get(responseHeader);
        if (challenge == null) {
            throw new IOException("Received authentication challenge is null");
        }
        String credentials = getAuthorizationCredentials(challenge);
        if (credentials == null) {
            return true; // could not find credentials, end request cycle
        }
        // set up the authorization credentials
        connected = false;
        setRequestProperty(followUpRequestHeader, credentials);
        return false;
    }

    /**
     * Returns the authorization credentials on the base of provided
     * authorization challenge
     *
     * @param challenge
     * @return authorization credentials
     * @throws IOException
     */
    private String getAuthorizationCredentials(String challenge) throws IOException {
        int idx = challenge.indexOf(" ");
        if (idx == -1) {
            return null;
        }
        String scheme = challenge.substring(0, idx);
        int realm = challenge.indexOf("realm=\"") + 7;
        String prompt = null;
        if (realm != -1) {
            int end = challenge.indexOf('"', realm);
            if (end != -1) {
                prompt = challenge.substring(realm, end);
            }
        }
        // The following will use the user-defined authenticator to get
        // the password
        PasswordAuthentication pa = Authenticator
                .requestPasswordAuthentication(getHostAddress(), getHostPort(),
                        url.getProtocol(), prompt, scheme);
        if (pa == null) {
            // could not retrieve the credentials
            return null;
        }
        // base64 encode the username and password
        String usernameAndPassword = pa.getUserName() + ":" + new String(pa.getPassword());
        byte[] bytes = usernameAndPassword.getBytes(Charsets.ISO_8859_1);
        String encoded = Base64.encode(bytes, Charsets.ISO_8859_1);
        return scheme + " " + encoded;
    }

    private void setProxy(String proxy) {
        int index = proxy.indexOf(':');
        if (index == -1) {
            proxyName = proxy;
            hostPort = defaultPort;
        } else {
            proxyName = proxy.substring(0, index);
            String port = proxy.substring(index + 1);
            try {
                hostPort = Integer.parseInt(port);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port: " + port);
            }
            if (hostPort < 0 || hostPort > 65535) {
                throw new IllegalArgumentException("Port out of range: " + hostPort);
            }
        }
    }
}
