/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package libcore.java.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LinkedHashMapTest extends junit.framework.TestCase {

    public void test_getOrDefault() {
        MapDefaultMethodTester
                .test_getOrDefault(new LinkedHashMap<>(), true /*acceptsNullKey*/,
                        true /*acceptsNullValue*/);

        // Test for access order
        Map<String, String> m = new LinkedHashMap<String, String>(8, .75f, true);
        m.put("key", "value");
        m.put("key1", "value1");
        m.put("key2", "value2");
        m.getOrDefault("key1", "value");
        Map.Entry<String, String> newest = null;
        for (Map.Entry<String, String> e : m.entrySet()) {
            newest = e;
        }
        assertEquals("key1", newest.getKey());
        assertEquals("value1", newest.getValue());
    }

    public void test_forEach() {
        MapDefaultMethodTester.test_forEach(new LinkedHashMap<>());
    }

    public void test_putIfAbsent() {
        MapDefaultMethodTester.test_putIfAbsent(new LinkedHashMap<>(), true /*acceptsNullKey*/,
                true /*acceptsNullValue*/);

        // Test for access order
        Map<String, String> m = new LinkedHashMap<String, String>(8, .75f, true);
        m.putIfAbsent("key", "value");
        m.putIfAbsent("key1", "value1");
        m.putIfAbsent("key2", "value2");
        Map.Entry<String, String> newest = null;
        for (Map.Entry<String, String> e : m.entrySet()) {
            newest = e;
        }
        assertEquals("key2", newest.getKey());
        assertEquals("value2", newest.getValue());

        // for existed key
        m.putIfAbsent("key1", "value1");
        for (Map.Entry<String, String> e : m.entrySet()) {
            newest = e;
        }
        assertEquals("key1", newest.getKey());
        assertEquals("value1", newest.getValue());
    }

    public void test_remove() {
        MapDefaultMethodTester.test_remove(new LinkedHashMap<>(), true /*acceptsNullKey*/,
                true /*acceptsNullValue*/);
    }

    public void test_replace$K$V$V() {
        MapDefaultMethodTester.
                test_replace$K$V$V(new LinkedHashMap<>(), true /*acceptsNullKey*/,
                        true /*acceptsNullValue*/);

        // Test for access order
        Map<String, String> m = new LinkedHashMap<>(8, .75f, true  /*accessOrder*/);
        m.put("key", "value");
        m.put("key1", "value1");
        m.put("key2", "value2");
        m.replace("key1", "value1", "value2");
        Map.Entry<String, String> newest = null;
        for (Map.Entry<String, String> e : m.entrySet()) {
            newest = e;
        }
        assertEquals("key1", newest.getKey());
        assertEquals("value2", newest.getValue());

        // for wrong pair of key and value, last accessed node should
        // not change
        m.replace("key2", "value1", "value3");
        for (Map.Entry<String, String> e : m.entrySet()) {
            newest = e;
        }
        assertEquals("key1", newest.getKey());
        assertEquals("value2", newest.getValue());
    }

    public void test_replace$K$V() {
        MapDefaultMethodTester.test_replace$K$V(new LinkedHashMap<>(), true /*acceptsNullKey*/,
                true /*acceptsNullValue*/);

        // Test for access order
        Map<String, String> m = new LinkedHashMap<>(8, .75f, true  /*accessOrder*/);
        m.put("key", "value");
        m.put("key1", "value1");
        m.put("key2", "value2");
        m.replace("key1", "value2");
        Map.Entry<String, String> newest = null;
        for (Map.Entry<String, String> e : m.entrySet()) {
            newest = e;
        }
        assertEquals("key1", newest.getKey());
        assertEquals("value2", newest.getValue());
    }

    public void test_computeIfAbsent() {
        MapDefaultMethodTester.test_computeIfAbsent(new LinkedHashMap<>(), true /*acceptsNullKey*/,
                true /*acceptsNullValue*/);

        // Test for access order
        Map<String, String> m = new LinkedHashMap<>(8, .75f, true  /*accessOrder*/);
        m.put("key", "value");
        m.put("key1", "value1");
        m.put("key2", "value2");
        m.computeIfAbsent("key1", (k) -> "value3");
        Map.Entry<String, String> newest = null;
        for (Map.Entry<String, String> e : m.entrySet()) {
            newest = e;
        }
        assertEquals("key1", newest.getKey());
        assertEquals("value1", newest.getValue());

        // When value is absent
        m.computeIfAbsent("key4", (k) -> "value3");
        newest = null;
        for (Map.Entry<String, String> e : m.entrySet()) {
            newest = e;
        }
        assertEquals("key4", newest.getKey());
        assertEquals("value3", newest.getValue());
    }

    public void test_computeIfPresent() {
        MapDefaultMethodTester.test_computeIfPresent(new LinkedHashMap<>(), true /*acceptsNullKey*/);

        // Test for access order
        Map<String, String> m = new LinkedHashMap<>(8, .75f, true  /*accessOrder*/);
        m.put("key", "value");
        m.put("key1", "value1");
        m.put("key2", "value2");
        m.computeIfPresent("key1", (k, v) -> "value3");
        Map.Entry<String, String> newest = null;
        for (Map.Entry<String, String> e : m.entrySet()) {
            newest = e;
        }
        assertEquals("key1", newest.getKey());
        assertEquals("value3", newest.getValue());
    }

    public void test_compute() {
        MapDefaultMethodTester.test_compute(new LinkedHashMap<>(), true /*acceptsNullKey*/);

        // Test for access order
        Map<String, String> m = new LinkedHashMap<>(8, .75f, true  /*accessOrder*/);
        m.put("key", "value");
        m.put("key1", "value1");
        m.put("key2", "value2");
        m.compute("key1", (k, v) -> "value3");
        Map.Entry<String, String> newest = null;
        for (Map.Entry<String, String> e : m.entrySet()) {
            newest = e;
        }
        assertEquals("key1", newest.getKey());
        assertEquals("value3", newest.getValue());

        m.compute("key4", (k, v) -> "value4");
        newest = null;
        for (Map.Entry<String, String> e : m.entrySet()) {
            newest = e;
        }
        assertEquals("key4", newest.getKey());
        assertEquals("value4", newest.getValue());
    }

    public void test_merge() {
        MapDefaultMethodTester.test_merge(new LinkedHashMap<>(), true /*acceptsNullKey*/);

        // Test for access order
        Map<String, String> m = new LinkedHashMap<>(8, .75f, true  /*accessOrder*/);
        m.put("key", "value");
        m.put("key1", "value1");
        m.put("key2", "value2");
        m.merge("key1", "value3", (k, v) -> "value3");
        Map.Entry<String, String> newest = null;
        for (Map.Entry<String, String> e : m.entrySet()) {
            newest = e;
        }
        assertEquals("key1", newest.getKey());
        assertEquals("value3", newest.getValue());
    }

    // http://b/27929722
    // This tests the behaviour is consistent with earlier Android releases.
    // This behaviour is NOT consistent with the RI. Future Android releases
    // might change this.
    public void test_removeEldestEntry() {
        final AtomicBoolean removeEldestEntryReturnValue = new AtomicBoolean(false);
        final AtomicInteger removeEldestEntryCallCount = new AtomicInteger(0);
        LinkedHashMap<String, String> m = new LinkedHashMap<String, String>() {
            @Override
            protected boolean removeEldestEntry(Entry eldest) {
                removeEldestEntryCallCount.incrementAndGet();
                return removeEldestEntryReturnValue.get();
            }
        };

        m.put("foo", "bar");
        assertEquals(0, removeEldestEntryCallCount.get());
        m.put("baz", "quux");
        assertEquals(1, removeEldestEntryCallCount.get());

        removeEldestEntryReturnValue.set(true);
        m.put("foob", "faab");
        assertEquals(2, removeEldestEntryCallCount.get());
        assertEquals(2, m.size());
        assertFalse(m.containsKey("foo"));
    }
}
