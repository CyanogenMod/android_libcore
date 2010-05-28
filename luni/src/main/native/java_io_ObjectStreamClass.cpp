/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#include "JNIHelp.h"

static jobject java_io_osc_getFieldSignature(JNIEnv* env, jclass,
                                                  jobject reflectField) {
    jclass lookupClass;
    jmethodID mid;

    lookupClass = env->FindClass("java/lang/reflect/Field");
    if(!lookupClass) {
        return NULL;
    }

    mid = env->GetMethodID(lookupClass, "getSignature",
            "()Ljava/lang/String;");
    if(!mid)
    {
        return NULL;
    }

    jclass fieldClass = env->GetObjectClass(reflectField);
    
    return env->CallNonvirtualObjectMethod(reflectField, 
            fieldClass, mid);
}

static jobject java_io_osc_getMethodSignature(JNIEnv* env, jclass,
                                                   jobject reflectMethod)
{
    jclass lookupClass;
    jmethodID mid;

    lookupClass = env->FindClass("java/lang/reflect/Method");
    if(!lookupClass) {
        return NULL;
    }

    mid = env->GetMethodID(lookupClass, "getSignature",
            "()Ljava/lang/String;");
    if(!mid) {
        return NULL;
    }
  
    jclass methodClass = env->GetObjectClass(reflectMethod);
    return env->CallNonvirtualObjectMethod(reflectMethod, 
            methodClass, mid);
}

static jobject java_io_osc_getConstructorSignature(JNIEnv* env,
                                                        jclass,
                                                        jobject
                                                        reflectConstructor)
{
    jclass lookupClass;
    jmethodID mid;

    lookupClass = env->FindClass("java/lang/reflect/Constructor");
    if(!lookupClass) {
        return NULL;
    }

    mid = env->GetMethodID(lookupClass, "getSignature",
            "()Ljava/lang/String;");
    if(!mid) {
        return NULL;
    }

    jclass constructorClass = env->GetObjectClass(reflectConstructor);
    return env->CallNonvirtualObjectMethod(reflectConstructor,
                                             constructorClass, mid);
}

static jboolean java_io_osc_hasClinit(JNIEnv * env, jclass, jclass targetClass) {
    jmethodID mid = env->GetStaticMethodID(targetClass, "<clinit>", "()V");
    env->ExceptionClear();
    return (mid != 0);
}

static JNINativeMethod gMethods[] = {
    { "getConstructorSignature", "(Ljava/lang/reflect/Constructor;)Ljava/lang/String;", (void*) java_io_osc_getConstructorSignature },
    { "getFieldSignature", "(Ljava/lang/reflect/Field;)Ljava/lang/String;", (void*) java_io_osc_getFieldSignature },
    { "getMethodSignature", "(Ljava/lang/reflect/Method;)Ljava/lang/String;", (void*) java_io_osc_getMethodSignature },
    { "hasClinit", "(Ljava/lang/Class;)Z", (void*) java_io_osc_hasClinit },
};
int register_java_io_ObjectStreamClass(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "java/io/ObjectStreamClass", gMethods, NELEM(gMethods));
}
