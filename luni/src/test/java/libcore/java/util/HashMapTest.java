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

import java.util.ConcurrentModificationException;
import java.util.HashMap;

public class HashMapTest extends junit.framework.TestCase {

    public void test_getOrDefault() {
        MapDefaultMethodTester.test_getOrDefault(new HashMap<>(), true /*acceptsNullKey*/,
                true /*acceptsNullValue*/);
    }

    public void test_forEach() {
        MapDefaultMethodTester.test_forEach(new HashMap<>());
    }

    public void test_putIfAbsent() {
        MapDefaultMethodTester.test_putIfAbsent(new HashMap<>(), true /*acceptsNullKey*/,
                true /*acceptsNullValue*/);
    }

    public void test_remove() {
        MapDefaultMethodTester
                .test_remove(new HashMap<>(), true /*acceptsNullKey*/, true /*acceptsNullValue*/);
    }

    public void test_replace$K$V$V() {
        MapDefaultMethodTester
                .test_replace$K$V$V(new HashMap<>(), true /*acceptsNullKey*/,
                        true /*acceptsNullValue*/);
    }

    public void test_replace$K$V() {
        MapDefaultMethodTester.test_replace$K$V(new HashMap<>(), true /*acceptsNullKey*/,
                true /*acceptsNullValue*/);
    }

    public void test_computeIfAbsent() {
        MapDefaultMethodTester.test_computeIfAbsent(new HashMap<>(), true /*acceptsNullKey*/,
                true /*acceptsNullValue*/);
    }

    public void test_computeIfPresent() {
        MapDefaultMethodTester.test_computeIfPresent(new HashMap<>(), true /*acceptsNullKey*/);
    }

    public void test_compute() {
        MapDefaultMethodTester
                .test_compute(new HashMap<>(), true /*acceptsNullKey*/);
    }

    public void test_merge() {
        MapDefaultMethodTester
                .test_merge(new HashMap<>(), true /*acceptsNullKey*/);
    }

    public void test_replaceAll() throws Exception {
        HashMap<String, String> map = new HashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");

        map.replaceAll((k, v) -> k + v);
        assertEquals("one1", map.get("one"));
        assertEquals("two2", map.get("two"));
        assertEquals("three3", map.get("three"));
        assertEquals(3, map.size());

        try {
            map.replaceAll(new java.util.function.BiFunction<String, String, String>() {
                @Override
                public String apply(String k, String v) {
                    map.put("foo1", v);
                    return v;
                }
            });
            fail();
        } catch(ConcurrentModificationException expected) {}

        try {
            map.replaceAll(null);
            fail();
        } catch(NullPointerException expected) {}
    }
}
