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

#define LOG_TAG "DateIntervalFormat"

#include "IcuUtilities.h"
#include "JniConstants.h"
#include "ScopedJavaUnicodeString.h"
#include "UniquePtr.h"
#include "cutils/log.h"
#include "unicode/dtitvfmt.h"

static jstring DateIntervalFormat_formatDateInterval(JNIEnv* env, jclass, jstring javaSkeleton, jstring javaLocaleName, jlong fromDate, jlong toDate) {
  Locale locale = getLocale(env, javaLocaleName);

  ScopedJavaUnicodeString skeletonHolder(env, javaSkeleton);
  if (!skeletonHolder.valid()) {
    return NULL;
  }

  UErrorCode status = U_ZERO_ERROR;
  UniquePtr<DateIntervalFormat> formatter(DateIntervalFormat::createInstance(skeletonHolder.unicodeString(), locale, status));
  if (maybeThrowIcuException(env, "DateIntervalFormat::createInstance", status)) {
    return NULL;
  }

  DateInterval date_interval(fromDate, toDate);

  UnicodeString result;
  FieldPosition pos = 0;
  formatter->format(&date_interval, result, pos, status);
  if (maybeThrowIcuException(env, "DateIntervalFormat::format", status)) {
      return NULL;
  }

  return env->NewString(result.getBuffer(), result.length());
}

static JNINativeMethod gMethods[] = {
  NATIVE_METHOD(DateIntervalFormat, formatDateInterval, "(Ljava/lang/String;Ljava/lang/String;JJ)Ljava/lang/String;"),
};
void register_libcore_icu_DateIntervalFormat(JNIEnv* env) {
  jniRegisterNativeMethods(env, "libcore/icu/DateIntervalFormat", gMethods, NELEM(gMethods));
}
