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

import java.io.IOException;
import java.io.InputStream;
import java.net.CacheRequest;

/**
 * An HTTP body with alternating chunk sizes and chunk bodies.
 */
final class ChunkedInputStream extends AbstractHttpInputStream {
    private static final int NO_CHUNK_YET = -1;
    private int bytesRemainingInChunk = NO_CHUNK_YET;
    private boolean noMoreChunks;

    ChunkedInputStream(InputStream is, CacheRequest cacheRequest,
            HttpURLConnectionImpl httpURLConnection) throws IOException {
        super(is, httpURLConnection, cacheRequest);
    }

    @Override public int read(byte[] buffer, int offset, int length) throws IOException {
        checkBounds(buffer, offset, length);
        checkNotClosed();

        if (noMoreChunks) {
            return -1;
        }
        if (bytesRemainingInChunk == 0 || bytesRemainingInChunk == NO_CHUNK_YET) {
            readChunkSize();
            if (noMoreChunks) {
                endOfInput(false);
                return -1;
            }
        }
        int count = in.read(buffer, offset, Math.min(length, bytesRemainingInChunk));
        if (count == -1) {
            unexpectedEndOfInput(); // the server didn't supply the promised chunk length
            throw new IOException("unexpected end of stream");
        }
        bytesRemainingInChunk -= count;
        cacheWrite(buffer, offset, count);
        return count;
    }

    private void readChunkSize() throws IOException {
        if (bytesRemainingInChunk == 0) {
            /*
             * Read the suffix of the previous chunk. We defer reading this
             * at the end of that chunk to avoid unnecessary blocking.
             */
            HttpURLConnectionImpl.readLine(in);
        }
        String chunkSizeString = HttpURLConnectionImpl.readLine(in);
        int index = chunkSizeString.indexOf(";");
        if (index != -1) {
            chunkSizeString = chunkSizeString.substring(0, index);
        }
        try {
            bytesRemainingInChunk = Integer.parseInt(chunkSizeString.trim(), 16);
        } catch (NumberFormatException e) {
            throw new IOException("Expected a hex chunk size, but was " + chunkSizeString);
        }
        if (bytesRemainingInChunk == 0) {
            noMoreChunks = true;
            httpURLConnection.readHeaders(); // actually trailers!
        }
    }

    @Override public int available() throws IOException {
        checkNotClosed();
        return noMoreChunks ? 0 : Math.min(in.available(), bytesRemainingInChunk);
    }

    @Override public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;
        if (!noMoreChunks) {
            unexpectedEndOfInput();
        }
    }
}
