/*
 * Copyright (C) 2007 The Android Open Source Project
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

#define LOG_TAG "OSMemory"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "UniquePtr.h"

#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>

/*
 * Cached dalvik.system.VMRuntime pieces.
 */
static struct {
    jmethodID method_trackExternalAllocation;
    jmethodID method_trackExternalFree;

    jobject runtimeInstance;
} gIDCache;

static const int MMAP_READ_ONLY = 1;
static const int MMAP_READ_WRITE = 2;
static const int MMAP_WRITE_COPY = 4;

template <typename T> static T cast(jint address) {
    return reinterpret_cast<T>(static_cast<uintptr_t>(address));
}

static jboolean OSMemory_isLittleEndianImpl(JNIEnv*, jclass) {
    long l = 0x01020304;
    unsigned char* c = reinterpret_cast<unsigned char*>(&l);
    return (*c == 0x04) ? JNI_TRUE : JNI_FALSE;
}

static jint OSMemory_getPointerSizeImpl(JNIEnv*, jclass) {
    return sizeof(void*);
}

static jint OSMemory_malloc(JNIEnv* env, jobject, jint size) {
    jboolean allowed = env->CallBooleanMethod(gIDCache.runtimeInstance,
        gIDCache.method_trackExternalAllocation, (jlong) size);
    if (!allowed) {
        LOGW("External allocation of %d bytes was rejected\n", size);
        jniThrowException(env, "java/lang/OutOfMemoryError", NULL);
        return 0;
    }

    LOGV("OSMemory alloc %d\n", size);
    void* block = malloc(size + sizeof(jlong));
    if (block == NULL) {
        jniThrowException(env, "java/lang/OutOfMemoryError", NULL);
        return 0;
    }

    /*
     * Tuck a copy of the size at the head of the buffer.  We need this
     * so OSMemory_free() knows how much memory is being freed.
     */
    jlong* result = reinterpret_cast<jlong*>(block);
    *result++ = size;
    return static_cast<jint>(reinterpret_cast<uintptr_t>(result));
}

static void OSMemory_free(JNIEnv* env, jobject, jint address) {
    jlong* p = reinterpret_cast<jlong*>(static_cast<uintptr_t>(address));
    jlong size = *--p;
    LOGV("OSMemory free %ld\n", size);
    env->CallVoidMethod(gIDCache.runtimeInstance, gIDCache.method_trackExternalFree, size);
    free(reinterpret_cast<void*>(p));
}

static void OSMemory_memset(JNIEnv*, jobject, jint dstAddress, jbyte value, jlong length) {
    memset(cast<void*>(dstAddress), value, length);
}

static void OSMemory_memmove(JNIEnv*, jobject, jint dstAddress, jint srcAddress, jlong length) {
    memmove(cast<void*>(dstAddress), cast<const void*>(srcAddress), length);
}

static jbyte OSMemory_getByte(JNIEnv*, jobject, jint srcAddress) {
    return *cast<const jbyte*>(srcAddress);
}

static void OSMemory_getByteArray(JNIEnv* env, jobject, jint srcAddress,
        jbyteArray dst, jint offset, jint length) {
    env->SetByteArrayRegion(dst, offset, length, cast<const jbyte*>(srcAddress));
}

static void OSMemory_setByte(JNIEnv*, jobject, jint dstAddress, jbyte value) {
    *cast<jbyte*>(dstAddress) = value;
}

static void OSMemory_setByteArray(JNIEnv* env, jobject,
        jint dstAddress, jbyteArray src, jint offset, jint length) {
    env->GetByteArrayRegion(src, offset, length, cast<jbyte*>(dstAddress));
}

static void swapShorts(jshort* shorts, int count) {
    jbyte* src = reinterpret_cast<jbyte*>(shorts);
    jbyte* dst = src;
    for (int i = 0; i < count; ++i) {
        jbyte b0 = *src++;
        jbyte b1 = *src++;
        *dst++ = b1;
        *dst++ = b0;
    }
}

static void swapInts(jint* ints, int count) {
    jbyte* src = reinterpret_cast<jbyte*>(ints);
    jbyte* dst = src;
    for (int i = 0; i < count; ++i) {
        jbyte b0 = *src++;
        jbyte b1 = *src++;
        jbyte b2 = *src++;
        jbyte b3 = *src++;
        *dst++ = b3;
        *dst++ = b2;
        *dst++ = b1;
        *dst++ = b0;
    }
}

static void OSMemory_setFloatArray(JNIEnv* env, jobject, jint dstAddress,
        jfloatArray src, jint offset, jint length, jboolean swap) {
    jfloat* dst = cast<jfloat*>(dstAddress);
    env->GetFloatArrayRegion(src, offset, length, dst);
    if (swap) {
        swapInts(reinterpret_cast<jint*>(dst), length);
    }
}

static void OSMemory_setIntArray(JNIEnv* env, jobject,
       jint dstAddress, jintArray src, jint offset, jint length, jboolean swap) {
    jint* dst = cast<jint*>(dstAddress);
    env->GetIntArrayRegion(src, offset, length, dst);
    if (swap) {
        swapInts(dst, length);
    }
}

static void OSMemory_setShortArray(JNIEnv* env, jobject,
       jint dstAddress, jshortArray src, jint offset, jint length, jboolean swap) {
    jshort* dst = cast<jshort*>(dstAddress);
    env->GetShortArrayRegion(src, offset, length, dst);
    if (swap) {
        swapShorts(dst, length);
    }
}

static jshort OSMemory_getShort(JNIEnv*, jobject, jint srcAddress) {
    if ((srcAddress & 0x1) == 0) {
        return *cast<const jshort*>(srcAddress);
    } else {
        // Handle unaligned memory access one byte at a time
        jshort result;
        const jbyte* src = cast<const jbyte*>(srcAddress);
        jbyte* dst = reinterpret_cast<jbyte*>(&result);
        dst[0] = src[0];
        dst[1] = src[1];
        return result;
    }
}

static void OSMemory_setShort(JNIEnv*, jobject, jint dstAddress, jshort value) {
    if ((dstAddress & 0x1) == 0) {
        *cast<jshort*>(dstAddress) = value;
    } else {
        // Handle unaligned memory access one byte at a time
        const jbyte* src = reinterpret_cast<const jbyte*>(&value);
        jbyte* dst = cast<jbyte*>(dstAddress);
        dst[0] = src[0];
        dst[1] = src[1];
    }
}

static jint OSMemory_getInt(JNIEnv*, jobject, jint srcAddress) {
    if ((srcAddress & 0x3) == 0) {
        return *cast<const jint*>(srcAddress);
    } else {
        // Handle unaligned memory access one byte at a time
        jint result;
        const jbyte* src = cast<const jbyte*>(srcAddress);
        jbyte* dst = reinterpret_cast<jbyte*>(&result);
        dst[0] = src[0];
        dst[1] = src[1];
        dst[2] = src[2];
        dst[3] = src[3];
        return result;
    }
}

static void OSMemory_setInt(JNIEnv*, jobject, jint dstAddress, jint value) {
    if ((dstAddress & 0x3) == 0) {
        *cast<jint*>(dstAddress) = value;
    } else {
        // Handle unaligned memory access one byte at a time
        const jbyte* src = reinterpret_cast<const jbyte*>(&value);
        jbyte* dst = cast<jbyte*>(dstAddress);
        dst[0] = src[0];
        dst[1] = src[1];
        dst[2] = src[2];
        dst[3] = src[3];
    }
}

template <typename T> static T get(jint srcAddress) {
    if ((srcAddress & (sizeof(T) - 1)) == 0) {
        return *cast<const T*>(srcAddress);
    } else {
        // Cast to void* so GCC can't assume correct alignment and optimize this out.
        const void* src = cast<const void*>(srcAddress);
        T result;
        memcpy(&result, src, sizeof(T));
        return result;
    }
}

template <typename T> static void set(jint dstAddress, T value) {
    if ((dstAddress & (sizeof(T) - 1)) == 0) {
        *cast<T*>(dstAddress) = value;
    } else {
        // Cast to void* so GCC can't assume correct alignment and optimize this out.
        void* dst = cast<void*>(dstAddress);
        memcpy(dst, &value, sizeof(T));
    }
}

static jlong OSMemory_getLong(JNIEnv*, jobject, jint srcAddress) {
    return get<jlong>(srcAddress);
}

static void OSMemory_setLong(JNIEnv*, jobject, jint dstAddress, jlong value) {
    set<jlong>(dstAddress, value);
}

static jfloat OSMemory_getFloat(JNIEnv*, jobject, jint srcAddress) {
    return get<jfloat>(srcAddress);
}

static void OSMemory_setFloat(JNIEnv*, jobject, jint dstAddress, jfloat value) {
    set<jfloat>(dstAddress, value);
}

static jdouble OSMemory_getDouble(JNIEnv*, jobject, jint srcAddress) {
    return get<jdouble>(srcAddress);
}

static void OSMemory_setDouble(JNIEnv*, jobject, jint dstAddress, jdouble value) {
    set<jdouble>(dstAddress, value);
}

static jint OSMemory_getAddress(JNIEnv*, jobject, jint srcAddress) {
    return *cast<const jint*>(srcAddress);
}

static void OSMemory_setAddress(JNIEnv*, jobject, jint dstAddress, jint value) {
    *cast<jint*>(dstAddress) = value;
}

static jint OSMemory_mmapImpl(JNIEnv* env, jobject, jint fd, jlong offset, jlong size, jint mapMode) {
    int prot, flags;
    switch (mapMode) {
    case MMAP_READ_ONLY:
        prot = PROT_READ;
        flags = MAP_SHARED;
        break;
    case MMAP_READ_WRITE:
        prot = PROT_READ|PROT_WRITE;
        flags = MAP_SHARED;
        break;
    case MMAP_WRITE_COPY:
        prot = PROT_READ|PROT_WRITE;
        flags = MAP_PRIVATE;
        break;
    default:
        jniThrowIOException(env, EINVAL);
        LOGE("bad mapMode %i", mapMode);
        return -1;
    }

    void* mapAddress = mmap(0, size, prot, flags, fd, offset);
    if (mapAddress == MAP_FAILED) {
        jniThrowIOException(env, errno);
    }
    return reinterpret_cast<uintptr_t>(mapAddress);
}

static void OSMemory_unmapImpl(JNIEnv*, jobject, jint address, jlong size) {
    munmap(cast<void*>(address), size);
}

static jint OSMemory_loadImpl(JNIEnv*, jobject, jint address, jlong size) {
    if (mlock(cast<void*>(address), size) != -1) {
        if (munlock(cast<void*>(address), size) != -1) {
              return 0;  /* normally */
        }
    } else {
         /* according to linux sys call, only root can mlock memory. */
         if (errno == EPERM) {
             return 0;
         }
    }
    return -1;
}

static jboolean OSMemory_isLoadedImpl(JNIEnv*, jobject, jint address, jlong size) {
    static int page_size = getpagesize();

    int align_offset = address % page_size;// addr should align with the boundary of a page.
    address -= align_offset;
    size += align_offset;
    int page_count = (size + page_size - 1) / page_size;

    UniquePtr<unsigned char[]> vec(new unsigned char[page_count]);
    int rc = mincore(cast<void*>(address), size, (MINCORE_POINTER_TYPE) vec.get());
    if (rc == -1) {
        return false;
    }

    for (int i = 0; i < page_count; ++i) {
        if (vec[i] != 1) {
            return false;
        }
    }
    return true;
}

static jint OSMemory_flushImpl(JNIEnv*, jobject, jint address, jlong size) {
    return msync(cast<void*>(address), size, MS_SYNC);
}

static JNINativeMethod gMethods[] = {
    { "flushImpl",          "(IJ)I",   (void*) OSMemory_flushImpl },
    { "free",               "(I)V",    (void*) OSMemory_free },
    { "getAddress",         "(I)I",    (void*) OSMemory_getAddress },
    { "getByte",            "(I)B",    (void*) OSMemory_getByte },
    { "getByteArray",       "(I[BII)V",(void*) OSMemory_getByteArray },
    { "getDouble",          "(I)D",    (void*) OSMemory_getDouble },
    { "getFloat",           "(I)F",    (void*) OSMemory_getFloat },
    { "getInt",             "(I)I",    (void*) OSMemory_getInt },
    { "getLong",            "(I)J",    (void*) OSMemory_getLong },
    { "getPointerSizeImpl", "()I",     (void*) OSMemory_getPointerSizeImpl },
    { "getShort",           "(I)S",    (void*) OSMemory_getShort },
    { "isLittleEndianImpl", "()Z",     (void*) OSMemory_isLittleEndianImpl },
    { "isLoadedImpl",       "(IJ)Z",   (void*) OSMemory_isLoadedImpl },
    { "loadImpl",           "(IJ)I",   (void*) OSMemory_loadImpl },
    { "malloc",             "(I)I",    (void*) OSMemory_malloc },
    { "memmove",            "(IIJ)V",  (void*) OSMemory_memmove },
    { "memset",             "(IBJ)V",  (void*) OSMemory_memset },
    { "mmapImpl",           "(IJJI)I", (void*) OSMemory_mmapImpl },
    { "setAddress",         "(II)V",   (void*) OSMemory_setAddress },
    { "setByte",            "(IB)V",   (void*) OSMemory_setByte },
    { "setByteArray",       "(I[BII)V",(void*) OSMemory_setByteArray },
    { "setDouble",          "(ID)V",   (void*) OSMemory_setDouble },
    { "setFloat",           "(IF)V",   (void*) OSMemory_setFloat },
    { "setFloatArray",      "(I[FIIZ)V",(void*) OSMemory_setFloatArray },
    { "setInt",             "(II)V",   (void*) OSMemory_setInt },
    { "setIntArray",        "(I[IIIZ)V",(void*) OSMemory_setIntArray },
    { "setLong",            "(IJ)V",   (void*) OSMemory_setLong },
    { "setShort",           "(IS)V",   (void*) OSMemory_setShort },
    { "setShortArray",      "(I[SIIZ)V",(void*) OSMemory_setShortArray },
    { "unmapImpl",          "(IJ)V",   (void*) OSMemory_unmapImpl },
};
int register_org_apache_harmony_luni_platform_OSMemory(JNIEnv* env) {
    /*
     * We need to call VMRuntime.trackExternal{Allocation,Free}.  Cache
     * method IDs and a reference to the singleton.
     */
    gIDCache.method_trackExternalAllocation = env->GetMethodID(JniConstants::vmRuntimeClass,
        "trackExternalAllocation", "(J)Z");
    gIDCache.method_trackExternalFree = env->GetMethodID(JniConstants::vmRuntimeClass,
        "trackExternalFree", "(J)V");

    jmethodID method_getRuntime = env->GetStaticMethodID(JniConstants::vmRuntimeClass,
        "getRuntime", "()Ldalvik/system/VMRuntime;");

    if (gIDCache.method_trackExternalAllocation == NULL ||
        gIDCache.method_trackExternalFree == NULL ||
        method_getRuntime == NULL)
    {
        LOGE("Unable to find VMRuntime methods\n");
        return -1;
    }

    jobject instance = env->CallStaticObjectMethod(JniConstants::vmRuntimeClass, method_getRuntime);
    if (instance == NULL) {
        LOGE("Unable to obtain VMRuntime instance\n");
        return -1;
    }
    gIDCache.runtimeInstance = env->NewGlobalRef(instance);

    return jniRegisterNativeMethods(env, "org/apache/harmony/luni/platform/OSMemory",
            gMethods, NELEM(gMethods));
}
