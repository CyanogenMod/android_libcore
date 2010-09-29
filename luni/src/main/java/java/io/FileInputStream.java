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

package java.io;

import java.nio.NioUtils;
import java.nio.channels.FileChannel;
import libcore.io.IoUtils;
import org.apache.harmony.luni.platform.IFileSystem;
import org.apache.harmony.luni.platform.Platform;

/**
 * An input stream that reads bytes from a file.
 * <pre>   {@code
 *   File file = ...
 *   InputStream in = null;
 *   try {
 *     in = new BufferedInputStream(new FileInputStream(file));
 *     ...
 *   } finally {
 *     if (in != null) {
 *       in.close();
 *     }
 *   }
 * }</pre>
 *
 * <p>This stream is <strong>not buffered</strong>. Most callers should wrap
 * this stream with a {@link BufferedInputStream}.
 *
 * <p>Use {@link FileReader} to read characters, as opposed to bytes, from a
 * file.
 *
 * @see BufferedInputStream
 * @see FileOutputStream
 */
public class FileInputStream extends InputStream implements Closeable {

    private final FileDescriptor fd;

    /** The unique file channel. Lazily initialized because it's rarely needed. */
    private FileChannel channel;

    private final boolean shouldCloseFd;

    private final Object repositioningLock = new Object();

    /**
     * Constructs a new {@code FileInputStream} that reads from {@code file}.
     *
     * @param file
     *            the file from which this stream reads.
     * @throws FileNotFoundException
     *             if {@code file} does not exist.
     * @throws SecurityException
     *             if a {@code SecurityManager} is installed and it denies the
     *             read request.
     */
    public FileInputStream(File file) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException("file == null");
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkRead(file.getPath());
        }
        fd = new FileDescriptor();
        fd.readOnly = true;
        fd.descriptor = Platform.FILE_SYSTEM.open(file.getAbsolutePath(), IFileSystem.O_RDONLY);
        shouldCloseFd = true;
    }

    /**
     * Constructs a new {@code FileInputStream} that reads from {@code fd}.
     *
     * @param fd
     *            the FileDescriptor from which this stream reads.
     * @throws NullPointerException
     *             if {@code fd} is {@code null}.
     * @throws SecurityException
     *             if a {@code SecurityManager} is installed and it denies the
     *             read request.
     */
    public FileInputStream(FileDescriptor fd) {
        if (fd == null) {
            throw new NullPointerException("fd == null");
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkRead(fd);
        }
        this.fd = fd;
        this.shouldCloseFd = false;
    }

    /**
     * Equivalent to {@code new FileInputStream(new File(path))}.
     */
    public FileInputStream(String path) throws FileNotFoundException {
        this(new File(path));
    }

    @Override
    public int available() throws IOException {
        checkOpen();
        return Platform.FILE_SYSTEM.ioctlAvailable(fd);
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            if (channel != null) {
                channel.close();
            }
            if (shouldCloseFd && fd.valid()) {
                IoUtils.close(fd);
            }
        }
    }

    /**
     * Ensures that all resources for this stream are released when it is about
     * to be garbage collected.
     *
     * @throws IOException
     *             if an error occurs attempting to finalize this stream.
     */
    @Override protected void finalize() throws IOException {
        try {
            close();
        } finally {
            try {
                super.finalize();
            } catch (Throwable t) {
                // for consistency with the RI, we must override Object.finalize() to
                // remove the 'throws Throwable' clause.
                throw new AssertionError(t);
            }
        }
    }

    /**
     * Returns a read-only {@link FileChannel} that shares its position with
     * this stream.
     */
    public FileChannel getChannel() {
        synchronized (this) {
            if (channel == null) {
                channel = NioUtils.newFileChannel(this, fd.descriptor, IFileSystem.O_RDONLY);
            }
            return channel;
        }
    }

    /**
     * Returns the underlying file descriptor.
     */
    public final FileDescriptor getFD() throws IOException {
        return fd;
    }

    @Override
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        int result = read(buffer, 0, 1);
        return result == -1 ? -1 : buffer[0] & 0xff;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        if (buffer == null) {
            throw new NullPointerException("buffer == null");
        }
        if ((count | offset) < 0 || count > buffer.length - offset) {
            throw new IndexOutOfBoundsException();
        }
        if (count == 0) {
            return 0;
        }
        checkOpen();
        synchronized (repositioningLock) {
            return (int) Platform.FILE_SYSTEM.read(fd.descriptor, buffer, offset, count);
        }
    }

    @Override
    public long skip(long byteCount) throws IOException {
        checkOpen();

        if (byteCount == 0) {
            return 0;
        }
        if (byteCount < 0) {
            throw new IOException("byteCount < 0");
        }

        // The RI doesn't treat stdin as special. It throws IOException for
        // all non-seekable streams, so we do too. If you did want to support
        // non-seekable streams, the best way to do it would be to recognize
        // when lseek(2) fails with ESPIPE and call super.skip(byteCount).
        synchronized (repositioningLock) {
            // Our seek returns the new offset, but we know it will throw an
            // exception if it couldn't perform exactly the seek we asked for.
            Platform.FILE_SYSTEM.seek(fd.descriptor, byteCount, IFileSystem.SEEK_CUR);
            return byteCount;
        }
    }

    private synchronized void checkOpen() throws IOException {
        if (!fd.valid()) {
            throw new IOException("stream is closed");
        }
    }
}
