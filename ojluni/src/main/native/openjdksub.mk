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
    Adler32.c \
    zip_util.c \
    jni_util.c \
    jni_util_md.c \
    io_util.c \
    canonicalize_md.c \
    FileDescriptor_md.c \
    DatagramChannelImpl.c \
    Console_md.c \
    IOUtil.c \
    EPollArrayWrapper.c \
    InheritedChannel.c \
    SocketChannelImpl.c \
    FileChannelImpl.c \
    FileDispatcherImpl.c \
    FileOutputStream_md.c \
    FileInputStream.c \
    io_util_md.c \
    NativeThread.c \
    FileKey.c \
    UnixFileSystem_md.c \
    FileSystem_md.c \
    ObjectStreamClass.c \
    InetAddress.c \
    InetAddressImplFactory.c \
    net_util.c \
    net_util_md.c \
    Net.c \
    MappedByteBuffer.c \
    Inet6Address.c \
    Inet4Address.c \
    linux_close.c \
    PlainSocketImpl.c \
    PlainDatagramSocketImpl.c \
    NetworkInterface.c \
    DatagramPacket.c \
    Inet4AddressImpl.c \
    Inet6AddressImpl.c \
    ServerSocketChannelImpl.c \
    SocketInputStream.c \
    SocketOutputStream.c \
    DefaultProxySelector.c \
    Float.c \
    Double.c \
    String.c \
    StrictMath.c \
    ProcessEnvironment_md.c \
    Signal.c \
    System.c \
    Runtime.c \
    NativeSignalHandler.c \
    Shutdown.c \
    UNIXProcess_md.c \
    Bits.c \
    Register.cpp \

LOCAL_C_INCLUDES += \
       libcore/$(srcdir) \
       external/fdlibm \
       external/openssl/include \
       external/zlib
