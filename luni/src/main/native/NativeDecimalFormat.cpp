/*
 * Copyright (C) 2006 The Android Open Source Project
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

#define LOG_TAG "NativeDecimalFormat"

#include "ErrorCode.h"
#include "JNIHelp.h"
#include "ScopedJavaUnicodeString.h"
#include "ScopedPrimitiveArray.h"
#include "ScopedUtfChars.h"
#include "cutils/log.h"
#include "digitlst.h"
#include "unicode/decimfmt.h"
#include "unicode/fmtable.h"
#include "unicode/numfmt.h"
#include "unicode/unum.h"
#include "unicode/ustring.h"
#include "valueOf.h"
#include <stdlib.h>
#include <string.h>

static DecimalFormat* toDecimalFormat(jint addr) {
    return reinterpret_cast<DecimalFormat*>(static_cast<uintptr_t>(addr));
}

static DecimalFormatSymbols* makeDecimalFormatSymbols(JNIEnv* env,
        jstring currencySymbol0, jchar decimalSeparator, jchar digit,
        jchar groupingSeparator0, jstring infinity0,
        jstring internationalCurrencySymbol0, jchar minusSign,
        jchar monetaryDecimalSeparator, jstring nan0, jchar patternSeparator,
        jchar percent, jchar perMill, jchar zeroDigit) {
    ScopedJavaUnicodeString currencySymbol(env, currencySymbol0);
    ScopedJavaUnicodeString infinity(env, infinity0);
    ScopedJavaUnicodeString internationalCurrencySymbol(env, internationalCurrencySymbol0);
    ScopedJavaUnicodeString nan(env, nan0);
    UnicodeString groupingSeparator(groupingSeparator0);

    DecimalFormatSymbols* result = new DecimalFormatSymbols;
    result->setSymbol(DecimalFormatSymbols::kCurrencySymbol, currencySymbol.unicodeString());
    result->setSymbol(DecimalFormatSymbols::kDecimalSeparatorSymbol, UnicodeString(decimalSeparator));
    result->setSymbol(DecimalFormatSymbols::kDigitSymbol, UnicodeString(digit));
    result->setSymbol(DecimalFormatSymbols::kGroupingSeparatorSymbol, groupingSeparator);
    result->setSymbol(DecimalFormatSymbols::kMonetaryGroupingSeparatorSymbol, groupingSeparator);
    result->setSymbol(DecimalFormatSymbols::kInfinitySymbol, infinity.unicodeString());
    result->setSymbol(DecimalFormatSymbols::kIntlCurrencySymbol, internationalCurrencySymbol.unicodeString());
    result->setSymbol(DecimalFormatSymbols::kMinusSignSymbol, UnicodeString(minusSign));
    result->setSymbol(DecimalFormatSymbols::kMonetarySeparatorSymbol, UnicodeString(monetaryDecimalSeparator));
    result->setSymbol(DecimalFormatSymbols::kNaNSymbol, nan.unicodeString());
    result->setSymbol(DecimalFormatSymbols::kPatternSeparatorSymbol, UnicodeString(patternSeparator));
    result->setSymbol(DecimalFormatSymbols::kPercentSymbol, UnicodeString(percent));
    result->setSymbol(DecimalFormatSymbols::kPerMillSymbol, UnicodeString(perMill));
    result->setSymbol(DecimalFormatSymbols::kZeroDigitSymbol, UnicodeString(zeroDigit));
    return result;
}

static void setDecimalFormatSymbols(JNIEnv* env, jclass, jint addr,
        jstring currencySymbol, jchar decimalSeparator, jchar digit,
        jchar groupingSeparator, jstring infinity,
        jstring internationalCurrencySymbol, jchar minusSign,
        jchar monetaryDecimalSeparator, jstring nan, jchar patternSeparator,
        jchar percent, jchar perMill, jchar zeroDigit) {
    DecimalFormatSymbols* symbols = makeDecimalFormatSymbols(env,
            currencySymbol, decimalSeparator, digit, groupingSeparator,
            infinity, internationalCurrencySymbol, minusSign,
            monetaryDecimalSeparator, nan, patternSeparator, percent, perMill,
            zeroDigit);
    toDecimalFormat(addr)->adoptDecimalFormatSymbols(symbols);
}

static jint openDecimalFormatImpl(JNIEnv* env, jclass, jstring pattern0,
        jstring currencySymbol, jchar decimalSeparator, jchar digit,
        jchar groupingSeparator, jstring infinity,
        jstring internationalCurrencySymbol, jchar minusSign,
        jchar monetaryDecimalSeparator, jstring nan, jchar patternSeparator,
        jchar percent, jchar perMill, jchar zeroDigit) {
    if (pattern0 == NULL) {
        jniThrowNullPointerException(env, NULL);
        return 0;
    }
    UErrorCode status = U_ZERO_ERROR;
    UParseError parseError;
    ScopedJavaUnicodeString pattern(env, pattern0);
    DecimalFormatSymbols* symbols = makeDecimalFormatSymbols(env,
            currencySymbol, decimalSeparator, digit, groupingSeparator,
            infinity, internationalCurrencySymbol, minusSign,
            monetaryDecimalSeparator, nan, patternSeparator, percent, perMill,
            zeroDigit);
    DecimalFormat* fmt = new DecimalFormat(pattern.unicodeString(), symbols, parseError, status);
    if (fmt == NULL) {
        delete symbols;
    }
    icu4jni_error(env, status);
    return static_cast<jint>(reinterpret_cast<uintptr_t>(fmt));
}

static void closeDecimalFormatImpl(JNIEnv*, jclass, jint addr) {
    delete toDecimalFormat(addr);
}

static void setRoundingMode(JNIEnv*, jclass, jint addr, jint mode, jdouble increment) {
    DecimalFormat* fmt = toDecimalFormat(addr);
    fmt->setRoundingMode(static_cast<DecimalFormat::ERoundingMode>(mode));
    fmt->setRoundingIncrement(increment);
}

static void setSymbol(JNIEnv* env, jclass, jint addr, jint symbol, jstring s) {
    const UChar* chars = env->GetStringChars(s, NULL);
    const int32_t charCount = env->GetStringLength(s);
    UErrorCode status = U_ZERO_ERROR;
    UNumberFormat* fmt = reinterpret_cast<UNumberFormat*>(static_cast<uintptr_t>(addr));
    unum_setSymbol(fmt, static_cast<UNumberFormatSymbol>(symbol), chars, charCount, &status);
    icu4jni_error(env, status);
    env->ReleaseStringChars(s, chars);
}

static void setAttribute(JNIEnv*, jclass, jint addr, jint symbol,
        jint value) {

    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    unum_setAttribute(fmt, (UNumberFormatAttribute) symbol, value);
}

static jint getAttribute(JNIEnv*, jclass, jint addr, jint symbol) {

    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    int res = unum_getAttribute(fmt, (UNumberFormatAttribute) symbol);

    return res;
}

static void setTextAttribute(JNIEnv* env, jclass, jint addr, jint symbol,
        jstring text) {

    // the errorcode returned by unum_setTextAttribute
    UErrorCode status = U_ZERO_ERROR;

    // get the pointer to the number format
    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    const UChar *textChars = env->GetStringChars(text, NULL);
    int textLen = env->GetStringLength(text);

    unum_setTextAttribute(fmt, (UNumberFormatTextAttribute) symbol, textChars,
            textLen, &status);

    env->ReleaseStringChars(text, textChars);

    icu4jni_error(env, status);
}

static jstring getTextAttribute(JNIEnv* env, jclass, jint addr,
        jint symbol) {

    uint32_t resultlength, reslenneeded;

    // the errorcode returned by unum_getTextAttribute
    UErrorCode status = U_ZERO_ERROR;

    // get the pointer to the number format
    UNumberFormat *fmt = (UNumberFormat *)(int)addr;

    UChar* result = NULL;
    resultlength=0;

    // find out how long the result will be
    reslenneeded=unum_getTextAttribute(fmt, (UNumberFormatTextAttribute) symbol,
            result, resultlength, &status);

    result = NULL;
    if(status==U_BUFFER_OVERFLOW_ERROR) {
        status=U_ZERO_ERROR;
        resultlength=reslenneeded+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        reslenneeded=unum_getTextAttribute(fmt,
                (UNumberFormatTextAttribute) symbol, result, resultlength,
                &status);
    }
    if (icu4jni_error(env, status) != FALSE) {
        return NULL;
    }

    jstring res = env->NewString(result, reslenneeded);

    free(result);

    return res;
}

static void applyPatternImpl(JNIEnv* env, jclass, jint addr, jboolean localized, jstring pattern0) {
    if (pattern0 == NULL) {
        jniThrowNullPointerException(env, NULL);
        return;
    }
    ScopedJavaUnicodeString pattern(env, pattern0);
    DecimalFormat* fmt = toDecimalFormat(addr);
    UErrorCode status = U_ZERO_ERROR;
    if (localized) {
        fmt->applyLocalizedPattern(pattern.unicodeString(), status);
    } else {
        fmt->applyPattern(pattern.unicodeString(), status);
    }
    icu4jni_error(env, status);
}

static jstring toPatternImpl(JNIEnv* env, jclass, jint addr, jboolean localized) {
    DecimalFormat* fmt = toDecimalFormat(addr);
    UnicodeString pattern;
    if (localized) {
        fmt->toLocalizedPattern(pattern);
    } else {
        fmt->toPattern(pattern);
    }
    return env->NewString(pattern.getBuffer(), pattern.length());
}

static jstring formatResult(JNIEnv* env, const UnicodeString &str, FieldPositionIterator *fpi, jobject fpIter) {
    static jclass gFPIClass = env->FindClass("com/ibm/icu4jni/text/NativeDecimalFormat$FieldPositionIterator");
    static jmethodID gFPI_setData = env->GetMethodID(gFPIClass, "setData", "([I)V");

    if (fpi != NULL) {
        int len = fpi->getData(NULL, 0);
        jintArray iary;
        if (len) {
            iary = env->NewIntArray(len);
            ScopedIntArrayRW ints(env, iary);
            fpi->getData(ints.get(), len);
        } else {
            iary = NULL;
        }
        env->CallVoidMethod(fpIter, gFPI_setData, iary);
    }

    return env->NewString(str.getBuffer(), str.length());
}

template <typename T>
static jstring format(JNIEnv* env, jint addr, jobject fpIter, T val) {
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString str;
    DecimalFormat* fmt = toDecimalFormat(addr);
    FieldPositionIterator fpi;
    FieldPositionIterator *pfpi = fpIter ? &fpi : NULL;
    fmt->format(val, str, pfpi, status);
    return formatResult(env, str, pfpi, fpIter);
}

static jstring formatLong(JNIEnv* env, jclass, jint addr, jlong value, jobject fpIter) {
    int64_t longValue = value;
    return format(env, addr, fpIter, (jlong) longValue);
}

static jstring formatDouble(JNIEnv* env, jclass, jint addr, jdouble value, jobject fpIter) {
    double doubleValue = value;
    return format(env, addr, fpIter, (jdouble) doubleValue);
}

static jstring formatDigitList(JNIEnv* env, jclass, jint addr, jstring value, jobject fpIter) {
    ScopedUtfChars chars(env, value);
    if (chars.c_str() == NULL) {
        return NULL;
    }
    StringPiece sp(chars.c_str());
    return format(env, addr, fpIter, sp);
}

static jobject newBigDecimal(JNIEnv* env, const char* value, jsize len) {
    static jclass gBigDecimalClass = (jclass) env->NewGlobalRef(env->FindClass("java/math/BigDecimal"));
    static jmethodID gBigDecimal_init = env->GetMethodID(gBigDecimalClass, "<init>", "(Ljava/lang/String;)V");

    // this is painful...
    // value is a UTF-8 string of invariant characters, but isn't guaranteed to be
    // null-terminated.  NewStringUTF requires a terminated UTF-8 string.  So we copy the
    // data to jchars using UnicodeString, and call NewString instead.
    UnicodeString tmp(value, len, UnicodeString::kInvariant);
    jobject str = env->NewString(tmp.getBuffer(), tmp.length());
    return env->NewObject(gBigDecimalClass, gBigDecimal_init, str);
}

static jmethodID gPP_getIndex = NULL;
static jmethodID gPP_setIndex = NULL;
static jmethodID gPP_setErrorIndex = NULL;

static jobject parse(JNIEnv* env, jclass, jint addr, jstring text,
                     jobject position, jboolean parseBigDecimal) {

    if (gPP_getIndex == NULL) {
        jclass ppClass = env->FindClass("java/text/ParsePosition");
        gPP_getIndex = env->GetMethodID(ppClass, "getIndex", "()I");
        gPP_setIndex = env->GetMethodID(ppClass, "setIndex", "(I)V");
        gPP_setErrorIndex = env->GetMethodID(ppClass, "setErrorIndex", "(I)V");
    }

    // make sure the ParsePosition is valid. Actually icu4c would parse a number
    // correctly even if the parsePosition is set to -1, but since the RI fails
    // for that case we have to fail too
    int parsePos = env->CallIntMethod(position, gPP_getIndex, NULL);
    const int strlength = env->GetStringLength(text);
    if (parsePos < 0 || parsePos > strlength) {
        return NULL;
    }

    Formattable res;
    ParsePosition pp(parsePos);
    ScopedJavaUnicodeString src(env, text);
    DecimalFormat *fmt = toDecimalFormat(addr);
    fmt->parse(src.unicodeString(), res, pp);

    if (pp.getErrorIndex() == -1) {
        env->CallVoidMethod(position, gPP_setIndex, (jint) pp.getIndex());
    } else {
        env->CallVoidMethod(position, gPP_setErrorIndex, (jint) pp.getErrorIndex());
        return NULL;
    }

    if (parseBigDecimal) {
        UErrorCode status = U_ZERO_ERROR;
        StringPiece str = res.getDecimalNumber(status);
        if (U_SUCCESS(status)) {
            int len = str.length();
            const char* data = str.data();
            if (strncmp(data, "NaN", 3) == 0 ||
                strncmp(data, "Inf", 3) == 0 ||
                strncmp(data, "-Inf", 4) == 0) {
                double resultDouble = res.getDouble(status);
                return doubleValueOf(env, (jdouble) resultDouble);
            }
            return newBigDecimal(env, data, len);
        }
        return NULL;
    }

    Formattable::Type numType = res.getType();
        switch(numType) {
        case Formattable::kDouble: {
            double resultDouble = res.getDouble();
            return doubleValueOf(env, (jdouble) resultDouble);
        }
        case Formattable::kLong: {
            long resultLong = res.getLong();
            return longValueOf(env, (jlong) resultLong);
        }
        case Formattable::kInt64: {
            int64_t resultInt64 = res.getInt64();
            return longValueOf(env, (jlong) resultInt64);
        }
        default: {
            return NULL;
        }
    }
}

static jint cloneDecimalFormatImpl(JNIEnv*, jclass, jint addr) {
    DecimalFormat* fmt = toDecimalFormat(addr);
    return static_cast<jint>(reinterpret_cast<uintptr_t>(fmt->clone()));
}

static JNINativeMethod gMethods[] = {
      {"applyPatternImpl", "(IZLjava/lang/String;)V", (void*) applyPatternImpl},
      {"cloneDecimalFormatImpl", "(I)I", (void*) cloneDecimalFormatImpl},
      {"closeDecimalFormatImpl", "(I)V", (void*) closeDecimalFormatImpl},
      {"format", "(IDLcom/ibm/icu4jni/text/NativeDecimalFormat$FieldPositionIterator;)Ljava/lang/String;", (void*) formatDouble},
      {"format", "(IJLcom/ibm/icu4jni/text/NativeDecimalFormat$FieldPositionIterator;)Ljava/lang/String;", (void*) formatLong},
      {"format", "(ILjava/lang/String;Lcom/ibm/icu4jni/text/NativeDecimalFormat$FieldPositionIterator;)Ljava/lang/String;", (void*) formatDigitList},
      {"getAttribute", "(II)I", (void*) getAttribute},
      {"getTextAttribute", "(II)Ljava/lang/String;", (void*) getTextAttribute},
      {"openDecimalFormatImpl", "(Ljava/lang/String;Ljava/lang/String;CCCLjava/lang/String;Ljava/lang/String;CCLjava/lang/String;CCCC)I", (void*) openDecimalFormatImpl},
      {"parse", "(ILjava/lang/String;Ljava/text/ParsePosition;Z)Ljava/lang/Number;", (void*) parse},
      {"setAttribute", "(III)V", (void*) setAttribute},
      {"setDecimalFormatSymbols", "(ILjava/lang/String;CCCLjava/lang/String;Ljava/lang/String;CCLjava/lang/String;CCCC)V", (void*) setDecimalFormatSymbols},
      {"setSymbol", "(IILjava/lang/String;)V", (void*) setSymbol},
      {"setRoundingMode", "(IID)V", (void*) setRoundingMode},
      {"setTextAttribute", "(IILjava/lang/String;)V", (void*) setTextAttribute},
      {"toPatternImpl", "(IZ)Ljava/lang/String;", (void*) toPatternImpl},
};

int register_com_ibm_icu4jni_text_NativeDecimalFormat(JNIEnv* env) {
    return jniRegisterNativeMethods(env,
            "com/ibm/icu4jni/text/NativeDecimalFormat", gMethods,
            NELEM(gMethods));
}
