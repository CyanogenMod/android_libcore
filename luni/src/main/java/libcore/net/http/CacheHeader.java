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
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Caching aspects of an HTTP request or response.
 */
final class CacheHeader {
    private int responseCode;
    private Date servedDate;
    private Date lastModified;
    private Date expires;

    /**
     * This header's name "no-cache" is misleading. It doesn't prevent us from
     * caching the response; it only means we have to validate the response with
     * the origin server before returning it. We can do this with a conditional
     * get.
     */
    private boolean noCache;
    private boolean noStore;
    private int maxAgeSeconds = -1;
    private int maxStaleSeconds = -1;
    private int minFreshSeconds = -1;
    private boolean noTransform;
    private boolean onlyIfCached;
    private boolean isPublic;
    private boolean isPrivate;
    private String privateField;
    private String noCacheField;
    private boolean mustRevalidate;
    private boolean proxyRevalidate;
    private int sMaxAgeSeconds;
    private String etag;

    public CacheHeader(HttpHeaders headers) {
        this.responseCode = headers.getResponseCode();
        for (int i = 0; i < headers.length(); i++) {
            if ("Cache-Control".equalsIgnoreCase(headers.getKey(i))) {
                parseCacheControl(headers.getValue(i));
            } else if ("Date".equalsIgnoreCase(headers.getKey(i))) {
                servedDate = HttpDate.parse(headers.getValue(i));
            } else if ("Expires".equalsIgnoreCase(headers.getKey(i))) {
                expires = HttpDate.parse(headers.getValue(i));
            } else if ("Last-Modified".equalsIgnoreCase(headers.getKey(i))) {
                lastModified = HttpDate.parse(headers.getValue(i));
            } else if ("ETag".equalsIgnoreCase(headers.getKey(i))) {
                etag = headers.getValue(i);
            } else if ("Pragma".equalsIgnoreCase(headers.getKey(i))) {
                if (headers.getValue(i).equals("no-cache")) {
                    noCache = true;
                } else {
                    System.out.println(headers.getKey(i) + " " + headers.getValue(i)); // TODO
                }
            }
        }
    }

    private void parseCacheControl(String value) {
        String directive = value;
        String parameter = null;
        int equals = value.indexOf('=');
        if (equals != -1) {
            directive = value.substring(0, equals);
            parameter = value.substring(equals + 1); // TODO: strip "quotes"
        }

        try {
            if (directive.equals("no-cache")) {
                noCache = true;
                noCacheField = parameter;
            } else if (directive.equals("no-store")) {
                noStore = true;
            } else if (directive.equals("max-age")) {
                maxAgeSeconds = Integer.parseInt(parameter);
            } else if (directive.equals("s-max-age")) {
                sMaxAgeSeconds = Integer.parseInt(parameter);
            } else if (directive.equals("max-stale")) {
                maxStaleSeconds = Integer.parseInt(parameter);
            } else if (directive.equals("max-fresh")) {
                minFreshSeconds = Integer.parseInt(parameter);
            } else if (directive.equals("no-transform")) {
                noTransform = true;
            } else if (directive.equals("only-if-cached")) {
                onlyIfCached = true;
            } else if (directive.equals("public")) {
                isPublic = true;
            } else if (directive.equals("private")) {
                isPrivate = true;
                privateField = parameter;
            } else if (directive.equals("must-revalidate")) {
                mustRevalidate = true;
            } else if (directive.equals("proxy-revalidate")) {
                proxyRevalidate = true;
            } else {
                System.out.println(value); // TODO
            }
        } catch (NumberFormatException e) {
            System.out.println(value + " " + e); // TODO
        }
    }

    /**
     * Returns the time at which this response will require validation.
     */
    private long getExpiresTimeMillis() {
        if (servedDate != null && maxAgeSeconds != -1) {
            return servedDate.getTime() + TimeUnit.SECONDS.toMillis(maxAgeSeconds);
        } else if (expires != null) {
            return expires.getTime();
        } else {
            /*
             * This response doesn't specify an expiration time, so for semantic
             * transparency we just assume it's expired.
             */
            return 0;
        }
    }

    /**
     * Returns the source to satisfy {@code request} given this cached response.
     */
    public ResponseSource chooseResponseSource(long nowMillis, HttpHeaders request) {
        boolean hasConditions = false;

        // TODO: if a "If-Modified-Since" or "If-None-Match" header exists, assume the user
        // knows better and just return CONDITIONAL_CACHE

        // TODO: honor request headers, like the client's requested max-age

        if (noStore) {
            return ResponseSource.NETWORK;
        }

        if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
            // TODO: whitelist cacheable responses rather than blacklisting invalid ones
            return ResponseSource.NETWORK;
        }

        if (!noCache && nowMillis < getExpiresTimeMillis()) {
            return ResponseSource.CACHE;
        }

        if (lastModified != null) {
            request.set("If-Modified-Since", HttpDate.format(lastModified));
            hasConditions = true;
        } else if (servedDate != null) {
            request.set("If-Modified-Since", HttpDate.format(servedDate));
            hasConditions = true;
        }

        if (etag != null) {
            request.set("If-None-Match", etag);
            hasConditions = true;
        }

        return hasConditions
                ? ResponseSource.CONDITIONAL_CACHE
                : ResponseSource.NETWORK;
    }

    /**
     * Returns true if this cached response should be used; false if the
     * network response should be used.
     */
    public boolean validate(HttpHeaders request, HttpHeaders response) {
        if (response.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
            return true;
        }

        /*
         * The HTTP spec says that if the network's response is older than our
         * cached response, we may return the cache's response. Like Chrome (but
         * unlike Firefox), this client prefers to return the newer response.
         */
        if (lastModified != null) {
            CacheHeader responseCacheHeader = new CacheHeader(response);
            if (responseCacheHeader.lastModified != null
                    && responseCacheHeader.lastModified.getTime() < lastModified.getTime()) {
                return true;
            }
        }

        return false;
    }
}
