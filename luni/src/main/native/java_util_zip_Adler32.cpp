/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "JNIHelp.h"
#include "ScopedPrimitiveArray.h"
#include "jni.h"
#include "zlib.h"

static jlong Adler32_updateImpl(JNIEnv* env, jobject, jbyteArray byteArray, int off, int len, jlong crc) {
    ScopedByteArray bytes(env, byteArray);
    if (bytes.get() == NULL) {
        jniThrowNullPointerException(env, NULL);
        return 0;
    }
    return adler32((uLong) crc, (Bytef *) (bytes.get() + off), (uInt) len);
}

static jlong Adler32_updateByteImpl(JNIEnv*, jobject, jint val, jlong crc) {
    Bytef bytefVal = val;
    return adler32((uLong) crc, (Bytef *) (&bytefVal), 1);
}

static JNINativeMethod gMethods[] = {
    { "updateImpl", "([BIIJ)J", (void*) Adler32_updateImpl },
    { "updateByteImpl", "(IJ)J", (void*) Adler32_updateByteImpl },
};
int register_java_util_zip_Adler32(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "java/util/zip/Adler32", gMethods, NELEM(gMethods));
}
