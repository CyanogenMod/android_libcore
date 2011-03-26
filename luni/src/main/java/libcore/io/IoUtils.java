/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import static libcore.io.OsConstants.*;

public final class IoUtils {
    private IoUtils() {
    }

    /**
     * java.io only throws FileNotFoundException when opening files, regardless of what actually
     * went wrong. Additionally, java.io is more restrictive than POSIX when it comes to opening
     * directories: POSIX says read-only is okay, but java.io doesn't even allow that. We also
     * have an Android-specific hack to alter the default permissions.
     */
    public static FileDescriptor open(String path, int flags) throws FileNotFoundException {
        FileDescriptor fd = null;
        try {
            // On Android, we don't want default permissions to allow global access.
            int mode = ((flags & O_ACCMODE) == O_RDONLY) ? 0 : 0600;
            fd = Libcore.os.open(path, flags, mode);
            if (fd.valid()) {
                // Posix open(2) fails with EISDIR only if you ask for write permission.
                // Java disallows reading directories too.
                boolean isDirectory = false;
                if (S_ISDIR(Libcore.os.fstat(fd).st_mode)) {
                    throw new ErrnoException("open", EISDIR);
                }
            }
            return fd;
        } catch (ErrnoException errnoException) {
            try {
                if (fd != null) {
                    close(fd);
                }
            } catch (IOException ignored) {
            }
            FileNotFoundException ex = new FileNotFoundException(path + ": " + errnoException.getMessage());
            ex.initCause(errnoException);
            throw ex;
        }
    }

    /**
     * java.io thinks that a read at EOF is an error and should return -1, contrary to traditional
     * Unix practice where you'd read until you got 0 bytes (and any future read would return -1).
     */
    public static int read(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws IOException {
        try {
            int readCount = Libcore.os.read(fd, bytes, byteOffset, byteCount);
            if (readCount == 0) {
                return -1;
            }
            return readCount;
        } catch (ErrnoException errnoException) {
            if (errnoException.errno == EAGAIN) {
                // We return 0 rather than throw if we try to read from an empty non-blocking pipe.
                return 0;
            }
            throw errnoException.rethrowAsIOException();
        }
    }

    /**
     * Calls close(2) on 'fd'. Also resets the internal int to -1.
     */
    public static native void close(FileDescriptor fd) throws IOException;

    /**
     * Closes 'closeable', ignoring any exceptions. Does nothing if 'closeable' is null.
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Closes 'socket', ignoring any exceptions. Does nothing if 'socket' is null.
     */
    public static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Returns the int file descriptor from within the given FileDescriptor 'fd'.
     */
    public static native int getFd(FileDescriptor fd);

    /**
     * Sets 'fd' to be blocking or non-blocking, according to the state of 'blocking'.
     */
    public static void setBlocking(FileDescriptor fd, boolean blocking) throws IOException {
        try {
            int flags = Libcore.os.fcntlVoid(fd, F_GETFL);
            if (!blocking) {
                flags |= O_NONBLOCK;
            } else {
                flags &= ~O_NONBLOCK;
            }
            Libcore.os.fcntlLong(fd, F_SETFL, flags);
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsIOException();
        }
    }

    /**
     * Returns the contents of 'path' as a byte array.
     */
    public static byte[] readFileAsByteArray(String path) throws IOException {
        RandomAccessFile f = null;
        try {
            f = new RandomAccessFile(path, "r");
            byte[] buf = new byte[(int) f.length()];
            f.readFully(buf);
            return buf;
        } finally {
            IoUtils.closeQuietly(f);
        }
    }
}
