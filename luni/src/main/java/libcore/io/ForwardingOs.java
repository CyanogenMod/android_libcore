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
import java.nio.ByteBuffer;

/**
 * Subclass this if you want to override some {@link Os} methods but otherwise delegate.
 */
public class ForwardingOs implements Os {
    protected final Os os;

    public ForwardingOs(Os os) {
        this.os = os;
    }

    public boolean access(String path, int mode) throws ErrnoException { return os.access(path, mode); }
    public void chmod(String path, int mode) throws ErrnoException { os.chmod(path, mode); }
    public String[] environ() { return os.environ(); }
    public int fcntlVoid(FileDescriptor fd, int cmd) throws ErrnoException { return os.fcntlVoid(fd, cmd); }
    public int fcntlLong(FileDescriptor fd, int cmd, long arg) throws ErrnoException { return os.fcntlLong(fd, cmd, arg); }
    public int fcntlFlock(FileDescriptor fd, int cmd, StructFlock arg) throws ErrnoException { return os.fcntlFlock(fd, cmd, arg); }
    public void fdatasync(FileDescriptor fd) throws ErrnoException { os.fdatasync(fd); }
    public StructStat fstat(FileDescriptor fd) throws ErrnoException { return os.fstat(fd); }
    public StructStatFs fstatfs(FileDescriptor fd) throws ErrnoException { return os.fstatfs(fd); }
    public void fsync(FileDescriptor fd) throws ErrnoException { os.fsync(fd); }
    public void ftruncate(FileDescriptor fd, long length) throws ErrnoException { os.ftruncate(fd, length); }
    public String getenv(String name) { return os.getenv(name); }
    public int ioctlInt(FileDescriptor fd, int cmd, int arg) throws ErrnoException { return os.ioctlInt(fd, cmd, arg); }
    public boolean isatty(FileDescriptor fd) { return os.isatty(fd); }
    public void listen(FileDescriptor fd, int backlog) throws ErrnoException { os.listen(fd, backlog); }
    public long lseek(FileDescriptor fd, long offset, int whence) throws ErrnoException { return os.lseek(fd, offset, whence); }
    public StructStat lstat(String path) throws ErrnoException { return os.lstat(path); }
    public void mincore(long address, long byteCount, byte[] vector) throws ErrnoException { os.mincore(address, byteCount, vector); }
    public void mkdir(String path, int mode) throws ErrnoException { os.mkdir(path, mode); }
    public void mlock(long address, long byteCount) throws ErrnoException { os.mlock(address, byteCount); }
    public long mmap(long address, long byteCount, int prot, int flags, FileDescriptor fd, long offset) throws ErrnoException { return os.mmap(address, byteCount, prot, flags, fd, offset); }
    public void msync(long address, long byteCount, int flags) throws ErrnoException { os.msync(address, byteCount, flags); }
    public void munlock(long address, long byteCount) throws ErrnoException { os.munlock(address, byteCount); }
    public void munmap(long address, long byteCount) throws ErrnoException { os.munmap(address, byteCount); }
    public FileDescriptor open(String path, int flags, int mode) throws ErrnoException { return os.open(path, flags, mode); }
    public FileDescriptor[] pipe() throws ErrnoException { return os.pipe(); }
    public int read(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException { return os.read(fd, buffer); }
    public int read(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException { return os.read(fd, bytes, byteOffset, byteCount); }
    public void remove(String path) throws ErrnoException { os.remove(path); }
    public void rename(String oldPath, String newPath) throws ErrnoException { os.rename(oldPath, newPath); }
    public void shutdown(FileDescriptor fd, int how) throws ErrnoException { os.shutdown(fd, how); }
    public StructStat stat(String path) throws ErrnoException { return os.stat(path); }
    public StructStatFs statfs(String path) throws ErrnoException { return os.statfs(path); }
    public String strerror(int errno) { return os.strerror(errno); }
    public void symlink(String oldPath, String newPath) throws ErrnoException { os.symlink(oldPath, newPath); }
    public long sysconf(int name) { return os.sysconf(name); }
    public StructUtsname uname() throws ErrnoException { return os.uname(); }
    public int write(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException { return os.write(fd, buffer); }
    public int write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException { return os.write(fd, bytes, byteOffset, byteCount); }
}
