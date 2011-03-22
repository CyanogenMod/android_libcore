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

#define LOG_TAG "Posix"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "JniException.h"
#include "ScopedPrimitiveArray.h"
#include "ScopedUtfChars.h"
#include "toStringArray.h"

#include <errno.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>

static void throwErrnoException(JNIEnv* env, const char* name) {
    int errnum = errno;

    jthrowable cause = NULL;
    if (env->ExceptionCheck()) {
        cause = env->ExceptionOccurred();
        env->ExceptionClear();
    }

    ScopedLocalRef<jstring> javaName(env, env->NewStringUTF(name));
    if (javaName.get() == NULL) {
        // Not really much we can do here. We're probably dead in the water,
        // but let's try to stumble on...
        env->ExceptionClear();
    }

    jobject exception;
    if (cause != NULL) {
        static jmethodID ctor = env->GetMethodID(JniConstants::errnoExceptionClass, "<init>",
                "(Ljava/lang/String;ILjava/lang/Throwable;)V");
        exception = env->NewObject(JniConstants::errnoExceptionClass, ctor,
                javaName.get(), errnum, cause);
    } else {
        static jmethodID ctor = env->GetMethodID(JniConstants::errnoExceptionClass, "<init>",
                "(Ljava/lang/String;I)V");
        exception = env->NewObject(JniConstants::errnoExceptionClass, ctor, javaName.get(), errnum);
    }
    env->Throw(reinterpret_cast<jthrowable>(exception));
}

template <typename rc_t>
static rc_t throwIfMinusOne(JNIEnv* env, const char* name, rc_t rc) {
    if (rc == rc_t(-1)) {
        throwErrnoException(env, name);
    }
    return rc;
}

static jobject makeStructStat(JNIEnv* env, const struct stat& sb) {
    static jmethodID ctor = env->GetMethodID(JniConstants::structStatClass, "<init>",
            "(JJIJIIJJJJJJJ)V");
    return env->NewObject(JniConstants::structStatClass, ctor,
            jlong(sb.st_dev), jlong(sb.st_ino), jint(sb.st_mode), jlong(sb.st_nlink),
            jint(sb.st_uid), jint(sb.st_gid), jlong(sb.st_rdev), jlong(sb.st_size),
            jlong(sb.st_atime), jlong(sb.st_mtime), jlong(sb.st_ctime),
            jlong(sb.st_blksize), jlong(sb.st_blocks));
}

static jobject doStat(JNIEnv* env, jstring javaPath, bool isLstat) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return NULL;
    }
    struct stat sb;
    int rc = isLstat ? TEMP_FAILURE_RETRY(lstat(path.c_str(), &sb))
                     : TEMP_FAILURE_RETRY(stat(path.c_str(), &sb));
    if (rc == -1) {
        throwErrnoException(env, isLstat ? "lstat" : "stat");
        return NULL;
    }
    return makeStructStat(env, sb);
}

static jboolean Posix_access(JNIEnv* env, jobject, jstring javaPath, jint mode) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }
    int rc = TEMP_FAILURE_RETRY(access(path.c_str(), mode));
    if (rc == -1) {
        throwErrnoException(env, "access");
    }
    return (rc == 0);
}

static jobjectArray Posix_environ(JNIEnv* env, jobject) {
    extern char** environ; // Standard, but not in any header file.
    return toStringArray(env, environ);
}

static void Posix_fdatasync(JNIEnv* env, jobject, jobject javaFd) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    throwIfMinusOne(env, "fdatasync", TEMP_FAILURE_RETRY(fdatasync(fd)));
}

static jobject Posix_fstat(JNIEnv* env, jobject, jobject javaFd) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    struct stat sb;
    int rc = TEMP_FAILURE_RETRY(fstat(fd, &sb));
    if (rc == -1) {
        throwErrnoException(env, "fstat");
        return NULL;
    }
    return makeStructStat(env, sb);
}

static void Posix_fsync(JNIEnv* env, jobject, jobject javaFd) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    throwIfMinusOne(env, "fsync", TEMP_FAILURE_RETRY(fsync(fd)));
}

static void Posix_ftruncate(JNIEnv* env, jobject, jobject javaFd, jlong length) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    throwIfMinusOne(env, "ftruncate", TEMP_FAILURE_RETRY(ftruncate64(fd, length)));
}

static jstring Posix_getenv(JNIEnv* env, jobject, jstring javaName) {
    ScopedUtfChars name(env, javaName);
    if (name.c_str() == NULL) {
        return NULL;
    }
    return env->NewStringUTF(getenv(name.c_str()));
}

static jboolean Posix_isatty(JNIEnv* env, jobject, jobject javaFd) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    return TEMP_FAILURE_RETRY(isatty(fd)) == 0;
}

static jlong Posix_lseek(JNIEnv* env, jobject, jobject javaFd, jlong offset, jint whence) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    return throwIfMinusOne(env, "lseek", TEMP_FAILURE_RETRY(lseek64(fd, offset, whence)));
}

static jobject Posix_lstat(JNIEnv* env, jobject, jstring javaPath) {
    return doStat(env, javaPath, true);
}

static void Posix_mincore(JNIEnv* env, jobject, jlong address, jlong byteCount, jbyteArray javaVector) {
    ScopedByteArrayRW vector(env, javaVector);
    if (vector.get() == NULL) {
        return;
    }
    void* ptr = reinterpret_cast<void*>(static_cast<uintptr_t>(address));
    unsigned char* vec = reinterpret_cast<unsigned char*>(vector.get());
    throwIfMinusOne(env, "mincore", TEMP_FAILURE_RETRY(mincore(ptr, byteCount, vec)));
}

static void Posix_mlock(JNIEnv* env, jobject, jlong address, jlong byteCount) {
    void* ptr = reinterpret_cast<void*>(static_cast<uintptr_t>(address));
    throwIfMinusOne(env, "mlock", TEMP_FAILURE_RETRY(mlock(ptr, byteCount)));
}

static jlong Posix_mmap(JNIEnv* env, jobject, jlong address, jlong byteCount, jint prot, jint flags, jobject javaFd, jlong offset) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    void* suggestedPtr = reinterpret_cast<void*>(static_cast<uintptr_t>(address));
    void* ptr = mmap(suggestedPtr, byteCount, prot, flags, fd, offset);
    if (ptr == MAP_FAILED) {
        throwErrnoException(env, "mmap");
    }
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(ptr));
}

static void Posix_msync(JNIEnv* env, jobject, jlong address, jlong byteCount, jint flags) {
    void* ptr = reinterpret_cast<void*>(static_cast<uintptr_t>(address));
    throwIfMinusOne(env, "msync", TEMP_FAILURE_RETRY(msync(ptr, byteCount, flags)));
}

static void Posix_munlock(JNIEnv* env, jobject, jlong address, jlong byteCount) {
    void* ptr = reinterpret_cast<void*>(static_cast<uintptr_t>(address));
    throwIfMinusOne(env, "munlock", TEMP_FAILURE_RETRY(munlock(ptr, byteCount)));
}

static void Posix_munmap(JNIEnv* env, jobject, jlong address, jlong byteCount) {
    void* ptr = reinterpret_cast<void*>(static_cast<uintptr_t>(address));
    throwIfMinusOne(env, "munmap", TEMP_FAILURE_RETRY(munmap(ptr, byteCount)));
}

static jobject Posix_stat(JNIEnv* env, jobject, jstring javaPath) {
    return doStat(env, javaPath, false);
}

static jstring Posix_strerror(JNIEnv* env, jobject, jint errnum) {
    char buffer[BUFSIZ];
    const char* message = jniStrError(errnum, buffer, sizeof(buffer));
    return env->NewStringUTF(message);
}

static jlong Posix_sysconf(JNIEnv* env, jobject, jint name) {
    // Since -1 is a valid result from sysconf(3), detecting failure is a little more awkward.
    errno = 0;
    long result = sysconf(name);
    if (result == -1L && errno == EINVAL) {
        throwErrnoException(env, "sysconf");
    }
    return result;
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(Posix, access, "(Ljava/lang/String;I)Z"),
    NATIVE_METHOD(Posix, environ, "()[Ljava/lang/String;"),
    NATIVE_METHOD(Posix, fdatasync, "(Ljava/io/FileDescriptor;)V"),
    NATIVE_METHOD(Posix, fstat, "(Ljava/io/FileDescriptor;)Llibcore/io/StructStat;"),
    NATIVE_METHOD(Posix, fsync, "(Ljava/io/FileDescriptor;)V"),
    NATIVE_METHOD(Posix, ftruncate, "(Ljava/io/FileDescriptor;J)V"),
    NATIVE_METHOD(Posix, getenv, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(Posix, isatty, "(Ljava/io/FileDescriptor;)Z"),
    NATIVE_METHOD(Posix, lseek, "(Ljava/io/FileDescriptor;JI)J"),
    NATIVE_METHOD(Posix, lstat, "(Ljava/lang/String;)Llibcore/io/StructStat;"),
    NATIVE_METHOD(Posix, mincore, "(JJ[B)V"),
    NATIVE_METHOD(Posix, mlock, "(JJ)V"),
    NATIVE_METHOD(Posix, mmap, "(JJIILjava/io/FileDescriptor;J)J"),
    NATIVE_METHOD(Posix, msync, "(JJI)V"),
    NATIVE_METHOD(Posix, munlock, "(JJ)V"),
    NATIVE_METHOD(Posix, munmap, "(JJ)V"),
    NATIVE_METHOD(Posix, stat, "(Ljava/lang/String;)Llibcore/io/StructStat;"),
    NATIVE_METHOD(Posix, strerror, "(I)Ljava/lang/String;"),
    NATIVE_METHOD(Posix, sysconf, "(I)J"),
};
int register_libcore_io_Posix(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "libcore/io/Posix", gMethods, NELEM(gMethods));
}
