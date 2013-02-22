/*
 * Copyright (C) 2013 The Android Open Source Project
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

#include "JNIHelp.h"
#include "JniConstants.h"
#include "utils/Log.h"
#include "utils/misc.h"

#ifdef HAVE_ANDROID_OS  // Do not build for host

#include <elf.h>
#include <stdio.h>
#include <stddef.h>
#include <stdlib.h>
#include <errno.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>

#include <sys/prctl.h>

#include <linux/unistd.h>
#include <linux/audit.h>
#include <linux/filter.h>
#include <linux/seccomp.h>

#define syscall_nr (offsetof(struct seccomp_data, nr))
#define arch_nr (offsetof(struct seccomp_data, arch))

#ifdef ARCH_ARM
#define AUDIT_ARCH_NR AUDIT_ARCH_ARM
#elif defined(ARCH_X86)
#define AUDIT_ARCH_NR AUDIT_ARCH_I386
#elif defined(ARCH_MIPS)
#define AUDIT_ARCH_NR AUDIT_ARCH_MIPS
#else
#error "Could not determine AUDIT_ARCH_NR for this architecture"
#endif

#define VALIDATE_ARCHITECTURE \
    BPF_STMT(BPF_LD+BPF_W+BPF_ABS, arch_nr), \
    BPF_JUMP(BPF_JMP+BPF_JEQ+BPF_K, AUDIT_ARCH_NR, 1, 0), \
    BPF_STMT(BPF_RET+BPF_K, SECCOMP_RET_KILL)

#define EXAMINE_SYSCALL \
    BPF_STMT(BPF_LD+BPF_W+BPF_ABS, syscall_nr)

#define ALLOW_SYSCALL(name) \
    BPF_JUMP(BPF_JMP+BPF_JEQ+BPF_K, __NR_##name, 0, 1), \
    BPF_STMT(BPF_RET+BPF_K, SECCOMP_RET_ALLOW)

#define ALLOW \
    BPF_STMT(BPF_RET+BPF_K, SECCOMP_RET_ALLOW)

#define TRAP \
    BPF_STMT(BPF_RET+BPF_K, SECCOMP_RET_TRAP)

#define KILL \
    BPF_STMT(BPF_RET+BPF_K, SECCOMP_RET_KILL)

#define GET_NUM_ELEMENTS(structure) \
    sizeof(structure)/sizeof(structure[0])

#ifdef DEBUG_SECCOMP
#define HANDLE_SYSCALL(name) \
    BPF_JUMP(BPF_JMP+BPF_JEQ+BPF_K, __NR_##name, 0, 1), \
    BPF_STMT(BPF_RET+BPF_K, SECCOMP_RET_TRAP)
#else
#define HANDLE_SYSCALL(name) \
    BPF_JUMP(BPF_JMP+BPF_JEQ+BPF_K, __NR_##name, 0, 1), \
    BPF_STMT(BPF_RET+BPF_K, SECCOMP_RET_KILL)
#endif

typedef void (*Policy)(JNIEnv *env);

#ifdef DEBUG_SECCOMP
static void handle_trap(int) {
    ALOGI("SECCOMP: Trapped");
    dvmAbort();
}
#endif // DEBUG_SECCOMP

static void installFilter(JNIEnv *env, sock_filter *filters, size_t num_filters) {
    struct sock_fprog prog = {
        (unsigned short) num_filters,
        filters,
    };

    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) < 0) {
        ALOGI((char*)"SECCOMP: Could not set PR_SET_NO_NEW_PRIVS");
        jniThrowExceptionFmt(env,
                                "dalvik/system/SeccompFailureException",
                                "Could not set PR_SET_NO_NEW_PRIVS: %s",
                                strerror(errno));
        return;
    }

    if (prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &prog) < 0) {
        ALOGI("SECCOMP: Could not set seccomp filter");
        jniThrowExceptionFmt(env,
                                "dalvik/system/SeccompFailureException",
                                "Could not set seccomp filter: %s",
                                strerror(errno));
        return;
    }

#ifdef DEBUG_SECCOMP
    signal(SIGSYS, handle_trap);
#endif // DEBUG_SECCOMP

}

static void setNormalAppPolicy(JNIEnv *env) {
    struct sock_filter filter[] = {
        VALIDATE_ARCHITECTURE,
        EXAMINE_SYSCALL,
        HANDLE_SYSCALL(init_module),
        HANDLE_SYSCALL(delete_module),
        HANDLE_SYSCALL(mknod),
        HANDLE_SYSCALL(chroot),
        ALLOW,
    };
    installFilter(env, filter, GET_NUM_ELEMENTS(filter));
}

static void Seccomp_setPolicy(JNIEnv* env, jclass, jint policy_idx) {
    Policy policies[] = {
        setNormalAppPolicy     // APP_POLICY = 0
    };
    int num_policies = GET_NUM_ELEMENTS(policies);
    if (policy_idx >= num_policies) {
        jniThrowExceptionFmt(env,
                                "java/lang/IllegalArgumentException",
                                "Could not find specified policy %d",
                                policy_idx);
        return;
    }
    policies[policy_idx](env);
}

#else // Host build

static void Seccomp_setPolicy(JNIEnv* env, jclass, jint policy_idx) {
    jniThrowExceptionFmt(env,
                            "dalvik/system/SeccompFailureException",
                            "Seccomp not supported for host builds");
}

#endif // HAVE_ANDROID_OS

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(Seccomp, setPolicy, "(I)V"),
};

void register_dalvik_system_Seccomp(JNIEnv* env) {
    jniRegisterNativeMethods(env, "dalvik/system/Seccomp", gMethods, NELEM(gMethods));
}