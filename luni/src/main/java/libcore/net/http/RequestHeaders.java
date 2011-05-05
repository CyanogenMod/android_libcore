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

import java.net.URI;

/**
 * Parsed HTTP request headers.
 */
final class RequestHeaders {
    final URI uri;
    final RawHeaders headers;

    /** Don't use a cache to satisfy this request. */
    boolean noCache;
    int maxAgeSeconds = -1;
    int maxStaleSeconds = -1;
    int minFreshSeconds = -1;

    /**
     * This field's name "only-if-cached" is misleading. It actually means "do
     * not use the network". It is set by a client who only wants to make a
     * request if it can be fully satisfied by the cache. Cached responses that
     * would require validation (ie. conditional gets) are not permitted if this
     * header is set.
     */
    boolean onlyIfCached;
    String noCacheField;

    /**
     * True if the request contains conditions that save the server from sending
     * a response that the client has locally. When the caller adds conditions,
     * this cache won't participate in the request.
     */
    boolean hasConditions;

    /**
     * True if the request contains an authorization field. Although this isn't
     * necessarily a shared cache, it follows the spec's strict requirements for
     * shared caches.
     */
    boolean hasAuthorization;

    public RequestHeaders(URI uri, RawHeaders headers) {
        this.uri = uri;
        this.headers = headers;

        HeaderParser.CacheControlHandler handler = new HeaderParser.CacheControlHandler() {
            @Override public void handle(String directive, String parameter) {
                if (directive.equalsIgnoreCase("no-cache")) {
                    noCache = true;
                    noCacheField = parameter;
                } else if (directive.equalsIgnoreCase("max-age")) {
                    maxAgeSeconds = HeaderParser.parseSeconds(parameter);
                } else if (directive.equalsIgnoreCase("max-stale")) {
                    maxStaleSeconds = HeaderParser.parseSeconds(parameter);
                } else if (directive.equalsIgnoreCase("min-fresh")) {
                    minFreshSeconds = HeaderParser.parseSeconds(parameter);
                } else if (directive.equalsIgnoreCase("only-if-cached")) {
                    onlyIfCached = true;
                }
            }
        };

        for (int i = 0; i < headers.length(); i++) {
            String fieldName = headers.getFieldName(i);
            String value = headers.getValue(i);
            if ("Cache-Control".equalsIgnoreCase(fieldName)) {
                HeaderParser.parseCacheControl(value, handler);
            } else if ("Pragma".equalsIgnoreCase(fieldName)) {
                if (value.equalsIgnoreCase("no-cache")) {
                    noCache = true;
                }
            } else if ("If-None-Match".equalsIgnoreCase(fieldName)) {
                hasConditions = true;
            } else if ("If-Modified-Since".equalsIgnoreCase(fieldName)) {
                hasConditions = true;
            } else if ("Authorization".equalsIgnoreCase(fieldName)) {
                hasAuthorization = true;
            }
        }
    }
}
