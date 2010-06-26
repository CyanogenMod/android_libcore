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

#define LOG_TAG "Matcher"

#include <stdlib.h>

#include "ErrorCode.h"
#include "JNIHelp.h"
#include "JniConstants.h"
#include "ScopedJavaUnicodeString.h"
#include "ScopedPrimitiveArray.h"
#include "UniquePtr.h"
#include "jni.h"
#include "unicode/parseerr.h"
#include "unicode/regex.h"

// ICU documentation: http://icu-project.org/apiref/icu4c/classRegexMatcher.html

// Copies the Java char[] onto the native heap so it doesn't move under our ICU RegexMatcher's feet.
class MatcherAndText {
public:
    MatcherAndText(RegexMatcher* m, const UnicodeString& t) : matcher(m), text(t) {
    }

    UniquePtr<RegexMatcher> matcher;
    UnicodeString text;

private:
    // Disallow copy and assignment.
    MatcherAndText(const MatcherAndText&);
    void operator=(const MatcherAndText&);
};

static RegexMatcher* toRegexMatcher(jint addr) {
    //return reinterpret_cast<RegexMatcher*>(static_cast<uintptr_t>(addr));
    return reinterpret_cast<MatcherAndText*>(static_cast<uintptr_t>(addr))->matcher.get();
}

static void updateOffsets(JNIEnv* env, RegexMatcher* matcher, jintArray javaOffsets) {
    ScopedIntArrayRW offsets(env, javaOffsets);
    UErrorCode status = U_ZERO_ERROR;
    for (size_t i = 0, groupCount = matcher->groupCount(); i <= groupCount; ++i) {
        offsets[2*i + 0] = matcher->start(i, status);
        offsets[2*i + 1] = matcher->end(i, status);
    }
}

static void RegexMatcher_closeImpl(JNIEnv*, jclass, jint addr) {
    //delete toRegexMatcher(addr);
    delete reinterpret_cast<MatcherAndText*>(static_cast<uintptr_t>(addr));
}

static jint RegexMatcher_findImpl(JNIEnv* env, jclass, jint addr, jint startIndex, jintArray offsets) {
    UErrorCode status = U_ZERO_ERROR;
    RegexMatcher* matcher = toRegexMatcher(addr);
    UBool result = matcher->find(startIndex, status);
    updateOffsets(env, matcher, offsets);
    icu4jni_error(env, status);
    return result;
}

static jint RegexMatcher_findNextImpl(JNIEnv* env, jclass, jint addr, jintArray offsets) {
    RegexMatcher* matcher = toRegexMatcher(addr);
    UBool result = matcher->find();
    updateOffsets(env, matcher, offsets);
    return result;
}

static jint RegexMatcher_groupCountImpl(JNIEnv*, jclass, jint addr) {
    return toRegexMatcher(addr)->groupCount();
}

static jint RegexMatcher_hitEndImpl(JNIEnv*, jclass, jint addr) {
    return toRegexMatcher(addr)->hitEnd();
}

static jint RegexMatcher_lookingAtImpl(JNIEnv* env, jclass, jint addr, jintArray offsets) {
    UErrorCode status = U_ZERO_ERROR;
    RegexMatcher* matcher = toRegexMatcher(addr);
    UBool result = matcher->lookingAt(status);
    updateOffsets(env, matcher, offsets);
    icu4jni_error(env, status);
    return result;
}

static jint RegexMatcher_matchesImpl(JNIEnv* env, jclass, jint addr, jintArray offsets) {
    UErrorCode status = U_ZERO_ERROR;
    RegexMatcher* matcher = toRegexMatcher(addr);
    UBool result = matcher->matches(status);
    updateOffsets(env, matcher, offsets);
    icu4jni_error(env, status);
    return result;
}

static jint RegexMatcher_openImpl(JNIEnv* env, jclass, jint patternAddr) {
    RegexPattern* pattern = reinterpret_cast<RegexPattern*>(static_cast<uintptr_t>(patternAddr));
    UErrorCode status = U_ZERO_ERROR;
    RegexMatcher* result = pattern->matcher(status);
    icu4jni_error(env, status);
    //return static_cast<jint>(reinterpret_cast<uintptr_t>(result));

    if (result == NULL) {
        return 0;
    }
    return static_cast<jint>(reinterpret_cast<uintptr_t>(new MatcherAndText(result, "")));
}

static jint RegexMatcher_requireEndImpl(JNIEnv*, jclass, jint addr) {
    return toRegexMatcher(addr)->requireEnd();
}

static void RegexMatcher_setInputImpl(JNIEnv* env, jclass, jint addr, jstring s, jint start, jint end) {
    // Copy the char[] from the jstring onto the native heap.
    MatcherAndText* mat = reinterpret_cast<MatcherAndText*>(static_cast<uintptr_t>(addr));
    mat->text = ScopedJavaUnicodeString(env, s).unicodeString();

    RegexMatcher* matcher = toRegexMatcher(addr);
    matcher->reset(mat->text);
    UErrorCode status = U_ZERO_ERROR;
    toRegexMatcher(addr)->region(start, end, status);
    icu4jni_error(env, status);
}

static void RegexMatcher_useAnchoringBoundsImpl(JNIEnv*, jclass, jint addr, jboolean value) {
    toRegexMatcher(addr)->useAnchoringBounds(value);
}

static void RegexMatcher_useTransparentBoundsImpl(JNIEnv*, jclass, jint addr, jboolean value) {
    toRegexMatcher(addr)->useTransparentBounds(value);
}

static JNINativeMethod gMethods[] = {
    { "closeImpl", "(I)V", (void*) RegexMatcher_closeImpl },
    { "findImpl", "(II[I)Z", (void*) RegexMatcher_findImpl },
    { "findNextImpl", "(I[I)Z", (void*) RegexMatcher_findNextImpl },
    { "groupCountImpl", "(I)I", (void*) RegexMatcher_groupCountImpl },
    { "hitEndImpl", "(I)Z", (void*) RegexMatcher_hitEndImpl },
    { "lookingAtImpl", "(I[I)Z", (void*) RegexMatcher_lookingAtImpl },
    { "matchesImpl", "(I[I)Z", (void*) RegexMatcher_matchesImpl },
    { "openImpl", "(I)I", (void*) RegexMatcher_openImpl },
    { "requireEndImpl", "(I)Z", (void*) RegexMatcher_requireEndImpl },
    { "setInputImpl", "(ILjava/lang/String;II)V", (void*) RegexMatcher_setInputImpl },
    { "useAnchoringBoundsImpl", "(IZ)V", (void*) RegexMatcher_useAnchoringBoundsImpl },
    { "useTransparentBoundsImpl", "(IZ)V", (void*) RegexMatcher_useTransparentBoundsImpl },
};
int register_java_util_regex_Matcher(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "java/util/regex/Matcher", gMethods, NELEM(gMethods));
}
