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

#define LOG_TAG "NativeIDN"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "ScopedStringChars.h"
#include "unicode/uidna.h"

static bool isLabelSeparator(const UChar ch) {
    switch (ch) {
    case 0x3002: // ideographic full stop
    case 0xff0e: // fullwidth full stop
    case 0xff61: // halfwidth ideographic full stop
        return true;
    default:
        return false;
    }
}

static jstring NativeIDN_convertImpl(JNIEnv* env, jclass, jstring javaSrc, jint flags, jboolean toAscii) {
    ScopedStringChars src(env, javaSrc);
    if (src.get() == NULL) {
        return NULL;
    }
    UChar dst[256];
    UErrorCode status = U_ZERO_ERROR;

    // We're stuck implementing IDNA-2003 for now since that's what we specify.
    //
    // TODO: Change our spec to IDNA-2008 + UTS-46 compatibility processing if
    // it's safe enough.
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
    size_t resultLength = toAscii
        ? uidna_IDNToASCII(src.get(), src.size(), &dst[0], sizeof(dst), flags, NULL, &status)
        : uidna_IDNToUnicode(src.get(), src.size(), &dst[0], sizeof(dst), flags, NULL, &status);
#pragma GCC diagnostic pop

    if (U_FAILURE(status)) {
        jniThrowException(env, "java/lang/IllegalArgumentException", u_errorName(status));
        return NULL;
    }
    if (!toAscii) {
        // ICU only translates separators to ASCII for toASCII.
        // Java expects the translation for toUnicode too.
        // We may as well do this here, while the string is still mutable.
        for (size_t i = 0; i < resultLength; ++i) {
            if (isLabelSeparator(dst[i])) {
                dst[i] = '.';
            }
        }
    }
    return env->NewString(&dst[0], resultLength);
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(NativeIDN, convertImpl, "(Ljava/lang/String;IZ)Ljava/lang/String;"),
};
void register_libcore_icu_NativeIDN(JNIEnv* env) {
    jniRegisterNativeMethods(env, "libcore/icu/NativeIDN", gMethods, NELEM(gMethods));
}
