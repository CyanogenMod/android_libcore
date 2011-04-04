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
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.NioUtils;
import libcore.util.MutableInt;
import libcore.util.MutableLong;

public final class Posix implements Os {
    Posix() { }

    public native boolean access(String path, int mode) throws ErrnoException;
    public native void chmod(String path, int mode) throws ErrnoException;
    public native void close(FileDescriptor fd) throws ErrnoException;
    public native String[] environ();
    public native int fcntlVoid(FileDescriptor fd, int cmd) throws ErrnoException;
    public native int fcntlLong(FileDescriptor fd, int cmd, long arg) throws ErrnoException;
    public native int fcntlFlock(FileDescriptor fd, int cmd, StructFlock arg) throws ErrnoException;
    public native void fdatasync(FileDescriptor fd) throws ErrnoException;
    public native StructStat fstat(FileDescriptor fd) throws ErrnoException;
    public native StructStatFs fstatfs(FileDescriptor fd) throws ErrnoException;
    public native void fsync(FileDescriptor fd) throws ErrnoException;
    public native void ftruncate(FileDescriptor fd, long length) throws ErrnoException;
    public native String getenv(String name);
    public native SocketAddress getsockname(FileDescriptor fd) throws ErrnoException;
    public native int getsockoptByte(FileDescriptor fd, int level, int option) throws ErrnoException;
    public native InetAddress getsockoptInAddr(FileDescriptor fd, int level, int option) throws ErrnoException;
    public native int getsockoptInt(FileDescriptor fd, int level, int option) throws ErrnoException;
    public native StructLinger getsockoptLinger(FileDescriptor fd, int level, int option) throws ErrnoException;
    public native StructTimeval getsockoptTimeval(FileDescriptor fd, int level, int option) throws ErrnoException;
    public native int ioctlInt(FileDescriptor fd, int cmd, MutableInt arg) throws ErrnoException;
    public native boolean isatty(FileDescriptor fd);
    public native void listen(FileDescriptor fd, int backlog) throws ErrnoException;
    public native long lseek(FileDescriptor fd, long offset, int whence) throws ErrnoException;
    public native void mincore(long address, long byteCount, byte[] vector) throws ErrnoException;
    public native void mkdir(String path, int mode) throws ErrnoException;
    public native void mlock(long address, long byteCount) throws ErrnoException;
    public native long mmap(long address, long byteCount, int prot, int flags, FileDescriptor fd, long offset) throws ErrnoException;
    public native void msync(long address, long byteCount, int flags) throws ErrnoException;
    public native void munlock(long address, long byteCount) throws ErrnoException;
    public native void munmap(long address, long byteCount) throws ErrnoException;
    public native FileDescriptor open(String path, int flags, int mode) throws ErrnoException;
    public native FileDescriptor[] pipe() throws ErrnoException;
    public native StructStat lstat(String path) throws ErrnoException;
    public int read(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException {
        if (buffer.isDirect()) {
            return readDirectBuffer(fd, buffer, buffer.position(), buffer.remaining());
        }
        return read(fd, NioUtils.unsafeArray(buffer), NioUtils.unsafeArrayOffset(buffer) + buffer.position(), buffer.remaining());
    }
    private native int readDirectBuffer(FileDescriptor fd, ByteBuffer buffer, int position, int remaining) throws ErrnoException;
    public native int read(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException;
    public native int readv(FileDescriptor fd, Object[] buffers, int[] offsets, int[] byteCounts) throws ErrnoException;
    public native void remove(String path) throws ErrnoException;
    public native void rename(String oldPath, String newPath) throws ErrnoException;
    public native long sendfile(FileDescriptor outFd, FileDescriptor inFd, MutableLong inOffset, long byteCount) throws ErrnoException;
    public native void setsockoptByte(FileDescriptor fd, int level, int option, int value) throws ErrnoException;
    public native void setsockoptInt(FileDescriptor fd, int level, int option, int value) throws ErrnoException;
    public native void setsockoptGroupReq(FileDescriptor fd, int level, int option, StructGroupReq value) throws ErrnoException;
    public native void setsockoptLinger(FileDescriptor fd, int level, int option, StructLinger value) throws ErrnoException;
    public native void setsockoptTimeval(FileDescriptor fd, int level, int option, StructTimeval value) throws ErrnoException;
    public native void shutdown(FileDescriptor fd, int how) throws ErrnoException;
    public native FileDescriptor socket(int domain, int type, int protocol) throws ErrnoException;
    public native StructStat stat(String path) throws ErrnoException;
    public native StructStatFs statfs(String path) throws ErrnoException;
    public native String strerror(int errno);
    public native void symlink(String oldPath, String newPath) throws ErrnoException;
    public native long sysconf(int name);
    public native StructUtsname uname() throws ErrnoException;
    public int write(FileDescriptor fd, ByteBuffer buffer) throws ErrnoException {
        if (buffer.isDirect()) {
            return writeDirectBuffer(fd, buffer, buffer.position(), buffer.remaining());
        }
        return write(fd, NioUtils.unsafeArray(buffer), NioUtils.unsafeArrayOffset(buffer) + buffer.position(), buffer.remaining());
    }
    private native int writeDirectBuffer(FileDescriptor fd, ByteBuffer buffer, int position, int remaining) throws ErrnoException;
    public native int write(FileDescriptor fd, byte[] bytes, int byteOffset, int byteCount) throws ErrnoException;
    public native int writev(FileDescriptor fd, Object[] buffers, int[] offsets, int[] byteCounts) throws ErrnoException;
}
