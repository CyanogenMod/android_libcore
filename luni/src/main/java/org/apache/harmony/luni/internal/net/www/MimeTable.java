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

package org.apache.harmony.luni.internal.net.www;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

/**
 * Instances of this class map file extensions to MIME content types based on a
 * default MIME table.
 *
 * The default values can be overridden by modifying the contents of the file
 * "content-types.properties".
 */
public class MimeTable implements FileNameMap {

    public static final String UNKNOWN = "content/unknown";

    /**
     * A hash table containing the mapping between extensions and mime types.
     */
    public static final Properties types = new Properties();

    // Default mapping.
    static {
        // Copied from the tables in Chrome's "net/base/mime_util.cc".
        // The order is the same as the original, for ease of comparison.
        types.setProperty("html", "text/html");
        types.setProperty("htm", "text/html");
        types.setProperty("css", "text/css");
        types.setProperty("xml", "text/xml");
        types.setProperty("gif", "image/gif");
        types.setProperty("jpeg", "image/jpeg");
        types.setProperty("jpg", "image/jpeg");
        types.setProperty("png", "image/png");
        types.setProperty("mp4", "video/mp4");
        types.setProperty("m4v", "video/mp4");
        types.setProperty("m4a", "audio/x-m4a");
        types.setProperty("mp3", "audio/mp3");
        types.setProperty("ogv", "video/ogg");
        types.setProperty("ogm", "video/ogg");
        types.setProperty("ogg", "audio/ogg");
        types.setProperty("oga", "audio/ogg");
        types.setProperty("xhtml", "application/xhtml+xml");
        types.setProperty("xht", "application/xhtml+xml");
        // { "application/x-chrome-extension", "crx" }
        types.setProperty("exe", "application/octet-stream");
        types.setProperty("com", "application/octet-stream");
        types.setProperty("bin", "application/octet-stream");
        types.setProperty("gz", "application/gzip");
        types.setProperty("pdf", "application/pdf");
        types.setProperty("ps", "application/postscript");
        types.setProperty("eps", "application/postscript");
        types.setProperty("ai", "application/postscript");
        types.setProperty("js", "application/x-javascript");
        types.setProperty("bmp", "image/bmp");
        types.setProperty("ico", "image/x-icon");
        types.setProperty("jfif", "image/jpeg");
        types.setProperty("pjpeg", "image/jpeg");
        types.setProperty("pjp", "image/jpeg");
        types.setProperty("tiff", "image/tiff");
        types.setProperty("tif", "image/tiff");
        types.setProperty("xbm", "image/x-xbitmap");
        types.setProperty("svg", "image/svg+xml");
        types.setProperty("svgz", "image/svg+xml");
        types.setProperty("eml", "message/rfc822");
        types.setProperty("txt", "text/plain");
        types.setProperty("text", "text/plain");
        types.setProperty("shtml", "text/html");
        types.setProperty("ehtml", "text/html");
        types.setProperty("rss", "application/rss+xml");
        types.setProperty("rdf", "application/rdf+xml");
        types.setProperty("xsl", "text/xml");
        types.setProperty("xbl", "text/xml");
        types.setProperty("xul", "application/vnd.mozilla.xul+xml");
        types.setProperty("swf", "application/x-shockwave-flash");
        types.setProperty("swl", "application/x-shockwave-flash");
    }

    /**
     * Constructs a MIME table using the default values defined in this class.
     *
     * It then augments this table by reading pairs of extensions and
     * corresponding content types from the file "content-types.properties",
     * which is represented in standard java.util.Properties.load(...) format.
     */
    public MimeTable() {
        InputStream str = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
            public InputStream run() {
                return getContentTypes();
            }
        });

        if (str != null) {
            try {
                try {
                    types.load(str);
                } finally {
                    str.close();
                }
            } catch (IOException ex) {
                // Ignore
            }
        }
    }

    private InputStream getContentTypes() {
        // User override?
        String userTable = System.getProperty("content.types.user.table");
        if (userTable != null) {
            try {
                return new FileInputStream(userTable);
            } catch (IOException e) {
                // Ignore invalid values
            }
        }

        // Standard location?
        String javahome = System.getProperty("java.home");
        File contentFile = new File(javahome, "lib"
                + File.separator + "content-types.properties");
        try {
            return new FileInputStream(contentFile);
        } catch (IOException e) {
            // Not found or can't read
        }

        return null;
    }

    /**
     * Determines the MIME type for the given filename.
     *
     * @param filename
     *            The file whose extension will be mapped.
     *
     * @return The mime type associated with the file's extension or null if no
     *         mapping is known.
     */
    public String getContentTypeFor(String filename) {
        if (filename.endsWith("/")) {
            // a directory, return html
            return (String) types.get("html");
        }
        int lastCharInExtension = filename.lastIndexOf('#');
        if (lastCharInExtension < 0) {
            lastCharInExtension = filename.length();
        }
        int firstCharInExtension = filename.lastIndexOf('.') + 1;
        String ext = "";
        if (firstCharInExtension > filename.lastIndexOf('/')) {
            ext = filename.substring(firstCharInExtension, lastCharInExtension);
        }
        return types.getProperty(ext.toLowerCase());
    }
}
