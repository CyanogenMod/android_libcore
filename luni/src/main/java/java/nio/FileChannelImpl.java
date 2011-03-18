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

package java.nio;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import libcore.io.ErrnoException;
import libcore.io.IoUtils;
import libcore.io.Libcore;
import org.apache.harmony.luni.platform.Platform;
import static libcore.io.OsConstants.*;

/*
 * The file channel impl class is the bridge between the logical channels
 * described by the NIO channel framework, and the file system implementation
 * provided by the port layer.
 *
 * This class is non-API, but implements the API of the FileChannel interface.
 */
final class FileChannelImpl extends FileChannel {
    private static final int ALLOC_GRANULARITY = Platform.FILE_SYSTEM.getAllocGranularity();

    private static final Comparator<FileLock> LOCK_COMPARATOR = new Comparator<FileLock>() {
        public int compare(FileLock lock1, FileLock lock2) {
            long position1 = lock1.position();
            long position2 = lock2.position();
            return position1 > position2 ? 1 : (position1 < position2 ? -1 : 0);
        }
    };

    private final Object stream;
    private final FileDescriptor fd;
    private final int mode;

    // The set of acquired and pending locks.
    private final SortedSet<FileLock> locks = new TreeSet<FileLock>(LOCK_COMPARATOR);

    private final Object repositioningLock = new Object();

    /**
     * Create a new file channel implementation class that wraps the given
     * fd and operates in the specified mode.
     */
    public FileChannelImpl(Object stream, FileDescriptor fd, int mode) {
        this.fd = fd;
        this.stream = stream;
        this.mode = mode;
    }

    /**
     * Helper method to throw an exception if the channel is already closed.
     * Note that we don't bother to synchronize on this test since the file may
     * be closed by operations beyond our control anyways.
     */
    private void checkOpen() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    private void checkReadable() {
        if ((mode & O_ACCMODE) == O_WRONLY) {
            throw new NonReadableChannelException();
        }
    }

    private void checkWritable() {
        if ((mode & O_ACCMODE) == O_RDONLY) {
            throw new NonWritableChannelException();
        }
    }

    protected void implCloseChannel() throws IOException {
        if (stream instanceof Closeable) {
            ((Closeable) stream).close();
        }
    }

    private FileLock basicLock(long position, long size, boolean shared, boolean wait) throws IOException {
        int accessMode = (mode & O_ACCMODE);
        if (accessMode == O_RDONLY) {
            if (!shared) {
                throw new NonWritableChannelException();
            }
        } else if (accessMode == O_WRONLY) {
            if (shared) {
                throw new NonReadableChannelException();
            }
        }

        if (position < 0 || size < 0) {
            throw new IllegalArgumentException("position=" + position + " size=" + size);
        }
        FileLock pendingLock = new FileLockImpl(this, position, size, shared);
        addLock(pendingLock);

        if (Platform.FILE_SYSTEM.lock(IoUtils.getFd(fd), position, size, shared, wait)) {
            return pendingLock;
        }

        // Lock acquisition failed
        removeLock(pendingLock);
        return null;
    }

    private static final class FileLockImpl extends FileLock {
        private boolean isReleased = false;

        public FileLockImpl(FileChannel channel, long position, long size, boolean shared) {
            super(channel, position, size, shared);
        }

        public boolean isValid() {
            return !isReleased && channel().isOpen();
        }

        public void release() throws IOException {
            if (!channel().isOpen()) {
                throw new ClosedChannelException();
            }
            if (!isReleased) {
                ((FileChannelImpl) channel()).release(this);
                isReleased = true;
            }
        }
    }

    public final FileLock lock(long position, long size, boolean shared) throws IOException {
        checkOpen();
        FileLock resultLock = null;
        {
            boolean completed = false;
            try {
                begin();
                resultLock = basicLock(position, size, shared, true);
                completed = true;
            } finally {
                end(completed);
            }
        }
        return resultLock;
    }

    public final FileLock tryLock(long position, long size, boolean shared) throws IOException {
        checkOpen();
        return basicLock(position, size, shared, false);
    }

    /**
     * Non-API method to release a given lock on a file channel. Assumes that
     * the lock will mark itself invalid after successful unlocking.
     */
    public void release(FileLock lock) throws IOException {
        checkOpen();
        Platform.FILE_SYSTEM.unlock(IoUtils.getFd(fd), lock.position(), lock.size());
        removeLock(lock);
    }

    public void force(boolean metadata) throws IOException {
        checkOpen();
        if ((mode & O_ACCMODE) != O_RDONLY) {
            try {
                if (metadata) {
                    Libcore.os.fsync(fd);
                } else {
                    Libcore.os.fdatasync(fd);
                }
            } catch (ErrnoException errnoException) {
                throw errnoException.rethrowAsIOException("sync failed");
            }
        }
    }

    public final MappedByteBuffer map(MapMode mapMode, long position, long size) throws IOException {
        checkOpen();
        if (mapMode == null) {
            throw new NullPointerException("mapMode == null");
        }
        if (position < 0 || size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("position=" + position + " size=" + size);
        }
        int accessMode = (mode & O_ACCMODE);
        if (accessMode == O_RDONLY) {
            if (mapMode != MapMode.READ_ONLY) {
                throw new NonWritableChannelException();
            }
        } else if (accessMode == O_WRONLY) {
            throw new NonReadableChannelException();
        }
        if (position + size > size()) {
            Platform.FILE_SYSTEM.truncate(IoUtils.getFd(fd), position + size);
        }
        long alignment = position - position % ALLOC_GRANULARITY;
        int offset = (int) (position - alignment);
        MemoryBlock block = MemoryBlock.mmap(IoUtils.getFd(fd), alignment, size + offset, mapMode);
        return new MappedByteBufferAdapter(block, (int) size, offset, mapMode);
    }

    /**
     * Returns the current file position.
     */
    public long position() throws IOException {
        checkOpen();
        if ((mode & O_APPEND) != 0) {
            return size();
        }
        return Platform.FILE_SYSTEM.seek(IoUtils.getFd(fd), 0L, SEEK_CUR);
    }

    /**
     * Sets the file pointer.
     */
    public FileChannel position(long newPosition) throws IOException {
        checkOpen();
        if (newPosition < 0) {
            throw new IllegalArgumentException("position: " + newPosition);
        }

        synchronized (repositioningLock) {
            Platform.FILE_SYSTEM.seek(IoUtils.getFd(fd), newPosition, SEEK_SET);
        }
        return this;
    }

    public int read(ByteBuffer buffer, long position) throws IOException {
        FileChannelImpl.checkWritable(buffer);
        if (position < 0) {
            throw new IllegalArgumentException("position: " + position);
        }
        checkOpen();
        checkReadable();
        if (!buffer.hasRemaining()) {
            return 0;
        }
        synchronized (repositioningLock) {
            int bytesRead = 0;
            long preReadPosition = position();
            position(position);
            try {
                bytesRead = read(buffer);
            } finally {
                position(preReadPosition);
            }
            return bytesRead;
        }
    }

    public int read(ByteBuffer buffer) throws IOException {
        FileChannelImpl.checkWritable(buffer);
        checkOpen();
        checkReadable();
        if (!buffer.hasRemaining()) {
            return 0;
        }
        boolean completed = false;
        int bytesRead = 0;
        synchronized (repositioningLock) {
            if (buffer.isDirect()) {
                try {
                    begin();
                    /*
                     * if (bytesRead <= EOF) dealt by read completed = false;
                     */
                    int address = NioUtils.getDirectBufferAddress(buffer);
                    bytesRead = (int) Platform.FILE_SYSTEM.readDirect(IoUtils.getFd(fd), address,
                            buffer.position(), buffer.remaining());
                    completed = true;
                } finally {
                    end(completed && bytesRead >= 0);
                }
            } else {
                try {
                    begin();
                    /*
                     * if (bytesRead <= EOF) dealt by read completed = false;
                     */
                    bytesRead = (int) Platform.FILE_SYSTEM.read(IoUtils.getFd(fd), buffer.array(),
                            buffer.arrayOffset() + buffer.position(), buffer.remaining());
                    completed = true;
                } finally {
                    end(completed && bytesRead >= 0);
                }
            }
            if (bytesRead > 0) {
                buffer.position(buffer.position() + bytesRead);
            }
        }
        return bytesRead;
    }

    public long read(ByteBuffer[] buffers, int offset, int length) throws IOException {
        Arrays.checkOffsetAndCount(buffers.length, offset, length);
        checkOpen();
        checkReadable();
        int count = FileChannelImpl.calculateTotalRemaining(buffers, offset, length, true);
        if (count == 0) {
            return 0;
        }
        ByteBuffer[] directBuffers = new ByteBuffer[length];
        int[] handles = new int[length];
        int[] offsets = new int[length];
        int[] lengths = new int[length];
        for (int i = 0; i < length; i++) {
            ByteBuffer buffer = buffers[i + offset];
            if (!buffer.isDirect()) {
                buffer = ByteBuffer.allocateDirect(buffer.remaining());
                directBuffers[i] = buffer;
                offsets[i] = 0;
            } else {
                offsets[i] = buffer.position();
            }
            handles[i] = NioUtils.getDirectBufferAddress(buffer);
            lengths[i] = buffer.remaining();
        }
        long bytesRead = 0;
        {
            boolean completed = false;
            try {
                begin();
                synchronized (repositioningLock) {
                    bytesRead = Platform.FILE_SYSTEM.readv(IoUtils.getFd(fd), handles, offsets, lengths, length);

                }
                completed = true;
                /*
                 * if (bytesRead < EOF) //dealt by readv? completed = false;
                 */
            } finally {
                end(completed);
            }
        }
        int end = offset + length;
        long bytesRemaining = bytesRead;
        for (int i = offset; i < end && bytesRemaining > 0; i++) {
            if (buffers[i].isDirect()) {
                if (lengths[i] < bytesRemaining) {
                    int pos = buffers[i].limit();
                    buffers[i].position(pos);
                    bytesRemaining -= lengths[i];
                } else {
                    int pos = (int) bytesRemaining;
                    buffers[i].position(pos);
                    break;
                }
            } else {
                ByteBuffer buf = directBuffers[i - offset];
                if (bytesRemaining < buf.remaining()) {
                    // this is the last step.
                    int pos = buf.position();
                    buffers[i].put(buf);
                    buffers[i].position(pos + (int) bytesRemaining);
                    bytesRemaining = 0;
                } else {
                    bytesRemaining -= buf.remaining();
                    buffers[i].put(buf);
                }
            }
        }
        return bytesRead;
    }

    /**
     * Returns the current file size, as an integer number of bytes.
     */
    public long size() throws IOException {
        checkOpen();
        try {
            return Libcore.os.fstat(fd).st_size;
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsIOException("fstat failed");
        }
    }

    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        checkOpen();
        if (!src.isOpen()) {
            throw new ClosedChannelException();
        }
        checkWritable();
        if (position < 0 || count < 0 || count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("position=" + position + " count=" + count);
        }
        if (position > size()) {
            return 0;
        }

        ByteBuffer buffer = null;

        try {
            if (src instanceof FileChannel) {
                FileChannel fileSrc = (FileChannel) src;
                long size = fileSrc.size();
                long filePosition = fileSrc.position();
                count = Math.min(count, size - filePosition);
                buffer = fileSrc.map(MapMode.READ_ONLY, filePosition, count);
                fileSrc.position(filePosition + count);
            } else {
                buffer = ByteBuffer.allocateDirect((int) count);
                src.read(buffer);
                buffer.flip();
            }
            return write(buffer, position);
        } finally {
            NioUtils.freeDirectBuffer(buffer);
        }
    }

    public long transferTo(long position, long count, WritableByteChannel target)
            throws IOException {
        checkOpen();
        if (!target.isOpen()) {
            throw new ClosedChannelException();
        }
        if (target instanceof FileChannelImpl) {
            ((FileChannelImpl) target).checkWritable();
        }
        if (position < 0 || count < 0) {
            throw new IllegalArgumentException("position=" + position + " count=" + count);
        }

        if (count == 0 || position >= size()) {
            return 0;
        }
        ByteBuffer buffer = null;
        count = Math.min(count, size() - position);
        if (target instanceof SocketChannelImpl) {
            // only socket can be transfered by system call
            return transferToSocket(fd, ((SocketChannelImpl) target).getFD(), position, count);
        }

        try {
            buffer = map(MapMode.READ_ONLY, position, count);
            return target.write(buffer);
        } finally {
            NioUtils.freeDirectBuffer(buffer);
        }
    }

    private long transferToSocket(FileDescriptor src, FileDescriptor dst, long position, long count) throws IOException {
        boolean completed = false;
        try {
            begin();
            long ret = Platform.FILE_SYSTEM.transfer(IoUtils.getFd(src), fd, position, count);
            completed = true;
            return ret;
        } finally {
            end(completed);
        }
    }

    public FileChannel truncate(long size) throws IOException {
        checkOpen();
        if (size < 0) {
            throw new IllegalArgumentException("size: " + size);
        }
        checkWritable();
        if (size < size()) {
            synchronized (repositioningLock) {
                long position = position();
                Platform.FILE_SYSTEM.truncate(IoUtils.getFd(fd), size);
                /*
                 * FIXME: currently the port library always modifies the
                 * position to given size. not sure it is a bug or intended
                 * behavior, so I always reset the position to proper value as
                 * Java Spec.
                 */
                position(position > size ? size : position);
            }
        }
        return this;
    }

    public int write(ByteBuffer buffer, long position) throws IOException {
        if (buffer == null) {
            throw new NullPointerException("buffer == null");
        }
        if (position < 0) {
            throw new IllegalArgumentException("position: " + position);
        }
        checkOpen();
        checkWritable();
        if (!buffer.hasRemaining()) {
            return 0;
        }
        int bytesWritten = 0;
        synchronized (repositioningLock) {
            long preWritePosition = position();
            position(position);
            try {
                bytesWritten = writeImpl(buffer);
            } finally {
                position(preWritePosition);
            }
        }
        return bytesWritten;
    }

    public int write(ByteBuffer buffer) throws IOException {
        checkOpen();
        checkWritable();
        if ((mode & O_APPEND) != 0) {
            position(size());
        }
        return writeImpl(buffer);
    }

    private int writeImpl(ByteBuffer buffer) throws IOException {
        int bytesWritten;
        boolean completed = false;
        synchronized (repositioningLock) {
            if (buffer.isDirect()) {
                try {
                    begin();
                    int address = NioUtils.getDirectBufferAddress(buffer);
                    bytesWritten = (int) Platform.FILE_SYSTEM.writeDirect(IoUtils.getFd(fd),
                            address, buffer.position(), buffer.remaining());
                    completed = true;
                } finally {
                    end(completed);
                }
            } else {
                try {
                    begin();
                    bytesWritten = (int) Platform.FILE_SYSTEM.write(IoUtils.getFd(fd), buffer
                            .array(), buffer.arrayOffset() + buffer.position(),
                            buffer.remaining());
                    completed = true;
                } finally {
                    end(completed);
                }
            }
            if (bytesWritten > 0) {
                buffer.position(buffer.position() + bytesWritten);
            }
        }
        return bytesWritten;
    }

    public long write(ByteBuffer[] buffers, int offset, int length) throws IOException {
        Arrays.checkOffsetAndCount(buffers.length, offset, length);
        checkOpen();
        checkWritable();
        int count = FileChannelImpl.calculateTotalRemaining(buffers, offset, length, false);
        if (count == 0) {
            return 0;
        }
        int[] handles = new int[length];
        int[] offsets = new int[length];
        int[] lengths = new int[length];
        // list of allocated direct ByteBuffers to prevent them from being GC-ed
        ByteBuffer[] allocatedBufs = new ByteBuffer[length];

        for (int i = 0; i < length; i++) {
            ByteBuffer buffer = buffers[i + offset];
            if (!buffer.isDirect()) {
                ByteBuffer directBuffer = ByteBuffer.allocateDirect(buffer.remaining());
                directBuffer.put(buffer);
                directBuffer.flip();
                buffer = directBuffer;
                allocatedBufs[i] = directBuffer;
                offsets[i] = 0;
            } else {
                offsets[i] = buffer.position();
                allocatedBufs[i] = null;
            }
            handles[i] = NioUtils.getDirectBufferAddress(buffer);
            lengths[i] = buffer.remaining();
        }

        long bytesWritten = 0;
        boolean completed = false;
        synchronized (repositioningLock) {
            try {
                begin();
                bytesWritten = Platform.FILE_SYSTEM.writev(IoUtils.getFd(fd), handles, offsets, lengths, length);
                completed = true;
            } finally {
                end(completed);
                for (ByteBuffer buffer : allocatedBufs) {
                    NioUtils.freeDirectBuffer(buffer);
                }
            }
        }

        long bytesRemaining = bytesWritten;
        for (int i = offset; i < length + offset; i++) {
            if (bytesRemaining > buffers[i].remaining()) {
                int pos = buffers[i].limit();
                buffers[i].position(pos);
                bytesRemaining -= buffers[i].remaining();
            } else {
                int pos = buffers[i].position() + (int) bytesRemaining;
                buffers[i].position(pos);
                break;
            }
        }
        return bytesWritten;
    }

    static void checkWritable(ByteBuffer buffer) {
        if (buffer.isReadOnly()) {
            throw new IllegalArgumentException("read-only buffer");
        }
    }

    /**
     * @param copyingIn true if we're copying data into the buffers (typically
     * because the caller is a file/network read operation), false if we're
     * copying data out of the buffers (for a file/network write operation).
     */
    static int calculateTotalRemaining(ByteBuffer[] buffers, int offset, int length, boolean copyingIn) {
        int count = 0;
        for (int i = offset; i < offset + length; ++i) {
            count += buffers[i].remaining();
            if (copyingIn) {
                checkWritable(buffers[i]);
            }
        }
        return count;
    }

    public FileDescriptor getFD() {
        return fd;
    }

    /**
     * Add a new pending lock to the manager. Throws an exception if the lock
     * would overlap an existing lock. Once the lock is acquired it remains in
     * this set as an acquired lock.
     */
    private synchronized void addLock(FileLock lock) throws OverlappingFileLockException {
        long lockEnd = lock.position() + lock.size();
        for (FileLock existingLock : locks) {
            if (existingLock.position() > lockEnd) {
                // This, and all remaining locks, start beyond our end (so
                // cannot overlap).
                break;
            }
            if (existingLock.overlaps(lock.position(), lock.size())) {
                throw new OverlappingFileLockException();
            }
        }
        locks.add(lock);
    }

    /**
     * Removes an acquired lock from the lock manager. If the lock did not exist
     * in the lock manager the operation is a no-op.
     */
    private synchronized void removeLock(FileLock lock) {
        locks.remove(lock);
    }
}
