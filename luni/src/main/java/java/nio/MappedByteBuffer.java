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

import java.nio.channels.FileChannel.MapMode;
import org.apache.harmony.luni.platform.OSMemory;

/**
 * {@code MappedByteBuffer} is a special kind of direct byte buffer which maps a
 * region of file to memory.
 * <p>
 * {@code MappedByteBuffer} can be created by calling
 * {@link java.nio.channels.FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long) FileChannel.map}.
 * Once created, the mapping between the byte buffer and the file region remains
 * valid until the byte buffer is garbage collected.
 * <p>
 * All or part of a {@code MappedByteBuffer}'s content may change or become
 * inaccessible at any time, since the mapped file region can be modified by
 * another thread or process at any time. If this happens, the behavior of the
 * {@code MappedByteBuffer} is undefined.
 */
public abstract class MappedByteBuffer extends ByteBuffer {

    final DirectByteBuffer wrapped;

    private final MapMode mapMode;

    MappedByteBuffer(ByteBuffer directBuffer) {
        super(directBuffer.capacity, directBuffer.block);
        if (!directBuffer.isDirect()) {
            throw new IllegalArgumentException();
        }
        this.wrapped = (DirectByteBuffer) directBuffer;
        this.mapMode = null;
    }

    MappedByteBuffer(MemoryBlock block, int capacity, int offset, MapMode mapMode) {
        super(capacity, block);
        this.mapMode = mapMode;
        if (mapMode == MapMode.READ_ONLY) {
            wrapped = new ReadOnlyDirectByteBuffer(block, capacity, offset);
        } else {
            wrapped = new ReadWriteDirectByteBuffer(block, capacity, offset);
        }
    }

    /**
     * Indicates whether this buffer's content is loaded. If the result is true
     * there is a high probability that the whole buffer memory is currently
     * loaded in RAM. If it is false it is unsure if it is loaded or not.
     *
     * @return {@code true} if this buffer's content is loaded, {@code false}
     *         otherwise.
     */
    public final boolean isLoaded() {
        return OSMemory.isLoaded(block.toInt(), block.getSize());
    }

    /**
     * Loads this buffer's content into memory but it is not guaranteed to
     * succeed.
     *
     * @return this buffer.
     */
    public final MappedByteBuffer load() {
        OSMemory.load(block.toInt(), block.getSize());
        return this;
    }

    /**
     * Writes all changes of the buffer to the mapped file. If the mapped file
     * is stored on a local device, it is guaranteed that the changes are
     * written to the file. No such guarantee is given if the file is located on
     * a remote device.
     *
     * @return this buffer.
     */
    public final MappedByteBuffer force() {
        if (mapMode == MapMode.READ_WRITE) {
            OSMemory.msync(block.toInt(), block.getSize());
        }
        return this;
    }
}
