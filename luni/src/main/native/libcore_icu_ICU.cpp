/*
 * Copyright (C) 2008 The Android Open Source Project
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

#define LOG_TAG "ICU"

#include "ErrorCode.h"
#include "JNIHelp.h"
#include "JniConstants.h"
#include "ScopedFd.h"
#include "ScopedJavaUnicodeString.h"
#include "ScopedLocalRef.h"
#include "ScopedUtfChars.h"
#include "UniquePtr.h"
#include "cutils/log.h"
#include "toStringArray.h"
#include "unicode/calendar.h"
#include "unicode/datefmt.h"
#include "unicode/dcfmtsym.h"
#include "unicode/decimfmt.h"
#include "unicode/dtfmtsym.h"
#include "unicode/gregocal.h"
#include "unicode/locid.h"
#include "unicode/numfmt.h"
#include "unicode/strenum.h"
#include "unicode/ubrk.h"
#include "unicode/ucal.h"
#include "unicode/uclean.h"
#include "unicode/ucol.h"
#include "unicode/ucurr.h"
#include "unicode/udat.h"
#include "unicode/ustring.h"
#include "ureslocs.h"
#include "valueOf.h"

#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <string>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>

class ScopedResourceBundle {
public:
    ScopedResourceBundle(UResourceBundle* bundle) : mBundle(bundle) {
    }

    ~ScopedResourceBundle() {
        if (mBundle != NULL) {
            ures_close(mBundle);
        }
    }

    UResourceBundle* get() {
        return mBundle;
    }

private:
    UResourceBundle* mBundle;

    // Disallow copy and assignment.
    ScopedResourceBundle(const ScopedResourceBundle&);
    void operator=(const ScopedResourceBundle&);
};

Locale getLocale(JNIEnv* env, jstring localeName) {
    return Locale::createFromName(ScopedUtfChars(env, localeName).c_str());
}

static jint ICU_getCurrencyFractionDigitsNative(JNIEnv* env, jclass, jstring javaCurrencyCode) {
    UErrorCode status = U_ZERO_ERROR;
    UniquePtr<NumberFormat> fmt(NumberFormat::createCurrencyInstance(status));
    if (U_FAILURE(status)) {
        return -1;
    }
    ScopedJavaUnicodeString currencyCode(env, javaCurrencyCode);
    fmt->setCurrency(currencyCode.unicodeString().getBuffer(), status);
    if (U_FAILURE(status)) {
        return -1;
    }
    // for CurrencyFormats the minimum and maximum fraction digits are the same.
    return fmt->getMinimumFractionDigits();
}

static jstring ICU_getCurrencyCodeNative(JNIEnv* env, jclass, jstring javaKey) {
    UErrorCode status = U_ZERO_ERROR;
    ScopedResourceBundle supplData(ures_openDirect(U_ICUDATA_CURR, "supplementalData", &status));
    if (U_FAILURE(status)) {
        return NULL;
    }

    ScopedResourceBundle currencyMap(ures_getByKey(supplData.get(), "CurrencyMap", NULL, &status));
    if (U_FAILURE(status)) {
        return NULL;
    }

    ScopedUtfChars key(env, javaKey);
    ScopedResourceBundle currency(ures_getByKey(currencyMap.get(), key.c_str(), NULL, &status));
    if (U_FAILURE(status)) {
        return NULL;
    }

    ScopedResourceBundle currencyElem(ures_getByIndex(currency.get(), 0, NULL, &status));
    if (U_FAILURE(status)) {
        return env->NewStringUTF("None");
    }

    // check if there is a 'to' date. If there is, the currency isn't used anymore.
    ScopedResourceBundle currencyTo(ures_getByKey(currencyElem.get(), "to", NULL, &status));
    if (!U_FAILURE(status)) {
        // return and let the caller throw an exception
        return NULL;
    }
    // We need to reset 'status'. It works like errno in that ICU doesn't set it
    // to U_ZERO_ERROR on success: it only touches it on error, and the test
    // above means it now holds a failure code.
    status = U_ZERO_ERROR;

    ScopedResourceBundle currencyId(ures_getByKey(currencyElem.get(), "id", NULL, &status));
    if (U_FAILURE(status)) {
        // No id defined for this country
        return env->NewStringUTF("None");
    }

    int length;
    const jchar* id = ures_getString(currencyId.get(), &length, &status);
    if (U_FAILURE(status) || length == 0) {
        return env->NewStringUTF("None");
    }
    return env->NewString(id, length);
}

static jstring ICU_getCurrencySymbolNative(JNIEnv* env, jclass, jstring locale, jstring currencyCode) {
    ScopedUtfChars localeName(env, locale);
    UErrorCode status = U_ZERO_ERROR;
    ScopedResourceBundle currLoc(ures_open(U_ICUDATA_CURR, localeName.c_str(), &status));
    if (U_FAILURE(status)) {
        return NULL;
    }

    ScopedResourceBundle currencies(ures_getByKey(currLoc.get(), "Currencies", NULL, &status));
    if (U_FAILURE(status)) {
        return NULL;
    }

    ScopedUtfChars currency(env, currencyCode);
    ScopedResourceBundle currencyElems(ures_getByKey(currencies.get(), currency.c_str(), NULL, &status));
    if (U_FAILURE(status)) {
        return NULL;
    }

    int currSymbL;
    const jchar* currSymbU = ures_getStringByIndex(currencyElems.get(), 0, &currSymbL, &status);
    if (U_FAILURE(status)) {
        return NULL;
    }

    return (currSymbL == 0) ? NULL : env->NewString(currSymbU, currSymbL);
}

static jstring ICU_getDisplayCountryNative(JNIEnv* env, jclass, jstring targetLocale, jstring locale) {
    Locale loc = getLocale(env, locale);
    Locale targetLoc = getLocale(env, targetLocale);
    UnicodeString str;
    targetLoc.getDisplayCountry(loc, str);
    return env->NewString(str.getBuffer(), str.length());
}

static jstring ICU_getDisplayLanguageNative(JNIEnv* env, jclass, jstring targetLocale, jstring locale) {
    Locale loc = getLocale(env, locale);
    Locale targetLoc = getLocale(env, targetLocale);
    UnicodeString str;
    targetLoc.getDisplayLanguage(loc, str);
    return env->NewString(str.getBuffer(), str.length());
}

static jstring ICU_getDisplayVariantNative(JNIEnv* env, jclass, jstring targetLocale, jstring locale) {
    Locale loc = getLocale(env, locale);
    Locale targetLoc = getLocale(env, targetLocale);
    UnicodeString str;
    targetLoc.getDisplayVariant(loc, str);
    return env->NewString(str.getBuffer(), str.length());
}

static jstring ICU_getISO3CountryNative(JNIEnv* env, jclass, jstring locale) {
    Locale loc = getLocale(env, locale);
    return env->NewStringUTF(loc.getISO3Country());
}

static jstring ICU_getISO3LanguageNative(JNIEnv* env, jclass, jstring locale) {
    Locale loc = getLocale(env, locale);
    return env->NewStringUTF(loc.getISO3Language());
}

static jobjectArray ICU_getISOCountriesNative(JNIEnv* env, jclass) {
    return toStringArray(env, Locale::getISOCountries());
}

static jobjectArray ICU_getISOLanguagesNative(JNIEnv* env, jclass) {
    return toStringArray(env, Locale::getISOLanguages());
}

static jobjectArray ICU_getAvailableLocalesNative(JNIEnv* env, jclass) {
    return toStringArray(env, uloc_countAvailable, uloc_getAvailable);
}

static jobjectArray ICU_getAvailableBreakIteratorLocalesNative(JNIEnv* env, jclass) {
    return toStringArray(env, ubrk_countAvailable, ubrk_getAvailable);
}

static jobjectArray ICU_getAvailableCalendarLocalesNative(JNIEnv* env, jclass) {
    return toStringArray(env, ucal_countAvailable, ucal_getAvailable);
}

static jobjectArray ICU_getAvailableCollatorLocalesNative(JNIEnv* env, jclass) {
    return toStringArray(env, ucol_countAvailable, ucol_getAvailable);
}

static jobjectArray ICU_getAvailableDateFormatLocalesNative(JNIEnv* env, jclass) {
    return toStringArray(env, udat_countAvailable, udat_getAvailable);
}

static jobjectArray ICU_getAvailableNumberFormatLocalesNative(JNIEnv* env, jclass) {
    return toStringArray(env, unum_countAvailable, unum_getAvailable);
}

static bool getDayIntVector(JNIEnv*, UResourceBundle* gregorian, int* values) {
    // get the First day of week and the minimal days in first week numbers
    UErrorCode status = U_ZERO_ERROR;
    ScopedResourceBundle gregorianElems(ures_getByKey(gregorian, "DateTimeElements", NULL, &status));
    if (U_FAILURE(status)) {
        return false;
    }

    int intVectSize;
    const int* result = ures_getIntVector(gregorianElems.get(), &intVectSize, &status);
    if (U_FAILURE(status) || intVectSize != 2) {
        return false;
    }

    values[0] = result[0];
    values[1] = result[1];
    return true;
}

// This allows you to leave extra space at the beginning or end of the array to support the
// month names and day names arrays.
static jobjectArray toStringArray(JNIEnv* env, UResourceBundle* rb, size_t size, int capacity, size_t offset) {
    if (capacity == -1) {
        capacity = size;
    }
    jobjectArray result = env->NewObjectArray(capacity, JniConstants::stringClass, NULL);
    if (result == NULL) {
        return NULL;
    }
    UErrorCode status = U_ZERO_ERROR;
    for (size_t i = 0; i < size; ++i) {
        int charCount;
        const jchar* chars = ures_getStringByIndex(rb, i, &charCount, &status);
        if (U_FAILURE(status)) {
            return NULL;
        }
        ScopedLocalRef<jstring> s(env, env->NewString(chars, charCount));
        if (env->ExceptionCheck()) {
            return NULL;
        }
        env->SetObjectArrayElement(result, offset + i, s.get());
        if (env->ExceptionCheck()) {
            return NULL;
        }
    }
    return result;
}

static jobjectArray getAmPmMarkers(JNIEnv* env, UResourceBundle* gregorian) {
    UErrorCode status = U_ZERO_ERROR;
    ScopedResourceBundle amPmMarkers(ures_getByKey(gregorian, "AmPmMarkers", NULL, &status));
    if (U_FAILURE(status)) {
        return NULL;
    }
    return toStringArray(env, amPmMarkers.get(), ures_getSize(amPmMarkers.get()), -1, 0);
}

static jobjectArray getEras(JNIEnv* env, UResourceBundle* gregorian) {
    UErrorCode status = U_ZERO_ERROR;
    ScopedResourceBundle eras(ures_getByKey(gregorian, "eras", NULL, &status));
    if (U_FAILURE(status)) {
        return NULL;
    }
    ScopedResourceBundle abbreviatedEras(ures_getByKey(eras.get(), "abbreviated", NULL, &status));
    if (U_FAILURE(status)) {
        return NULL;
    }
    return toStringArray(env, abbreviatedEras.get(), ures_getSize(abbreviatedEras.get()), -1, 0);
}

enum NameType { REGULAR, STAND_ALONE };
enum NameWidth { LONG, SHORT };
static jobjectArray getNames(JNIEnv* env, UResourceBundle* namesBundle, bool months, NameType type, NameWidth width) {
    const char* typeKey = (type == REGULAR) ? "format" : "stand-alone";
    const char* widthKey = (width == LONG) ? "wide" : "abbreviated";
    UErrorCode status = U_ZERO_ERROR;
    ScopedResourceBundle formatBundle(ures_getByKey(namesBundle, typeKey, NULL, &status));
    ScopedResourceBundle valuesBundle(ures_getByKey(formatBundle.get(), widthKey, NULL, &status));
    if (U_FAILURE(status)) {
        return NULL;
    }

    // The months array has a trailing empty string. The days array has a leading empty string.
    int count = ures_getSize(valuesBundle.get());
    int offset = months ? 0 : 1;
    jobjectArray result = toStringArray(env, valuesBundle.get(), count, count + 1, offset);
    env->SetObjectArrayElement(result, months ? count : 0, env->NewStringUTF(""));
    return result;
}

static void setIntegerField(JNIEnv* env, jobject obj, const char* fieldName, int value) {
    ScopedLocalRef<jobject> integerValue(env, integerValueOf(env, value));
    jfieldID fid = env->GetFieldID(JniConstants::localeDataClass, fieldName, "Ljava/lang/Integer;");
    env->SetObjectField(obj, fid, integerValue.get());
}

static void setStringField(JNIEnv* env, jobject obj, const char* fieldName, jstring value) {
    jfieldID fid = env->GetFieldID(JniConstants::localeDataClass, fieldName, "Ljava/lang/String;");
    env->SetObjectField(obj, fid, value);
}

static void setStringArrayField(JNIEnv* env, jobject obj, const char* fieldName, jobjectArray value) {
    jfieldID fid = env->GetFieldID(JniConstants::localeDataClass, fieldName, "[Ljava/lang/String;");
    env->SetObjectField(obj, fid, value);
}

static void setStringField(JNIEnv* env, jobject obj, const char* fieldName, UResourceBundle* bundle, int index) {
    UErrorCode status = U_ZERO_ERROR;
    int charCount;
    const UChar* chars = ures_getStringByIndex(bundle, index, &charCount, &status);
    if (U_SUCCESS(status)) {
        setStringField(env, obj, fieldName, env->NewString(chars, charCount));
    } else {
        LOGE("Error setting String field %s from ICU resource: %s", fieldName, u_errorName(status));
    }
}

static void setCharField(JNIEnv* env, jobject obj, const char* fieldName, UResourceBundle* bundle, int index) {
    UErrorCode status = U_ZERO_ERROR;
    int charCount;
    const UChar* chars = ures_getStringByIndex(bundle, index, &charCount, &status);
    if (U_SUCCESS(status)) {
        jfieldID fid = env->GetFieldID(JniConstants::localeDataClass, fieldName, "C");
        env->SetCharField(obj, fid, chars[0]);
    } else {
        LOGE("Error setting char field %s from ICU resource: %s", fieldName, u_errorName(status));
    }
}

static jboolean ICU_initLocaleDataImpl(JNIEnv* env, jclass, jstring locale, jobject localeData) {
    ScopedUtfChars localeName(env, locale);
    UErrorCode status = U_ZERO_ERROR;
    ScopedResourceBundle root(ures_open(NULL, localeName.c_str(), &status));
    if (U_FAILURE(status)) {
        LOGE("Error getting ICU resource bundle: %s", u_errorName(status));
        status = U_ZERO_ERROR;
        return JNI_FALSE;
    }

    ScopedResourceBundle calendar(ures_getByKey(root.get(), "calendar", NULL, &status));
    if (U_FAILURE(status)) {
        LOGE("Error getting ICU calendar resource bundle: %s", u_errorName(status));
        return JNI_FALSE;
    }

    ScopedResourceBundle gregorian(ures_getByKey(calendar.get(), "gregorian", NULL, &status));
    if (U_FAILURE(status)) {
        LOGE("Error getting ICU gregorian resource bundle: %s", u_errorName(status));
        return JNI_FALSE;
    }

    int firstDayVals[] = { 0, 0 };
    if (getDayIntVector(env, gregorian.get(), firstDayVals)) {
        setIntegerField(env, localeData, "firstDayOfWeek", firstDayVals[0]);
        setIntegerField(env, localeData, "minimalDaysInFirstWeek", firstDayVals[1]);
    }

    setStringArrayField(env, localeData, "amPm", getAmPmMarkers(env, gregorian.get()));
    setStringArrayField(env, localeData, "eras", getEras(env, gregorian.get()));

    ScopedResourceBundle dayNames(ures_getByKey(gregorian.get(), "dayNames", NULL, &status));
    ScopedResourceBundle monthNames(ures_getByKey(gregorian.get(), "monthNames", NULL, &status));

    // Get the regular month and weekday names.
    jobjectArray longMonthNames = getNames(env, monthNames.get(), true, REGULAR, LONG);
    jobjectArray shortMonthNames = getNames(env, monthNames.get(), true, REGULAR, SHORT);
    jobjectArray longWeekdayNames = getNames(env, dayNames.get(), false, REGULAR, LONG);
    jobjectArray shortWeekdayNames = getNames(env, dayNames.get(), false, REGULAR, SHORT);
    setStringArrayField(env, localeData, "longMonthNames", longMonthNames);
    setStringArrayField(env, localeData, "shortMonthNames", shortMonthNames);
    setStringArrayField(env, localeData, "longWeekdayNames", longWeekdayNames);
    setStringArrayField(env, localeData, "shortWeekdayNames", shortWeekdayNames);

    // Get the stand-alone month and weekday names. If they're not available (as they aren't for
    // English), we reuse the regular names. If we returned null to Java, the usual fallback
    // mechanisms would come into play and we'd end up with the bogus stand-alone names from the
    // root locale ("1" for January, and so on).
    jobjectArray longStandAloneMonthNames = getNames(env, monthNames.get(), true, STAND_ALONE, LONG);
    if (longStandAloneMonthNames == NULL) {
        longStandAloneMonthNames = longMonthNames;
    }
    jobjectArray shortStandAloneMonthNames = getNames(env, monthNames.get(), true, STAND_ALONE, SHORT);
    if (shortStandAloneMonthNames == NULL) {
        shortStandAloneMonthNames = shortMonthNames;
    }
    jobjectArray longStandAloneWeekdayNames = getNames(env, dayNames.get(), false, STAND_ALONE, LONG);
    if (longStandAloneWeekdayNames == NULL) {
        longStandAloneWeekdayNames = longWeekdayNames;
    }
    jobjectArray shortStandAloneWeekdayNames = getNames(env, dayNames.get(), false, STAND_ALONE, SHORT);
    if (shortStandAloneWeekdayNames == NULL) {
        shortStandAloneWeekdayNames = shortWeekdayNames;
    }
    setStringArrayField(env, localeData, "longStandAloneMonthNames", longStandAloneMonthNames);
    setStringArrayField(env, localeData, "shortStandAloneMonthNames", shortStandAloneMonthNames);
    setStringArrayField(env, localeData, "longStandAloneWeekdayNames", longStandAloneWeekdayNames);
    setStringArrayField(env, localeData, "shortStandAloneWeekdayNames", shortStandAloneWeekdayNames);

    ScopedResourceBundle dateTimePatterns(ures_getByKey(gregorian.get(), "DateTimePatterns", NULL, &status));
    if (U_SUCCESS(status)) {
        setStringField(env, localeData, "fullTimeFormat", dateTimePatterns.get(), 0);
        setStringField(env, localeData, "longTimeFormat", dateTimePatterns.get(), 1);
        setStringField(env, localeData, "mediumTimeFormat", dateTimePatterns.get(), 2);
        setStringField(env, localeData, "shortTimeFormat", dateTimePatterns.get(), 3);
        setStringField(env, localeData, "fullDateFormat", dateTimePatterns.get(), 4);
        setStringField(env, localeData, "longDateFormat", dateTimePatterns.get(), 5);
        setStringField(env, localeData, "mediumDateFormat", dateTimePatterns.get(), 6);
        setStringField(env, localeData, "shortDateFormat", dateTimePatterns.get(), 7);
    }
    status = U_ZERO_ERROR;

    ScopedResourceBundle numberElements(ures_getByKey(root.get(), "NumberElements", NULL, &status));
    if (U_SUCCESS(status) && ures_getSize(numberElements.get()) >= 11) {
        setCharField(env, localeData, "zeroDigit", numberElements.get(), 4);
        setCharField(env, localeData, "digit", numberElements.get(), 5);
        setCharField(env, localeData, "decimalSeparator", numberElements.get(), 0);
        setCharField(env, localeData, "groupingSeparator", numberElements.get(), 1);
        setCharField(env, localeData, "patternSeparator", numberElements.get(), 2);
        setCharField(env, localeData, "percent", numberElements.get(), 3);
        setCharField(env, localeData, "perMill", numberElements.get(), 8);
        setCharField(env, localeData, "monetarySeparator", numberElements.get(), 0);
        setCharField(env, localeData, "minusSign", numberElements.get(), 6);
        setStringField(env, localeData, "exponentSeparator", numberElements.get(), 7);
        setStringField(env, localeData, "infinity", numberElements.get(), 9);
        setStringField(env, localeData, "NaN", numberElements.get(), 10);
    }
    status = U_ZERO_ERROR;

    jstring countryCode = env->NewStringUTF(Locale::createFromName(localeName.c_str()).getCountry());
    jstring internationalCurrencySymbol = ICU_getCurrencyCodeNative(env, NULL, countryCode);
    jstring currencySymbol = NULL;
    if (internationalCurrencySymbol != NULL) {
        currencySymbol = ICU_getCurrencySymbolNative(env, NULL, locale, internationalCurrencySymbol);
    } else {
        internationalCurrencySymbol = env->NewStringUTF("XXX");
    }
    if (currencySymbol == NULL) {
        // This is the UTF-8 encoding of U+00A4 (CURRENCY SIGN).
        currencySymbol = env->NewStringUTF("\xc2\xa4");
    }
    setStringField(env, localeData, "currencySymbol", currencySymbol);
    setStringField(env, localeData, "internationalCurrencySymbol", internationalCurrencySymbol);

    ScopedResourceBundle numberPatterns(ures_getByKey(root.get(), "NumberPatterns", NULL, &status));
    if (U_SUCCESS(status) && ures_getSize(numberPatterns.get()) >= 3) {
        setStringField(env, localeData, "numberPattern", numberPatterns.get(), 0);
        setStringField(env, localeData, "currencyPattern", numberPatterns.get(), 1);
        setStringField(env, localeData, "percentPattern", numberPatterns.get(), 2);
    }

    return JNI_TRUE;
}

static jstring ICU_toLowerCase(JNIEnv* env, jclass, jstring javaString, jstring localeName) {
    ScopedJavaUnicodeString scopedString(env, javaString);
    UnicodeString& s(scopedString.unicodeString());
    UnicodeString original(s);
    s.toLower(Locale::createFromName(ScopedUtfChars(env, localeName).c_str()));
    return s == original ? javaString : env->NewString(s.getBuffer(), s.length());
}

static jstring ICU_toUpperCase(JNIEnv* env, jclass, jstring javaString, jstring localeName) {
    ScopedJavaUnicodeString scopedString(env, javaString);
    UnicodeString& s(scopedString.unicodeString());
    UnicodeString original(s);
    s.toUpper(Locale::createFromName(ScopedUtfChars(env, localeName).c_str()));
    return s == original ? javaString : env->NewString(s.getBuffer(), s.length());
}

static jstring versionString(JNIEnv* env, const UVersionInfo& version) {
    char versionString[U_MAX_VERSION_STRING_LENGTH];
    u_versionToString(const_cast<UVersionInfo&>(version), &versionString[0]);
    return env->NewStringUTF(versionString);
}

static jstring ICU_getIcuVersion(JNIEnv* env, jclass) {
    UVersionInfo icuVersion;
    u_getVersion(icuVersion);
    return versionString(env, icuVersion);
}

static jstring ICU_getUnicodeVersion(JNIEnv* env, jclass) {
    UVersionInfo unicodeVersion;
    u_getUnicodeVersion(unicodeVersion);
    return versionString(env, unicodeVersion);
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(ICU, getAvailableBreakIteratorLocalesNative, "()[Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getAvailableCalendarLocalesNative, "()[Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getAvailableCollatorLocalesNative, "()[Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getAvailableDateFormatLocalesNative, "()[Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getAvailableLocalesNative, "()[Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getAvailableNumberFormatLocalesNative, "()[Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getCurrencyCodeNative, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getCurrencyFractionDigitsNative, "(Ljava/lang/String;)I"),
    NATIVE_METHOD(ICU, getCurrencySymbolNative, "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getDisplayCountryNative, "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getDisplayLanguageNative, "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getDisplayVariantNative, "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getISO3CountryNative, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getISO3LanguageNative, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getISOCountriesNative, "()[Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getISOLanguagesNative, "()[Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getIcuVersion, "()Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getUnicodeVersion, "()Ljava/lang/String;"),
    NATIVE_METHOD(ICU, initLocaleDataImpl, "(Ljava/lang/String;Llibcore/icu/LocaleData;)Z"),
    NATIVE_METHOD(ICU, toLowerCase, "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, toUpperCase, "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
};
int register_libcore_icu_ICU(JNIEnv* env) {
    std::string path;
    path = u_getDataDirectory();
    path += "/";
    path += U_ICUDATA_NAME;
    path += ".dat";

    #define FAIL_WITH_STRERROR(s) \
        LOGE("Couldn't " s " '%s': %s", path.c_str(), strerror(errno)); \
        return -1;
    #define MAYBE_FAIL_WITH_ICU_ERROR(s) \
        if (status != U_ZERO_ERROR) {\
            LOGE("Couldn't initialize ICU (" s "): %s (%s)", u_errorName(status), path.c_str()); \
            return -1; \
        }

    // Open the file and get its length.
    ScopedFd fd(open(path.c_str(), O_RDONLY));
    if (fd.get() == -1) {
        FAIL_WITH_STRERROR("open");
    }
    struct stat sb;
    if (fstat(fd.get(), &sb) == -1) {
        FAIL_WITH_STRERROR("stat");
    }

    // Map it.
    void* data = mmap(NULL, sb.st_size, PROT_READ, MAP_SHARED, fd.get(), 0);
    if (data == MAP_FAILED) {
        FAIL_WITH_STRERROR("mmap");
    }

    // Tell the kernel that accesses are likely to be random rather than sequential.
    if (madvise(data, sb.st_size, MADV_RANDOM) == -1) {
        FAIL_WITH_STRERROR("madvise(MADV_RANDOM)");
    }

    // Tell ICU to use our memory-mapped data.
    UErrorCode status = U_ZERO_ERROR;
    udata_setCommonData(data, &status);
    MAYBE_FAIL_WITH_ICU_ERROR("udata_setCommonData");
    // Tell ICU it can *only* use our memory-mapped data.
    udata_setFileAccess(UDATA_NO_FILES, &status);
    MAYBE_FAIL_WITH_ICU_ERROR("udata_setFileAccess");

    // Failures to find the ICU data tend to be somewhat obscure because ICU loads its data on first
    // use, which can be anywhere. Force initialization up front so we can report a nice clear error
    // and bail.
    u_init(&status);
    MAYBE_FAIL_WITH_ICU_ERROR("u_init");
    return jniRegisterNativeMethods(env, "libcore/icu/ICU", gMethods, NELEM(gMethods));
}
