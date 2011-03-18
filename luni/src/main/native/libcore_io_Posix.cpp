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
#include "ScopedUtfChars.h"
#include "toStringArray.h"

#include <errno.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>

static jboolean maybeThrow(JNIEnv* env, int rc, int errnum) {
    if (rc != -1) {
        return false;
    }

    jthrowable cause = NULL;
    if (env->ExceptionCheck()) {
        cause = env->ExceptionOccurred();
        env->ExceptionClear();
    }

    jobject exception;
    if (cause != NULL) {
        static jmethodID ctor2 = env->GetMethodID(JniConstants::errnoExceptionClass, "<init>",
                "(ILjava/lang/Throwable;)V");
        exception = env->NewObject(JniConstants::errnoExceptionClass, ctor2, errnum, cause);
    } else {
        static jmethodID ctor1 = env->GetMethodID(JniConstants::errnoExceptionClass, "<init>",
                "(I)V");
        exception = env->NewObject(JniConstants::errnoExceptionClass, ctor1, errnum);
    }
    env->Throw(reinterpret_cast<jthrowable>(exception));
    return true;
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
    if (maybeThrow(env, rc, errno)) {
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
    maybeThrow(env, rc, errno);
    return (rc == 0);
}

static jobjectArray Posix_environ(JNIEnv* env, jobject) {
    extern char** environ; // Standard, but not in any header file.
    return toStringArray(env, environ);
}

static void Posix_fdatasync(JNIEnv* env, jobject, jobject javaFd) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    int rc = TEMP_FAILURE_RETRY(fdatasync(fd));
    maybeThrow(env, rc, errno);
}

static jobject Posix_fstat(JNIEnv* env, jobject, jobject javaFd) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    struct stat sb;
    int rc = TEMP_FAILURE_RETRY(fstat(fd, &sb));
    if (maybeThrow(env, rc, errno)) {
        return NULL;
    }
    return makeStructStat(env, sb);
}

static void Posix_fsync(JNIEnv* env, jobject, jobject javaFd) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    int rc = TEMP_FAILURE_RETRY(fsync(fd));
    maybeThrow(env, rc, errno);
}

static jstring Posix_getenv(JNIEnv* env, jobject, jstring javaName) {
    ScopedUtfChars name(env, javaName);
    if (name.c_str() == NULL) {
        return NULL;
    }
    return env->NewStringUTF(getenv(name.c_str()));
}

static jobject Posix_lstat(JNIEnv* env, jobject, jstring javaPath) {
    return doStat(env, javaPath, true);
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
        maybeThrow(env, -1, errno);
    }
    return result;
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(Posix, access, "(Ljava/lang/String;I)Z"),
    NATIVE_METHOD(Posix, environ, "()[Ljava/lang/String;"),
    NATIVE_METHOD(Posix, fdatasync, "(Ljava/io/FileDescriptor;)V"),
    NATIVE_METHOD(Posix, fstat, "(Ljava/io/FileDescriptor;)Llibcore/io/StructStat;"),
    NATIVE_METHOD(Posix, fsync, "(Ljava/io/FileDescriptor;)V"),
    NATIVE_METHOD(Posix, getenv, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(Posix, lstat, "(Ljava/lang/String;)Llibcore/io/StructStat;"),
    NATIVE_METHOD(Posix, stat, "(Ljava/lang/String;)Llibcore/io/StructStat;"),
    NATIVE_METHOD(Posix, strerror, "(I)Ljava/lang/String;"),
    NATIVE_METHOD(Posix, sysconf, "(I)J"),
};
int register_libcore_io_Posix(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "libcore/io/Posix", gMethods, NELEM(gMethods));
}
