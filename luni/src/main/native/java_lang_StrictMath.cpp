/*
 * Copyright 2006 The Android Open Source Project 
 *
 * Native functions for java.lang.StrictMath.
 */
#include "jni.h"
#include "JNIHelp.h"

#include <stdlib.h>
/* This static way is the "best" way to integrate fdlibm without a conflict
 * into the android environment.
 */

#if defined(__P)
#undef __P
#endif /* defined(__P) */

extern "C" {
#include "../../external/fdlibm/fdlibm.h"
}
_LIB_VERSION_TYPE _LIB_VERSION = _IEEE_;

/* native public static double sin(double a); */
static jdouble jsin(JNIEnv*, jclass, jdouble a)
{
    return ieee_sin(a);
}

/* native public static double cos(double a); */
static jdouble jcos(JNIEnv*, jclass, jdouble a)
{
    return ieee_cos(a);
}

/* native public static double tan(double a); */
static jdouble jtan(JNIEnv*, jclass, jdouble a)
{
    return ieee_tan(a);
}

/* native public static double asin(double a); */
static jdouble jasin(JNIEnv*, jclass, jdouble a)
{
    return ieee_asin(a);
}

/* native public static double acos(double a); */
static jdouble jacos(JNIEnv*, jclass, jdouble a)
{
    return ieee_acos(a);
}

/* native public static double atan(double a); */
static jdouble jatan(JNIEnv*, jclass, jdouble a)
{
    return ieee_atan(a);
}

/* native public static double exp(double a); */
static jdouble jexp(JNIEnv*, jclass, jdouble a)
{
    return ieee_exp(a);
}

/* native public static double log(double a); */
static jdouble jlog(JNIEnv*, jclass, jdouble a)
{
    return ieee_log(a);
}

/* native public static double sqrt(double a); */
static jdouble jsqrt2(JNIEnv*, jclass, jdouble a)
{
    return ieee_sqrt(a);
}

/* native public static double IEEEremainder(double a, double b); */
static jdouble jieee_remainder(JNIEnv*, jclass, jdouble a, jdouble b)
{
    return ieee_remainder(a, b);
}

/* native public static double floor(double a); */
static jdouble jfloor(JNIEnv*, jclass, jdouble a)
{
    return ieee_floor(a);
}

/* native public static double ceil(double a); */
static jdouble jceil(JNIEnv*, jclass, jdouble a)
{
    return ieee_ceil(a);
}

/* native public static double rint(double a); */
static jdouble jrint(JNIEnv*, jclass, jdouble a)
{
    return ieee_rint(a);
}

/* native public static double atan2(double a, double b); */
static jdouble jatan2(JNIEnv*, jclass, jdouble a, jdouble b)
{
    return ieee_atan2(a, b);
}

/* native public static double pow(double a, double b); */
static jdouble jpow(JNIEnv*, jclass, jdouble a, jdouble b)
{
    return ieee_pow(a,b);
}

/* native public static double sinh(double a); */
static jdouble jsinh(JNIEnv*, jclass, jdouble a)
{
    return ieee_sinh(a);
}

/* native public static double tanh(double a); */
static jdouble jtanh(JNIEnv*, jclass, jdouble a)
{
    return ieee_tanh(a);
}

/* native public static double cosh(double a); */
static jdouble jcosh(JNIEnv*, jclass, jdouble a)
{
    return ieee_cosh(a);
}

/* native public static double log10(double a); */
static jdouble jlog10(JNIEnv*, jclass, jdouble a)
{
    return ieee_log10(a);
}

/* native public static double cbrt(double a); */
static jdouble jcbrt(JNIEnv*, jclass, jdouble a)
{
    return ieee_cbrt(a);
}

/* native public static double expm1(double a); */
static jdouble jexpm1(JNIEnv*, jclass, jdouble a)
{
    return ieee_expm1(a);
}

/* native public static double hypot(double a, double b); */
static jdouble jhypot(JNIEnv*, jclass, jdouble a, jdouble b)
{
    return ieee_hypot(a, b);
}

/* native public static double log1p(double a); */
static jdouble jlog1p(JNIEnv*, jclass, jdouble a)
{
    return ieee_log1p(a);
}

/* native public static double nextafter(double a, double b); */
static jdouble jnextafter(JNIEnv*, jclass, jdouble a, jdouble b)
{
    return ieee_nextafter(a, b);
}

/* native public static float nextafterf(float a, float b); */
static jfloat jnextafterf(JNIEnv*, jclass, jfloat arg1, jfloat arg2)
{
    jint hx = *(jint*)&arg1;
    jint hy = *(jint*)&arg2;

    if (!(hx&0x7fffffff)) { /* arg1 == 0 */
      *(jint*)&arg1 = (hy & 0x80000000) | 0x1;
      return arg1;
    }

    if((hx > 0) ^ (hx > hy)) { /* |arg1| < |arg2| */
        hx += 1;
    } else {
        hx -= 1;
    }
    *(jint*)&arg1 = hx;
    return arg1;
}

static JNINativeMethod gMethods[] = {
    { "IEEEremainder", "(DD)D", (void*)jieee_remainder },
    { "acos",          "(D)D",  (void*)jacos },
    { "asin",          "(D)D",  (void*)jasin },
    { "atan",          "(D)D",  (void*)jatan },
    { "atan2",         "(DD)D", (void*)jatan2 },
    { "cbrt",          "(D)D",  (void*)jcbrt },
    { "ceil",          "(D)D",  (void*)jceil },
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
    { "sqrt",          "(D)D",  (void*)jsqrt2 },
    { "tan",           "(D)D",  (void*)jtan },
    { "tanh",          "(D)D",  (void*)jtanh },
};

int register_java_lang_StrictMath(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "java/lang/StrictMath", gMethods, NELEM(gMethods));
}
