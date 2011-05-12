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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.ResponseCache;
import java.net.SecureCacheResponse;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.Charsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import libcore.io.Base64;
import libcore.io.DiskLruCache;
import libcore.io.IoUtils;
import libcore.io.Streams;

/**
 * Cache responses in a cache directory.
 */
public final class HttpResponseCache extends ResponseCache implements Closeable {
    // TODO: default max size
    // TODO: default cache directory for android apps
    // TODO: move this class to android.util
    // TODO: add APIs to iterate the cache

    private static final int VERSION = 201105;
    private static final int ENTRY_METADATA = 0;
    private static final int ENTRY_BODY = 1;
    private static final int ENTRY_COUNT = 2;

    private final DiskLruCache cache;

    /* read and write statistics, all guarded by 'this' */
    private int writeSuccessCount;
    private int writeAbortCount;
    private int networkCount;
    private int hitCount;
    private int requestCount;

    public HttpResponseCache(File directory, int maxSize) throws IOException {
        cache = DiskLruCache.open(directory, VERSION, ENTRY_COUNT, maxSize);
    }

    private String uriToKey(URI uri) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] md5bytes = messageDigest.digest(uri.toString().getBytes(Charsets.UTF_8));
            return IntegralToString.bytesToHexString(md5bytes, false);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    @Override public CacheResponse get(URI uri, String requestMethod,
            Map<String, List<String>> requestHeaders) throws IOException {
        String key = uriToKey(uri);
        DiskLruCache.Snapshot snapshot = cache.get(key);

        if (snapshot == null) {
            return null;
        }

        Entry entry = new Entry(new BufferedInputStream(snapshot.newInputStream(ENTRY_METADATA)));
        if (!entry.matches(uri, requestMethod)) {
            snapshot.close();
            return null;
        }

        InputStream body = newBodyInputStream(snapshot);
        return entry.isHttps()
                ? entry.newSecureCacheResponse(body)
                : entry.newCacheResponse(body);
    }

    /**
     * Returns an input stream that reads the body of a snapshot, closing the
     * snapshot when the stream is closed.
     */
    private InputStream newBodyInputStream(final DiskLruCache.Snapshot snapshot) {
        return new FilterInputStream(snapshot.newInputStream(ENTRY_BODY)) {
            @Override public void close() throws IOException {
                snapshot.close();
                super.close();
            }
        };
    }

    @Override public CacheRequest put(URI uri, URLConnection urlConnection) throws IOException {
        if (!(urlConnection instanceof HttpURLConnection)) {
            return null;
        }

        HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
        String requestMethod = httpConnection.getRequestMethod();
        String key = uriToKey(uri);

        // Invalidate the cache on POST, PUT and DELETE.
        if (requestMethod.equals(HttpEngine.POST)
                || requestMethod.equals(HttpEngine.PUT)
                || requestMethod.equals(HttpEngine.DELETE)) {
            cache.remove(key);
        }

        /*
         * Don't cache non-GET responses. We're technically allowed to cache
         * HEAD requests and some POST requests, but the complexity of doing so
         * is high and the benefit is low.
         */
        if (!requestMethod.equals(HttpEngine.GET)) {
            return null;
        }

        // For implementation simplicity, don't  cache responses that have a Vary field.
        if (httpConnection.getHeaderField("Vary") != null) {
            return null;
        }

        DiskLruCache.Editor editor = cache.edit(key);
        new Entry(uri, httpConnection).writeTo(editor);
        return new CacheRequestImpl(editor);
    }

    /**
     * Closes this cache. Stored contents will remain on the filesystem.
     */
    public void close() throws IOException {
        cache.close();
    }

    /**
     * Closes this cache and deletes all of its stored contents.
     */
    public void delete() throws IOException {
        cache.delete();
    }

    /**
     * Returns the number of responses that were aborted before they were
     * stored.
     */
    synchronized int getWriteAbortCount() {
        return writeAbortCount;
    }

    /**
     * Returns the number of responses that were stored successfully.
     */
    synchronized int getWriteSuccessCount() {
        return writeSuccessCount;
    }

    /**
     * Returns the number of HTTP requests that required the network to either
     * supply a response or validate a locally cached response.
     */
    public synchronized int getNetworkCount() {
        return networkCount;
    }

    /**
     * Returns the number of HTTP requests whose response was provided by the
     * cache. This may include conditional {@code GET} requests that were
     * validated over the network.
     */
    public synchronized int getHitCount() {
        return hitCount;
    }

    /**
     * Returns the total number of HTTP requests that were made. This includes
     * both client requests and requests that were made on the client's behalf
     * to handle a redirects and retries.
     */
    public synchronized int getRequestCount() {
        return requestCount;
    }

    synchronized void trackResponse(ResponseSource source) {
        requestCount++;

        switch (source) {
        case CACHE:
            hitCount++;
            break;
        case CONDITIONAL_CACHE:
        case NETWORK:
            networkCount++;
            break;
        }
    }

    synchronized void trackConditionalCacheHit() {
        hitCount++;
    }

    private final class CacheRequestImpl extends CacheRequest {
        private final DiskLruCache.Editor editor;
        private OutputStream cacheOut;
        private boolean done;
        private OutputStream body;

        public CacheRequestImpl(final DiskLruCache.Editor editor) throws IOException {
            this.editor = editor;
            this.cacheOut = editor.newOutputStream(ENTRY_BODY);
            this.body = new FilterOutputStream(cacheOut) {
                @Override public void close() throws IOException {
                    synchronized (HttpResponseCache.this) {
                        if (done) {
                            return;
                        }
                        done = true;
                        writeSuccessCount++;
                    }
                    super.close();
                    editor.commit();
                }
            };
        }

        @Override public void abort() {
            synchronized (HttpResponseCache.this) {
                if (done) {
                    return;
                }
                done = true;
                writeAbortCount++;
            }
            IoUtils.closeQuietly(cacheOut);
            try {
                editor.abort(); // TODO: fix abort() to not throw?
            } catch (IOException ignored) {
            }
        }

        @Override public OutputStream getBody() throws IOException {
            return body;
        }
    }

    private static final class Entry {
        private final String uri;
        private final String requestMethod;
        private final RawHeaders responseHeaders;
        private final String cipherSuite;
        private final Certificate[] peerCertificates;
        private final Certificate[] localCertificates;

        /*
         * Reads an entry from an input stream. A typical entry looks like this:
         *   http://google.com/foo
         *   GET
         *   HTTP/1.1 200 OK
         *   3
         *   Content-Type: image/png
         *   Content-Length: 100
         *   Cache-Control: max-age=600
         *
         * A typical HTTPS file looks like this:
         *   https://google.com/foo
         *   GET
         *   HTTP/1.1 200 OK
         *   3
         *   Content-Type: image/png
         *   Content-Length: 100
         *   Cache-Control: max-age=600
         *
         *   AES_256_WITH_MD5
         *   2
         *   base64-encoded peerCertificate[0]
         *   base64-encoded peerCertificate[1]
         *   -1
         *
         * The file is newline separated. The first three lines are the URL, the
         * request method and the response status line.
         *
         * The next line contains the number of HTTP response header lines. It
         * is followed by that number of header lines.
         *
         * HTTPS responses also contain SSL session information. This begins
         * with a blank line, and then a line containing the cipher suite. Next
         * is the length of the peer certificate chain. These certificates are
         * base64-encoded and appear each on their own line. The next line
         * contains the length of the local certificate chain. These
         * certificates are also base64-encoded and appear each on their own
         * line. A length of -1 is used to encode a null array.
         */
        public Entry(InputStream in) throws IOException {
            try {
                uri = Streams.readAsciiLine(in);
                requestMethod = Streams.readAsciiLine(in);
                responseHeaders = new RawHeaders();
                responseHeaders.setStatusLine(Streams.readAsciiLine(in));
                int headerCount = readInt(in);
                for (int i = 0; i < headerCount; i++) {
                    responseHeaders.addLine(Streams.readAsciiLine(in));
                }

                if (isHttps()) {
                    String blank = Streams.readAsciiLine(in);
                    if (!blank.isEmpty()) {
                        throw new IOException("expected \"\" but was \"" + blank + "\"");
                    }
                    cipherSuite = Streams.readAsciiLine(in);
                    peerCertificates = readCertArray(in);
                    localCertificates = readCertArray(in);
                } else {
                    cipherSuite = null;
                    peerCertificates = null;
                    localCertificates = null;
                }
            } finally {
                in.close();
            }
        }

        public Entry(URI uri, HttpURLConnection httpConnection) {
            this.uri = uri.toString();
            this.requestMethod = httpConnection.getRequestMethod();
            this.responseHeaders = RawHeaders.fromMultimap(httpConnection.getHeaderFields());

            if (isHttps()) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) httpConnection;
                cipherSuite = httpsConnection.getCipherSuite();
                Certificate[] peerCertificatesNonFinal = null;
                try {
                    peerCertificatesNonFinal = httpsConnection.getServerCertificates();
                } catch (SSLPeerUnverifiedException ignored) {
                }
                peerCertificates = peerCertificatesNonFinal;
                localCertificates = httpsConnection.getLocalCertificates();
            } else {
                cipherSuite = null;
                peerCertificates = null;
                localCertificates = null;
            }
        }

        public void writeTo(DiskLruCache.Editor editor) throws IOException {
            OutputStream out = editor.newOutputStream(0);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, Charsets.UTF_8));
            writer.write(uri + '\n');
            writer.write(requestMethod + '\n');
            writer.write(responseHeaders.getStatusLine() + '\n');
            writer.write(Integer.toString(responseHeaders.length()) + '\n');
            for (int i = 0; i < responseHeaders.length(); i++) {
                writer.write(responseHeaders.getFieldName(i) + ": "
                        + responseHeaders.getValue(i) + '\n');
            }
            if (isHttps()) {
                writer.write('\n');
                writer.write(cipherSuite + '\n');
                writeCertArray(writer, peerCertificates);
                writeCertArray(writer, localCertificates);
            }
            writer.close();
        }

        private boolean isHttps() {
            return uri.startsWith("https://");
        }

        private int readInt(InputStream in) throws IOException {
            String intString = Streams.readAsciiLine(in);
            try {
                return Integer.parseInt(intString);
            } catch (NumberFormatException e) {
                throw new IOException("expected an int but was \"" + intString + "\"");
            }
        }

        private Certificate[] readCertArray(InputStream in) throws IOException {
            int length = readInt(in);
            if (length == -1) {
                return null;
            }
            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                Certificate[] result = new Certificate[length];
                for (int i = 0; i < result.length; i++) {
                    String line = Streams.readAsciiLine(in);
                    byte[] bytes = Base64.decode(line.getBytes(Charsets.US_ASCII));
                    result[i] = certificateFactory.generateCertificate(
                            new ByteArrayInputStream(bytes));
                }
                return result;
            } catch (CertificateException e) {
                throw new IOException(e);
            }
        }

        private void writeCertArray(Writer writer, Certificate[] certificates) throws IOException {
            if (certificates == null) {
                writer.write("-1\n");
                return;
            }
            try {
                writer.write(Integer.toString(certificates.length) + '\n');
                for (Certificate certificate : certificates) {
                    byte[] bytes = certificate.getEncoded();
                    String line = Base64.encode(bytes);
                    writer.write(line + '\n');
                }
            } catch (CertificateEncodingException e) {
                throw new IOException(e);
            }
        }

        public boolean matches(URI uri, String requestMethod) {
            return this.uri.equals(uri.toString()) && this.requestMethod.equals(requestMethod);
        }

        public CacheResponse newCacheResponse(final InputStream in) {
            return new CacheResponse() {
                @Override public Map<String, List<String>> getHeaders() {
                    return responseHeaders.toMultimap();
                }

                @Override public InputStream getBody() {
                    return in;
                }
            };
        }

        public SecureCacheResponse newSecureCacheResponse(final InputStream in) {
            return new SecureCacheResponse() {
                @Override public Map<String, List<String>> getHeaders() {
                    return responseHeaders.toMultimap();
                }

                @Override public InputStream getBody() {
                    return in;
                }

                @Override public String getCipherSuite() {
                    return cipherSuite;
                }

                @Override public List<Certificate> getServerCertificateChain()
                        throws SSLPeerUnverifiedException {
                    if (peerCertificates == null || peerCertificates.length == 0) {
                        throw new SSLPeerUnverifiedException(null);
                    }
                    return Arrays.asList(peerCertificates.clone());
                }

                @Override public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
                    if (peerCertificates == null || peerCertificates.length == 0) {
                        throw new SSLPeerUnverifiedException(null);
                    }
                    return ((X509Certificate) peerCertificates[0]).getSubjectX500Principal();
                }

                @Override public List<Certificate> getLocalCertificateChain() {
                    if (localCertificates == null || localCertificates.length == 0) {
                        return null;
                    }
                    return Arrays.asList(localCertificates.clone());
                }

                @Override public Principal getLocalPrincipal() {
                    if (localCertificates == null || localCertificates.length == 0) {
                        return null;
                    }
                    return ((X509Certificate) localCertificates[0]).getSubjectX500Principal();
                }
            };
        }
    }
}
