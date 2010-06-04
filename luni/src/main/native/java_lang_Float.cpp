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

typedef union {
    unsigned int    bits;
    float           f;
} Float;

#define NaN (0x7fc00000)

/*
 * Local helper function.
 */
static int IsNaN(unsigned bits)
{
    return ((bits >= 0x7f800001U && bits <= 0x7fffffffU)
        ||  (bits >= 0xff800001U && bits <= 0xffffffffU));
}

/*
 * public static native int floatToIntBits(float value)
 */
static jint floatToIntBits(JNIEnv*, jclass, jfloat val)
{
    Float   f;

    f.f = val;

    //  For this method all values in the NaN range are
    //  normalized to the canonical NaN value.

    if (IsNaN(f.bits))
        f.bits = NaN;

    return f.bits;
}

/*
 * public static native int floatToRawBits(float value)
 */
static jint floatToRawBits(JNIEnv*, jclass, jfloat val)
{
    Float   f;

    f.f = val;

    return f.bits;
}

/*
 * public static native float intBitsToFloat(int bits)
 */
static jfloat intBitsToFloat(JNIEnv*, jclass, jint val)
{
    Float   f;

    f.bits = val;

    return f.f;
}

static JNINativeMethod gMethods[] = {
    { "floatToIntBits",         "(F)I",     (void*)floatToIntBits },
    { "floatToRawIntBits",      "(F)I",     (void*)floatToRawBits },
    { "intBitsToFloat",         "(I)F",     (void*)intBitsToFloat },
};
int register_java_lang_Float(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "java/lang/Float", gMethods, NELEM(gMethods));
}
