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

package libcore.net.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * The HTTP status and header lines of a single HTTP message. This class
 * maintains the order of the header lines within the HTTP message.
 *
 * <p>This class tracks fields line-by-line. A field with multiple comma-
 * separated values on the same line will be treated as a field with a single
 * value by this class. It is the caller's responsibility to detect and split
 * on commas if their field permits multiple values. This simplifies use of
 * single-valued fields whose values routinely contain commas, such as cookies
 * or dates.
 *
 * <p>This class trims whitespace from values. It never returns values with
 * leading or trailing whitespace.
 */
final class HttpHeaders implements Cloneable {

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

    private final List<String> alternatingKeysAndValues = new ArrayList<String>(20);
    private String statusLine;
    private int httpMinorVersion = 1;
    private int responseCode = -1;
    private String responseMessage;

    public HttpHeaders() {}

    public HttpHeaders(HttpHeaders copyFrom) {
        alternatingKeysAndValues.addAll(copyFrom.alternatingKeysAndValues);
        statusLine = copyFrom.statusLine;
        httpMinorVersion = copyFrom.httpMinorVersion;
        responseCode = copyFrom.responseCode;
        responseMessage = copyFrom.responseMessage;
    }

    public void setStatusLine(String statusLine) {
        this.statusLine = statusLine;

        // Status line sample: "HTTP/1.0 200 OK"
        if (statusLine == null || !statusLine.startsWith("HTTP/")) {
            return;
        }
        statusLine = statusLine.trim();
        int mark = statusLine.indexOf(" ") + 1;
        if (mark == 0) {
            return;
        }
        if (statusLine.charAt(mark - 2) != '1') {
            this.httpMinorVersion = 0;
        }
        int last = mark + 3;
        if (last > statusLine.length()) {
            last = statusLine.length();
        }
        this.responseCode = Integer.parseInt(statusLine.substring(mark, last));
        if (last + 1 <= statusLine.length()) {
            this.responseMessage = statusLine.substring(last + 1);
        }
    }

    public String getStatusLine() {
        return statusLine;
    }

    /**
     * Returns the status line's HTTP minor version. This returns 0 for HTTP/1.0
     * and 1 for HTTP/1.1. This returns 1 if the HTTP version is unknown.
     */
    public int getHttpMinorVersion() {
        return httpMinorVersion != -1 ? httpMinorVersion : 1;
    }

    /**
     * Returns the HTTP status code or -1 if it is unknown.
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Returns the HTTP status message or null if it is unknown.
     */
    public String getResponseMessage() {
        return responseMessage;
    }

    /**
     * Add a field with the specified value.
     */
    public void add(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("key == null");
        }
        if (value == null) {
            /*
             * Given null values, the RI sends a malformed header line like
             * "Accept\r\n". For platform compatibility and HTTP compliance, we
             * print a warning and ignore null values.
             */
            System.logW("Ignoring HTTP header field '" + key + "' because its value is null");
            return;
        }
        alternatingKeysAndValues.add(key);
        alternatingKeysAndValues.add(value.trim());
    }

    public void removeAll(String key) {
        for (int i = 0; i < alternatingKeysAndValues.size(); i += 2) {
            if (key.equalsIgnoreCase(alternatingKeysAndValues.get(i))) {
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
     * added. If the field is found, the existing values are replaced.
     */
    public void set(String key, String value) {
        removeAll(key);
        add(key, value);
    }

    /**
     * Returns the number of header lines.
     */
    public int length() {
        return alternatingKeysAndValues.size() / 2;
    }

    /**
     * Returns the key at {@code position} or null if that is out of range.
     */
    public String getKey(int index) {
        int keyIndex = index * 2;
        if (keyIndex < 0 || keyIndex >= alternatingKeysAndValues.size()) {
            return null;
        }
        return alternatingKeysAndValues.get(keyIndex);
    }

    /**
     * Returns the value at {@code index} or null if that is out of range.
     */
    public String getValue(int index) {
        int valueIndex = index * 2 + 1;
        if (valueIndex < 0 || valueIndex >= alternatingKeysAndValues.size()) {
            return null;
        }
        return alternatingKeysAndValues.get(valueIndex);
    }

    /**
     * Returns the last value corresponding to the specified key, or null.
     */
    public String get(String key) {
        for (int i = alternatingKeysAndValues.size() - 2; i >= 0; i -= 2) {
            if (key.equalsIgnoreCase(alternatingKeysAndValues.get(i))) {
                return alternatingKeysAndValues.get(i + 1);
            }
        }
        return null;
    }

    public String toHeaderString() {
        StringBuilder result = new StringBuilder(256);
        result.append(statusLine).append("\r\n");
        for (int i = 0; i < alternatingKeysAndValues.size(); i += 2) {
            result.append(alternatingKeysAndValues.get(i)).append(": ")
                    .append(alternatingKeysAndValues.get(i + 1)).append("\r\n");
        }
        result.append("\r\n");
        return result.toString();
    }

    /**
     * Returns an immutable map containing each field to its list of values. The
     * status line is mapped to null.
     */
    public Map<String, List<String>> toMultimap() {
        Map<String, List<String>> result = new TreeMap<String, List<String>>(HEADER_COMPARATOR);
        for (int i = 0; i < alternatingKeysAndValues.size(); i += 2) {
            String key = alternatingKeysAndValues.get(i);
            String value = alternatingKeysAndValues.get(i + 1);

            List<String> allValues = new ArrayList<String>();
            List<String> otherValues = result.get(key);
            if (otherValues != null) {
                allValues.addAll(otherValues);
            }
            allValues.add(value);
            result.put(key, Collections.unmodifiableList(allValues));
        }
        if (statusLine != null) {
            result.put(null, Collections.unmodifiableList(Collections.singletonList(statusLine)));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Creates a header from the given map of fields to values. If present, the
     * null key's last element will be used to set the status line.
     */
    public static HttpHeaders fromMultimap(Map<String, List<String>> map) {
        HttpHeaders result = new HttpHeaders();
        for (Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (key != null) {
                result.addAll(key, values);
            } else if (!values.isEmpty()) {
                result.setStatusLine(values.get(values.size() - 1));
            }
        }
        return result;
    }
}
