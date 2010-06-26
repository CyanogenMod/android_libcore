# This file is included by the top-level libcore Android.mk.
# It's not a normal makefile, so we don't include CLEAR_VARS
# or BUILD_*_LIBRARY.

LOCAL_SRC_FILES := \
	ErrorCode.cpp \
	ICU.cpp \
	JniConstants.cpp \
	NativeBidi.cpp \
	NativeBreakIterator.cpp \
	NativeCollation.cpp \
	NativeConverter.cpp \
	NativeDecimalFormat.cpp \
	NativeIDN.cpp \
	NativeNormalizer.cpp \
	NativePluralRules.cpp \
	NetworkUtilities.cpp \
	Register.cpp \
	TimeZones.cpp \
	UCharacter.cpp \
	cbigint.cpp \
	commonDblParce.cpp \
	java_io_Console.cpp \
	java_io_File.cpp \
	java_io_FileDescriptor.cpp \
	java_io_ObjectInputStream.cpp \
	java_io_ObjectOutputStream.cpp \
	java_io_ObjectStreamClass.cpp \
	java_lang_Double.cpp \
	java_lang_Float.cpp \
	java_lang_Math.cpp \
	java_lang_ProcessManager.cpp \
	java_lang_StrictMath.cpp \
	java_lang_System.cpp \
	java_net_InetAddress.cpp \
	java_net_NetworkInterface.cpp \
	java_util_regex_Matcher.cpp \
	java_util_regex_Pattern.cpp \
	java_util_zip_Adler32.cpp \
	java_util_zip_CRC32.cpp \
	java_util_zip_Deflater.cpp \
	java_util_zip_Inflater.cpp \
	org_apache_harmony_luni_platform_OSFileSystem.cpp \
	org_apache_harmony_luni_platform_OSMemory.cpp \
	org_apache_harmony_luni_platform_OSNetworkSystem.cpp \
	org_apache_harmony_luni_util_NumberConvert.cpp \
	org_apache_harmony_luni_util_fltparse.cpp \
	org_apache_harmony_xml_ExpatParser.cpp \
	org_apache_harmony_xnet_provider_jsse_NativeCrypto.cpp \
	valueOf.cpp


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
