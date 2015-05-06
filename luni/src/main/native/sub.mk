# -*- mode: makefile -*-
# This file is included by the top-level libcore Android.mk.
# It's not a normal makefile, so we don't include CLEAR_VARS
# or BUILD_*_LIBRARY.

LOCAL_SRC_FILES := \
    AsynchronousCloseMonitor.cpp \
    ExecStrings.cpp \
    IcuUtilities.cpp \
    JniException.cpp \
    NetworkUtilities.cpp \
    Register.cpp \
    ZipUtilities.cpp \
    android_system_OsConstants.cpp \
    canonicalize_path.cpp \
    cbigint.cpp \
    java_lang_Character.cpp \
    java_lang_Double.cpp \
    java_lang_Float.cpp \
    java_lang_Math.cpp \
    java_lang_ProcessManager.cpp \
    java_lang_RealToString.cpp \
    java_lang_StrictMath.cpp \
    java_lang_StringToReal.cpp \
    java_lang_System.cpp \
    java_math_NativeBN.cpp \
    java_nio_ByteOrder.cpp \
    java_nio_charset_Charsets.cpp \
    java_util_jar_StrictJarFile.cpp \
    libcore_icu_AlphabeticIndex.cpp \
    libcore_icu_DateIntervalFormat.cpp \
    libcore_icu_ICU.cpp \
    libcore_icu_NativeBreakIterator.cpp \
    libcore_icu_NativeCollation.cpp \
    libcore_icu_NativeIDN.cpp \
    libcore_icu_NativeNormalizer.cpp \
    libcore_icu_NativePluralRules.cpp \
    libcore_icu_TimeZoneNames.cpp \
    libcore_icu_Transliterator.cpp \
    libcore_io_AsynchronousCloseMonitor.cpp \
    libcore_io_Memory.cpp \
    libcore_io_Posix.cpp \
    org_apache_harmony_xml_ExpatParser.cpp \
    readlink.cpp \
    sun_misc_Unsafe.cpp \
    valueOf.cpp \

LOCAL_C_INCLUDES += \
    external/icu/icu4c/source/common \
    external/icu/icu4c/source/i18n \
    external/openssl/include \
    external/zlib \
    system/core/include \

LOCAL_STATIC_LIBRARIES += \
    libfdlibm \

LOCAL_SHARED_LIBRARIES += \
    liblog \
    libnativehelper \
