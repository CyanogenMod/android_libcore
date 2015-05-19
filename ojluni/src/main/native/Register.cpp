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

#define LOG_TAG "libcore" // We'll be next to "dalvikvm" in the log; make the distinction clear.

#include "cutils/log.h"
#include "JniConstants.h"
#include "ScopedLocalFrame.h"

#include <stdlib.h>

extern "C" {

extern void register_java_util_zip_ZipFile(JNIEnv*);
extern void register_java_util_zip_Inflater(JNIEnv*);
extern void register_java_util_zip_Deflater(JNIEnv*);
extern void register_java_util_zip_CRC32(JNIEnv*);
extern void register_java_io_FileSystem(JNIEnv*);
extern void register_sun_nio_ch_IOUtil(JNIEnv*);
extern void register_sun_nio_ch_FileChannelImpl(JNIEnv*);
extern void register_sun_nio_ch_FileDispatcherImpl(JNIEnv*);
extern void register_java_io_FileOutputStream(JNIEnv*);
extern void register_java_io_FileInputStream(JNIEnv*);
extern void register_java_io_FileDescriptor(JNIEnv*);
extern void register_sun_nio_ch_NativeThread(JNIEnv*);
extern void register_sun_nio_ch_FileKey(JNIEnv*);
extern void register_java_io_ObjectStreamClass(JNIEnv*);
extern void register_java_net_InetAddress(JNIEnv*);
extern void register_java_net_Inet4Address(JNIEnv*);
extern void register_java_net_Inet6Address(JNIEnv*);
extern void register_java_net_InetAddressImplFactory(JNIEnv*);
extern void register_java_net_PlainSocketImpl(JNIEnv*);
extern void register_java_net_PlainDatagramSocketImpl(JNIEnv*);
extern void register_java_net_NetworkInterface(JNIEnv*);
extern void register_java_net_DatagramPacket(JNIEnv*);
extern void register_java_net_Inet4AddressImpl(JNIEnv*);
extern void register_java_net_Inet6AddressImpl(JNIEnv*);
extern void register_java_net_SocketInputStream(JNIEnv*);
extern void register_java_net_SocketOutputStream(JNIEnv*);
extern void register_sun_net_spi_DefaultProxySelector(JNIEnv*);
extern void register_java_lang_Float(JNIEnv*);
extern void register_java_lang_Double(JNIEnv*);
extern void register_java_lang_String(JNIEnv*);
extern void register_java_lang_StrictMath(JNIEnv*);
extern void register_java_lang_Runtime(JNIEnv*);
extern void register_java_lang_System(JNIEnv*);
extern void register_java_lang_Thread(JNIEnv*);
extern void register_java_lang_ProcessEnvironment(JNIEnv*);
extern void register_sun_misc_Signal(JNIEnv*);
extern void register_sun_misc_NativeSignalHandler(JNIEnv*);
extern void register_java_lang_Shutdown(JNIEnv*);
extern void register_java_lang_UNIXProcess(JNIEnv*);

extern jint net_JNI_OnLoad(JavaVM*, void*);

}

// DalvikVM calls this on startup, so we can statically register all our native methods.
jint JNI_OnLoad(JavaVM* vm, void*) { JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        ALOGE("JavaVM::GetEnv() failed");
        abort();
    }

    ScopedLocalFrame localFrame(env);
    register_java_util_zip_ZipFile(env);
    register_java_util_zip_Inflater(env);
    register_java_util_zip_Deflater(env);
    register_java_util_zip_CRC32(env);
    register_java_io_FileSystem(env);
    register_sun_nio_ch_IOUtil(env);
    register_sun_nio_ch_FileChannelImpl(env);
    register_sun_nio_ch_FileDispatcherImpl(env);
    register_java_io_FileOutputStream(env);
    register_java_io_FileInputStream(env);
    register_java_io_FileDescriptor(env);
    register_sun_nio_ch_NativeThread(env);
    register_sun_nio_ch_FileKey(env);
    register_java_io_ObjectStreamClass(env);
    register_java_net_InetAddress(env);
    register_java_net_Inet4Address(env);
    register_java_net_Inet6Address(env);
    register_java_net_InetAddressImplFactory(env);
    register_java_net_PlainSocketImpl(env);
    register_java_net_PlainDatagramSocketImpl(env);
    register_java_net_NetworkInterface(env);
    register_java_net_DatagramPacket(env);
    register_java_net_Inet4AddressImpl(env);
    register_java_net_Inet6AddressImpl(env);
    register_java_net_SocketInputStream(env);
    register_java_net_SocketOutputStream(env);
    register_sun_net_spi_DefaultProxySelector(env);
    register_java_lang_Float(env);
    register_java_lang_Double(env);
    register_java_lang_String(env);
    register_java_lang_StrictMath(env);
    register_java_lang_ProcessEnvironment(env);
    register_sun_misc_Signal(env);
    register_java_lang_Runtime(env);
    register_java_lang_System(env);
    register_sun_misc_NativeSignalHandler(env);
    register_java_lang_Shutdown(env);
    register_java_lang_UNIXProcess(env);
    net_JNI_OnLoad(vm, NULL);
    return JNI_VERSION_1_6;
}
