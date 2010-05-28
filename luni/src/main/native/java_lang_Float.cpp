//
//  java_lang_Float.c
//  Android
//
//  Copyright 2005 The Android Open Source Project
//
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
