/*
 * Copyright (C) 2015 The Android Open Source Project
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

package libcore.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.jar.JarFile;
import java.util.jar.StrictJarFile;
import java.util.zip.ZipEntry;
import sun.net.www.protocol.jar.Handler;

/**
 * A {@link URLStreamHandler} for a specific class path {@link JarFile}. This class avoids the need
 * to open a jar file multiple times to read resources if the jar file can be held open. The
 * {@link URLConnection} objects created are a subclass of {@link JarURLConnection}.
 *
 * <p>Use {@link #getEntryUrlOrNull(String)} to obtain a URL backed by this stream handler.
 */
public class ClassPathURLStreamHandler extends Handler {
  private final String fileUri;
  private final StrictJarFile strictJarFile;
  /**
   * Created on demand if somebody calls {@link JarURLConnection#getJarFile()} and
   * {@link URLConnection#getUseCaches()} is true.
   */
  private JarFile jarFile;

  public ClassPathURLStreamHandler(String jarFileName) throws IOException {
    // We use StrictJarFile because it is much less heap memory hungry than ZipFile / JarFile.
    strictJarFile = new StrictJarFile(jarFileName);

    // File.toURI() is compliant with RFC 1738 in always creating absolute path names. If we
    // construct the URL by concatenating strings, we might end up with illegal URLs for relative
    // names.
    this.fileUri = new File(jarFileName).toURI().toString();
  }

  /**
   * Obtains a cached {@link JarFile} that points to the same jar file as {@link #strictJarFile}.
   */
  JarFile getSharedJarFile() throws IOException {
    synchronized (this) {
      if (jarFile == null) {
        jarFile = new JarFile(strictJarFile.getName());
      }
    }
    return jarFile;
  }

  /**
   * Returns a URL backed by this stream handler for the named resource, or {@code null} if the
   * entry cannot be found under the exact name presented.
   */
  public URL getEntryUrlOrNull(String entryName) {
    if (findEntryWithDirectoryFallback(strictJarFile, entryName) != null) {
      try {
        // We rely on the URL/the stream handler to deal with any url encoding necessary here, and
        // we assume it is completely reversible.
        return new URL("jar", null, -1, fileUri + "!/" + entryName, this);
      } catch (MalformedURLException e) {
        throw new RuntimeException("Invalid entry name", e);
      }
    }
    return null;
  }

  /**
   * Returns true if an entry with the specified name exists and is stored (not compressed),
   * and false otherwise.
   */
  public boolean isEntryStored(String entryName) {
    ZipEntry entry = strictJarFile.findEntry(entryName);
    return entry != null && entry.getMethod() == ZipEntry.STORED;
  }

  @Override
  protected URLConnection openConnection(URL url) throws IOException {
    return new ClassPathURLConnection(url);
  }

  /** Used from tests to indicate this stream handler is finished with. */
  public void close() throws IOException {
    strictJarFile.close();
    if (jarFile != null) {
      jarFile.close();
    }
  }

  /**
   * Finds an entry with the specified name in the {@code jarFile}. If an exact match isn't found it
   * will also try with "/" appended, if appropriate. This is to maintain compatibility with
   * {@link sun.net.www.protocol.jar.Handler} and its treatment of directory entries.
   */
  static ZipEntry findEntryWithDirectoryFallback(StrictJarFile jarFile, String entryName) {
    ZipEntry entry = jarFile.findEntry(entryName);
    if (entry == null && !entryName.endsWith("/") ) {
      entry = jarFile.findEntry(entryName + "/");
    }
    return entry;
  }

  private class ClassPathURLConnection extends JarURLConnection {
    // The StrictJarFile instance is shared across URLConnections and must not be closed.
    private final StrictJarFile strictJarFile;

    private ZipEntry jarEntry;
    private InputStream jarInput;
    private boolean closed;

    /**
     * Indicates the behavior of the {@link #jarFile}. If true, the reference is shared and should
     * not be closed. If false, it must be closed.
     */
    private boolean jarFileMustBehaveAsCached;
    private JarFile jarFile;

    public ClassPathURLConnection(URL url) throws MalformedURLException {
      super(url);
      this.strictJarFile = ClassPathURLStreamHandler.this.strictJarFile;
    }

    @Override
    public void connect() throws IOException {
      if (!connected) {
        this.jarEntry = findEntryWithDirectoryFallback(strictJarFile, getEntryName());
        if (jarEntry == null) {
          throw new FileNotFoundException(
              "URL does not correspond to an entry in the zip file. URL=" + url
              + ", zipfile=" + strictJarFile.getName());
        }
        jarFileMustBehaveAsCached = getUseCaches();
        connected = true;
      }
    }

    @Override
    public JarFile getJarFile() throws IOException {
      connect();

      // This is more expensive than JarURLConnectionImpl because we only pretend that we wrap a
      // JarFile. We do cache in the surrounding class if jarFileMustBehaveAsCached is true to
      // preserve garbage collection semantics to avoid leak warnings.
      String jarFileName = strictJarFile.getName();
      if (jarFileMustBehaveAsCached) {
        jarFile = ClassPathURLStreamHandler.this.getSharedJarFile();
      } else {
        jarFile = new JarFile(jarFileName);
      }
      return jarFile;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      if (closed) {
        throw new IllegalStateException("JarURLConnection InputStream has been closed");
      }
      connect();
      if (jarInput != null) {
        return jarInput;
      }
      return jarInput = new FilterInputStream(strictJarFile.getInputStream(jarEntry)) {
        @Override
        public void close() throws IOException {
          super.close();
          // If the jar file is not cached closing the input stream will close the URLConnection and
          // any JarFile returned from getJarFile().
          if (jarFile != null && !jarFileMustBehaveAsCached) {
            jarFile.close();
            closed = true;
          }
        }
      };
    }

    /**
     * Returns the content type of the entry based on the name of the entry. Returns
     * non-null results ("content/unknown" for unknown types).
     *
     * @return the content type
     */
    @Override
    public String getContentType() {
      String cType = guessContentTypeFromName(getEntryName());
      if (cType == null) {
        cType = "content/unknown";
      }
      return cType;
    }

    @Override
    public int getContentLength() {
      try {
        connect();
        return (int) getJarEntry().getSize();
      } catch (IOException e) {
        // Ignored
      }
      return -1;
    }
  }
}
