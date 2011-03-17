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

import dalvik.system.CloseGuard;
import java.nio.NioUtils;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import libcore.io.IoUtils;
import static libcore.io.OsConstants.*;
import org.apache.harmony.luni.platform.Platform;

/**
 * An output stream that writes bytes to a file. If the output file exists, it
 * can be replaced or appended to. If it does not exist, a new file will be
 * created.
 * <pre>   {@code
 *   File file = ...
 *   OutputStream out = null;
 *   try {
 *     out = new BufferedOutputStream(new FileOutputStream(file));
 *     ...
 *   } finally {
 *     if (out != null) {
 *       out.close();
 *     }
 *   }
 * }</pre>
 *
 * <p>This stream is <strong>not buffered</strong>. Most callers should wrap
 * this stream with a {@link BufferedOutputStream}.
 *
 * <p>Use {@link FileWriter} to write characters, as opposed to bytes, to a file.
 *
 * @see BufferedOutputStream
 * @see FileInputStream
 */
public class FileOutputStream extends OutputStream implements Closeable {

    private final FileDescriptor fd;

    private final boolean shouldCloseFd;

    /** The unique file channel. Lazily initialized because it's rarely needed. */
    private FileChannel channel;

    /** File access mode */
    private final int mode;

    private final CloseGuard guard = CloseGuard.get();

    /**
     * Constructs a new {@code FileOutputStream} that writes to {@code file}.
     *
     * @param file the file to which this stream writes.
     * @throws FileNotFoundException if file cannot be opened for writing.
     */
    public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }

    /**
     * Constructs a new {@code FileOutputStream} that writes to {@code file},
     * creating it if necessary. If {@code append} is true and the file already
     * exists, it will be appended to. Otherwise a new file will be created.
     *
     * @param file the file to which this stream writes.
     * @param append true to append to an existing file.
     * @throws FileNotFoundException if the file cannot be opened for writing.
     */
    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        this.fd = new FileDescriptor();
        this.mode = O_WRONLY | O_CREAT | (append ? O_APPEND : O_TRUNC);
        this.fd.descriptor = Platform.FILE_SYSTEM.open(file.getAbsolutePath(), mode);
        this.shouldCloseFd = true;
        this.guard.open("close");
    }

    /**
     * Constructs a new {@code FileOutputStream} that writes to {@code fd}.
     *
     * @param fd the FileDescriptor to which this stream writes.
     * @throws NullPointerException if {@code fd} is null.
     */
    public FileOutputStream(FileDescriptor fd) {
        if (fd == null) {
            throw new NullPointerException();
        }
        this.fd = fd;
        this.shouldCloseFd = false;
        this.mode = O_WRONLY;
        this.channel = NioUtils.newFileChannel(this, fd, mode);
        // Note that we do not call guard.open here because the
        // FileDescriptor is not owned by the stream.
    }

    /**
     * Equivalent to {@code new FileOutputStream(new File(path), false)}.
     */
    public FileOutputStream(String path) throws FileNotFoundException {
        this(path, false);
    }

    /**
     * Equivalent to {@code new FileOutputStream(new File(path), append)}.
     */
    public FileOutputStream(String path, boolean append) throws FileNotFoundException {
        this(new File(path), append);
    }

    @Override
    public void close() throws IOException {
        guard.close();
        synchronized (this) {
            if (channel != null) {
                channel.close();
            }
            if (shouldCloseFd && fd.valid()) {
                IoUtils.close(fd);
            }
        }
    }

    @Override protected void finalize() throws IOException {
        try {
            if (guard != null) {
                guard.warnIfOpen();
            }
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
     * Returns a write-only {@link FileChannel} that shares its position with
     * this stream.
     */
    public FileChannel getChannel() {
        synchronized (this) {
            if (channel == null) {
                channel = NioUtils.newFileChannel(this, fd, mode);
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
    public void write(byte[] buffer, int offset, int byteCount) throws IOException {
        Arrays.checkOffsetAndCount(buffer.length, offset, byteCount);
        if (byteCount == 0) {
            return;
        }
        checkOpen();
        Platform.FILE_SYSTEM.write(fd.descriptor, buffer, offset, byteCount);
    }

    @Override
    public void write(int oneByte) throws IOException {
        checkOpen();
        byte[] buffer = { (byte) oneByte };
        Platform.FILE_SYSTEM.write(fd.descriptor, buffer, 0, 1);
    }

    private synchronized void checkOpen() throws IOException {
        if (!fd.valid()) {
            throw new IOException("stream is closed");
        }
    }
}
