/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <signal.h>
#include <stdlib.h>

#include <jni.h>
#include <jlong.h>
#include "sun_misc_NativeSignalHandler.h"
#include "JNIHelp.h"

#define NATIVE_METHOD(className, functionName, signature) \
{ #functionName, signature, (void*)(className ## _ ## functionName) }

typedef void (*sig_handler_t)(jint, void *, void *);

JNIEXPORT void JNICALL
NativeSignalHandler_handle0(JNIEnv *env, jclass cls, jint sig, jlong f)
{
    /* We've lost the siginfo and context */
    (*(sig_handler_t)jlong_to_ptr(f))(sig, NULL, NULL);
}

static JNINativeMethod gMethods[] = {
  NATIVE_METHOD(NativeSignalHandler, handle0, "(IJ)V"),
};

void register_sun_misc_NativeSignalHandler(JNIEnv* env) {
  jniRegisterNativeMethods(env, "sun/misc/NativeSignalHandler", gMethods, NELEM(gMethods));
}
