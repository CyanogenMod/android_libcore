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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public final class Streams {
    private static AtomicReference<byte[]> skipBuffer = new AtomicReference<byte[]>();

    private Streams() {}

    /**
     * Implements {@link java.io.DataInputStream#readFully(byte[], int, int)}.
     */
    public static void readFully(InputStream in, byte[] dst, int offset, int byteCount) throws IOException {
        if (byteCount == 0) {
            return;
        }
        if (in == null) {
            throw new NullPointerException("in == null");
        }
        if (dst == null) {
            throw new NullPointerException("dst == null");
        }
        Arrays.checkOffsetAndCount(dst.length, offset, byteCount);
        while (byteCount > 0) {
            int bytesRead = in.read(dst, offset, byteCount);
            if (bytesRead < 0) {
                throw new EOFException();
            }
            offset += bytesRead;
            byteCount -= bytesRead;
        }
    }

    /**
     * Returns a new byte[] containing the entire contents of the given InputStream.
     * Useful when you don't know in advance how much data there is to be read.
     */
    public static byte[] readFully(InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        while (true) {
            int byteCount = in.read(buffer);
            if (byteCount == -1) {
                return bytes.toByteArray();
            }
            bytes.write(buffer, 0, byteCount);
        }
    }

    public static void skipAll(InputStream in) throws IOException {
        do {
            in.skip(Long.MAX_VALUE);
        } while (in.read() != -1);
    }

    /**
     * Call {@code in.read()} repeatedly until either the stream is exhausted or
     * {@code byteCount} bytes have been read.
     *
     * <p>This method reuses the skip buffer but is careful to never use it at
     * the same time that another stream is using it. Otherwise streams that use
     * the caller's buffer for consistency checks like CRC could be clobbered by
     * other threads. A thread-local buffer is also insufficient because some
     * streams may call other streams in their skip() method, also clobbering the
     * buffer.
     */
    public static long skipByReading(InputStream in, long byteCount) throws IOException {
        // acquire the shared skip buffer.
        byte[] buffer = skipBuffer.getAndSet(null);
        if (buffer == null) {
            buffer = new byte[4096];
        }

        long skipped = 0;
        while (skipped < byteCount) {
            int toRead = (int) Math.min(byteCount - skipped, buffer.length);
            int read = in.read(buffer, 0, toRead);
            if (read == -1) {
                break;
            }
            skipped += read;
            if (read < toRead) {
                break;
            }
        }

        // release the shared skip buffer.
        skipBuffer.set(buffer);

        return skipped;
    }

    /**
     * Copies all of the bytes from {@code in} to {@code out}. Neither stream is closed.
     * Returns the total number of bytes transferred.
     */
    public static int copy(InputStream in, OutputStream out) throws IOException {
        int total = 0;
        byte[] buffer = new byte[8192];
        int c;
        while ((c = in.read(buffer)) != -1) {
            total += c;
            out.write(buffer, 0, c);
        }
        return total;
    }
}
