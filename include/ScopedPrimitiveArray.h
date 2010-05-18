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

#ifndef SCOPED_PRIMITIVE_ARRAY_H_included
#define SCOPED_PRIMITIVE_ARRAY_H_included

#include "JNIHelp.h"

// ScopedBooleanArray, ScopedByteArray, ScopedCharArray, ScopedDoubleArray, ScopedFloatArray,
// ScopedIntArray, ScopedLongArray, and ScopedShortArray provide convenient read-only access to
// Java arrays from JNI code.
#define INSTANTIATE_SCOPED_PRIMITIVE_ARRAY(PRIMITIVE_TYPE, NAME) \
    class Scoped ## NAME ## Array { \
    public: \
        Scoped ## NAME ## Array(JNIEnv* env, PRIMITIVE_TYPE ## Array javaArray) \
        : mEnv(env), mJavaArray(javaArray), mRawArray(NULL) { \
            mRawArray = mEnv->Get ## NAME ## ArrayElements(mJavaArray, NULL); \
        } \
        ~Scoped ## NAME ## Array() { \
            if (mRawArray) { \
                mEnv->Release ## NAME ## ArrayElements(mJavaArray, mRawArray, JNI_ABORT); \
            } \
        } \
        const PRIMITIVE_TYPE* get() const { \
            return mRawArray; \
        } \
        const PRIMITIVE_TYPE& operator[](size_t n) const { \
            return mRawArray[n]; \
        } \
        size_t size() const { \
            return mEnv->GetArrayLength(mJavaArray); \
        } \
    private: \
        JNIEnv* mEnv; \
        PRIMITIVE_TYPE ## Array mJavaArray; \
        PRIMITIVE_TYPE* mRawArray; \
        Scoped ## NAME ## Array(const Scoped ## NAME ## Array&); \
        void operator=(const Scoped ## NAME ## Array&); \
    }

INSTANTIATE_SCOPED_PRIMITIVE_ARRAY(jboolean, Boolean);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY(jbyte, Byte);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY(jchar, Char);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY(jdouble, Double);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY(jfloat, Float);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY(jint, Int);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY(jlong, Long);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY(jshort, Short);

#undef INSTANTIATE_SCOPED_PRIMITIVE_ARRAY

#endif  // SCOPED_PRIMITIVE_ARRAY_H_included
