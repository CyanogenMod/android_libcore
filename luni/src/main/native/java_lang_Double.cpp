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

#include <math.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>

typedef union {
    uint64_t    bits;
    double      d;
} Double;

#define NaN (0x7ff8000000000000ULL)

/*
 * public static native long doubleToLongBits(double value)
 */
static jlong doubleToLongBits(JNIEnv*, jclass, jdouble val)
{
    Double   d;

    d.d = val;

    //  For this method all values in the NaN range are
    //  normalized to the canonical NaN value.

    if (isnan(d.d))
        d.bits = NaN;

    return d.bits;
}

/*
 * public static native long doubleToRawLongBits(double value)
 */
static jlong doubleToRawLongBits(JNIEnv*, jclass, jdouble val)
{
    Double   d;

    d.d = val;

    return d.bits;
}

/*
 * public static native double longBitsToDouble(long bits)
 */
static jdouble longBitsToDouble(JNIEnv*, jclass, jlong val)
{
    Double   d;

    d.bits = val;

    return d.d;
}

static JNINativeMethod gMethods[] = {
  { "doubleToLongBits",       "(D)J",     (void*)doubleToLongBits },
  { "doubleToRawLongBits",    "(D)J",     (void*)doubleToRawLongBits },
  { "longBitsToDouble",       "(J)D",     (void*)longBitsToDouble },
};
int register_java_lang_Double(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "java/lang/Double", gMethods, NELEM(gMethods));
}
