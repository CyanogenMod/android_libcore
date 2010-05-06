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

#ifndef SCOPED_GLOBAL_REF_H_included
#define SCOPED_GLOBAL_REF_H_included

#include "JNIHelp.h"

// A smart pointer that provides access to a JNI global reference
class ScopedGlobalRef {
public:
    ScopedGlobalRef(JNIEnv* env, jobject localRef)
    : mEnv(env), mGlobalRef(NULL)
    {
        mGlobalRef = env->NewGlobalRef(localRef);
    }

    ~ScopedGlobalRef() {
        reset();
    }

    void reset() {
        if (mGlobalRef != NULL) {
            mEnv->DeleteGlobalRef(mGlobalRef);
            mGlobalRef = NULL;
        }
    }

    jobject get () const {
        return mGlobalRef;
    }

private:
    JNIEnv* mEnv;
    jobject mGlobalRef;

    // Disallow copy and assignment.
    ScopedGlobalRef(const ScopedGlobalRef&);
    void operator=(const ScopedGlobalRef&);
};

#endif  // SCOPED_GLOBAL_REF_H_included
