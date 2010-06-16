/*
 * Copyright (C) 2010 The Android Open Source Project
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

#define LOG_TAG "valueOf"

#include "valueOf.h"
#include "JNIHelp.h"

jobject booleanValueOf(JNIEnv * env, jboolean value) {
    static jclass c = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Boolean"));
    static jmethodID valueOfMethod = env->GetStaticMethodID(c, "valueOf", "(Z)Ljava/lang/Boolean;");
    return env->CallStaticObjectMethod(c, valueOfMethod, value);
}

jobject doubleValueOf(JNIEnv* env, jdouble value) {
    static jclass c = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Double"));
    static jmethodID valueOfMethod = env->GetStaticMethodID(c, "valueOf", "(D)Ljava/lang/Double;");
    return env->CallStaticObjectMethod(c, valueOfMethod, value);
}

jobject integerValueOf(JNIEnv* env, jint value) {
    static jclass c = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Integer"));
    static jmethodID valueOfMethod = env->GetStaticMethodID(c, "valueOf", "(I)Ljava/lang/Integer;");
    return env->CallStaticObjectMethod(c, valueOfMethod, value);
}

jobject longValueOf(JNIEnv* env, jlong value) {
    static jclass c = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Long"));
    static jmethodID valueOfMethod = env->GetStaticMethodID(c, "valueOf", "(J)Ljava/lang/Long;");
    return env->CallStaticObjectMethod(c, valueOfMethod, value);
}
