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

package libcore.net.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.ResponseCache;
import java.net.SecureCacheResponse;
import java.net.URI;
import java.net.URLConnection;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import static libcore.net.http.HttpURLConnectionImpl.DELETE;
import static libcore.net.http.HttpURLConnectionImpl.GET;
import static libcore.net.http.HttpURLConnectionImpl.POST;
import static libcore.net.http.HttpURLConnectionImpl.PUT;

/**
 * Cache all responses in memory by URI.
 *
 * TODO: disk storage, tuning knobs, LRU
 * TODO: move this class to android.util
 */
public final class HttpResponseCache extends ResponseCache {
    private final Map<URI, Entry> entries = new HashMap<URI, Entry>();
    private int abortCount;
    private int successCount;
    private int hitCount;
    private int missCount;

    @Override public synchronized CacheResponse get(URI uri, String requestMethod,
            Map<String, List<String>> requestHeaders) throws IOException {
        Entry entry = entries.get(uri);
        if (entry == null) {
            missCount++;
            return null;
        }

        if (!requestMethod.equals(entry.requestMethod)) {
            return null;
        }

        // HttpHeaders headers = HttpHeaders.fromMultimap(entry.responseHeaders);

        hitCount++;
        return entry.asResponse();
    }

    @Override public CacheRequest put(URI uri, URLConnection urlConnection)
            throws IOException {
        if (!(urlConnection instanceof HttpURLConnection)) {
            return null;
        }

        HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
        String requestMethod = httpConnection.getRequestMethod();

        // Invalidate the cache on POST, PUT and DELETE.
        if (requestMethod.equals(POST) || requestMethod.equals(PUT)
                || requestMethod.equals(DELETE)) {
            entries.remove(uri);
        }

        /*
         * Don't cache non-GET responses. We're technically allowed to cache
         * HEAD requests and some POST requests, but the complexity of doing so
         * is high and the benefit is low.
         */
        if (!requestMethod.equals(GET)) {
            return null;
        }

        return new Entry(uri, httpConnection).asRequest();
    }

    public synchronized Map<URI, Entry> getContents() {
        return new HashMap<URI, Entry>(entries);
    }

    /**
     * Returns the number of requests that were aborted before they were closed.
     */
    public synchronized int getAbortCount() {
        return abortCount;
    }

    /**
     * Returns the number of requests that were closed successfully.
     */
    public synchronized int getSuccessCount() {
        return successCount;
    }

    /**
     * Returns the number of responses served by the cache.
     */
    public synchronized int getHitCount() {
        return hitCount;
    }

    /**
     * Returns the number of responses that couldn't be served by the cache.
     */
    public synchronized int getMissCount() {
        return missCount;
    }

    public final class Entry {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream() {
            private boolean closed;
            @Override public void close() throws IOException {
                synchronized (HttpResponseCache.this) {
                    if (closed) {
                        return;
                    }

                    super.close();
                    entries.put(uri, Entry.this);
                    successCount++;
                    closed = true;
                }
            }
        };

        private final String requestMethod;
        private final Map<String, List<String>> responseHeaders;
        private final URI uri;
        private final CacheResponse cacheResponse;

        private Entry(URI uri, HttpURLConnection connection) {
            this.uri = uri;
            this.requestMethod = connection.getRequestMethod();
            this.responseHeaders = deepCopy(connection.getHeaderFields());
            this.cacheResponse = connection instanceof HttpsURLConnection
                    ? new SecureCacheResponseImpl(responseHeaders, body,
                            (HttpsURLConnection) connection)
                    : new CacheResponseImpl(responseHeaders, body);
        }

        public CacheRequest asRequest() {
            return new CacheRequest() {
                private boolean aborted;
                @Override public void abort() {
                    synchronized (HttpResponseCache.this) {
                        if (aborted) {
                            return;
                        }

                        abortCount++;
                        aborted = true;
                    }
                }
                @Override public OutputStream getBody() throws IOException {
                    return body;
                }
            };
        }

        public CacheResponse asResponse() {
            return cacheResponse;
        }

        public byte[] getBytes() {
            return body.toByteArray();
        }
    }

    private final class CacheResponseImpl extends CacheResponse {
        private final Map<String, List<String>> headers;
        private final ByteArrayOutputStream bytesOut;

        public CacheResponseImpl(Map<String, List<String>> headers,
                ByteArrayOutputStream bytesOut) {
            this.headers = headers;
            this.bytesOut = bytesOut;
        }

        @Override public Map<String, List<String>> getHeaders() {
            return deepCopy(headers);
        }

        @Override public InputStream getBody() {
            return new ByteArrayInputStream(bytesOut.toByteArray());
        }
    }

    private final class SecureCacheResponseImpl extends SecureCacheResponse {
        private final Map<String, List<String>> headers;
        private final ByteArrayOutputStream bytesOut;
        private final String cipherSuite;
        private final Certificate[] localCertificates;
        private final List<Certificate> serverCertificates;
        private final Principal peerPrincipal;
        private final Principal localPrincipal;

        public SecureCacheResponseImpl(Map<String, List<String>> headers,
                ByteArrayOutputStream bytesOut,
                HttpsURLConnection httpsConnection) {
            this.headers = headers;
            this.bytesOut = bytesOut;

            /*
             * Retrieve the fields eagerly to avoid needing a strong
             * reference to the connection. We do acrobatics for the two
             * methods that can throw so that the cache response also
             * throws.
             */
            List<Certificate> serverCertificatesNonFinal = null;
            try {
                serverCertificatesNonFinal = Arrays.asList(
                        httpsConnection.getServerCertificates());
            } catch (SSLPeerUnverifiedException ignored) {
            }
            Principal peerPrincipalNonFinal = null;
            try {
                peerPrincipalNonFinal = httpsConnection.getPeerPrincipal();
            } catch (SSLPeerUnverifiedException ignored) {
            }
            this.cipherSuite = httpsConnection.getCipherSuite();
            this.localCertificates = httpsConnection.getLocalCertificates();
            this.serverCertificates = serverCertificatesNonFinal;
            this.peerPrincipal = peerPrincipalNonFinal;
            this.localPrincipal = httpsConnection.getLocalPrincipal();
        }

        @Override public Map<String, List<String>> getHeaders() {
            return deepCopy(headers);
        }

        @Override public InputStream getBody() {
            return new ByteArrayInputStream(bytesOut.toByteArray());
        }

        @Override public String getCipherSuite() {
            return cipherSuite;
        }

        @Override public List<Certificate> getLocalCertificateChain() {
            return localCertificates != null
                    ? Arrays.asList(localCertificates.clone())
                    : null;
        }

        @Override public List<Certificate> getServerCertificateChain()
                throws SSLPeerUnverifiedException {
            if (serverCertificates == null) {
                throw new SSLPeerUnverifiedException(null);
            }
            return new ArrayList<Certificate>(serverCertificates);
        }

        @Override public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
            if (peerPrincipal == null) {
                throw new SSLPeerUnverifiedException(null);
            }
            return peerPrincipal;
        }

        @Override public Principal getLocalPrincipal() {
            return localPrincipal;
        }
    }

    private static Map<String, List<String>> deepCopy(Map<String, List<String>> input) {
        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>(input);
        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            entry.setValue(new ArrayList<String>(entry.getValue()));
        }
        return result;
    }
}
