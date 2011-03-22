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

import java.io.FileDescriptor;

/**
 * Subclass this if you want to override some {@link Os} methods but otherwise delegate.
 */
public class ForwardingOs implements Os {
    protected final Os os;

    public ForwardingOs(Os os) {
        this.os = os;
    }

    public boolean access(String path, int mode) throws ErrnoException { return os.access(path, mode); }
    public String[] environ() { return os.environ(); }
    public void fdatasync(FileDescriptor fd) throws ErrnoException { os.fdatasync(fd); }
    public StructStat fstat(FileDescriptor fd) throws ErrnoException { return os.fstat(fd); }
    public void fsync(FileDescriptor fd) throws ErrnoException { os.fsync(fd); }
    public void ftruncate(FileDescriptor fd, long length) throws ErrnoException { os.ftruncate(fd, length); }
    public String getenv(String name) { return os.getenv(name); }
    public boolean isatty(FileDescriptor fd) { return os.isatty(fd); }
    public long lseek(FileDescriptor fd, long offset, int whence) throws ErrnoException { return os.lseek(fd, offset, whence); }
    public StructStat lstat(String path) throws ErrnoException { return os.lstat(path); }
    public void mincore(long address, long byteCount, byte[] vector) throws ErrnoException { os.mincore(address, byteCount, vector); }
    public void mlock(long address, long byteCount) throws ErrnoException { os.mlock(address, byteCount); }
    public long mmap(long address, long byteCount, int prot, int flags, FileDescriptor fd, long offset) throws ErrnoException { return os.mmap(address, byteCount, prot, flags, fd, offset); }
    public void msync(long address, long byteCount, int flags) throws ErrnoException { os.msync(address, byteCount, flags); }
    public void munlock(long address, long byteCount) throws ErrnoException { os.munlock(address, byteCount); }
    public void munmap(long address, long byteCount) throws ErrnoException { os.munmap(address, byteCount); }
    public StructStat stat(String path) throws ErrnoException { return os.stat(path); }
    public String strerror(int errno) { return os.strerror(errno); }
    public long sysconf(int name) { return os.sysconf(name); }
}
