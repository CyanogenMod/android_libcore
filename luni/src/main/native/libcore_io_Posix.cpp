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
#include "ScopedUtfChars.h"

#include <stdlib.h>

static jstring Posix_getenv(JNIEnv* env, jobject, jstring javaName) {
    ScopedUtfChars name(env, javaName);
    if (name.c_str() == NULL) {
        return NULL;
    }
    return env->NewStringUTF(getenv(name.c_str()));
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(Posix, getenv, "(Ljava/lang/String;)Ljava/lang/String;"),
};
int register_libcore_io_Posix(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "libcore/io/Posix", gMethods, NELEM(gMethods));
}
