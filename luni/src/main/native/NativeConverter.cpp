/**
*******************************************************************************
* Copyright (C) 1996-2006, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*
*******************************************************************************
*/
/*
 *  @(#) icujniinterface.c	1.2 00/10/11
 *
 * (C) Copyright IBM Corp. 2000 - All Rights Reserved
 *  A JNI wrapper to ICU native converter Interface
 * @author: Ram Viswanadha
 */

#define LOG_TAG "NativeConverter"

#include "ErrorCode.h"
#include "JNIHelp.h"
#include "ScopedLocalRef.h"
#include "ScopedPrimitiveArray.h"
#include "ScopedUtfChars.h"
#include "UniquePtr.h"
#include "cutils/log.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_cb.h"
#include "unicode/uset.h"
#include "unicode/ustring.h"
#include "unicode/utypes.h"
#include <stdlib.h>
#include <string.h>

#define NativeConverter_REPORT 0
#define NativeConverter_IGNORE 1
#define NativeConverter_REPLACE 2

struct DecoderCallbackContext {
    int length;
    UChar subUChars[256];
    UConverterToUCallback onUnmappableInput;
    UConverterToUCallback onMalformedInput;
};

struct EncoderCallbackContext {
    int length;
    char subBytes[256];
    UConverterFromUCallback onUnmappableInput;
    UConverterFromUCallback onMalformedInput;
};

static jlong openConverter(JNIEnv* env, jclass, jstring converterName) {
    ScopedUtfChars converterNameChars(env, converterName);
    if (!converterNameChars.c_str()) {
        return 0;
    }
    UErrorCode errorCode = U_ZERO_ERROR;
    UConverter* conv = ucnv_open(converterNameChars.c_str(), &errorCode);
    icu4jni_error(env, errorCode);
    return reinterpret_cast<uintptr_t>(conv);
}

static void closeConverter(JNIEnv*, jclass, jlong handle) {
    UConverter* cnv = reinterpret_cast<UConverter*>(handle);
    if (!cnv) {
        return;
    }
    // Free up contexts created in setCallback[Encode|Decode].
    UConverterToUCallback toAction;
    UConverterFromUCallback fromAction;
    void* context1 = NULL;
    void* context2 = NULL;
    // TODO: ICU API bug?
    // The documentation clearly states that the caller owns the returned
    // pointers: http://icu-project.org/apiref/icu4c/ucnv_8h.html
    ucnv_getToUCallBack(cnv, &toAction, const_cast<const void**>(&context1));
    ucnv_getFromUCallBack(cnv, &fromAction, const_cast<const void**>(&context2));
    ucnv_close(cnv);
    delete reinterpret_cast<DecoderCallbackContext*>(context1);
    delete reinterpret_cast<EncoderCallbackContext*>(context2);
}

/**
 * Converts a buffer of Unicode code units to target encoding
 * @param env environment handle for JNI
 * @param jClass handle for the class
 * @param handle address of ICU converter
 * @param source buffer of Unicode chars to convert
 * @param sourceEnd limit of the source buffer
 * @param target buffer to recieve the converted bytes
 * @param targetEnd the limit of the target buffer
 * @param data buffer to recieve state of the current conversion
 * @param flush boolean that specifies end of source input
 */
static UErrorCode convertCharsToBytes(JNIEnv* env, jclass, jlong handle,  jcharArray source,  jint sourceEnd, jbyteArray target, jint targetEnd, jintArray data, jboolean flush) {
    UConverter* cnv = (UConverter*)handle;
    if (!cnv) {
        return U_ILLEGAL_ARGUMENT_ERROR;
    }
    ScopedCharArray uSource(env, source);
    UErrorCode errorCode = U_ZERO_ERROR;
    jint* myData = (jint*) env->GetPrimitiveArrayCritical(data, NULL);
    if (myData) {
        jint* sourceOffset = &myData[0];
        jint* targetOffset = &myData[1];
        if (uSource.get() != NULL) {
            jbyte* uTarget=(jbyte*) env->GetPrimitiveArrayCritical(target,NULL);
            if(uTarget) {
                const jchar* mySource = uSource.get() + *sourceOffset;
                const UChar* mySourceLimit= uSource.get() + sourceEnd;
                char* cTarget = reinterpret_cast<char*>(uTarget+ *targetOffset);
                const char* cTargetLimit = reinterpret_cast<const char*>(uTarget+targetEnd);

                ucnv_fromUnicode( cnv , &cTarget, cTargetLimit,&mySource, mySourceLimit,NULL,(UBool) flush, &errorCode);

                *sourceOffset = (jint) (mySource - uSource.get())-*sourceOffset;
                *targetOffset = (jint) ((jbyte*)cTarget - uTarget)- *targetOffset;
                if(U_FAILURE(errorCode)) {
                    env->ReleasePrimitiveArrayCritical(target,uTarget,0);
                    env->ReleasePrimitiveArrayCritical(data,(jint*)myData,0);
                    return errorCode;
                }
            }else{
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            }
            env->ReleasePrimitiveArrayCritical(target,uTarget,0);
        }else{
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        }
    }else{
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    }
    env->ReleasePrimitiveArrayCritical(data,(jint*)myData,0);
    return errorCode;
}

static jint encode(JNIEnv* env, jclass, jlong handle, jcharArray source, jint sourceEnd, jbyteArray target, jint targetEnd, jintArray data, jboolean flush) {
    UErrorCode ec = convertCharsToBytes(env, NULL, handle, source, sourceEnd, target, targetEnd, data, flush);
    UConverter* cnv = (UConverter*)handle;
    if (cnv) {
        UErrorCode errorCode = U_ZERO_ERROR;
        jint count = ucnv_fromUCountPending(cnv, &errorCode);
        env->SetIntArrayRegion(data, 3, 1, &count);

        if (ec == U_ILLEGAL_CHAR_FOUND || ec == U_INVALID_CHAR_FOUND) {
            int8_t len = 32;
            UChar invalidUChars[32];
            ucnv_getInvalidUChars(cnv, invalidUChars, &len, &errorCode);
            if (U_SUCCESS(errorCode)) {
                jint value = len;
                env->SetIntArrayRegion(data, 2, 1, &value);
            }
        }
    }
    return ec;
}

/**
 * Converts a buffer of encoded bytes to Unicode code units
 * @param env environment handle for JNI
 * @param jClass handle for the class
 * @param handle address of ICU converter
 * @param source buffer of Unicode chars to convert
 * @param sourceEnd limit of the source buffer
 * @param target buffer to recieve the converted bytes
 * @param targetEnd the limit of the target buffer
 * @param data buffer to recieve state of the current conversion
 * @param flush boolean that specifies end of source input
 */
static UErrorCode convertBytesToChars(JNIEnv* env, jclass, jlong handle, jbyteArray source, jint sourceEnd, jcharArray target, jint targetEnd, jintArray data, jboolean flush) {
    UErrorCode errorCode =U_ZERO_ERROR;
    UConverter* cnv = (UConverter*)handle;
    if (cnv) {
        ScopedByteArray uSource(env, source);
        jint* myData = (jint*) env->GetPrimitiveArrayCritical(data,NULL);
        if(myData) {
            jint* sourceOffset = &myData[0];
            jint* targetOffset = &myData[1];

            if (uSource.get() != NULL) {
                jchar* uTarget=(jchar*) env->GetPrimitiveArrayCritical(target,NULL);
                if(uTarget) {
                    const jbyte* mySource = uSource.get() + *sourceOffset;
                    const char* mySourceLimit = reinterpret_cast<const char*>(uSource.get() + sourceEnd);
                    UChar* cTarget=uTarget+ *targetOffset;
                    const UChar* cTargetLimit=uTarget+targetEnd;

                    ucnv_toUnicode( cnv , &cTarget, cTargetLimit,(const char**)&mySource,
                                   mySourceLimit,NULL,(UBool) flush, &errorCode);

                    *sourceOffset = mySource - uSource.get() - *sourceOffset;
                    *targetOffset = cTarget - uTarget - *targetOffset;
                    if(U_FAILURE(errorCode)) {
                        env->ReleasePrimitiveArrayCritical(target,uTarget,0);
                        env->ReleasePrimitiveArrayCritical(data,(jint*)myData,0);
                        return errorCode;
                    }
                }else{
                    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
                }
                env->ReleasePrimitiveArrayCritical(target,uTarget,0);
            }else{
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            }
        }else{
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        }
        env->ReleasePrimitiveArrayCritical(data,(jint*)myData,0);
        return errorCode;
    }
    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    return errorCode;
}

static jint decode(JNIEnv* env, jclass, jlong handle, jbyteArray source, jint sourceEnd, jcharArray target, jint targetEnd, jintArray data, jboolean flush) {
    UErrorCode ec = convertBytesToChars(env, NULL, handle, source, sourceEnd, target, targetEnd, data, flush);
    UConverter* cnv = (UConverter*)handle;
    if (cnv) {
        UErrorCode errorCode = U_ZERO_ERROR;
        jint count = ucnv_toUCountPending(cnv, &errorCode);
        env->SetIntArrayRegion(data, 3, 1, &count);

        if (ec == U_ILLEGAL_CHAR_FOUND || ec == U_INVALID_CHAR_FOUND) {
            int8_t len = 32;
            char invalidChars[32] = {'\0'};
            ucnv_getInvalidChars(cnv, invalidChars, &len, &errorCode);
            if (U_SUCCESS(errorCode)) {
                jint value = len;
                env->SetIntArrayRegion(data, 2, 1, &value);
            }
        }
    }
    return ec;
}

static void resetByteToChar(JNIEnv*, jclass, jlong handle) {
    UConverter* cnv = (UConverter*)handle;
    if (cnv) {
        ucnv_resetToUnicode(cnv);
    }
}

static void resetCharToByte(JNIEnv*, jclass, jlong handle) {
    UConverter* cnv = (UConverter*)handle;
    if (cnv) {
        ucnv_resetFromUnicode(cnv);
    }
}

static jint getMaxBytesPerChar(JNIEnv*, jclass, jlong handle) {
    UConverter* cnv = (UConverter*)handle;
    return (cnv != NULL) ? ucnv_getMaxCharSize(cnv) : -1;
}

static jint getMinBytesPerChar(JNIEnv*, jclass, jlong handle) {
    UConverter* cnv = (UConverter*)handle;
    return (cnv != NULL) ? ucnv_getMinCharSize(cnv) : -1;
}

static jfloat getAveBytesPerChar(JNIEnv*, jclass, jlong handle) {
    UConverter* cnv = (UConverter*)handle;
    return (cnv != NULL) ? ((ucnv_getMaxCharSize(cnv) + ucnv_getMinCharSize(cnv)) / 2.0) : -1;
}

static jint flushByteToChar(JNIEnv* env, jclass,jlong handle, jcharArray target, jint targetEnd, jintArray data) {
    UErrorCode errorCode =U_ZERO_ERROR;
    UConverter* cnv = (UConverter*)handle;
    if(cnv) {
        jbyte source ='\0';
        jint* myData = (jint*) env->GetPrimitiveArrayCritical(data,NULL);
        if(myData) {
            jint* targetOffset = &myData[1];
            jchar* uTarget=(jchar*) env->GetPrimitiveArrayCritical(target,NULL);
            if(uTarget) {
                const jbyte* mySource = &source;
                const char* mySourceLimit = reinterpret_cast<char*>(&source);
                UChar* cTarget=uTarget+ *targetOffset;
                const UChar* cTargetLimit=uTarget+targetEnd;

                ucnv_toUnicode( cnv , &cTarget, cTargetLimit,(const char**)&mySource,
                               mySourceLimit,NULL,TRUE, &errorCode);


                *targetOffset = (jint) ((jchar*)cTarget - uTarget)- *targetOffset;
                if(U_FAILURE(errorCode)) {
                    env->ReleasePrimitiveArrayCritical(target,uTarget,0);
                    env->ReleasePrimitiveArrayCritical(data,(jint*)myData,0);
                    return errorCode;
                }
            }else{
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            }
            env->ReleasePrimitiveArrayCritical(target,uTarget,0);

        }else{
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        }
        env->ReleasePrimitiveArrayCritical(data,(jint*)myData,0);
        return errorCode;
    }
    errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    return errorCode;
}

static jint flushCharToByte (JNIEnv* env, jclass, jlong handle, jbyteArray target, jint targetEnd, jintArray data) {

    UErrorCode errorCode =U_ZERO_ERROR;
    UConverter* cnv = (UConverter*)handle;
    jchar source = '\0';
    if(cnv) {
        jint* myData = (jint*) env->GetPrimitiveArrayCritical(data,NULL);
        if(myData) {
            jint* targetOffset = &myData[1];
            jbyte* uTarget=(jbyte*) env->GetPrimitiveArrayCritical(target,NULL);
            if(uTarget) {
                const jchar* mySource = &source;
                const UChar* mySourceLimit= &source;
                char* cTarget = reinterpret_cast<char*>(uTarget+ *targetOffset);
                const char* cTargetLimit = reinterpret_cast<char*>(uTarget+targetEnd);

                ucnv_fromUnicode( cnv , &cTarget, cTargetLimit,&mySource,
                                  mySourceLimit,NULL,TRUE, &errorCode);


                *targetOffset = (jint) ((jbyte*)cTarget - uTarget)- *targetOffset;
                if(U_FAILURE(errorCode)) {
                    env->ReleasePrimitiveArrayCritical(target,uTarget,0);

                    env->ReleasePrimitiveArrayCritical(data,(jint*)myData,0);
                    return errorCode;
                }
            }else{
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            }
            env->ReleasePrimitiveArrayCritical(target,uTarget,0);
        }else{
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        }
        env->ReleasePrimitiveArrayCritical(data,(jint*)myData,0);
        return errorCode;
    }
    return U_ILLEGAL_ARGUMENT_ERROR;
}

static jboolean canEncode(JNIEnv*, jclass, jlong handle, jint codeUnit) {
    UErrorCode errorCode =U_ZERO_ERROR;
    UConverter* cnv = (UConverter*)handle;
    if(cnv) {
        UChar source[3];
        UChar *mySource=source;
        const UChar* sourceLimit = (codeUnit<0x010000) ? &source[1] : &source[2];
        char target[5];
        char *myTarget = target;
        const char* targetLimit = &target[4];
        int i=0;
        UTF_APPEND_CHAR(&source[0],i,2,codeUnit);

        ucnv_fromUnicode(cnv, &myTarget, targetLimit,
                (const UChar**)&mySource, sourceLimit, NULL, TRUE, &errorCode);
        if (U_SUCCESS(errorCode)) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

/*
 * If a charset listed in the IANA Charset Registry is supported by an implementation
 * of the Java platform then its canonical name must be the name listed in the registry.
 * Many charsets are given more than one name in the registry, in which case the registry
 * identifies one of the names as MIME-preferred. If a charset has more than one registry
 * name then its canonical name must be the MIME-preferred name and the other names in
 * the registry must be valid aliases. If a supported charset is not listed in the IANA
 * registry then its canonical name must begin with one of the strings "X-" or "x-".
 */
static jstring getJavaCanonicalName(JNIEnv* env, const char* icuCanonicalName) {
    UErrorCode status = U_ZERO_ERROR;

    // Check to see if this is a well-known MIME or IANA name.
    const char* cName = NULL;
    if ((cName = ucnv_getStandardName(icuCanonicalName, "MIME", &status)) != NULL) {
        return env->NewStringUTF(cName);
    } else if ((cName = ucnv_getStandardName(icuCanonicalName, "IANA", &status)) != NULL) {
        return env->NewStringUTF(cName);
    }

    // Check to see if an alias already exists with "x-" prefix, if yes then
    // make that the canonical name.
    int32_t aliasCount = ucnv_countAliases(icuCanonicalName, &status);
    for (int i = 0; i < aliasCount; ++i) {
        const char* name = ucnv_getAlias(icuCanonicalName, i, &status);
        if (name != NULL && name[0] == 'x' && name[1] == '-') {
            return env->NewStringUTF(name);
        }
    }

    // As a last resort, prepend "x-" to any alias and make that the canonical name.
    status = U_ZERO_ERROR;
    const char* name = ucnv_getStandardName(icuCanonicalName, "UTR22", &status);
    if (name == NULL && strchr(icuCanonicalName, ',') != NULL) {
        name = ucnv_getAlias(icuCanonicalName, 1, &status);
    }
    // If there is no UTR22 canonical name then just return the original name.
    if (name == NULL) {
        name = icuCanonicalName;
    }
    UniquePtr<char[]> result(new char[2 + strlen(name) + 1]);
    strcpy(&result[0], "x-");
    strcat(&result[0], name);
    return env->NewStringUTF(&result[0]);
}

static jobjectArray getAvailableCharsetNames(JNIEnv* env, jclass) {
    int32_t num = ucnv_countAvailable();
    jobjectArray result = env->NewObjectArray(num, env->FindClass("java/lang/String"), NULL);
    for (int i = 0; i < num; ++i) {
        const char* name = ucnv_getAvailableName(i);
        ScopedLocalRef<jstring> javaCanonicalName(env, getJavaCanonicalName(env, name));
        env->SetObjectArrayElement(result, i, javaCanonicalName.get());
    }
    return result;
}

static jobjectArray getAliases(JNIEnv* env, const char* icuCanonicalName) {
    // Get an upper bound on the number of aliases...
    const char* myEncName = icuCanonicalName;
    UErrorCode error = U_ZERO_ERROR;
    int32_t aliasCount = ucnv_countAliases(myEncName, &error);
    if (aliasCount == 0 && myEncName[0] == 'x' && myEncName[1] == '-') {
        myEncName = myEncName + 2;
        aliasCount = ucnv_countAliases(myEncName, &error);
    }
    if (!U_SUCCESS(error)) {
        return NULL;
    }

    // Collect the aliases we want...
    const char* aliasArray[aliasCount];
    int actualAliasCount = 0;
    for(int i = 0; i < aliasCount; ++i) {
        const char* name = ucnv_getAlias(myEncName, (uint16_t) i, &error);
        if (!U_SUCCESS(error)) {
            return NULL;
        }
        // TODO: why do we ignore these ones?
        if (strchr(name, '+') == 0 && strchr(name, ',') == 0) {
            aliasArray[actualAliasCount++]= name;
        }
    }

    // Convert our C++ char*[] into a Java String[]...
    jobjectArray result = env->NewObjectArray(actualAliasCount, env->FindClass("java/lang/String"), NULL);
    for (int i = 0; i < actualAliasCount; ++i) {
        ScopedLocalRef<jstring> alias(env, env->NewStringUTF(aliasArray[i]));
        env->SetObjectArrayElement(result, i, alias.get());
    }
    return result;
}

static const char* getICUCanonicalName(const char* name) {
    UErrorCode error = U_ZERO_ERROR;
    const char* canonicalName = NULL;
    if ((canonicalName = ucnv_getCanonicalName(name, "MIME", &error)) != NULL) {
        return canonicalName;
    } else if((canonicalName = ucnv_getCanonicalName(name, "IANA", &error)) != NULL) {
        return canonicalName;
    } else if((canonicalName = ucnv_getCanonicalName(name, "", &error)) != NULL) {
        return canonicalName;
    } else if((canonicalName =  ucnv_getAlias(name, 0, &error)) != NULL) {
        /* we have some aliases in the form x-blah .. match those first */
        return canonicalName;
    } else if (strstr(name, "x-") == name) {
        /* check if the converter can be opened with the name given */
        error = U_ZERO_ERROR;
        UConverter* conv = ucnv_open(name + 2, &error);
        if (conv != NULL) {
            ucnv_close(conv);
            return name + 2;
        }
    }
    return NULL;
}

static void CHARSET_ENCODER_CALLBACK(const void* rawContext, UConverterFromUnicodeArgs* args,
        const UChar* codeUnits, int32_t length, UChar32 codePoint, UConverterCallbackReason reason,
        UErrorCode* status) {
    if (!rawContext) {
        return;
    }
    const EncoderCallbackContext* ctx = reinterpret_cast<const EncoderCallbackContext*>(rawContext);
    switch(reason) {
    case UCNV_UNASSIGNED:
        ctx->onUnmappableInput(ctx, args, codeUnits, length, codePoint, reason, status);
        return;
    case UCNV_ILLEGAL:
    case UCNV_IRREGULAR:
        ctx->onMalformedInput(ctx, args, codeUnits, length, codePoint, reason, status);
        return;
    default:
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
}

static void encoderReplaceCallback(const void* rawContext,
        UConverterFromUnicodeArgs *fromArgs, const UChar*, int32_t, UChar32,
        UConverterCallbackReason, UErrorCode * err) {
    if (rawContext == NULL) {
        return;
    }
    const EncoderCallbackContext* context = reinterpret_cast<const EncoderCallbackContext*>(rawContext);
    *err = U_ZERO_ERROR;
    ucnv_cbFromUWriteBytes(fromArgs, context->subBytes, context->length, 0, err);
}

static UConverterFromUCallback getFromUCallback(int32_t mode) {
    switch(mode) {
    case NativeConverter_REPORT:
        return UCNV_FROM_U_CALLBACK_STOP;
    case NativeConverter_IGNORE:
        return UCNV_FROM_U_CALLBACK_SKIP;
    case NativeConverter_REPLACE:
        return encoderReplaceCallback;
    }
    abort();
}

static jint setCallbackEncode(JNIEnv* env, jclass, jlong handle, jint onMalformedInput, jint onUnmappableInput, jbyteArray subBytes) {
    UConverter* conv = (UConverter*)handle;
    if (!conv) {
        return U_ILLEGAL_ARGUMENT_ERROR;
    }
    UConverterFromUCallback fromUOldAction = NULL;
    void* fromUOldContext = NULL;
    ucnv_getFromUCallBack(conv, &fromUOldAction, const_cast<const void**>(&fromUOldContext));

    /* fromUOldContext can only be DecodeCallbackContext since
     * the converter created is private data for the decoder
     * and callbacks can only be set via this method!
     */
    EncoderCallbackContext* fromUNewContext=NULL;
    UConverterFromUCallback fromUNewAction=NULL;
    if (fromUOldContext == NULL) {
        fromUNewContext = new EncoderCallbackContext;
        fromUNewAction = CHARSET_ENCODER_CALLBACK;
    } else {
        fromUNewContext = (EncoderCallbackContext*) fromUOldContext;
        fromUNewAction = fromUOldAction;
        fromUOldAction = NULL;
        fromUOldContext = NULL;
    }
    fromUNewContext->onMalformedInput = getFromUCallback(onMalformedInput);
    fromUNewContext->onUnmappableInput = getFromUCallback(onUnmappableInput);
    ScopedByteArray sub(env, subBytes);
    if (sub.get() == NULL) {
        return U_ILLEGAL_ARGUMENT_ERROR;
    }
    fromUNewContext->length = sub.size();
    memcpy(fromUNewContext->subBytes, sub.get(), sub.size());
    UErrorCode errorCode = U_ZERO_ERROR;
    ucnv_setFromUCallBack(conv, fromUNewAction, fromUNewContext, &fromUOldAction, (const void**)&fromUOldContext, &errorCode);
    return errorCode;
}

static void decoderIgnoreCallback(const void*, UConverterToUnicodeArgs*, const char*, int32_t, UConverterCallbackReason, UErrorCode* err) {
    // The icu4c UCNV_FROM_U_CALLBACK_SKIP callback requires that the context is NULL, which is
    // never true for us.
    *err = U_ZERO_ERROR;
}

static void decoderReplaceCallback(const void* rawContext,
        UConverterToUnicodeArgs* toArgs, const char*, int32_t, UConverterCallbackReason,
        UErrorCode* err) {
    if (!rawContext) {
        return;
    }
    const DecoderCallbackContext* context = reinterpret_cast<const DecoderCallbackContext*>(rawContext);
    *err = U_ZERO_ERROR;
    ucnv_cbToUWriteUChars(toArgs,context->subUChars, context->length, 0, err);
}

static UConverterToUCallback getToUCallback(int32_t mode) {
    switch (mode) {
    case NativeConverter_IGNORE: return decoderIgnoreCallback;
    case NativeConverter_REPLACE: return decoderReplaceCallback;
    case NativeConverter_REPORT: return UCNV_TO_U_CALLBACK_STOP;
    }
    abort();
}

static void CHARSET_DECODER_CALLBACK(const void* rawContext, UConverterToUnicodeArgs* args,
        const char* codeUnits, int32_t length,
        UConverterCallbackReason reason, UErrorCode* status) {
    if (!rawContext) {
        return;
    }
    const DecoderCallbackContext* ctx = reinterpret_cast<const DecoderCallbackContext*>(rawContext);
    switch(reason) {
    case UCNV_UNASSIGNED:
        ctx->onUnmappableInput(ctx, args, codeUnits, length, reason, status);
        return;
    case UCNV_ILLEGAL:
    case UCNV_IRREGULAR:
        ctx->onMalformedInput(ctx, args, codeUnits, length, reason, status);
        return;
    default:
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
}

static jint setCallbackDecode(JNIEnv* env, jclass, jlong handle, jint onMalformedInput, jint onUnmappableInput, jcharArray subChars) {
    UConverter* conv = (UConverter*)handle;
    if (conv == NULL) {
        return U_ILLEGAL_ARGUMENT_ERROR;
    }

    UConverterToUCallback toUOldAction;
    void* toUOldContext;
    ucnv_getToUCallBack(conv, &toUOldAction, const_cast<const void**>(&toUOldContext));

    /* toUOldContext can only be DecodeCallbackContext since
     * the converter created is private data for the decoder
     * and callbacks can only be set via this method!
     */
    DecoderCallbackContext* toUNewContext = NULL;
    UConverterToUCallback toUNewAction = NULL;
    if (toUOldContext==NULL) {
        toUNewContext = new DecoderCallbackContext;
        toUNewAction = CHARSET_DECODER_CALLBACK;
    } else {
        toUNewContext = reinterpret_cast<DecoderCallbackContext*>(toUOldContext);
        toUNewAction = toUOldAction;
        toUOldAction = NULL;
        toUOldContext = NULL;
    }
    toUNewContext->onMalformedInput = getToUCallback(onMalformedInput);
    toUNewContext->onUnmappableInput = getToUCallback(onUnmappableInput);
    ScopedCharArray sub(env, subChars);
    if (sub.get() == NULL) {
        return U_ILLEGAL_ARGUMENT_ERROR;
    }
    toUNewContext->length = sub.size();
    u_strncpy(toUNewContext->subUChars, sub.get(), sub.size());
    UErrorCode errorCode = U_ZERO_ERROR;
    ucnv_setToUCallBack(conv, toUNewAction, toUNewContext,
            &toUOldAction, (const void**)&toUOldContext, &errorCode);
    return errorCode;
}

static jfloat getAveCharsPerByte(JNIEnv* env, jclass, jlong handle) {
    return (1 / (jfloat) getMaxBytesPerChar(env, NULL, handle));
}

static jbyteArray getSubstitutionBytes(JNIEnv* env, jclass, jlong handle) {
    const UConverter* cnv = reinterpret_cast<const UConverter*>(handle);
    if (cnv == NULL) {
        return NULL;
    }
    UErrorCode status = U_ZERO_ERROR;
    char subBytes[10];
    int8_t len = sizeof(subBytes);
    ucnv_getSubstChars(cnv, subBytes, &len, &status);
    if (!U_SUCCESS(status)) {
        return env->NewByteArray(0);
    }
    jbyteArray result = env->NewByteArray(len);
    if (result == NULL) {
        return NULL;
    }
    env->SetByteArrayRegion(result, 0, len, reinterpret_cast<jbyte*>(subBytes));
    return result;
}

static jboolean contains(JNIEnv*, jclass, jlong handle1, jlong handle2) {
    UErrorCode status = U_ZERO_ERROR;
    const UConverter * cnv1 = (const UConverter *) handle1;
    const UConverter * cnv2 = (const UConverter *) handle2;
    UBool bRet = 0;

    if(cnv1 != NULL && cnv2 != NULL) {
        /* open charset 1 */
        USet* set1 = uset_open(1, 2);
        ucnv_getUnicodeSet(cnv1, set1, UCNV_ROUNDTRIP_SET, &status);

        if(U_SUCCESS(status)) {
            /* open charset 2 */
            status = U_ZERO_ERROR;
            USet* set2 = uset_open(1, 2);
            ucnv_getUnicodeSet(cnv2, set2, UCNV_ROUNDTRIP_SET, &status);

            /* contains?      */
            if(U_SUCCESS(status)) {
                bRet = uset_containsAll(set1, set2);
                uset_close(set2);
            }
            uset_close(set1);
        }
    }
    return bRet;
}

static jobject charsetForName(JNIEnv* env, jclass, jstring charsetName) {
    ScopedUtfChars charsetNameChars(env, charsetName);
    if (!charsetNameChars.c_str()) {
        return NULL;
    }
    // Get ICU's canonical name for this charset.
    const char* icuCanonicalName = getICUCanonicalName(charsetNameChars.c_str());
    if (icuCanonicalName == NULL) {
        return NULL;
    }
    // Get Java's canonical name for this charset.
    jstring javaCanonicalName = getJavaCanonicalName(env, icuCanonicalName);
    if (env->ExceptionOccurred()) {
        return NULL;
    }

    // Check that this charset is supported.
    // ICU doesn't offer any "isSupported", so we just open and immediately close.
    // We ignore the UErrorCode because ucnv_open returning NULL is all the information we need.
    UErrorCode dummy = U_ZERO_ERROR;
    UConverter* conv = ucnv_open(icuCanonicalName, &dummy);
    if (conv == NULL) {
        return NULL;
    }
    ucnv_close(conv);

    // Get the aliases for this charset.
    jobjectArray aliases = getAliases(env, icuCanonicalName);
    if (env->ExceptionOccurred()) {
        return NULL;
    }

    // Construct the CharsetICU object.
    jclass charsetClass = env->FindClass("com/ibm/icu4jni/charset/CharsetICU");
    if (env->ExceptionOccurred()) {
        return NULL;
    }
    jmethodID charsetConstructor = env->GetMethodID(charsetClass, "<init>",
            "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V");
    if (env->ExceptionOccurred()) {
        return NULL;
    }
    return env->NewObject(charsetClass, charsetConstructor,
            javaCanonicalName, env->NewStringUTF(icuCanonicalName), aliases);
}

static JNINativeMethod gMethods[] = {
    { "canEncode", "(JI)Z", (void*) canEncode },
    { "charsetForName", "(Ljava/lang/String;)Ljava/nio/charset/Charset;", (void*) charsetForName },
    { "closeConverter", "(J)V", (void*) closeConverter },
    { "contains", "(JJ)Z", (void*) contains },
    { "decode", "(J[BI[CI[IZ)I", (void*) decode },
    { "encode", "(J[CI[BI[IZ)I", (void*) encode },
    { "flushByteToChar", "(J[CI[I)I", (void*) flushByteToChar },
    { "flushCharToByte", "(J[BI[I)I", (void*) flushCharToByte },
    { "getAvailableCharsetNames", "()[Ljava/lang/String;", (void*) getAvailableCharsetNames },
    { "getAveBytesPerChar", "(J)F", (void*) getAveBytesPerChar },
    { "getAveCharsPerByte", "(J)F", (void*) getAveCharsPerByte },
    { "getMaxBytesPerChar", "(J)I", (void*) getMaxBytesPerChar },
    { "getMinBytesPerChar", "(J)I", (void*) getMinBytesPerChar },
    { "getSubstitutionBytes", "(J)[B", (void*) getSubstitutionBytes },
    { "openConverter", "(Ljava/lang/String;)J", (void*) openConverter },
    { "resetByteToChar", "(J)V", (void*) resetByteToChar },
    { "resetCharToByte", "(J)V", (void*) resetCharToByte },
    { "setCallbackDecode", "(JII[C)I", (void*) setCallbackDecode },
    { "setCallbackEncode", "(JII[B)I", (void*) setCallbackEncode },
};
int register_com_ibm_icu4jni_converters_NativeConverter(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "com/ibm/icu4jni/charset/NativeConverter",
                gMethods, NELEM(gMethods));
}
