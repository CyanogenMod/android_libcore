/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "ScopedPrimitiveArray.h"
#include "zip.h"

static struct {
    jfieldID inRead;
    jfieldID finished;
    jfieldID needsDictionary;
} gCachedFields;

/* Create a new stream . This stream cannot be used until it has been properly initialized. */
static jlong Inflater_createStream(JNIEnv* env, jobject, jboolean noHeader) {
    UniquePtr<NativeZipStream> jstream(new NativeZipStream);
    if (jstream.get() == NULL) {
        jniThrowOutOfMemoryError(env, NULL);
        return -1;
    }
    jstream->stream.adler = 1;

    /*
     * In the range 8..15 for checked, or -8..-15 for unchecked inflate. Unchecked
     * is appropriate for formats like zip that do their own validity checking.
     */
    /* Window bits to use. 15 is fastest but consumes the most memory */
    int wbits = 15;               /*Use MAX for fastest */
    if (noHeader) {
        wbits = wbits / -1;
    }
    int err = inflateInit2(&jstream->stream, wbits);
    if (err != Z_OK) {
        throwExceptionForZlibError(env, "java/lang/IllegalArgumentException", err);
        return -1;
    }
    return reinterpret_cast<uintptr_t>(jstream.release());
}

static void Inflater_setInputImpl(JNIEnv* env, jobject, jbyteArray buf, jint off, jint len, jlong handle) {
    toNativeZipStream(handle)->setInput(env, buf, off, len);
}

static jint Inflater_setFileInputImpl(JNIEnv* env, jobject, jobject javaFileDescriptor, jlong off, jint len, jlong handle) {
    NativeZipStream* stream = toNativeZipStream(handle);
    if (stream->inCap < len) {
        stream->setInput(env, NULL, 0, len);
    }
    // TODO: is it okay to be this sloppy about errors?
    int fd = jniGetFDFromFileDescriptor(env, javaFileDescriptor);
    lseek(fd, off, SEEK_SET);
    return read(fd, &stream->input[0], len);
}

static jint Inflater_inflateImpl(JNIEnv* env, jobject recv, jbyteArray buf, int off, int len, jlong handle) {
    jfieldID fid2 = 0;

    /* We need to get the number of bytes already read */
    jfieldID fid = gCachedFields.inRead;
    jint inBytes = env->GetIntField(recv, fid);

    NativeZipStream* stream = toNativeZipStream(handle);
    stream->stream.avail_out = len;
    jint sin = stream->stream.total_in;
    jint sout = stream->stream.total_out;
    ScopedByteArrayRO out(env, buf);
    if (out.get() == NULL) {
        jniThrowOutOfMemoryError(env, NULL);
        return -1;
    }
    stream->stream.next_out = (Bytef *) out.get() + off;
    int err = inflate(&stream->stream, Z_SYNC_FLUSH);
    if (err != Z_OK) {
        if (err == Z_STREAM_ERROR) {
            return 0;
        }
        if (err == Z_STREAM_END || err == Z_NEED_DICT) {
            env->SetIntField(recv, fid, (jint) stream->stream.total_in - sin + inBytes);
            if (err == Z_STREAM_END) {
                fid2 = gCachedFields.finished;
            } else {
                fid2 = gCachedFields.needsDictionary;
            }
            env->SetBooleanField(recv, fid2, JNI_TRUE);
            return stream->stream.total_out - sout;
        } else {
            throwExceptionForZlibError(env, "java/util/zip/DataFormatException", err);
            return -1;
        }
    }

    /* Need to update the number of input bytes read. Is there a better way
     * (Maybe global the fid then delete when end is called)?
     */
    env->SetIntField(recv, fid, (jint) stream->stream.total_in - sin + inBytes);
    return stream->stream.total_out - sout;
}

static jint Inflater_getAdlerImpl(JNIEnv*, jobject, jlong handle) {
    return toNativeZipStream(handle)->stream.adler;
}

static void Inflater_endImpl(JNIEnv*, jobject, jlong handle) {
    NativeZipStream* stream = toNativeZipStream(handle);
    inflateEnd(&stream->stream);
    delete stream;
}

static void Inflater_setDictionaryImpl(JNIEnv* env, jobject, jbyteArray dict, int off, int len, jlong handle) {
    toNativeZipStream(handle)->setDictionary(env, dict, off, len, true);
}

static void Inflater_resetImpl(JNIEnv* env, jobject, jlong handle) {
    int err = inflateReset(&toNativeZipStream(handle)->stream);
    if (err != Z_OK) {
        throwExceptionForZlibError(env, "java/lang/IllegalArgumentException", err);
    }
}

static jlong Inflater_getTotalOutImpl(JNIEnv*, jobject, jlong handle) {
    return toNativeZipStream(handle)->stream.total_out;
}

static jlong Inflater_getTotalInImpl(JNIEnv*, jobject, jlong handle) {
    return toNativeZipStream(handle)->stream.total_in;
}

static JNINativeMethod gMethods[] = {
    { "createStream", "(Z)J", (void*) Inflater_createStream },
    { "endImpl", "(J)V", (void*) Inflater_endImpl },
    { "getAdlerImpl", "(J)I", (void*) Inflater_getAdlerImpl },
    { "getTotalInImpl", "(J)J", (void*) Inflater_getTotalInImpl },
    { "getTotalOutImpl", "(J)J", (void*) Inflater_getTotalOutImpl },
    { "inflateImpl", "([BIIJ)I", (void*) Inflater_inflateImpl },
    { "resetImpl", "(J)V", (void*) Inflater_resetImpl },
    { "setDictionaryImpl", "([BIIJ)V", (void*) Inflater_setDictionaryImpl },
    { "setFileInputImpl", "(Ljava/io/FileDescriptor;JIJ)I", (void*) Inflater_setFileInputImpl },
    { "setInputImpl", "([BIIJ)V", (void*) Inflater_setInputImpl },
};
int register_java_util_zip_Inflater(JNIEnv* env) {
    jclass inflaterClass = env->FindClass("java/util/zip/Inflater");
    gCachedFields.finished = env->GetFieldID(inflaterClass, "finished", "Z");
    gCachedFields.inRead = env->GetFieldID(inflaterClass, "inRead", "I");
    gCachedFields.needsDictionary = env->GetFieldID(inflaterClass, "needsDictionary", "Z");
    return jniRegisterNativeMethods(env, "java/util/zip/Inflater", gMethods, NELEM(gMethods));
}
