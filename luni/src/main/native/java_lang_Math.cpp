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

#define LOG_TAG "Math"

#include "jni.h"
#include "JNIHelp.h"

#include <stdlib.h>
#include <math.h>

static jdouble Math_sin(JNIEnv*, jclass, jdouble a) {
    return sin(a);
}

static jdouble Math_cos(JNIEnv*, jclass, jdouble a) {
    return cos(a);
}

static jdouble Math_tan(JNIEnv*, jclass, jdouble a) {
    return tan(a);
}

static jdouble Math_asin(JNIEnv*, jclass, jdouble a) {
    return asin(a);
}

static jdouble Math_acos(JNIEnv*, jclass, jdouble a) {
    return acos(a);
}

static jdouble Math_atan(JNIEnv*, jclass, jdouble a) {
    return atan(a);
}

static jdouble Math_exp(JNIEnv*, jclass, jdouble a) {
    return exp(a);
}

static jdouble Math_log(JNIEnv*, jclass, jdouble a) {
    return log(a);
}

static jdouble Math_sqrt(JNIEnv*, jclass, jdouble a) {
    return sqrt(a);
}

static jdouble Math_IEEEremainder(JNIEnv*, jclass, jdouble a, jdouble b) {
    return remainder(a, b);
}

static jdouble Math_floor(JNIEnv*, jclass, jdouble a) {
    return floor(a);
}

static jdouble Math_ceil(JNIEnv*, jclass, jdouble a) {
    return ceil(a);
}

static jdouble Math_rint(JNIEnv*, jclass, jdouble a) {
    return rint(a);
}

static jdouble Math_atan2(JNIEnv*, jclass, jdouble a, jdouble b) {
    return atan2(a, b);
}

static jdouble Math_pow(JNIEnv*, jclass, jdouble a, jdouble b) {
    return pow(a, b);
}

static jdouble Math_sinh(JNIEnv*, jclass, jdouble a) {
    return sinh(a);
}

static jdouble Math_tanh(JNIEnv*, jclass, jdouble a) {
    return tanh(a);
}

static jdouble Math_cosh(JNIEnv*, jclass, jdouble a) {
    return cosh(a);
}

static jdouble Math_log10(JNIEnv*, jclass, jdouble a) {
    return log10(a);
}

static jdouble Math_cbrt(JNIEnv*, jclass, jdouble a) {
    return cbrt(a);
}

static jdouble Math_expm1(JNIEnv*, jclass, jdouble a) {
    return expm1(a);
}

static jdouble Math_hypot(JNIEnv*, jclass, jdouble a, jdouble b) {
    return hypot(a, b);
}

static jdouble Math_log1p(JNIEnv*, jclass, jdouble a) {
    return log1p(a);
}

static jdouble Math_nextafter(JNIEnv*, jclass, jdouble a, jdouble b) {
    return nextafter(a, b);
}

static jfloat Math_nextafterf(JNIEnv*, jclass, jfloat a, jfloat b) {
    return nextafterf(a, b);
}

static jdouble Math_copySign(JNIEnv*, jclass, jdouble a, jdouble b) {
    // Our StrictMath.copySign delegates to Math.copySign, so we need to treat NaN as positive.
    return copysign(a, isnan(b) ? 1.0 : b);
}

static jfloat Math_copySign_f(JNIEnv*, jclass, jfloat a, jfloat b) {
    // Our StrictMath.copySign delegates to Math.copySign, so we need to treat NaN as positive.
    return copysignf(a, isnan(b) ? 1.0 : b);
}

static JNINativeMethod gMethods[] = {
    { "IEEEremainder", "(DD)D", (void*) Math_IEEEremainder },
    { "acos",          "(D)D",  (void*) Math_acos },
    { "asin",          "(D)D",  (void*) Math_asin },
    { "atan",          "(D)D",  (void*) Math_atan },
    { "atan2",         "(DD)D", (void*) Math_atan2 },
    { "cbrt",          "(D)D",  (void*) Math_cbrt },
    { "ceil",          "(D)D",  (void*) Math_ceil },
    { "copySign",      "(DD)D", (void*) Math_copySign },
    { "copySign",      "(FF)F", (void*) Math_copySign_f },
    { "cos",           "(D)D",  (void*) Math_cos },
    { "cosh",          "(D)D",  (void*) Math_cosh },
    { "exp",           "(D)D",  (void*) Math_exp },
    { "expm1",         "(D)D",  (void*) Math_expm1 },
    { "floor",         "(D)D",  (void*) Math_floor },
    { "hypot",         "(DD)D", (void*) Math_hypot },
    { "log",           "(D)D",  (void*) Math_log },
    { "log10",         "(D)D",  (void*) Math_log10 },
    { "log1p",         "(D)D",  (void*) Math_log1p },
    { "nextafter",     "(DD)D", (void*) Math_nextafter },
    { "nextafterf",    "(FF)F", (void*) Math_nextafterf },
    { "pow",           "(DD)D", (void*) Math_pow },
    { "rint",          "(D)D",  (void*) Math_rint },
    { "sin",           "(D)D",  (void*) Math_sin },
    { "sinh",          "(D)D",  (void*) Math_sinh },
    { "sqrt",          "(D)D",  (void*) Math_sqrt },
    { "tan",           "(D)D",  (void*) Math_tan },
    { "tanh",          "(D)D",  (void*) Math_tanh },
};

int register_java_lang_Math(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "java/lang/Math", gMethods, NELEM(gMethods));
}
