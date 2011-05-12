/*
 * Copyright (C) 2011 The Android Open Source Project
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

import dalvik.system.BlockGuard;
import java.io.FileDescriptor;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import static libcore.io.OsConstants.*;

/**
 * Informs BlockGuard of any activity it should be aware of.
 */
public class BlockGuardOs extends ForwardingOs {
    public BlockGuardOs(Os os) {
        super(os);
    }

    @Override public void connect(FileDescriptor fd, InetAddress address, int port) throws ErrnoException {
        BlockGuard.getThreadPolicy().onNetwork();
        os.connect(fd, address, port);
    }

    @Override public void fdatasync(FileDescriptor fd) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        os.fdatasync(fd);
    }

    @Override public void fsync(FileDescriptor fd) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        os.fsync(fd);
    }

    @Override public void ftruncate(FileDescriptor fd, long length) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        os.ftruncate(fd, length);
    }

    @Override public FileDescriptor open(String path, int flags, int mode) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        if ((mode & O_ACCMODE) != O_RDONLY) {
            BlockGuard.getThreadPolicy().onWriteToDisk();
        }
        return os.open(path, flags, mode);
    }

    @Override public int poll(StructPollfd[] fds, int timeoutMs) throws ErrnoException {
        BlockGuard.getThreadPolicy().onNetwork();
        return os.poll(fds, timeoutMs);
    }

    @Override public int read(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return os.read(fd, buffer);
    }

    @Override public int read(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return os.read(fd, bytes, byteOffset, byteCount);
    }

    @Override public int readv(FileDescriptor fd, Object[] buffers, int[] offsets, int[] byteCounts) throws ErrnoException {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return os.readv(fd, buffers, offsets, byteCounts);
    }

    @Override public int write(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return os.write(fd, buffer);
    }

    @Override public int write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return os.write(fd, bytes, byteOffset, byteCount);
    }

    @Override public int writev(FileDescriptor fd, Object[] buffers, int[] offsets, int[] byteCounts) throws ErrnoException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return os.writev(fd, buffers, offsets, byteCounts);
    }
}
