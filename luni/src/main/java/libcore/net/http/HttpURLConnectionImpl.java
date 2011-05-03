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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketPermission;
import java.net.URL;
import java.nio.charset.Charsets;
import java.security.Permission;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.harmony.luni.util.Base64;

/**
 * This implementation uses HttpEngine to send requests and receive responses.
 * This class may use multiple HttpEngines to follow redirects, authentication
 * retries, etc. to retrieve the final response body.
 *
 * <h3>What does 'connected' mean?</h3>
 * This class inherits a {@code connected} field from the superclass. That field
 * is <strong>not</strong> used to indicate not whether this URLConnection is
 * currently connected. Instead, it indicates whether a connection has ever been
 * attempted. Once a connection has been attempted, certain properties (request
 * header fields, request method, etc.) are immutable. Test the {@code
 * connection} field on this class for null/non-null to determine of an instance
 * is currently connected to a server.
 */
public class HttpURLConnectionImpl extends HttpURLConnection {

    private final int defaultPort;

    private Proxy proxy;

    // TODO: should these be set by URLConnection.setDefaultRequestProperty ?
    private static RawHeaders defaultRequestHeaders = new RawHeaders();

    protected RawHeaders rawRequestHeaders = new RawHeaders(defaultRequestHeaders);

    private int redirectionCount;

    protected IOException httpEngineFailure;
    protected HttpEngine httpEngine;

    protected HttpURLConnectionImpl(URL url, int port) {
        super(url);
        defaultPort = port;
    }

    protected HttpURLConnectionImpl(URL url, int port, Proxy proxy) {
        this(url, port);
        this.proxy = proxy;
    }

    @Override public final void connect() throws IOException {
        initHttpEngine();
        try {
            httpEngine.sendRequest();
        } catch (IOException e) {
            httpEngineFailure = e;
            throw e;
        }
    }

    /**
     * Close the socket connection to the remote origin server or proxy.
     */
    @Override public final void disconnect() {
        // TODO: what happens if they call disconnect() before connect?
        if (httpEngine != null) {
            httpEngine.releaseSocket(false);
        }
    }

    /**
     * Returns an input stream from the server in the case of error such as the
     * requested file (txt, htm, html) is not found on the remote server.
     */
    @Override public final InputStream getErrorStream() {
        try {
            HttpEngine response = getResponse();
            if (response.hasResponseBody()
                    && response.getResponseHeaders().getResponseCode() >= HTTP_BAD_REQUEST) {
                return response.getResponseBody();
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the value of the field at {@code position}. Returns null if there
     * are fewer than {@code position} headers.
     */
    @Override public final String getHeaderField(int position) {
        try {
            return getResponse().getResponseHeaders().getValue(position);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the value of the field corresponding to the {@code fieldName}, or
     * null if there is no such field. If the field has multiple values, the
     * last value is returned.
     */
    @Override public final String getHeaderField(String fieldName) {
        try {
            RawHeaders responseHeaders = getResponse().getResponseHeaders();
            return fieldName == null
                    ? responseHeaders.getStatusLine()
                    : responseHeaders.get(fieldName);
        } catch (IOException e) {
            return null;
        }
    }

    @Override public final String getHeaderFieldKey(int position) {
        try {
            return getResponse().getResponseHeaders().getFieldName(position);
        } catch (IOException e) {
            return null;
        }
    }

    @Override public final Map<String, List<String>> getHeaderFields() {
        try {
            return getResponse().getResponseHeaders().toMultimap();
        } catch (IOException e) {
            return null;
        }
    }

    @Override public final Map<String, List<String>> getRequestProperties() {
        if (connected) {
            throw new IllegalStateException(
                    "Cannot access request header fields after connection is set");
        }
        return rawRequestHeaders.toMultimap();
    }

    @Override public final InputStream getInputStream() throws IOException {
        if (!doInput) {
            throw new ProtocolException("This protocol does not support input");
        }

        HttpEngine response = getResponse();

        /*
         * if the requested file does not exist, throw an exception formerly the
         * Error page from the server was returned if the requested file was
         * text/html this has changed to return FileNotFoundException for all
         * file types
         */
        if (getResponseCode() >= HTTP_BAD_REQUEST) {
            throw new FileNotFoundException(url.toString());
        }

        InputStream result = response.getResponseBody();
        if (result == null) {
            throw new IOException("No response body exists; responseCode=" + getResponseCode());
        }
        return result;
    }

    @Override public final OutputStream getOutputStream() throws IOException {
        connect();
        return httpEngine.getRequestBody();
    }

    @Override public final Permission getPermission() throws IOException {
        String connectToAddress = getConnectToHost() + ":" + getConnectToPort();
        return new SocketPermission(connectToAddress, "connect, resolve");
    }

    private String getConnectToHost() {
        return usingProxy()
                ? ((InetSocketAddress) proxy.address()).getHostName()
                : getURL().getHost();
    }

    private int getConnectToPort() {
        int hostPort = usingProxy()
                ? ((InetSocketAddress) proxy.address()).getPort()
                : getURL().getPort();
        return hostPort < 0 ? getDefaultPort() : hostPort;
    }

    @Override public final String getRequestProperty(String field) {
        if (field == null) {
            return null;
        }
        return rawRequestHeaders.get(field);
    }

    private void initHttpEngine() throws IOException {
        if (httpEngineFailure != null) {
            throw httpEngineFailure;
        } else if (httpEngine != null) {
            return;
        }

        connected = true;
        try {
            httpEngine = newHttpEngine(method, rawRequestHeaders, null, null);
        } catch (IOException e) {
            httpEngineFailure = e;
            throw e;
        }
    }

    /**
     * Create a new HTTP engine. This hook method is non-final so it can be
     * overridden by HttpsURLConnectionImpl.
     */
    protected HttpEngine newHttpEngine(String method, RawHeaders requestHeaders,
            HttpConnection connection, RetryableOutputStream requestBody) throws IOException {
        return new HttpEngine(this, method, requestHeaders, connection, requestBody);
    }

    /**
     * Aggressively tries to get the final HTTP response, potentially making
     * many HTTP requests in the process in order to cope with redirects and
     * authentication.
     */
    private HttpEngine getResponse() throws IOException {
        initHttpEngine();

        if (httpEngine.hasResponse()) {
            return httpEngine;
        }

        try {
            while (true) {
                httpEngine.sendRequest();
                httpEngine.readResponse();

                Retry retry = processResponseHeaders();
                if (retry == Retry.NONE) {
                    break;
                }

                /*
                 * The first request was insufficient. Prepare for another...
                 */
                OutputStream requestBody = httpEngine.getRequestBody();
                if (requestBody != null && !(requestBody instanceof RetryableOutputStream)) {
                    throw new HttpRetryException("Cannot retry streamed HTTP body",
                            httpEngine.getResponseHeaders().getResponseCode());
                }

                if (retry == Retry.SAME_CONNECTION && httpEngine.hasConnectionCloseHeaders()) {
                    retry = Retry.NEW_CONNECTION;
                }

                HttpConnection connection = null;
                if (retry == Retry.NEW_CONNECTION) {
                    httpEngine.discardResponseBody();
                    httpEngine.releaseSocket(true);
                } else {
                    httpEngine.dontReleaseSocketToPool();
                    httpEngine.discardResponseBody();
                    connection = httpEngine.getConnection();
                }

                httpEngine = newHttpEngine(method, rawRequestHeaders, connection,
                        (RetryableOutputStream) requestBody);
            }
            return httpEngine;
        } catch (IOException e) {
            httpEngineFailure = e;
            throw e;
        }
    }

    enum Retry {
        NONE,
        SAME_CONNECTION,
        NEW_CONNECTION
    }

    /**
     * Returns the retry action to take for the current response headers. The
     * headers, proxy and target URL or this connection may be adjusted to
     * prepare for a follow up request.
     */
    private Retry processResponseHeaders() throws IOException {
        RawHeaders responseHeaders = httpEngine.getResponseHeaders();
        int responseCode = responseHeaders.getResponseCode();
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
            if (!getInstanceFollowRedirects()) {
                return Retry.NONE;
            }
            if (httpEngine.getRequestBody() != null) {
                // TODO: follow redirects for retryable output streams...
                return Retry.NONE;
            }
            if (++redirectionCount > HttpEngine.MAX_REDIRECTS) {
                throw new ProtocolException("Too many redirects");
            }
            String location = responseHeaders.get("Location");
            if (location == null) {
                return Retry.NONE;
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
                return Retry.NEW_CONNECTION;
            }
            URL previousUrl = url;
            url = new URL(previousUrl, location);
            if (!previousUrl.getProtocol().equals(url.getProtocol())) {
                return Retry.NONE; // the scheme changed; don't retry.
            }
            if (previousUrl.getHost().equals(url.getHost())
                    && previousUrl.getEffectivePort() == url.getEffectivePort()) {
                return Retry.SAME_CONNECTION;
            } else {
                // TODO: strip cookies?
                rawRequestHeaders.removeAll("Host");
                return Retry.NEW_CONNECTION;
            }

        default:
            return Retry.NONE;
        }
    }

    private void setProxy(String proxy) {
        // TODO: convert IllegalArgumentException etc. to ProtocolException?
        int colon = proxy.indexOf(':');
        String host;
        int port;
        if (colon != -1) {
            host = proxy.substring(0, colon);
            port = Integer.parseInt(proxy.substring(colon + 1));
        } else {
            host = proxy;
            port = getDefaultPort();
        }
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }

    /**
     * React to a failed authorization response by looking up new credentials.
     */
    private Retry processAuthHeader(String fieldName, String value) throws IOException {
        // keep asking for username/password until authorized
        String challenge = httpEngine.getResponseHeaders().get(fieldName);
        if (challenge == null) {
            throw new IOException("Received authentication challenge is null");
        }
        String credentials = getAuthorizationCredentials(challenge);
        if (credentials == null) {
            return Retry.NONE; // could not find credentials, end request cycle
        }
        // add authorization credentials, bypassing the already-connected check
        rawRequestHeaders.set(value, credentials);
        return Retry.SAME_CONNECTION;
    }

    /**
     * Returns the authorization credentials on the base of provided challenge.
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
        // use the global authenticator to get the password
        PasswordAuthentication pa = Authenticator.requestPasswordAuthentication(
                getConnectToInetAddress(), getConnectToPort(), url.getProtocol(), prompt, scheme);
        if (pa == null) {
            return null;
        }
        // base64 encode the username and password
        String usernameAndPassword = pa.getUserName() + ":" + new String(pa.getPassword());
        byte[] bytes = usernameAndPassword.getBytes(Charsets.ISO_8859_1);
        String encoded = Base64.encode(bytes, Charsets.ISO_8859_1);
        return scheme + " " + encoded;
    }

    private InetAddress getConnectToInetAddress() throws IOException {
        return usingProxy()
                ? ((InetSocketAddress) proxy.address()).getAddress()
                : InetAddress.getByName(getURL().getHost());
    }

    final int getDefaultPort() {
        return defaultPort;
    }

    /** @see HttpURLConnection#setFixedLengthStreamingMode(int) */
    final int getFixedContentLength() {
        return fixedContentLength;
    }

    /** @see HttpURLConnection#setChunkedStreamingMode(int) */
    final int getChunkLength() {
        return chunkLength;
    }

    final Proxy getProxy() {
        return proxy;
    }

    final void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }


    @Override public final boolean usingProxy() {
        return (proxy != null && proxy.type() != Proxy.Type.DIRECT);
    }

    @Override public String getResponseMessage() throws IOException {
        return getResponse().getResponseHeaders().getResponseMessage();
    }

    @Override public final int getResponseCode() throws IOException {
        return getResponse().getResponseHeaders().getResponseCode();
    }

    @Override public final void setIfModifiedSince(long newValue) {
        // TODO: set this lazily in prepareRequestHeaders()
        super.setIfModifiedSince(newValue);
        rawRequestHeaders.add("If-Modified-Since", HttpDate.format(new Date(newValue)));
    }

    @Override public final void setRequestProperty(String field, String newValue) {
        if (connected) {
            throw new IllegalStateException("Cannot set request property after connection is made");
        }
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        rawRequestHeaders.set(field, newValue);
    }

    @Override public final void addRequestProperty(String field, String value) {
        if (connected) {
            throw new IllegalStateException("Cannot add request property after connection is made");
        }
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        rawRequestHeaders.add(field, value);
    }
}
