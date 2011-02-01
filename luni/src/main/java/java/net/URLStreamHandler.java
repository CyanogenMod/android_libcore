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

package java.net;

import java.io.IOException;
import java.nio.charset.Charsets;
import libcore.base.Objects;
import org.apache.harmony.luni.util.URLUtil;

/**
 * The abstract class {@code URLStreamHandler} is the base for all classes which
 * can handle the communication with a URL object over a particular protocol
 * type.
 */
public abstract class URLStreamHandler {
    /**
     * Establishes a new connection to the resource specified by the URL {@code
     * u}. Since different protocols also have unique ways of connecting, it
     * must be overwritten by the subclass.
     *
     * @param u
     *            the URL to the resource where a connection has to be opened.
     * @return the opened URLConnection to the specified resource.
     * @throws IOException
     *             if an I/O error occurs during opening the connection.
     */
    protected abstract URLConnection openConnection(URL u) throws IOException;

    /**
     * Establishes a new connection to the resource specified by the URL {@code
     * u} using the given {@code proxy}. Since different protocols also have
     * unique ways of connecting, it must be overwritten by the subclass.
     *
     * @param u
     *            the URL to the resource where a connection has to be opened.
     * @param proxy
     *            the proxy that is used to make the connection.
     * @return the opened URLConnection to the specified resource.
     * @throws IOException
     *             if an I/O error occurs during opening the connection.
     * @throws IllegalArgumentException
     *             if any argument is {@code null} or the type of proxy is
     *             wrong.
     * @throws UnsupportedOperationException
     *             if the protocol handler doesn't support this method.
     */
    protected URLConnection openConnection(URL u, Proxy proxy) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the clear text URL in {@code str} into a URL object. URL strings
     * generally have the following format:
     * <p>
     * http://www.company.com/java/file1.java#reference
     * <p>
     * The string is parsed in HTTP format. If the protocol has a different URL
     * format this method must be overridden.
     *
     * @param u
     *            the URL to fill in the parsed clear text URL parts.
     * @param str
     *            the URL string that is to be parsed.
     * @param start
     *            the string position from where to begin parsing.
     * @param end
     *            the string position to stop parsing.
     * @see #toExternalForm
     * @see URL
     */
    protected void parseURL(URL u, String str, int start, int end) {
        // For compatibility, refer to Harmony-2941
        if (str.startsWith("//", start)
                && str.indexOf('/', start + 2) == -1
                && end <= Integer.MIN_VALUE + 1) {
            throw new StringIndexOutOfBoundsException(end - 2 - start);
        }
        if (end < start) {
            if (this != u.strmHandler) {
                throw new SecurityException();
            }
            return;
        }
        String parseString = "";
        if (start < end) {
            parseString = str.substring(start, end);
        }
        end -= start;
        int fileIdx = 0;

        // Default is to use info from context
        String host = u.getHost();
        int port = u.getPort();
        String ref = u.getRef();
        String file = u.getPath();
        String query = u.getQuery();
        String authority = u.getAuthority();
        String userInfo = u.getUserInfo();

        int refIdx = parseString.indexOf('#', 0);
        if (parseString.startsWith("//")) {
            int hostIdx = 2;
            port = -1;
            fileIdx = parseString.indexOf('/', hostIdx);
            int questionMarkIndex = parseString.indexOf('?', hostIdx);
            if (questionMarkIndex != -1 && (fileIdx == -1 || fileIdx > questionMarkIndex)) {
                fileIdx = questionMarkIndex;
            }
            if (fileIdx == -1) {
                fileIdx = end;
                // Use default
                file = "";
            }
            int hostEnd = fileIdx;
            if (refIdx != -1 && refIdx < fileIdx) {
                hostEnd = refIdx;
                fileIdx = refIdx;
                file = "";
            }
            int userIdx = parseString.lastIndexOf('@', hostEnd);
            authority = parseString.substring(hostIdx, hostEnd);
            if (userIdx != -1) {
                userInfo = parseString.substring(hostIdx, userIdx);
                hostIdx = userIdx + 1;
            }

            int endOfIPv6Addr = parseString.indexOf(']', hostIdx);
            if (endOfIPv6Addr >= hostEnd) {
                endOfIPv6Addr = -1;
            }

            // the port separator must be immediately after an IPv6 address "http://[::1]:80/"
            int portIdx = -1;
            if (endOfIPv6Addr != -1) {
                int maybeColon = endOfIPv6Addr + 1;
                if (maybeColon < hostEnd && parseString.charAt(maybeColon) == ':') {
                    portIdx = maybeColon;
                }
            } else {
                portIdx = parseString.indexOf(':', hostIdx);
            }

            if (portIdx == -1 || portIdx > hostEnd) {
                host = parseString.substring(hostIdx, hostEnd);
            } else {
                host = parseString.substring(hostIdx, portIdx);
                String portString = parseString.substring(portIdx + 1, hostEnd);
                if (portString.length() == 0) {
                    port = -1;
                } else {
                    port = Integer.parseInt(portString);
                }
            }
        }

        if (refIdx > -1) {
            ref = parseString.substring(refIdx + 1, end);
        }
        int fileEnd = (refIdx == -1 ? end : refIdx);

        int queryIdx = parseString.lastIndexOf('?', fileEnd);
        boolean canonicalize = false;
        if (queryIdx > -1) {
            query = parseString.substring(queryIdx + 1, fileEnd);
            if (queryIdx == 0 && file != null) {
                if (file.isEmpty()) {
                    file = "/";
                } else if (file.startsWith("/")) {
                    canonicalize = true;
                }
                int last = file.lastIndexOf('/') + 1;
                file = file.substring(0, last);
            }
            fileEnd = queryIdx;
        } else
        // Don't inherit query unless only the ref is changed
        if (refIdx != 0) {
            query = null;
        }

        if (fileIdx > -1) {
            if (fileIdx < end && parseString.charAt(fileIdx) == '/') {
                file = parseString.substring(fileIdx, fileEnd);
            } else if (fileEnd > fileIdx) {
                if (file == null) {
                    file = "";
                } else if (file.isEmpty()) {
                    file = "/";
                } else if (file.startsWith("/")) {
                    canonicalize = true;
                }
                int last = file.lastIndexOf('/') + 1;
                if (last == 0) {
                    file = parseString.substring(fileIdx, fileEnd);
                } else {
                    file = file.substring(0, last)
                            + parseString.substring(fileIdx, fileEnd);
                }
            }
        }
        if (file == null) {
            file = "";
        }

        if (host == null) {
            host = "";
        }

        if (canonicalize) {
            // modify file if there's any relative referencing
            file = URLUtil.canonicalizePath(file);
        }

        setURL(u, u.getProtocol(), host, port, authority, userInfo, file,
                query, ref);
    }

    /**
     * Sets the fields of the URL {@code u} to the values of the supplied
     * arguments.
     *
     * @param u
     *            the non-null URL object to be set.
     * @param protocol
     *            the protocol.
     * @param host
     *            the host name.
     * @param port
     *            the port number.
     * @param file
     *            the file component.
     * @param ref
     *            the reference.
     * @deprecated use setURL(URL, String String, int, String, String, String,
     *             String, String) instead.
     */
    @Deprecated
    protected void setURL(URL u, String protocol, String host, int port,
            String file, String ref) {
        if (this != u.strmHandler) {
            throw new SecurityException();
        }
        u.set(protocol, host, port, file, ref);
    }

    /**
     * Sets the fields of the URL {@code u} to the values of the supplied
     * arguments.
     *
     * @param u
     *            the non-null URL object to be set.
     * @param protocol
     *            the protocol.
     * @param host
     *            the host name.
     * @param port
     *            the port number.
     * @param authority
     *            the authority.
     * @param userInfo
     *            the user info.
     * @param file
     *            the file component.
     * @param query
     *            the query.
     * @param ref
     *            the reference.
     */
    protected void setURL(URL u, String protocol, String host, int port,
            String authority, String userInfo, String file, String query,
            String ref) {
        if (this != u.strmHandler) {
            throw new SecurityException();
        }
        u.set(protocol, host, port, authority, userInfo, file, query, ref);
    }

    /**
     * Returns the clear text representation of a given URL using HTTP format.
     *
     * @param url
     *            the URL object to be converted.
     * @return the clear text representation of the specified URL.
     * @see #parseURL
     * @see URL#toExternalForm()
     */
    protected String toExternalForm(URL url) {
        return toExternalForm(url, false);
    }

    String toExternalForm(URL url, boolean escapeIllegalCharacters) {
        StringBuilder result = new StringBuilder();
        result.append(url.getProtocol());
        result.append(':');

        String authority = url.getAuthority();
        if (authority != null && !authority.isEmpty()) {
            result.append("//");
            if (escapeIllegalCharacters) {
                authority = fixEncoding(authority, "$,;@&=+:[]");
            }
            result.append(authority);
        }

        String fileAndQuery = url.getFile();
        if (fileAndQuery != null) {
            if (escapeIllegalCharacters) {
                fileAndQuery = fixEncoding(fileAndQuery, "$,;@&=+:/?");
            }
            result.append(fileAndQuery);
        }

        String ref = url.getRef();
        if (ref != null) {
            result.append('#');
            if (escapeIllegalCharacters) {
                ref = fixEncoding(ref, "$,;@&=+:/?[]");
            }
            result.append(ref);
        }

        return result.toString();
    }

    /**
     * Escapes the unescaped characters of {@code s} that are not permitted.
     * Permitted characters are:
     * <ul>
     *   <li>Unreserved characters in RFC 2396.
     *   <li>{@code extraOkayChars},
     *   <li>non-ASCII, non-control, non-whitespace characters
     * </ul>
     *
     * <p>Unlike the methods in {@code URI}, this method ignores input that has
     * already been escaped. For example, input of "hello%20world" is unchanged
     * by this method but would be double-escaped to "hello%2520world" by URI.
     *
     * <p>UTF-8 is used to encode escaped characters. A single input character
     * like "\u0080" may be encoded to multiple octets like %C2%80.
     */
    private String fixEncoding(String s, String extraPermittedChars) {
        StringBuilder result = null;
        int copiedCount = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '%') {
                i += 2; // this is a 3-character sequence like "%20"
                continue;
            }

            // unreserved characters: alphanum | mark
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '-' || c == '_' || c == '.' || c == '!' || c == '~'
                    || c == '*' || c == '\'' || c == '(' || c == ')') {
                continue;
            }

            // characters permitted in this context
            if (extraPermittedChars.indexOf(c) != -1) {
                continue;
            }

            // other characters
            if (c > 0x7f && !Character.isISOControl(c) && !Character.isSpaceChar(c)) {
                continue;
            }

            /*
             * We've encountered a character that must be escaped.
             */
            if (result == null) {
                result = new StringBuilder();
            }
            result.append(s, copiedCount, i);

            if (c < 0x7f) {
                URIEncoderDecoder.appendHex(result, (byte) c);
            } else {
                for (byte b : s.substring(i, i + 1).getBytes(Charsets.UTF_8)) {
                    URIEncoderDecoder.appendHex(result, b);
                }
            }

            copiedCount = i + 1;
        }

        if (result == null) {
            return s;
        } else {
            result.append(s, copiedCount, s.length());
            return result.toString();
        }
    }

    /**
     * Returns true if {@code a} and {@code b} have the same protocol, host,
     * port, file, and reference.
     */
    protected boolean equals(URL a, URL b) {
        return sameFile(a, b)
                && Objects.equal(a.getRef(), b.getRef())
                && Objects.equal(a.getQuery(), b.getQuery());
    }

    /**
     * Returns the default port of the protocol used by the handled URL. The
     * default implementation always returns {@code -1}.
     */
    protected int getDefaultPort() {
        return -1;
    }

    /**
     * Returns the host address of {@code url}.
     */
    protected InetAddress getHostAddress(URL url) {
        try {
            String host = url.getHost();
            if (host == null || host.length() == 0) {
                return null;
            }
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Returns the hash code of {@code url}.
     */
    protected int hashCode(URL url) {
        return toExternalForm(url).hashCode();
    }

    /**
     * Returns true if the hosts of {@code a} and {@code b} are equal.
     */
    protected boolean hostsEqual(URL a, URL b) {
        // URLs with the same case-insensitive host name have equal hosts
        String aHost = getHost(a);
        String bHost = getHost(b);
        return aHost != null && aHost.equalsIgnoreCase(bHost);
    }

    /**
     * Returns true if {@code a} and {@code b} have the same protocol, host,
     * port and file.
     */
    protected boolean sameFile(URL a, URL b) {
        return Objects.equal(a.getProtocol(), b.getProtocol())
                && hostsEqual(a, b)
                && a.getEffectivePort() == b.getEffectivePort()
                && Objects.equal(a.getFile(), b.getFile());
    }

    private static String getHost(URL url) {
        String host = url.getHost();
        if ("file".equals(url.getProtocol()) && host.isEmpty()) {
            host = "localhost";
        }
        return host;
    }
}
