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
    java_lang_StringToReal.cpp \
    java_math_NativeBN.cpp \
    java_nio_ByteOrder.cpp \
    java_util_jar_StrictJarFile.cpp \
    java_util_regex_Matcher.cpp \
    java_util_regex_Pattern.cpp \
    libcore_icu_AlphabeticIndex.cpp \
    libcore_icu_ICU.cpp \
    libcore_icu_NativeCollation.cpp \
    libcore_icu_NativeConverter.cpp \
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

LOCAL_STATIC_LIBRARIES += \
    libfdlibm \

LOCAL_SHARED_LIBRARIES += \
    liblog \
    libnativehelper \
