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

static void maybeThrow(JNIEnv* env, int rc, int errnum) {
    if (rc != -1) {
        return;
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
}

static jboolean Posix_access(JNIEnv* env, jobject, jstring javaPath, jint mode) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }
    int rc = access(path.c_str(), mode);
    maybeThrow(env, rc, errno);
    return (rc == 0);
}

static jobjectArray Posix_environ(JNIEnv* env, jobject) {
    extern char** environ; // Standard, but not in any header file.
    return toStringArray(env, environ);
}

static void Posix_fdatasync(JNIEnv* env, jobject, jobject javaFd) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    int rc = fdatasync(fd);
    maybeThrow(env, rc, errno);
}

static void Posix_fsync(JNIEnv* env, jobject, jobject javaFd) {
    int fd = jniGetFDFromFileDescriptor(env, javaFd);
    int rc = fsync(fd);
    maybeThrow(env, rc, errno);
}

static jstring Posix_getenv(JNIEnv* env, jobject, jstring javaName) {
    ScopedUtfChars name(env, javaName);
    if (name.c_str() == NULL) {
        return NULL;
    }
    return env->NewStringUTF(getenv(name.c_str()));
}

static jstring Posix_strerror(JNIEnv* env, jobject, jint errnum) {
    char buffer[BUFSIZ];
    const char* message = jniStrError(errnum, buffer, sizeof(buffer));
    return env->NewStringUTF(message);
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(Posix, access, "(Ljava/lang/String;I)Z"),
    NATIVE_METHOD(Posix, environ, "()[Ljava/lang/String;"),
    NATIVE_METHOD(Posix, fdatasync, "(Ljava/io/FileDescriptor;)V"),
    NATIVE_METHOD(Posix, fsync, "(Ljava/io/FileDescriptor;)V"),
    NATIVE_METHOD(Posix, getenv, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(Posix, strerror, "(I)Ljava/lang/String;"),
};
int register_libcore_io_Posix(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "libcore/io/Posix", gMethods, NELEM(gMethods));
}
