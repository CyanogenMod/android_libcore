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
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketImpl;
import java.util.Arrays;

/**
 * The SocketInputStream supports the streamed reading of bytes from a socket.
 * Multiple streams may be opened on a socket, so care should be taken to manage
 * opened streams and coordinate read operations between threads.
 */
class SocketInputStream extends InputStream {

    private final PlainSocketImpl socket;

    /**
     * Constructs a SocketInputStream for the <code>socket</code>. Read
     * operations are forwarded to the <code>socket</code>.
     *
     * @param socket the socket to be read
     * @see Socket
     */
    public SocketInputStream(SocketImpl socket) {
        super();
        this.socket = (PlainSocketImpl) socket;
    }

    @Override
    public int available() throws IOException {
        return socket.available();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    @Override
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        int result = socket.read(buffer, 0, 1);
        return (result == -1) ? result : buffer[0] & 0xFF;
    }

    @Override
    public int read(byte[] buffer, int offset, int byteCount) throws IOException {
        if (byteCount == 0) {
            return 0;
        }
        Arrays.checkOffsetAndCount(buffer.length, offset, byteCount);
        return socket.read(buffer, offset, byteCount);
    }
}
