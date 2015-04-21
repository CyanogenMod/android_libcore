/*
 * Copyright (C) 2010 The Android Open Source Project
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

#define LOG_TAG "libcore" // We'll be next to "dalvikvm" in the log; make the distinction clear.

#include "cutils/log.h"
#include "JniConstants.h"
#include "ScopedLocalFrame.h"

#include <stdlib.h>

extern "C" {

extern void register_java_util_zip_ZipFile(JNIEnv*);
extern void register_java_util_zip_Inflater(JNIEnv*);
extern void register_java_util_zip_Deflater(JNIEnv*);
extern void register_java_util_zip_CRC32(JNIEnv*);

}

// DalvikVM calls this on startup, so we can statically register all our native methods.
jint JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        ALOGE("JavaVM::GetEnv() failed");
        abort();
    }

    ScopedLocalFrame localFrame(env);
    register_java_util_zip_ZipFile(env);
    register_java_util_zip_Inflater(env);
    register_java_util_zip_Deflater(env);
    register_java_util_zip_CRC32(env);
    return JNI_VERSION_1_6;
}
