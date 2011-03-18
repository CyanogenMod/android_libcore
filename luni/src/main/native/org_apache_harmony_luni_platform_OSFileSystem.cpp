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

#define LOG_TAG "OSFileSystem"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "JniException.h"
#include "LocalArray.h"
#include "ScopedPrimitiveArray.h"
#include "ScopedUtfChars.h"
#include "UniquePtr.h"

#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

#if HAVE_SYS_SENDFILE_H
#include <sys/sendfile.h>
#else
/*
 * Define a small adapter function: sendfile() isn't part of a standard,
 * and its definition differs between Linux, BSD, and OS X. This version
 * works for OS X but will probably not work on other BSDish systems.
 * Note: We rely on function overloading here to define a same-named
 * function with different arguments.
 */
#include <sys/socket.h>
#include <sys/types.h>
static inline ssize_t sendfile(int out_fd, int in_fd, off_t* offset, size_t count) {
    off_t len = count;
    int result = sendfile(in_fd, out_fd, *offset, &len, NULL, 0);
    if (result < 0) {
        return -1;
    }
    return len;
}
#endif

static jlong translateLockLength(jlong length) {
    // FileChannel.tryLock uses Long.MAX_VALUE to mean "lock the whole
    // file", where POSIX would use 0. We can support that special case,
    // even for files whose actual length we can't represent. For other
    // out of range lengths, though, we want our range checking to fire.
    return (length == 0x7fffffffffffffffLL) ? 0 : length;
}

static struct flock64 flockFromStartAndLength(jlong start, jlong length) {
    struct flock64 lock;
    memset(&lock, 0, sizeof(lock));

    lock.l_whence = SEEK_SET;
    lock.l_start = start;
    lock.l_len = length;

    return lock;
}

static jint OSFileSystem_lockImpl(JNIEnv* env, jobject, jint fd,
        jlong start, jlong length, jint lockType, jboolean waitFlag) {

    length = translateLockLength(length);
    struct flock64 lock(flockFromStartAndLength(start, length));
    lock.l_type = lockType;
    int rc = TEMP_FAILURE_RETRY(fcntl(fd, waitFlag ? F_SETLKW64 : F_SETLK64, &lock));
    if (lockType == F_UNLCK && rc == -1) {
        jniThrowIOException(env, errno);
    }
    return rc;
}

// Translate three Java int[]s to a native iovec[] for readv and writev.
static iovec* initIoVec(JNIEnv* env,
        jintArray jBuffers, jintArray jOffsets, jintArray jLengths, jint size) {
    UniquePtr<iovec[]> vectors(new iovec[size]);
    if (vectors.get() == NULL) {
        jniThrowOutOfMemoryError(env, NULL);
        return NULL;
    }
    ScopedIntArrayRO buffers(env, jBuffers);
    if (buffers.get() == NULL) {
        return NULL;
    }
    ScopedIntArrayRO offsets(env, jOffsets);
    if (offsets.get() == NULL) {
        return NULL;
    }
    ScopedIntArrayRO lengths(env, jLengths);
    if (lengths.get() == NULL) {
        return NULL;
    }
    for (int i = 0; i < size; ++i) {
        vectors[i].iov_base = reinterpret_cast<void*>(buffers[i] + offsets[i]);
        vectors[i].iov_len = lengths[i];
    }
    return vectors.release();
}

static jlong OSFileSystem_readv(JNIEnv* env, jobject, jint fd,
        jintArray jBuffers, jintArray jOffsets, jintArray jLengths, jint size) {
    UniquePtr<iovec[]> vectors(initIoVec(env, jBuffers, jOffsets, jLengths, size));
    if (vectors.get() == NULL) {
        return -1;
    }
    long result = readv(fd, vectors.get(), size);
    if (result == 0) {
        return -1;
    }
    if (result == -1) {
        jniThrowIOException(env, errno);
    }
    return result;
}

static jlong OSFileSystem_writev(JNIEnv* env, jobject, jint fd,
        jintArray jBuffers, jintArray jOffsets, jintArray jLengths, jint size) {
    UniquePtr<iovec[]> vectors(initIoVec(env, jBuffers, jOffsets, jLengths, size));
    if (vectors.get() == NULL) {
        return -1;
    }
    long result = writev(fd, vectors.get(), size);
    if (result == -1) {
        jniThrowIOException(env, errno);
    }
    return result;
}

static jlong OSFileSystem_transfer(JNIEnv* env, jobject, jint fd, jobject sd,
        jlong offset, jlong count) {

    int socket = jniGetFDFromFileDescriptor(env, sd);
    if (socket == -1) {
        return -1;
    }

    /* Value of offset is checked in jint scope (checked in java layer)
       The conversion here is to guarantee no value lost when converting offset to off_t
     */
    off_t off = offset;

    ssize_t rc = sendfile(socket, fd, &off, count);
    if (rc == -1) {
        jniThrowIOException(env, errno);
    }
    return rc;
}

static jlong OSFileSystem_readDirect(JNIEnv* env, jobject, jint fd,
        jint buf, jint offset, jint byteCount) {
    if (byteCount == 0) {
        return 0;
    }
    jbyte* dst = reinterpret_cast<jbyte*>(buf + offset);
    jlong rc = TEMP_FAILURE_RETRY(read(fd, dst, byteCount));
    if (rc == 0) {
        return -1;
    }
    if (rc == -1) {
        // We return 0 rather than throw if we try to read from an empty non-blocking pipe.
        if (errno == EAGAIN) {
            return 0;
        }
        jniThrowIOException(env, errno);
    }
    return rc;
}

static jlong OSFileSystem_read(JNIEnv* env, jobject, jint fd,
        jbyteArray byteArray, jint offset, jint byteCount) {
    ScopedByteArrayRW bytes(env, byteArray);
    if (bytes.get() == NULL) {
        return 0;
    }
    jint buf = static_cast<jint>(reinterpret_cast<uintptr_t>(bytes.get()));
    return OSFileSystem_readDirect(env, NULL, fd, buf, offset, byteCount);
}

static jlong OSFileSystem_writeDirect(JNIEnv* env, jobject, jint fd,
        jint buf, jint offset, jint byteCount) {
    if (byteCount == 0) {
        return 0;
    }
    jbyte* src = reinterpret_cast<jbyte*>(buf + offset);
    jlong rc = TEMP_FAILURE_RETRY(write(fd, src, byteCount));
    if (rc == -1) {
        jniThrowIOException(env, errno);
    }
    return rc;
}

static jlong OSFileSystem_write(JNIEnv* env, jobject, jint fd,
        jbyteArray byteArray, jint offset, jint byteCount) {
    ScopedByteArrayRO bytes(env, byteArray);
    if (bytes.get() == NULL) {
        return 0;
    }
    jint buf = static_cast<jint>(reinterpret_cast<uintptr_t>(bytes.get()));
    return OSFileSystem_writeDirect(env, NULL, fd, buf, offset, byteCount);
}

static jlong OSFileSystem_seek(JNIEnv* env, jobject, jint fd, jlong offset, jint whence) {
    jlong result = lseek64(fd, offset, whence);
    if (result == -1) {
        if (errno == ESPIPE) {
            jniThrowExceptionWithErrno(env,
                    "org/apache/harmony/luni/platform/IFileSystem$SeekPipeException",
                    errno);
        } else {
            jniThrowIOException(env, errno);
        }
    }
    return result;
}

static jint OSFileSystem_open(JNIEnv* env, jobject, jstring javaPath, jint flags) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return -1;
    }
    // On Android, we don't want default permissions to allow global access.
    int mode = ((flags & O_ACCMODE) == O_RDONLY) ? 0 : 0600;
    jint fd = TEMP_FAILURE_RETRY(open(path.c_str(), flags, mode));

    // Posix open(2) fails with EISDIR only if you ask for write permission.
    // Java disallows reading directories too.
    if (fd != -1) {
        struct stat sb;
        int rc = fstat(fd, &sb);
        if (rc == -1 || S_ISDIR(sb.st_mode)) {
            // Use EISDIR if that was the case; fail with the fstat(2) error otherwise.
            close(fd);
            fd = -1;
            if (S_ISDIR(sb.st_mode)) {
                errno = EISDIR;
            }
        }
    }

    if (fd == -1) {
        // Get the human-readable form of errno.
        char buffer[80];
        const char* reason = jniStrError(errno, &buffer[0], sizeof(buffer));

        // Construct a message that includes the path and the reason.
        LocalArray<128> message(path.size() + 2 + strlen(reason) + 1 + 1);
        snprintf(&message[0], message.size(), "%s (%s)", path.c_str(), reason);

        // We always throw FileNotFoundException, regardless of the specific
        // failure. (This appears to be true of the RI too.)
        jniThrowException(env, "java/io/FileNotFoundException", &message[0]);
    }
    return fd;
}

static jint OSFileSystem_ioctlAvailable(JNIEnv*env, jobject, jobject fileDescriptor) {
    /*
     * On underlying platforms Android cares about (read "Linux"),
     * ioctl(fd, FIONREAD, &avail) is supposed to do the following:
     *
     * If the fd refers to a regular file, avail is set to
     * the difference between the file size and the current cursor.
     * This may be negative if the cursor is past the end of the file.
     *
     * If the fd refers to an open socket or the read end of a
     * pipe, then avail will be set to a number of bytes that are
     * available to be read without blocking.
     *
     * If the fd refers to a special file/device that has some concept
     * of buffering, then avail will be set in a corresponding way.
     *
     * If the fd refers to a special device that does not have any
     * concept of buffering, then the ioctl call will return a negative
     * number, and errno will be set to ENOTTY.
     *
     * If the fd refers to a special file masquerading as a regular file,
     * then avail may be returned as negative, in that the special file
     * may appear to have zero size and yet a previous read call may have
     * actually read some amount of data and caused the cursor to be
     * advanced.
     */
    int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
    if (fd == -1) {
        return -1;
    }
    int avail = 0;
    int rc = ioctl(fd, FIONREAD, &avail);
    if (rc >= 0) {
        /*
         * Success, but make sure not to return a negative number (see
         * above).
         */
        if (avail < 0) {
            avail = 0;
        }
    } else if (errno == ENOTTY) {
        /* The fd is unwilling to opine about its read buffer. */
        avail = 0;
    } else {
        /* Something strange is happening. */
        jniThrowIOException(env, errno);
    }

    return avail;
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(OSFileSystem, ioctlAvailable, "(Ljava/io/FileDescriptor;)I"),
    NATIVE_METHOD(OSFileSystem, lockImpl, "(IJJIZ)I"),
    NATIVE_METHOD(OSFileSystem, open, "(Ljava/lang/String;I)I"),
    NATIVE_METHOD(OSFileSystem, read, "(I[BII)J"),
    NATIVE_METHOD(OSFileSystem, readDirect, "(IIII)J"),
    NATIVE_METHOD(OSFileSystem, readv, "(I[I[I[II)J"),
    NATIVE_METHOD(OSFileSystem, seek, "(IJI)J"),
    NATIVE_METHOD(OSFileSystem, transfer, "(ILjava/io/FileDescriptor;JJ)J"),
    NATIVE_METHOD(OSFileSystem, write, "(I[BII)J"),
    NATIVE_METHOD(OSFileSystem, writeDirect, "(IIII)J"),
    NATIVE_METHOD(OSFileSystem, writev, "(I[I[I[II)J"),
};
int register_org_apache_harmony_luni_platform_OSFileSystem(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "org/apache/harmony/luni/platform/OSFileSystem", gMethods,
            NELEM(gMethods));
}
