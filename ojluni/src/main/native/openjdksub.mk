# -*- mode: makefile -*-
# This file is included by the top-level libcore Android.mk.
# It's not a normal makefile, so we don't include CLEAR_VARS
# or BUILD_*_LIBRARY.

srcdir := ojluni/src/main/native
LOCAL_SRC_FILES := \
    java_util_zip_ZipFile.c \
    java_util_zip_Inflater.c \
    java_util_zip_Deflater.c \
    java_util_zip_CRC32.c \
    zip_util.c \
    jni_util.c \
    jni_util_md.c \
    io_util.c \
    canonicalize_md.c \
    FileDescriptor_md.c \
    Register.cpp \

LOCAL_C_INCLUDES += \
       libcore/$(srcdir) \
       external/fdlibm \
       external/openssl/include \
       external/zlib
