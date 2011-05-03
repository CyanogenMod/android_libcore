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
import java.util.Date;
import junit.framework.TestCase;

public final class ParsedHeadersTest extends TestCase {

    private URI uri;

    @Override protected void setUp() throws Exception {
        super.setUp();
        uri = new URI("http", "localhost", "/");
    }

    public void testUpperCaseHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("CACHE-CONTROL", "no-store");
        headers.add("DATE", "Thu, 01 Jan 1970 00:00:01 UTC");
        headers.add("EXPIRES", "Thu, 01 Jan 1970 00:00:02 UTC");
        headers.add("LAST-MODIFIED", "Thu, 01 Jan 1970 00:00:03 UTC");
        headers.add("ETAG", "v1");
        headers.add("PRAGMA", "no-cache");
        ResponseHeaders parsedHeaders = new ResponseHeaders(uri, headers);
        assertTrue(parsedHeaders.noStore);
        assertEquals(new Date(1000), parsedHeaders.servedDate);
        assertEquals(new Date(2000), parsedHeaders.expires);
        assertEquals(new Date(3000), parsedHeaders.lastModified);
        assertEquals("v1", parsedHeaders.etag);
        assertTrue(parsedHeaders.noCache);
    }

    public void testCommaSeparatedCacheControlHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-store, max-age=60, public");
        ResponseHeaders parsedHeaders = new ResponseHeaders(uri, headers);
        assertTrue(parsedHeaders.noStore);
        assertEquals(60, parsedHeaders.maxAgeSeconds);
        assertTrue(parsedHeaders.isPublic);
    }

    public void testQuotedFieldName() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "private=\"Set-Cookie\"");
        ResponseHeaders parsedHeaders = new ResponseHeaders(uri, headers);
        assertEquals("Set-Cookie", parsedHeaders.privateField);
    }

    public void testUnquotedValue() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "private=Set-Cookie, no-store");
        ResponseHeaders parsedHeaders = new ResponseHeaders(uri, headers);
        assertEquals("Set-Cookie", parsedHeaders.privateField);
        assertTrue(parsedHeaders.noStore);
    }

    public void testQuotedValue() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "private=\" a, no-cache, c \", no-store");
        ResponseHeaders parsedHeaders = new ResponseHeaders(uri, headers);
        assertEquals(" a, no-cache, c ", parsedHeaders.privateField);
        assertTrue(parsedHeaders.noStore);
        assertFalse(parsedHeaders.noCache);
    }

    public void testDanglingQuote() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "private=\"a, no-cache, c");
        ResponseHeaders parsedHeaders = new ResponseHeaders(uri, headers);
        assertEquals("a, no-cache, c", parsedHeaders.privateField);
        assertFalse(parsedHeaders.noCache);
    }

    public void testTrailingComma() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "public,");
        ResponseHeaders parsedHeaders = new ResponseHeaders(uri, headers);
        assertTrue(parsedHeaders.isPublic);
        assertNull(parsedHeaders.privateField);
    }

    public void testTrailingEquals() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "private=");
        ResponseHeaders parsedHeaders = new ResponseHeaders(uri, headers);
        assertEquals("", parsedHeaders.privateField);
    }

    public void testSpaceBeforeEquals() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "max-age =60");
        RequestHeaders parsedHeaders = new RequestHeaders(uri, headers);
        assertEquals(60, parsedHeaders.maxAgeSeconds);
    }

    public void testSpaceAfterEqualsWithQuotes() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "max-age= \"60\"");
        RequestHeaders parsedHeaders = new RequestHeaders(uri, headers);
        assertEquals(60, parsedHeaders.maxAgeSeconds);
    }

    public void testSpaceAfterEqualsWithoutQuotes() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "max-age= 60");
        RequestHeaders parsedHeaders = new RequestHeaders(uri, headers);
        assertEquals(60, parsedHeaders.maxAgeSeconds);
    }

    public void testCacheControlRequestDirectivesAreCaseInsensitive() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "NO-CACHE");
        headers.add("Cache-Control", "MAX-AGE=60");
        headers.add("Cache-Control", "MAX-STALE=70");
        headers.add("Cache-Control", "MIN-FRESH=80");
        headers.add("Cache-Control", "ONLY-IF-CACHED");
        RequestHeaders parsedHeaders = new RequestHeaders(uri, headers);
        assertTrue(parsedHeaders.noCache);
        assertEquals(60, parsedHeaders.maxAgeSeconds);
        assertEquals(70, parsedHeaders.maxStaleSeconds);
        assertEquals(80, parsedHeaders.minFreshSeconds);
        assertTrue(parsedHeaders.onlyIfCached);
    }

    public void testCacheControlResponseDirectivesAreCaseInsensitive() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "NO-CACHE");
        headers.add("Cache-Control", "NO-STORE");
        headers.add("Cache-Control", "MAX-AGE=60");
        headers.add("Cache-Control", "S-MAXAGE=70");
        headers.add("Cache-Control", "PUBLIC");
        headers.add("Cache-Control", "PRIVATE=a");
        headers.add("Cache-Control", "MUST-REVALIDATE");
        ResponseHeaders parsedHeaders = new ResponseHeaders(uri, headers);
        assertTrue(parsedHeaders.noCache);
        assertTrue(parsedHeaders.noStore);
        assertEquals(60, parsedHeaders.maxAgeSeconds);
        assertEquals(70, parsedHeaders.sMaxAgeSeconds);
        assertTrue(parsedHeaders.isPublic);
        assertEquals("a", parsedHeaders.privateField);
        assertTrue(parsedHeaders.mustRevalidate);
    }

    public void testPragmaDirectivesAreCaseInsensitive() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Pragma", "NO-CACHE");
        RequestHeaders parsedHeaders = new RequestHeaders(uri, headers);
        assertTrue(parsedHeaders.noCache);
    }

    public void testMissingInteger() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "max-age");
        RequestHeaders parsedHeaders = new RequestHeaders(uri, headers);
        assertEquals(-1, parsedHeaders.maxAgeSeconds);
    }

    public void testInvalidInteger() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "MAX-AGE=pi");
        RequestHeaders requestHeaders = new RequestHeaders(uri, headers);
        assertEquals(-1, requestHeaders.maxAgeSeconds);
    }

    public void testVeryLargeInteger() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "MAX-AGE=" + (Integer.MAX_VALUE + 1L));
        RequestHeaders parsedHeaders = new RequestHeaders(uri, headers);
        assertEquals(Integer.MAX_VALUE, parsedHeaders.maxAgeSeconds);
    }

    public void testNegativeInteger() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "MAX-AGE=-2");
        RequestHeaders parsedHeaders = new RequestHeaders(uri, headers);
        assertEquals(0, parsedHeaders.maxAgeSeconds);
    }
}
