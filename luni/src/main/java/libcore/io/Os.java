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

public interface Os {
    public boolean access(String path, int mode) throws ErrnoException;
    public void chmod(String path, int mode) throws ErrnoException;
    public String[] environ();
    public int fcntlVoid(FileDescriptor fd, int cmd) throws ErrnoException;
    public int fcntlLong(FileDescriptor fd, int cmd, long arg) throws ErrnoException;
    public int fcntlFlock(FileDescriptor fd, int cmd, StructFlock arg) throws ErrnoException;
    public void fdatasync(FileDescriptor fd) throws ErrnoException;
    public StructStat fstat(FileDescriptor fd) throws ErrnoException;
    public StructStatFs fstatfs(FileDescriptor fd) throws ErrnoException;
    public void fsync(FileDescriptor fd) throws ErrnoException;
    public void ftruncate(FileDescriptor fd, long length) throws ErrnoException;
    public String getenv(String name);
    /* TODO: this is a questionable translation of int* ioctls in general. */
    public int ioctlInt(FileDescriptor fd, int cmd, int arg) throws ErrnoException;
    public boolean isatty(FileDescriptor fd);
    public void listen(FileDescriptor fd, int backlog) throws ErrnoException;
    public long lseek(FileDescriptor fd, long offset, int whence) throws ErrnoException;
    public void mincore(long address, long byteCount, byte[] vector) throws ErrnoException;
    public void mkdir(String path, int mode) throws ErrnoException;
    public void mlock(long address, long byteCount) throws ErrnoException;
    public long mmap(long address, long byteCount, int prot, int flags, FileDescriptor fd, long offset) throws ErrnoException;
    public void msync(long address, long byteCount, int flags) throws ErrnoException;
    public void munlock(long address, long byteCount) throws ErrnoException;
    public void munmap(long address, long byteCount) throws ErrnoException;
    public FileDescriptor open(String path, int flags, int mode) throws ErrnoException;
    public FileDescriptor[] pipe() throws ErrnoException;
    public StructStat lstat(String path) throws ErrnoException;
    public int read(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException;
    public int read(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException;
    public void remove(String path) throws ErrnoException;
    public void rename(String oldPath, String newPath) throws ErrnoException;
    public void shutdown(FileDescriptor fd, int how) throws ErrnoException;
    public StructStat stat(String path) throws ErrnoException;
    /* TODO: replace statfs with statvfs. */
    public StructStatFs statfs(String path) throws ErrnoException;
    public String strerror(int errno);
    public void symlink(String oldPath, String newPath) throws ErrnoException;
    public long sysconf(int name);
    public StructUtsname uname() throws ErrnoException;
    public int write(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException;
    public int write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException;
}
