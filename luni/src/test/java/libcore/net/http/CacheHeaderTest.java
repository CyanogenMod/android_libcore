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

import java.util.Date;
import junit.framework.TestCase;

public final class CacheHeaderTest extends TestCase {

    public void testUpperCaseHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("CACHE-CONTROL", "no-store");
        headers.add("DATE", "Thu, 01 Jan 1970 00:00:01 UTC");
        headers.add("EXPIRES", "Thu, 01 Jan 1970 00:00:02 UTC");
        headers.add("LAST-MODIFIED", "Thu, 01 Jan 1970 00:00:03 UTC");
        headers.add("ETAG", "v1");
        headers.add("PRAGMA", "no-cache");

        CacheHeader cacheHeader = new CacheHeader(headers);
        assertTrue(cacheHeader.noStore);
        assertEquals(new Date(1000), cacheHeader.servedDate);
        assertEquals(new Date(2000), cacheHeader.expires);
        assertEquals(new Date(3000), cacheHeader.lastModified);
        assertEquals("v1", cacheHeader.etag);
        assertTrue(cacheHeader.noCache);
    }

    public void testCommaSeparatedCacheControlHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-store, max-age=60, private");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertTrue(cacheHeader.noStore);
        assertEquals(60, cacheHeader.maxAgeSeconds);
        assertTrue(cacheHeader.isPrivate);
    }

    public void testQuotedFieldName() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "private=\"Set-Cookie\"");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertEquals("Set-Cookie", cacheHeader.privateField);
    }

    public void testUnquotedValue() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "private=Set-Cookie, no-store");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertEquals("Set-Cookie", cacheHeader.privateField);
        assertTrue(cacheHeader.noStore);
    }

    public void testQuotedValue() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "private=\" a, no-cache, c \", no-store");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertEquals(" a, no-cache, c ", cacheHeader.privateField);
        assertTrue(cacheHeader.noStore);
        assertFalse(cacheHeader.noCache);
    }

    public void testDanglingQuote() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "private=\"a, no-cache, c");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertEquals("a, no-cache, c", cacheHeader.privateField);
        assertFalse(cacheHeader.noCache);
    }

    public void testTrailingComma() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "private,");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertTrue(cacheHeader.isPrivate);
        assertNull(cacheHeader.privateField);
    }

    public void testTrailingEquals() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "private=");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertTrue(cacheHeader.isPrivate);
        assertEquals("", cacheHeader.privateField);
    }

    public void testSpaceBeforeEquals() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "max-age =60");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertEquals(60, cacheHeader.maxAgeSeconds);
    }

    public void testSpaceAfterEqualsWithQuotes() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "max-age= \"60\"");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertEquals(60, cacheHeader.maxAgeSeconds);
    }

    public void testSpaceAfterEqualsWithoutQuotes() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "max-age= 60");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertEquals(60, cacheHeader.maxAgeSeconds);
    }

    public void testCacheControlDirectivesAreCaseInsensitive() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "NO-CACHE");
        headers.add("Cache-Control", "NO-STORE");
        headers.add("Cache-Control", "MAX-AGE=60");
        headers.add("Cache-Control", "S-MAXAGE=70");
        headers.add("Cache-Control", "MAX-STALE=80");
        headers.add("Cache-Control", "MIN-FRESH=90");
        headers.add("Cache-Control", "NO-TRANSFORM");
        headers.add("Cache-Control", "ONLY-IF-CACHED");
        headers.add("Cache-Control", "PUBLIC");
        headers.add("Cache-Control", "PRIVATE");
        headers.add("Cache-Control", "MUST-REVALIDATE");
        headers.add("Cache-Control", "PROXY-REVALIDATE");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertTrue(cacheHeader.noCache);
        assertTrue(cacheHeader.noStore);
        assertEquals(60, cacheHeader.maxAgeSeconds);
        assertEquals(70, cacheHeader.sMaxAgeSeconds);
        assertEquals(80, cacheHeader.maxStaleSeconds);
        assertEquals(90, cacheHeader.minFreshSeconds);
        assertTrue(cacheHeader.noTransform);
        assertTrue(cacheHeader.onlyIfCached);
        assertTrue(cacheHeader.isPublic);
        assertTrue(cacheHeader.isPrivate);
        assertTrue(cacheHeader.mustRevalidate);
        assertTrue(cacheHeader.proxyRevalidate);
    }

    public void testPragmaDirectivesAreCaseInsensitive() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Pragma", "NO-CACHE");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertTrue(cacheHeader.noCache);
    }

    public void testMissingInteger() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "max-age");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertEquals(-1, cacheHeader.maxAgeSeconds);
    }

    public void testInvalidInteger() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "MAX-AGE=pi");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertEquals(-1, cacheHeader.maxAgeSeconds);
    }

    public void testVeryLargeInteger() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "MAX-AGE=" + (Integer.MAX_VALUE + 1L));
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertEquals(Integer.MAX_VALUE, cacheHeader.maxAgeSeconds);
    }

    public void testNegativeInteger() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "MAX-AGE=-2");
        CacheHeader cacheHeader = new CacheHeader(headers);
        assertEquals(0, cacheHeader.maxAgeSeconds);
    }
}
