/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.luni.internal.net.www.protocol.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * The HTTP status and header lines of a single HTTP message. This class tracks
 * the order of the header lines within the HTTP message. This treats the status
 * line as a header value for the null field.
 */
public final class Header implements Cloneable {

    // TODO: drop the map?

    private static final Comparator<String> HEADER_COMPARATOR = new Comparator<String>() {
        @Override public int compare(String a, String b) {
            if (a == b) {
                return 0;
            } else if (a == null) {
                return -1;
            } else if (b == null) {
                return 1;
            } else {
                return String.CASE_INSENSITIVE_ORDER.compare(a, b);
            }
        }
    };

    private ArrayList<String> alternatingKeysAndValues = new ArrayList<String>(20);
    private TreeMap<String, LinkedList<String>> keysToValuesMap
            = new TreeMap<String, LinkedList<String>>(HEADER_COMPARATOR);

    public Header() {}

    public Header(Map<String, List<String>> copyFrom) {
        for (Entry<String, List<String>> entry : copyFrom.entrySet()) {
            addAll(entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Header clone() {
        try {
            Header clone = (Header) super.clone();
            clone.alternatingKeysAndValues = (ArrayList<String>) alternatingKeysAndValues.clone();
            clone.keysToValuesMap = (TreeMap<String, LinkedList<String>>) keysToValuesMap.clone();
            for (Map.Entry<String, LinkedList<String>> entry : clone.keysToValuesMap.entrySet()) {
                entry.setValue((LinkedList<String>) entry.getValue().clone());
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Add a field with the specified value.
     */
    public void add(String key, String value) {
        if (value == null) {
            /*
             * Given null values, the RI sends a malformed header line like
             * "Accept\r\n". For platform compatibility and HTTP compliance, we
             * print a warning and ignore null values.
             */
            Logger.getAnonymousLogger().warning(
                    "Ignoring HTTP header field " + key + " because its value is null.");
            return;
        }
        LinkedList<String> list = keysToValuesMap.get(key);
        if (list == null) {
            list = new LinkedList<String>();
            keysToValuesMap.put(key, list);
        }
        list.add(value);
        if (key == null) {
            alternatingKeysAndValues.add(0, key);
            alternatingKeysAndValues.add(1, value);
        } else {
            alternatingKeysAndValues.add(key);
            alternatingKeysAndValues.add(value);
        }
    }

    public void removeAll(String key) {
        keysToValuesMap.remove(key);

        for (int i = 0; i < alternatingKeysAndValues.size(); i += 2) {
            if (key.equals(alternatingKeysAndValues.get(i))) {
                alternatingKeysAndValues.remove(i); // key
                alternatingKeysAndValues.remove(i); // value
            }
        }
    }

    public void addAll(String key, List<String> headers) {
        for (String header : headers) {
            add(key, header);
        }
    }

    public void addIfAbsent(String key, String value) {
        if (get(key) == null) {
            add(key, value);
        }
    }

    /**
     * Set a field with the specified value. If the field is not found, it is
     * added. If the field is found, the existing value(s) are overwritten.
     */
    public void set(String key, String value) {
        removeAll(key);
        add(key, value);
    }

    /**
     * Provides an unmodifiable map with all String header names mapped to their
     * String values. The map keys are Strings and the values are unmodifiable
     * Lists of Strings.
     *
     * @return an unmodifiable map of the headers
     */
    public Map<String, List<String>> getFieldMap() {
        @SuppressWarnings("unchecked") // cloning a collection retains type parameters
        Map<String, List<String>> result = (TreeMap<String, List<String>>) keysToValuesMap.clone();
        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns the value at {@code position} or null if that is out of range.
     */
    public String get(int valueIndex) {
        int index = valueIndex * 2 + 1;
        if (index < 0 || index >= alternatingKeysAndValues.size()) {
            return null;
        }
        return alternatingKeysAndValues.get(index);
    }

    /**
     * Returns the key at {@code position} or null if that is out of range.
     */
    public String getKey(int keyIndex) {
        int index = keyIndex * 2;
        if (index < 0 || index >= alternatingKeysAndValues.size()) {
            return null;
        }
        return alternatingKeysAndValues.get(index);
    }

    /**
     * Returns the value corresponding to the specified key, or null.
     */
    public String get(String key) {
        LinkedList<String> result = keysToValuesMap.get(key);
        if (result == null) {
            return null;
        }
        return result.getLast();
    }

    /**
     * Returns the number of keys stored in this header
     */
    public int length() {
        return alternatingKeysAndValues.size() / 2;
    }

    public void setStatusLine(String statusLine) {
        add(null, statusLine);
    }

    public String getStatusLine() {
        return get(null);
    }
}
