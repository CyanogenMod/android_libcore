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

#define LOG_TAG "Float"

#include "JNIHelp.h"

#include <math.h>
#include <stdlib.h>
#include <stdio.h>

union Float {
    unsigned int bits;
    float f;
};

static const jint NaN = 0x7fc00000;

static jint Float_floatToIntBits(JNIEnv*, jclass, jfloat val) {
    Float f;
    f.f = val;
    //  For this method all values in the NaN range are normalized to the canonical NaN value.
    return isnanf(f.f) ? NaN : f.bits;
}

jint Float_floatToRawBits(JNIEnv*, jclass, jfloat val) {
    Float f;
    f.f = val;
    return f.bits;
}

jfloat Float_intBitsToFloat(JNIEnv*, jclass, jint val) {
    Float f;
    f.bits = val;
    return f.f;
}

static JNINativeMethod gMethods[] = {
    { "floatToIntBits",         "(F)I",     (void*)Float_floatToIntBits },
    { "floatToRawIntBits",      "(F)I",     (void*)Float_floatToRawBits },
    { "intBitsToFloat",         "(I)F",     (void*)Float_intBitsToFloat },
};
int register_java_lang_Float(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "java/lang/Float", gMethods, NELEM(gMethods));
}
