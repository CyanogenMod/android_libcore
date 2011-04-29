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
    int responseCode;
    Date servedDate;
    Date lastModified;
    Date expires;

    /**
     * This header's name "no-cache" is misleading. It doesn't prevent us from
     * caching the response; it only means we have to validate the response with
     * the origin server before returning it. We can do this with a conditional
     * get.
     */
    boolean noCache;
    boolean noStore;
    int maxAgeSeconds = -1;
    int maxStaleSeconds = -1;
    int minFreshSeconds = -1;
    boolean noTransform;
    boolean onlyIfCached;
    boolean isPublic;
    boolean isPrivate;
    String privateField;
    String noCacheField;
    boolean mustRevalidate;
    boolean proxyRevalidate;
    int sMaxAgeSeconds;
    String etag;

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
                if (headers.getValue(i).equalsIgnoreCase("no-cache")) {
                    noCache = true;
                }
            }
        }
    }

    /**
     * Parse a comma-separated list of cache control header values.
     */
    private void parseCacheControl(String value) {
        int pos = 0;
        while (pos < value.length()) {
            int tokenStart = pos;
            pos = skipUntil(value, pos, "=,");
            String directive = value.substring(tokenStart, pos).trim();

            if (pos == value.length() || value.charAt(pos) == ',') {
                pos++; // consume ',' (if necessary)
                handleCacheControlDirective(directive, null);
                continue;
            }

            pos++; // consume '='
            pos = skipWhitespace(value, pos);

            String parameter;

            // quoted string
            if (pos < value.length() && value.charAt(pos) == '\"') {
                pos++; // consume '"' open quote
                int parameterStart = pos;
                pos = skipUntil(value, pos, "\"");
                parameter = value.substring(parameterStart, pos);
                pos++; // consume '"' close quote (if necessary)

            // unquoted string
            } else {
                int parameterStart = pos;
                pos = skipUntil(value, pos, ",");
                parameter = value.substring(parameterStart, pos).trim();
            }

            handleCacheControlDirective(directive, parameter);
        }
    }

    /**
     * Returns the next index in {@code input} at or after {@code pos} that
     * contains a character from {@code characters}. Returns the input length if
     * none of the requested characters can be found.
     */
    private int skipUntil(String input, int pos, String characters) {
        for (; pos < input.length(); pos++) {
            if (characters.indexOf(input.charAt(pos)) != -1) {
                break;
            }
        }
        return pos;
    }

    /**
     * Returns the next non-whitespace character in {@code input} that is white
     * space. Result is undefined if input contains newline characters.
     */
    private int skipWhitespace(String input, int pos) {
        for (; pos < input.length(); pos++) {
            char c = input.charAt(pos);
            if (c != ' ' && c != '\t') {
                break;
            }
        }
        return pos;
    }

    private void handleCacheControlDirective(String directive, String parameter) {
        if (directive.equalsIgnoreCase("no-cache")) {
            noCache = true;
            noCacheField = parameter;
        } else if (directive.equalsIgnoreCase("no-store")) {
            noStore = true;
        } else if (directive.equalsIgnoreCase("max-age")) {
            maxAgeSeconds = parseSeconds(parameter);
        } else if (directive.equalsIgnoreCase("s-max-age")) {
            sMaxAgeSeconds = parseSeconds(parameter);
        } else if (directive.equalsIgnoreCase("max-stale")) {
            maxStaleSeconds = parseSeconds(parameter);
        } else if (directive.equalsIgnoreCase("min-fresh")) {
            minFreshSeconds = parseSeconds(parameter);
        } else if (directive.equalsIgnoreCase("no-transform")) {
            noTransform = true;
        } else if (directive.equalsIgnoreCase("only-if-cached")) {
            onlyIfCached = true;
        } else if (directive.equalsIgnoreCase("public")) {
            isPublic = true;
        } else if (directive.equalsIgnoreCase("private")) {
            isPrivate = true;
            privateField = parameter;
        } else if (directive.equalsIgnoreCase("must-revalidate")) {
            mustRevalidate = true;
        } else if (directive.equalsIgnoreCase("proxy-revalidate")) {
            proxyRevalidate = true;
        }
    }

    private int parseSeconds(String parameter) {
        try {
            long seconds = Long.parseLong(parameter);
            if (seconds > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            } else if (seconds < 0) {
                return 0;
            } else {
                return (int) seconds;
            }
        } catch (NumberFormatException e) {
            return -1;
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
