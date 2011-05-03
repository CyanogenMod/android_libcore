/*
 * Copyright (C) 2011 The Android Open Source Project
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

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Parsed HTTP response headers.
 */
final class ResponseHeaders {

    /** HTTP header name for the local time when the request was sent. */
    public static final String SENT_MILLIS = "X-Android-Sent-Millis";

    /** HTTP header name for the local time when the response was received. */
    public static final String RECEIVED_MILLIS = "X-Android-Received-Millis";

    final URI uri;
    final HttpHeaders headers;

    /** The server's time when this response was served, if known. */
    Date servedDate;

    /** The last modified date of the response, if known. */
    Date lastModified;

    /**
     * The expiration date of the response, if known. If both this field and the
     * max age are set, the max age is preferred.
     */
    Date expires;

    /**
     * Extension header set by HttpURLConnectionImpl specifying the timestamp
     * when the HTTP request was first initiated.
     */
    long sentRequestMillis;

    /**
     * Extension header set by HttpURLConnectionImpl specifying the timestamp
     * when the HTTP response was first received.
     */
    long receivedResponseMillis;

    /**
     * In the response, this field's name "no-cache" is misleading. It doesn't
     * prevent us from caching the response; it only means we have to validate
     * the response with the origin server before returning it. We can do this
     * with a conditional get.
     */
    boolean noCache;

    /** If true, this response should not be cached. */
    boolean noStore;

    /**
     * The duration past the response's served date that it can be served
     * without validation.
     */
    int maxAgeSeconds = -1;

    /**
     * The "s-maxage" directive is the max age for shared caches. Not to be
     * confused with "max-age" for non-shared caches, As in Firefox and Chrome,
     * this directive is not honored by this cache.
     */
    int sMaxAgeSeconds = -1;

    /**
     * This request header field's name "only-if-cached" is misleading. It
     * actually means "do not use the network". It is set by a client who only
     * wants to make a request if it can be fully satisfied by the cache.
     * Cached responses that would require validation (ie. conditional gets) are
     * not permitted if this header is set.
     */
    boolean isPublic;
    String privateField;
    boolean mustRevalidate;
    String etag;
    int ageSeconds = -1;

    public ResponseHeaders(URI uri, HttpHeaders headers) {
        this.uri = uri;
        this.headers = headers;

        HeaderParser.CacheControlHandler handler = new HeaderParser.CacheControlHandler() {
            @Override public void handle(String directive, String parameter) {
                if (directive.equalsIgnoreCase("no-cache")) {
                    noCache = true;
                } else if (directive.equalsIgnoreCase("no-store")) {
                    noStore = true;
                } else if (directive.equalsIgnoreCase("max-age")) {
                    maxAgeSeconds = HeaderParser.parseSeconds(parameter);
                } else if (directive.equalsIgnoreCase("s-maxage")) {
                    sMaxAgeSeconds = HeaderParser.parseSeconds(parameter);
                } else if (directive.equalsIgnoreCase("public")) {
                    isPublic = true;
                } else if (directive.equalsIgnoreCase("private")) {
                    privateField = parameter;
                } else if (directive.equalsIgnoreCase("must-revalidate")) {
                    mustRevalidate = true;
                }
            }
        };

        for (int i = 0; i < headers.length(); i++) {
            String fieldName = headers.getFieldName(i);
            String value = headers.getValue(i);
            if ("Cache-Control".equalsIgnoreCase(fieldName)) {
                HeaderParser.parseCacheControl(value, handler);
            } else if ("Date".equalsIgnoreCase(fieldName)) {
                servedDate = HttpDate.parse(value);
            } else if ("Expires".equalsIgnoreCase(fieldName)) {
                expires = HttpDate.parse(value);
            } else if ("Last-Modified".equalsIgnoreCase(fieldName)) {
                lastModified = HttpDate.parse(value);
            } else if ("ETag".equalsIgnoreCase(fieldName)) {
                etag = value;
            } else if ("Pragma".equalsIgnoreCase(fieldName)) {
                if (value.equalsIgnoreCase("no-cache")) {
                    noCache = true;
                }
            } else if ("Age".equalsIgnoreCase(fieldName)) {
                ageSeconds = HeaderParser.parseSeconds(value);
            } else if (SENT_MILLIS.equalsIgnoreCase(fieldName)) {
                sentRequestMillis = Long.parseLong(value);
            } else if (RECEIVED_MILLIS.equalsIgnoreCase(fieldName)) {
                receivedResponseMillis = Long.parseLong(value);
            }
        }
    }

    /**
     * Returns the current age of the response, in milliseconds. The calculation
     * is specified by RFC 2616, 13.2.3 Age Calculations.
     */
    private long computeAge(long nowMillis) {
        long apparentReceivedAge = servedDate != null
                ? Math.max(0, receivedResponseMillis - servedDate.getTime())
                : 0;
        long receivedAge = ageSeconds != -1
                ? Math.max(apparentReceivedAge, TimeUnit.SECONDS.toMillis(ageSeconds))
                : apparentReceivedAge;
        long responseDuration = receivedResponseMillis - sentRequestMillis;
        long residentDuration = nowMillis - receivedResponseMillis;
        return receivedAge + responseDuration + residentDuration;
    }

    /**
     * Returns the number of milliseconds that the response was fresh for,
     * starting from the served date.
     */
    private long computeFreshnessLifetime() {
        if (maxAgeSeconds != -1) {
            return TimeUnit.SECONDS.toMillis(maxAgeSeconds);
        } else if (expires != null) {
            long servedMillis = servedDate != null ? servedDate.getTime() : receivedResponseMillis;
            long delta = expires.getTime() - servedMillis;
            return delta > 0 ? delta : 0;
        } else if (lastModified != null && uri.getRawQuery() == null) {
            /*
             * As recommended by the HTTP RFC and implemented in Firefox, the
             * max age of a document should be defaulted to 10% of the
             * document's age at the time it was served. Default expiration
             * dates aren't used for URIs containing a query.
             */
            long servedMillis = servedDate != null ? servedDate.getTime() : sentRequestMillis;
            long delta = servedMillis - lastModified.getTime();
            return delta > 0 ? (delta / 10) : 0;
        }
        return 0;
    }

    /**
     * Returns true if this response can be stored to later serve another
     * request.
     */
    public boolean isCacheable(RequestHeaders request) {
        /*
         * Always go to network for uncacheable response codes (RFC 2616, 13.4),
         * This implementation doesn't support caching partial content.
         */
        int responseCode = headers.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK
                && responseCode != HttpURLConnection.HTTP_NOT_AUTHORITATIVE
                && responseCode != HttpURLConnection.HTTP_MULT_CHOICE
                && responseCode != HttpURLConnection.HTTP_MOVED_PERM
                && responseCode != HttpURLConnection.HTTP_GONE) {
            return false;
        }

        /*
         * Responses to authorized requests aren't cacheable unless they include
         * a 'public', 'must-revalidate' or 's-maxage' directive.
         */
        if (request.hasAuthorization
                && !isPublic
                && !mustRevalidate
                && sMaxAgeSeconds == -1) {
            return false;
        }

        if (noStore) {
            return false;
        }

        return true;
    }

    /**
     * Returns the source to satisfy {@code request} given this cached response.
     */
    public ResponseSource chooseResponseSource(long nowMillis, RequestHeaders request) {
        /*
         * If this response shouldn't have been stored, it should never be used
         * as a response source. This check should be redundant as long as the
         * persistence store is well-behaved and the rules are constant.
         */
        if (!isCacheable(request)) {
            return ResponseSource.NETWORK;
        }

        if (request.noCache || request.hasConditions) {
            return ResponseSource.NETWORK;
        }

        long ageMillis = computeAge(nowMillis);
        long freshMillis = computeFreshnessLifetime();

        if (request.maxAgeSeconds != -1) {
            freshMillis = Math.min(freshMillis,
                    TimeUnit.SECONDS.toMillis(request.maxAgeSeconds));
        }

        long minFreshMillis = 0;
        if (request.minFreshSeconds != -1) {
            minFreshMillis = TimeUnit.SECONDS.toMillis(request.minFreshSeconds);
        }

        long maxStaleMillis = 0;
        if (request.maxStaleSeconds != -1) {
            maxStaleMillis = TimeUnit.SECONDS.toMillis(request.maxStaleSeconds);
        }

        if (!noCache && ageMillis + minFreshMillis < freshMillis + maxStaleMillis) {
            // TODO: attach a warning if the response is stale
            return ResponseSource.CACHE;
        }

        if (lastModified != null) {
            request.headers.add("If-Modified-Since", HttpDate.format(lastModified));
            request.hasConditions = true;
        } else if (servedDate != null) {
            request.headers.add("If-Modified-Since", HttpDate.format(servedDate));
            request.hasConditions = true;
        }

        if (etag != null) {
            request.headers.add("If-None-Match", etag);
            request.hasConditions = true;
        }

        return request.hasConditions
                ? ResponseSource.CONDITIONAL_CACHE
                : ResponseSource.NETWORK;
    }

    /**
     * Returns true if this cached response should be used; false if the
     * network response should be used.
     */
    public boolean validate(ResponseHeaders networkResponse) {
        if (networkResponse.headers.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
            return true;
        }

        /*
         * The HTTP spec says that if the network's response is older than our
         * cached response, we may return the cache's response. Like Chrome (but
         * unlike Firefox), this client prefers to return the newer response.
         */
        if (lastModified != null
                && networkResponse.lastModified != null
                && networkResponse.lastModified.getTime() < lastModified.getTime()) {
            return true;
        }

        return false;
    }
}
