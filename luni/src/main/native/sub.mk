# This file is included by the top-level libcore Android.mk.
# It's not a normal makefile, so we don't include CLEAR_VARS
# or BUILD_*_LIBRARY.

LOCAL_SRC_FILES := \
	java_io_Console.cpp \
	java_io_File.cpp \
	java_io_FileDescriptor.c \
	java_io_ObjectInputStream.c \
	java_io_ObjectOutputStream.c \
	java_io_ObjectStreamClass.c \
	java_lang_Double.c \
	java_lang_Float.c \
	java_lang_Math.c \
        java_lang_ProcessManager.cpp \
	java_lang_StrictMath.c \
        java_lang_System.cpp \
	java_net_InetAddress.cpp \
	java_net_NetworkInterface.cpp \
	java_util_zip_Adler32.c \
	java_util_zip_CRC32.c \
	java_util_zip_Deflater.c \
	java_util_zip_Inflater.c \
	cbigint.c \
	commonDblParce.c \
	org_apache_harmony_luni_util_fltparse.c \
	org_apache_harmony_luni_util_NumberConvert.c \
	org_apache_harmony_luni_platform_OSNetworkSystem.cpp \
	org_apache_harmony_luni_platform_OSFileSystem.cpp \
	org_apache_harmony_luni_platform_OSMemory.cpp \
	org_apache_harmony_xml_ExpatParser.cpp \
	org_apache_harmony_xnet_provider_jsse_NativeCrypto.cpp \
	zip.c \
	zipalloc.c \
	sieb.c \
        BidiWrapper.cpp \
        ErrorCode.cpp \
        ICU.cpp \
        NativeBreakIterator.cpp \
        NativeCollation.cpp \
        NativeConverter.cpp \
        NativeDecimalFormat.cpp \
        NativeIDN.cpp \
        NativeNormalizer.cpp \
        NativeRegEx.cpp \
        Register.cpp \
        UCharacter.cpp


LOCAL_C_INCLUDES += \
	external/expat/lib \
        external/icu4c/common \
        external/icu4c/i18n \
	external/openssl/include \
	external/zlib

# Any shared/static libs that are listed here must also
# be listed in libs/nativehelper/Android.mk.
# TODO: fix this requirement

LOCAL_SHARED_LIBRARIES += \
	libcrypto \
	libcutils \
        libexpat \
        libicudata \
        libicuuc \
        libicui18n \
        libssl \
	libutils \
	libz

LOCAL_STATIC_LIBRARIES += \
	libfdlibm
