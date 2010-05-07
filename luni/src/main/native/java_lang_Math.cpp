/*
 * Copyright 2006 The Android Open Source Project 
 *
 * Native functions for java.lang.Math.
 */
#include "jni.h"
#include "JNIHelp.h"

#include <stdlib.h>
#include <math.h>

/* native public static double sin(double a); */
static jdouble jsin(JNIEnv*, jclass, jdouble a)
{
    return sin(a);
}

/* native public static double cos(double a); */
static jdouble jcos(JNIEnv*, jclass, jdouble a)
{
    return cos(a);
}

/* native public static double tan(double a); */
static jdouble jtan(JNIEnv*, jclass, jdouble a)
{
    return tan(a);
}

/* native public static double asin(double a); */
static jdouble jasin(JNIEnv*, jclass, jdouble a)
{
    return asin(a);
}

/* native public static double acos(double a); */
static jdouble jacos(JNIEnv*, jclass, jdouble a)
{
    return acos(a);
}

/* native public static double atan(double a); */
static jdouble jatan(JNIEnv*, jclass, jdouble a)
{
    return atan(a);
}

/* native public static double exp(double a); */
static jdouble jexp(JNIEnv*, jclass, jdouble a)
{
    return exp(a);
}

/* native public static double log(double a); */
static jdouble jlog(JNIEnv*, jclass, jdouble a)
{
    return log(a);
}

/* native public static double sqrt(double a); */
static jdouble jsqrt(JNIEnv*, jclass, jdouble a)
{
    return sqrt(a);
}

/* native public static double IEEEremainder(double a, double b); */
static jdouble jieee_remainder(JNIEnv*, jclass, jdouble a, jdouble b)
{
    return remainder(a, b);
}

/* native public static double floor(double a); */
static jdouble jfloor(JNIEnv*, jclass, jdouble a)
{
    return floor(a);
}

/* native public static double ceil(double a); */
static jdouble jceil(JNIEnv*, jclass, jdouble a)
{
    return ceil(a);
}

/* native public static double rint(double a); */
static jdouble jrint(JNIEnv*, jclass, jdouble a)
{
    return rint(a);
}

/* native public static double atan2(double a, double b); */
static jdouble jatan2(JNIEnv*, jclass, jdouble a, jdouble b)
{
    return atan2(a, b);
}

/* native public static double pow(double a, double b); */
static jdouble jpow(JNIEnv*, jclass, jdouble a, jdouble b)
{
    return pow(a, b);
}

/* native public static double sinh(double a); */
static jdouble jsinh(JNIEnv*, jclass, jdouble a)
{
    return sinh(a);
}

/* native public static double tanh(double a); */
static jdouble jtanh(JNIEnv*, jclass, jdouble a)
{
    return tanh(a);
}

/* native public static double cosh(double a); */
static jdouble jcosh(JNIEnv*, jclass, jdouble a)
{
    return cosh(a);
}

/* native public static double log10(double a); */
static jdouble jlog10(JNIEnv*, jclass, jdouble a)
{
    return log10(a);
}

/* native public static double cbrt(double a); */
static jdouble jcbrt(JNIEnv*, jclass, jdouble a)
{
    return cbrt(a);
}

/* native public static double expm1(double a); */
static jdouble jexpm1(JNIEnv*, jclass, jdouble a)
{
    return expm1(a);
}

/* native public static double hypot(double a, double b); */
static jdouble jhypot(JNIEnv*, jclass, jdouble a, jdouble b)
{
    return hypot(a, b);
}

/* native public static double log1p(double a); */
static jdouble jlog1p(JNIEnv*, jclass, jdouble a)
{
    return log1p(a);
}

/* native public static double nextafter(double a, double b); */
static jdouble jnextafter(JNIEnv*, jclass, jdouble a, jdouble b)
{
    return nextafter(a, b);
}

/* native public static float nextafterf(float a, float b); */
static jfloat jnextafterf(JNIEnv*, jclass, jfloat a, jfloat b)
{
    return nextafterf(a, b);
}

static jdouble copySign(JNIEnv*, jclass, jdouble a, jdouble b) {
    // Our StrictMath.copySign delegates to Math.copySign, so we need to treat NaN as positive.
    return copysign(a, isnan(b) ? 1.0 : b);
}

static jfloat copySign_f(JNIEnv*, jclass, jfloat a, jfloat b) {
    // Our StrictMath.copySign delegates to Math.copySign, so we need to treat NaN as positive.
    return copysignf(a, isnan(b) ? 1.0 : b);
}

static JNINativeMethod gMethods[] = {
    { "IEEEremainder", "(DD)D", (void*)jieee_remainder },
    { "acos",          "(D)D",  (void*)jacos },
    { "asin",          "(D)D",  (void*)jasin },
    { "atan",          "(D)D",  (void*)jatan },
    { "atan2",         "(DD)D", (void*)jatan2 },
    { "cbrt",          "(D)D",  (void*)jcbrt },
    { "ceil",          "(D)D",  (void*)jceil },
    { "copySign",      "(DD)D", (void*)copySign },
    { "copySign",      "(FF)F", (void*)copySign_f },
    { "cos",           "(D)D",  (void*)jcos },
    { "cosh",          "(D)D",  (void*)jcosh },
    { "exp",           "(D)D",  (void*)jexp },
    { "expm1",         "(D)D",  (void*)jexpm1 },
    { "floor",         "(D)D",  (void*)jfloor },
    { "hypot",         "(DD)D", (void*)jhypot },
    { "log",           "(D)D",  (void*)jlog },
    { "log10",         "(D)D",  (void*)jlog10 },
    { "log1p",         "(D)D",  (void*)jlog1p },
    { "nextafter",     "(DD)D", (void*)jnextafter },
    { "nextafterf",    "(FF)F", (void*)jnextafterf },
    { "pow",           "(DD)D", (void*)jpow },
    { "rint",          "(D)D",  (void*)jrint },
    { "sin",           "(D)D",  (void*)jsin },
    { "sinh",          "(D)D",  (void*)jsinh },
    { "sqrt",          "(D)D",  (void*)jsqrt },
    { "tan",           "(D)D",  (void*)jtan },
    { "tanh",          "(D)D",  (void*)jtanh },
};

int register_java_lang_Math(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "java/lang/Math", gMethods, NELEM(gMethods));
}
