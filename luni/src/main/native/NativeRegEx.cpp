/*
 * Copyright (C) 2007 The Android Open Source Project
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

#define LOG_TAG "NativeRegEx"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "JNIHelp.h"
#include "JniConstants.h"
#include "ScopedJavaUnicodeString.h"
#include "ScopedPrimitiveArray.h"
#include "UniquePtr.h"
#include "jni.h"
#include "unicode/parseerr.h"
#include "unicode/uregex.h"
#include "unicode/utypes.h"

static jchar EMPTY_STRING = 0;

/**
 * A data structure that ties together an ICU regular expression and the
 * character data it refers to (but does not have a copy of), so we can
 * manage memory properly.
 */
struct RegExData {
    RegExData() : regex(NULL), text(NULL) {
    }

    ~RegExData() {
        uregex_close(regex);
        if (text != &EMPTY_STRING) {
            delete[] text;
        }
    }

    // A pointer to the ICU regular expression
    URegularExpression* regex;
    // A pointer to (a copy of) the input text that *we* manage
    jchar* text;
};

static void throwPatternSyntaxException(JNIEnv* env, UErrorCode status,
                                        jstring pattern, UParseError error)
{
    jmethodID method = env->GetMethodID(JniConstants::patternSyntaxExceptionClass,
            "<init>", "(Ljava/lang/String;Ljava/lang/String;I)V");
    jstring message = env->NewStringUTF(u_errorName(status));
    jobject exception = env->NewObject(JniConstants::patternSyntaxExceptionClass, method,
            message, pattern, error.offset);
    env->Throw(reinterpret_cast<jthrowable>(exception));
}

static void throwRuntimeException(JNIEnv* env, UErrorCode status) {
    jniThrowRuntimeException(env, u_errorName(status));
}

static void NativeRegEx_close(JNIEnv*, jclass, RegExData* data) {
    delete data;
}

static RegExData* NativeRegEx_open(JNIEnv* env, jclass clazz, jstring javaPattern, jint flags) {
    flags = flags | UREGEX_ERROR_ON_UNKNOWN_ESCAPES;

    UErrorCode status = U_ZERO_ERROR;
    UParseError error;
    error.offset = -1;

    ScopedJavaUnicodeString pattern(env, javaPattern);
    UnicodeString& patternString(pattern.unicodeString());
    UniquePtr<RegExData> data(new RegExData);
    data->regex = uregex_open(patternString.getBuffer(), patternString.length(), flags, &error, &status);
    if (!U_SUCCESS(status)) {
        throwPatternSyntaxException(env, status, javaPattern, error);
        return NULL;
    }

    return data.release();
}

static RegExData* NativeRegEx_clone(JNIEnv* env, jclass, RegExData* data) {
    UErrorCode status = U_ZERO_ERROR;
    URegularExpression* clonedRegex = uregex_clone(data->regex, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
        return NULL;
    }

    RegExData* result = new RegExData;
    result->regex = clonedRegex;
    return result;
}

static void NativeRegEx_setText(JNIEnv* env, jclass, RegExData* data, jstring text) {
    UErrorCode status = U_ZERO_ERROR;

    uregex_setText(data->regex, &EMPTY_STRING, 0, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
        return;
    }

    if (data->text != &EMPTY_STRING) {
        delete[] data->text;
        data->text = NULL;
    }

    int textLen = env->GetStringLength(text);
    if (textLen == 0) {
        data->text = &EMPTY_STRING;
    } else {
        data->text = new jchar[textLen + 1];
        env->GetStringRegion(text, 0, textLen, data->text);
        data->text[textLen] = 0;
    }

    uregex_setText(data->regex, data->text, textLen, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
}

static jboolean NativeRegEx_matches(JNIEnv* env, jclass, RegExData* data, jint startIndex) {
    UErrorCode status = U_ZERO_ERROR;
    jboolean result = uregex_matches(data->regex, startIndex, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
    return result;
}

static jboolean NativeRegEx_lookingAt(JNIEnv* env, jclass, RegExData* data, jint startIndex) {
    UErrorCode status = U_ZERO_ERROR;
    jboolean result = uregex_lookingAt(data->regex, startIndex, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
    return result;
}

static jboolean NativeRegEx_find(JNIEnv* env, jclass, RegExData* data, jint startIndex) {
    UErrorCode status = U_ZERO_ERROR;
    jboolean result = uregex_find(data->regex, startIndex, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
    return result;
}

static jboolean NativeRegEx_findNext(JNIEnv* env, jclass, RegExData* data) {
    UErrorCode status = U_ZERO_ERROR;
    jboolean result = uregex_findNext(data->regex, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
    return result;
}

static jint NativeRegEx_groupCount(JNIEnv* env, jclass, RegExData* data) {
    UErrorCode status = U_ZERO_ERROR;
    jint result = uregex_groupCount(data->regex, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
    return result;
}

static void NativeRegEx_startEnd(JNIEnv* env, jclass, RegExData* data, jintArray javaOffsets) {
    UErrorCode status = U_ZERO_ERROR;
    ScopedIntArrayRW offsets(env, javaOffsets);
    int groupCount = uregex_groupCount(data->regex, &status);
    for (int i = 0; i <= groupCount && U_SUCCESS(status); i++) {
        offsets[2 * i + 0] = uregex_start(data->regex, i, &status);
        offsets[2 * i + 1] = uregex_end(data->regex, i, &status);
    }
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
}

static void NativeRegEx_setRegion(JNIEnv* env, jclass, RegExData* data, jint start, jint end) {
    UErrorCode status = U_ZERO_ERROR;
    uregex_setRegion(data->regex, start, end, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
}

static jint NativeRegEx_regionStart(JNIEnv* env, jclass, RegExData* data) {
    UErrorCode status = U_ZERO_ERROR;
    int result = uregex_regionStart(data->regex, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
    return result;
}

static jint NativeRegEx_regionEnd(JNIEnv* env, jclass, RegExData* data) {
    UErrorCode status = U_ZERO_ERROR;
    int result = uregex_regionEnd(data->regex, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
    return result;
}

static void NativeRegEx_useTransparentBounds(JNIEnv* env, jclass, RegExData* data, jboolean value) {
    UErrorCode status = U_ZERO_ERROR;
    uregex_useTransparentBounds(data->regex, value, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
}

static jboolean NativeRegEx_hasTransparentBounds(JNIEnv* env, jclass, RegExData* data) {
    UErrorCode status = U_ZERO_ERROR;
    jboolean result = uregex_hasTransparentBounds(data->regex, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
    return result;
}

static void NativeRegEx_useAnchoringBounds(JNIEnv* env, jclass, RegExData* data, jboolean value) {
    UErrorCode status = U_ZERO_ERROR;
    uregex_useAnchoringBounds(data->regex, value, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
}

static jboolean NativeRegEx_hasAnchoringBounds(JNIEnv* env, jclass, RegExData* data) {
    UErrorCode status = U_ZERO_ERROR;
    jboolean result = uregex_hasAnchoringBounds(data->regex, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
    return result;
}

static jboolean NativeRegEx_hitEnd(JNIEnv* env, jclass, RegExData* data) {
    UErrorCode status = U_ZERO_ERROR;
    jboolean result = uregex_hitEnd(data->regex, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
    return result;
}

static jboolean NativeRegEx_requireEnd(JNIEnv* env, jclass, RegExData* data) {
    UErrorCode status = U_ZERO_ERROR;
    jboolean result = uregex_requireEnd(data->regex, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
    return result;
}

static void NativeRegEx_reset(JNIEnv* env, jclass, RegExData* data, jint position) {
    UErrorCode status = U_ZERO_ERROR;
    uregex_reset(data->regex, position, &status);
    if (!U_SUCCESS(status)) {
        throwRuntimeException(env, status);
    }
}

static JNINativeMethod gMethods[] = {
    { "open",                 "(Ljava/lang/String;I)I", (void*) NativeRegEx_open },
    { "clone",                "(I)I",                   (void*) NativeRegEx_clone },
    { "close",                "(I)V",                   (void*) NativeRegEx_close },
    { "setText",              "(ILjava/lang/String;)V", (void*) NativeRegEx_setText },
    { "matches",              "(II)Z",                  (void*) NativeRegEx_matches },
    { "lookingAt",            "(II)Z",                  (void*) NativeRegEx_lookingAt },
    { "find",                 "(II)Z",                  (void*) NativeRegEx_find },
    { "findNext",             "(I)Z",                   (void*) NativeRegEx_findNext },
    { "groupCount",           "(I)I",                   (void*) NativeRegEx_groupCount },
    { "startEnd",             "(I[I)V",                 (void*) NativeRegEx_startEnd },
    { "setRegion",            "(III)V",                 (void*) NativeRegEx_setRegion },
    { "regionStart",          "(I)I",                   (void*) NativeRegEx_regionStart },
    { "regionEnd",            "(I)I",                   (void*) NativeRegEx_regionEnd },
    { "useTransparentBounds", "(IZ)V",                  (void*) NativeRegEx_useTransparentBounds },
    { "hasTransparentBounds", "(I)Z",                   (void*) NativeRegEx_hasTransparentBounds },
    { "useAnchoringBounds",   "(IZ)V",                  (void*) NativeRegEx_useAnchoringBounds },
    { "hasAnchoringBounds",   "(I)Z",                   (void*) NativeRegEx_hasAnchoringBounds },
    { "hitEnd",               "(I)Z",                   (void*) NativeRegEx_hitEnd },
    { "requireEnd",           "(I)Z",                   (void*) NativeRegEx_requireEnd },
    { "reset",                "(II)V",                  (void*) NativeRegEx_reset },
};
int register_com_ibm_icu4jni_regex_NativeRegEx(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "com/ibm/icu4jni/regex/NativeRegEx",
            gMethods, NELEM(gMethods));
}
