/*
 * Copyright (C) 2013 The Android Open Source Project
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

package libcore.java.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class EvilMapTest extends junit.framework.TestCase {
  public static class EvilMap<K,V> implements Map<K,V> {
    private final Set<Entry<K,V>> entries = new HashSet<Entry<K,V>>();

    public EvilMap() {
      entries.add(new HashMapEntry("hi", "there", "there".hashCode(), null));
    }

    // Claim we're empty...
    @Override public int size() { return 0; }
    // ...but potentially return many entries.
    @Override public Set<Entry<K, V>> entrySet() { return entries; }

    @Override public V put(K key, V val) { return val; }
    @Override public V remove(Object val) { return (V) val; }
    @Override public V get(Object key) { return null; }
    @Override public boolean containsKey(Object key) { return false; }
    @Override public void clear() { }
    @Override public boolean isEmpty() { return true; }
    @Override public void putAll(Map<? extends K, ? extends V> map) { }
    @Override public Set<V> values() { return null; }
    @Override public Set<K> keySet() { return null; }
    @Override public boolean containsValue(Object val) { return false; }
  }

  public static class HashMapEntry<K, V> implements Map.Entry<K, V> {
    final K key;
    V value;
    final int hash;
    HashMapEntry<K, V> next;

    HashMapEntry(K key, V value, int hash, HashMapEntry<K, V> next) {
      this.key = key;
      this.value = value;
      this.hash = hash;
      this.next = next;
    }

    public final K getKey() { return key; }

    public final V getValue() { return value; }

    public final V setValue(V value) {
      V oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    @Override public final boolean equals(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
      return e.getKey().equals(key) && e.getValue().equals(value);
    }

    @Override public final int hashCode() {
      return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }

    @Override public final String toString() {
      return key + "=" + value;
    }
  }

  // https://code.google.com/p/android/issues/detail?id=48055
  public void test_48055_HashMap() throws Exception {
    Map<String, String> evil = new EvilMap<String, String>();
    evil.put("hi", "there");
    // Corrupt one HashMap...
    HashMap<String, String> map = new HashMap<String, String>(evil);
    // ...and now they're all corrupted.
    HashMap<String, String> map2 = new HashMap<String, String>();
    assertNull(map2.get("hi"));
  }

  public void test_48055_Hashtable() throws Exception {
    Map<String, String> evil = new EvilMap<String, String>();
    evil.put("hi", "there");
    // Corrupt one Hashtable...
    Hashtable<String, String> map = new Hashtable<String, String>(evil);
    // ...and now they're all corrupted.
    Hashtable<String, String> map2 = new Hashtable<String, String>();
    assertNull(map2.get("hi"));
  }

  public void test_48055_LinkedHashMap() throws Exception {
    Map<String, String> evil = new EvilMap<String, String>();
    evil.put("hi", "there");
    // Corrupt one LinkedHashMap...
    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(evil);
    // ...and now they're all corrupted.
    LinkedHashMap<String, String> map2 = new LinkedHashMap<String, String>();
    assertNull(map2.get("hi"));
  }

  public void test_48055_WeakHashMap() throws Exception {
    Map<String, String> evil = new EvilMap<String, String>();
    evil.put("hi", "there");
    // Corrupt one WeakHashMap...
    WeakHashMap<String, String> map = new WeakHashMap<String, String>(evil);
    // ...and now they're all corrupted.
    WeakHashMap<String, String> map2 = new WeakHashMap<String, String>();
    assertNull(map2.get("hi"));
  }

  public void test_48055_IdentityHashMap() throws Exception {
    Map<String, String> evil = new EvilMap<String, String>();
    evil.put("hi", "there");
    // Corrupt one IdentityHashMap...
    IdentityHashMap<String, String> map = new IdentityHashMap<String, String>(evil);
    // ...and now they're all corrupted.
    IdentityHashMap<String, String> map2 = new IdentityHashMap<String, String>();
    assertNull(map2.get("hi"));
  }

  public void test_48055_ConcurrentHashMap() throws Exception {
    Map<String, String> evil = new EvilMap<String, String>();
    evil.put("hi", "there");
    // Corrupt one ConcurrentHashMap...
    ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>(evil);
    // ...and now they're all corrupted.
    ConcurrentHashMap<String, String> map2 = new ConcurrentHashMap<String, String>();
    assertNull(map2.get("hi"));
  }


}
