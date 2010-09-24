/*
 * Copyright (C) 2005 The Android Open Source Project
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

#define LOG_TAG "Double"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "java_lang_Double.h"

#include <math.h>
#include <stdlib.h>
#include <stdio.h>

static const jlong NaN = 0x7ff8000000000000ULL;

static jlong Double_doubleToLongBits(JNIEnv*, jclass, jdouble doubleValue) {
    //  For this method all values in the NaN range are normalized to the canonical NaN value.
    return isnan(doubleValue) ? NaN : Double::doubleToRawLongBits(doubleValue);
}

static jlong Double_doubleToRawLongBits(JNIEnv*, jclass, jdouble doubleValue) {
    return Double::doubleToRawLongBits(doubleValue);
}

static jdouble Double_longBitsToDouble(JNIEnv*, jclass, jlong bits) {
    return Double::longBitsToDouble(bits);
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(Double, doubleToLongBits, "(D)J"),
    NATIVE_METHOD(Double, doubleToRawLongBits, "(D)J"),
    NATIVE_METHOD(Double, longBitsToDouble, "(J)D"),
};
int register_java_lang_Double(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "java/lang/Double", gMethods, NELEM(gMethods));
}
