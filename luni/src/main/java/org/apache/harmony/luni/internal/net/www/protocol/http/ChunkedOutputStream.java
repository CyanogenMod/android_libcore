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

package org.apache.harmony.luni.internal.net.www.protocol.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An HTTP body with alternating chunk sizes and chunk bodies. Chunks are
 * buffered until {@code maxChunkLength} bytes are ready, at which point the
 * chunk is written and the buffer is cleared.
 */
final class ChunkedOutputStream extends AbstractHttpOutputStream {
    private static final byte[] CRLF = new byte[] { '\r', '\n' };
    private static final byte[] HEX_DIGITS = new byte[] {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    private static final byte[] FINAL_CHUNK = new byte[] { '0', '\r', '\n', '\r', '\n' };

    private final OutputStream socketOut;
    private final int maxChunkLength;
    private final ByteArrayOutputStream currentChunk;
    private byte[] hex = new byte[8];

    public ChunkedOutputStream(OutputStream socketOut, int maxChunkLength) {
        this.socketOut = socketOut;
        this.maxChunkLength = Math.max(1, dataLength(maxChunkLength));
        this.currentChunk = new ByteArrayOutputStream(maxChunkLength);
    }

    /**
     * Returns the amount of data that can be transmitted in a chunk whose total
     * length (data+headers) is {@code dataPlusHeaderLength}. This is presumably
     * useful to match sizes with wire-protocol packets.
     */
    private int dataLength(int dataPlusHeaderLength) {
        int headerLength = 4; // "\r\n" after the size plus another "\r\n" after the data
        for (int i = dataPlusHeaderLength - headerLength; i > 0; i >>= 4) {
            headerLength++;
        }
        return dataPlusHeaderLength - headerLength;
    }

    @Override public synchronized void write(byte[] buffer, int offset, int count)
            throws IOException {
        checkNotClosed();
        checkBounds(buffer, offset, count);

        while (count > 0) {
            int numBytesWritten;

            // write to the current chunk and then maybe write that to the stream
            if (currentChunk.size() > 0 || count < maxChunkLength) {
                numBytesWritten = Math.min(count, maxChunkLength - currentChunk.size());
                currentChunk.write(buffer, offset, numBytesWritten);
                if (currentChunk.size() == maxChunkLength) {
                    writeCurrentChunkToSocket();
                }

            // write a single chunk of size maxChunkLength to the stream
            } else {
                numBytesWritten = maxChunkLength;
                writeHex(numBytesWritten);
                socketOut.write(CRLF);
                socketOut.write(buffer, offset, numBytesWritten);
                socketOut.write(CRLF);
            }

            offset += numBytesWritten;
            count -= numBytesWritten;
        }
    }

    /**
     * Equivalent to, but cheaper than, Integer.toHexString().getBytes().
     */
    private void writeHex(int i) throws IOException {
        int cursor = 8;
        do {
            hex[--cursor] = HEX_DIGITS[i & 0xf];
        } while ((i >>>= 4) != 0);
        socketOut.write(hex, cursor, 8 - cursor);
    }

    @Override public synchronized void flush() throws IOException {
        checkNotClosed();
        writeCurrentChunkToSocket();
        socketOut.flush();
    }

    @Override public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        writeCurrentChunkToSocket();
        socketOut.write(FINAL_CHUNK);
    }

    private void writeCurrentChunkToSocket() throws IOException {
        int size = currentChunk.size();
        if (size <= 0) {
            return;
        }

        writeHex(size);
        socketOut.write(CRLF);
        currentChunk.writeTo(socketOut);
        currentChunk.reset();
        socketOut.write(CRLF);
    }
}
