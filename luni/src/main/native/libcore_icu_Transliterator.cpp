/*
 * Copyright (C) 2013 The Android Open Source Project
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

#define LOG_TAG "Transliterator"

#include "IcuUtilities.h"
#include "JNIHelp.h"
#include "JniConstants.h"
#include "JniException.h"
#include "ScopedJavaUnicodeString.h"
#include "ScopedStringChars.h"
#include "unicode/translit.h"

extern jobjectArray fromStringEnumeration(JNIEnv* env, StringEnumeration*);

static jobjectArray Transliterator_getAvailableIDs(JNIEnv* env, jclass) {
  UErrorCode status = U_ZERO_ERROR;
  return fromStringEnumeration(env, Transliterator::getAvailableIDs(status));
}

static jstring Transliterator_transliterate(JNIEnv* env, jclass, jstring javaId, jstring javaString) {
  ScopedJavaUnicodeString id(env, javaId);
  if (!id.valid()) {
    return NULL;
  }
  ScopedJavaUnicodeString string(env, javaString);
  if (!string.valid()) {
    return NULL;
  }

  UErrorCode status = U_ZERO_ERROR;
  Transliterator* t = Transliterator::createInstance(id.unicodeString(), UTRANS_FORWARD, status);
  if (maybeThrowIcuException(env, "Transliterator::createInstance", status)) {
    return NULL;
  }

  UnicodeString& s(string.unicodeString());
  t->transliterate(s);

  return env->NewString(s.getBuffer(), s.length());
}

static JNINativeMethod gMethods[] = {
  NATIVE_METHOD(Transliterator, getAvailableIDs, "()[Ljava/lang/String;"),
  NATIVE_METHOD(Transliterator, transliterate, "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
};
void register_libcore_icu_Transliterator(JNIEnv* env) {
  jniRegisterNativeMethods(env, "libcore/icu/Transliterator", gMethods, NELEM(gMethods));
}
