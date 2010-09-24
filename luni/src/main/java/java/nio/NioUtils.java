/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.nio;

import java.nio.channels.FileChannel;
import org.apache.harmony.luni.platform.IFileSystem;

/**
 * @hide internal use only
 */
public final class NioUtils {
    private NioUtils() {
    }

    /**
     * Gets the start address of a direct buffer.
     * <p>
     * This method corresponds to the JNI function:
     *
     * <pre>
     *    void* GetDirectBufferAddress(JNIEnv* env, jobject buf);
     * </pre>
     *
     * @param buf
     *            the direct buffer whose address shall be returned must not be
     *            <code>null</code>.
     * @return the address of the buffer given, or zero if the buffer is not a
     *         direct Buffer.
     */
    public static int getDirectBufferAddress(Buffer buffer) {
        return buffer.effectiveDirectAddress;
    }

    public static void freeDirectBuffer(ByteBuffer buffer) {
        if (buffer == null) {
            return;
        }
        if (buffer instanceof DirectByteBuffer) {
            ((DirectByteBuffer) buffer).free();
        } else if (buffer instanceof MappedByteBuffer) {
            ((MappedByteBufferAdapter) buffer).free();
        } else {
            throw new AssertionError();
        }
    }

    /**
     * Returns the int file descriptor from within the given FileChannel 'fc'.
     */
    public static int getFd(FileChannel fc) {
        return ((FileChannelImpl) fc).getHandle();
    }

    /**
     * Helps bridge between io and nio.
     */
    public static FileChannel newFileChannel(Object stream, int fd, int mode) {
        switch (mode) {
        case IFileSystem.O_RDONLY:
            return new ReadOnlyFileChannel(stream, fd);
        case IFileSystem.O_WRONLY:
            return new WriteOnlyFileChannel(stream, fd);
        case IFileSystem.O_RDWR:
            return new ReadWriteFileChannel(stream, fd);
        case IFileSystem.O_RDWRSYNC:
            return new ReadWriteFileChannel(stream, fd);
        case IFileSystem.O_APPEND:
            return new WriteOnlyFileChannel(stream, fd, true);
        default:
            throw new RuntimeException("Unknown mode: " + mode);
        }
    }
}
