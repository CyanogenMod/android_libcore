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

package tests.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.CacheResponse;
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

/**
 * Cache all responses in memory by URI.
 */
public final class DefaultResponseCache extends ResponseCache {
    private final Map<URI, Entry> entries = new HashMap<URI, Entry>();
    private int abortCount;
    private int successCount;
    private int hitCount;
    private int missCount;

    @Override public synchronized CacheResponse get(URI uri, String requestMethod,
            Map<String, List<String>> requestHeaders) throws IOException {
        // TODO: honor the request headers in the cache key
        Entry entry = entries.get(uri);
        if (entry != null) {
            hitCount++;
            return entry.asResponse();
        } else {
            missCount++;
            return null;
        }
    }

    @Override public CacheRequest put(URI uri, URLConnection urlConnection)
            throws IOException {
        // TODO: honor the response headers for cache invalidation
        return new Entry(uri, urlConnection).asRequest();
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
        private final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream() {
            private boolean closed;
            @Override public void close() throws IOException {
                synchronized (DefaultResponseCache.this) {
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
        private final Map<String, List<String>> headers;
        private final URI uri;
        private final CacheResponse cacheResponse;

        private Entry(URI uri, URLConnection connection) {
            this.uri = uri;
            this.headers = deepCopy(connection.getHeaderFields());
            this.cacheResponse = connectionToCacheResponse(connection);
        }

        public CacheRequest asRequest() {
            return new CacheRequest() {
                private boolean aborted;
                @Override public void abort() {
                    synchronized (DefaultResponseCache.this) {
                        if (aborted) {
                            return;
                        }

                        abortCount++;
                        aborted = true;
                    }
                }
                @Override public OutputStream getBody() throws IOException {
                    return bytesOut;
                }
            };
        }

        private CacheResponse connectionToCacheResponse(URLConnection connection) {
            if (!(connection instanceof HttpsURLConnection)) {
                return new CacheResponse() {
                    @Override public InputStream getBody() {
                        return new ByteArrayInputStream(getBytes());
                    }
                    @Override public Map<String, List<String>> getHeaders() {
                        return deepCopy(headers);
                    }
                };
            }

            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

            /*
             * Retrieve the fields eagerly to avoid needing a strong reference
             * to the connection. We do acrobatics for the two methods that can
             * throw so that the cache response also throws.
             */
            List<Certificate> serverCertificatesNonFinal = null;
            try {
                serverCertificatesNonFinal = Arrays.asList(httpsConnection.getServerCertificates());
            } catch (SSLPeerUnverifiedException ignored) {
            }
            Principal peerPrincipalNonFinal = null;
            try {
                peerPrincipalNonFinal = httpsConnection.getPeerPrincipal();
            } catch (SSLPeerUnverifiedException ignored) {
            }
            final String cipherSuite = httpsConnection.getCipherSuite();
            final Certificate[] localCertificates = httpsConnection.getLocalCertificates();
            final List<Certificate> serverCertificates = serverCertificatesNonFinal;
            final Principal peerPrincipal = peerPrincipalNonFinal;
            final Principal localPrincipal = httpsConnection.getLocalPrincipal();

            return new SecureCacheResponse() {
                @Override public InputStream getBody() {
                    return new ByteArrayInputStream(getBytes());
                }
                @Override public Map<String, List<String>> getHeaders() {
                    return deepCopy(headers);
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
            };
        }

        public CacheResponse asResponse() {
            return cacheResponse;
        }

        public byte[] getBytes() {
            return bytesOut.toByteArray();
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
