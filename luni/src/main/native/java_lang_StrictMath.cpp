/*
 * Copyright (C) 2006 The Android Open Source Project
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

#define LOG_TAG "StrictMath"

#include "../../external/fdlibm/fdlibm.h"

#include "jni.h"
#include "JNIHelp.h"
#include "JniConstants.h"

static jdouble StrictMath_sin(JNIEnv*, jclass, jdouble a) {
    return ieee_sin(a);
}

static jdouble StrictMath_cos(JNIEnv*, jclass, jdouble a) {
    return ieee_cos(a);
}

static jdouble StrictMath_tan(JNIEnv*, jclass, jdouble a) {
    return ieee_tan(a);
}

static jdouble StrictMath_sqrt(JNIEnv*, jclass, jdouble a) {
    return ieee_sqrt(a);
}

static jdouble StrictMath_IEEEremainder(JNIEnv*, jclass, jdouble a, jdouble b) {
    return ieee_remainder(a, b);
}

static jdouble StrictMath_floor(JNIEnv*, jclass, jdouble a) {
    return ieee_floor(a);
}

static jdouble StrictMath_ceil(JNIEnv*, jclass, jdouble a) {
    return ieee_ceil(a);
}

static jdouble StrictMath_rint(JNIEnv*, jclass, jdouble a) {
    return ieee_rint(a);
}

static jdouble StrictMath_pow(JNIEnv*, jclass, jdouble a, jdouble b) {
    return ieee_pow(a,b);
}

static jdouble StrictMath_hypot(JNIEnv*, jclass, jdouble a, jdouble b) {
    return ieee_hypot(a, b);
}

static jdouble StrictMath_nextafter(JNIEnv*, jclass, jdouble a, jdouble b) {
    return ieee_nextafter(a, b);
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(StrictMath, IEEEremainder, "!(DD)D"),
    NATIVE_METHOD(StrictMath, ceil, "!(D)D"),
    NATIVE_METHOD(StrictMath, cos, "!(D)D"),
    NATIVE_METHOD(StrictMath, floor, "!(D)D"),
    NATIVE_METHOD(StrictMath, hypot, "!(DD)D"),
    NATIVE_METHOD(StrictMath, nextafter, "!(DD)D"),
    NATIVE_METHOD(StrictMath, pow, "!(DD)D"),
    NATIVE_METHOD(StrictMath, rint, "!(D)D"),
    NATIVE_METHOD(StrictMath, sin, "!(D)D"),
    NATIVE_METHOD(StrictMath, sqrt, "!(D)D"),
    NATIVE_METHOD(StrictMath, tan, "!(D)D"),
};
void register_java_lang_StrictMath(JNIEnv* env) {
    jniRegisterNativeMethods(env, "java/lang/StrictMath", gMethods, NELEM(gMethods));
}
