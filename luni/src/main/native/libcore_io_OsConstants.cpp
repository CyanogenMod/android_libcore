/*
 * Copyright (C) 2011 The Android Open Source Project
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

#define LOG_TAG "OsConstants"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "ScopedLocalRef.h"

#include <errno.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

static void initConstant(JNIEnv* env, jclass c, const char* fieldName, int value) {
    jfieldID field = env->GetStaticFieldID(c, fieldName, "I");
    env->SetStaticIntField(c, field, value);
}

static void OsConstants_initConstants(JNIEnv* env, jclass c) {
    initConstant(env, c, "AF_INET", AF_INET);
    initConstant(env, c, "AF_INET6", AF_INET6);
    initConstant(env, c, "AF_UNIX", AF_UNIX);
    initConstant(env, c, "AF_UNSPEC", AF_UNSPEC);
    initConstant(env, c, "E2BIG", E2BIG);
    initConstant(env, c, "EACCES", EACCES);
    initConstant(env, c, "EADDRINUSE", EADDRINUSE);
    initConstant(env, c, "EADDRNOTAVAIL", EADDRNOTAVAIL);
    initConstant(env, c, "EAFNOSUPPORT", EAFNOSUPPORT);
    initConstant(env, c, "EAGAIN", EAGAIN);
    initConstant(env, c, "EALREADY", EALREADY);
    initConstant(env, c, "EBADF", EBADF);
    initConstant(env, c, "EBADMSG", EBADMSG);
    initConstant(env, c, "EBUSY", EBUSY);
    initConstant(env, c, "ECANCELED", ECANCELED);
    initConstant(env, c, "ECHILD", ECHILD);
    initConstant(env, c, "ECONNABORTED", ECONNABORTED);
    initConstant(env, c, "ECONNREFUSED", ECONNREFUSED);
    initConstant(env, c, "ECONNRESET", ECONNRESET);
    initConstant(env, c, "EDEADLK", EDEADLK);
    initConstant(env, c, "EDESTADDRREQ", EDESTADDRREQ);
    initConstant(env, c, "EDOM", EDOM);
    initConstant(env, c, "EDQUOT", EDQUOT);
    initConstant(env, c, "EEXIST", EEXIST);
    initConstant(env, c, "EFAULT", EFAULT);
    initConstant(env, c, "EFBIG", EFBIG);
    initConstant(env, c, "EHOSTUNREACH", EHOSTUNREACH);
    initConstant(env, c, "EIDRM", EIDRM);
    initConstant(env, c, "EILSEQ", EILSEQ);
    initConstant(env, c, "EINPROGRESS", EINPROGRESS);
    initConstant(env, c, "EINTR", EINTR);
    initConstant(env, c, "EINVAL", EINVAL);
    initConstant(env, c, "EIO", EIO);
    initConstant(env, c, "EISCONN", EISCONN);
    initConstant(env, c, "EISDIR", EISDIR);
    initConstant(env, c, "ELOOP", ELOOP);
    initConstant(env, c, "EMFILE", EMFILE);
    initConstant(env, c, "EMLINK", EMLINK);
    initConstant(env, c, "EMSGSIZE", EMSGSIZE);
    initConstant(env, c, "EMULTIHOP", EMULTIHOP);
    initConstant(env, c, "ENAMETOOLONG", ENAMETOOLONG);
    initConstant(env, c, "ENETDOWN", ENETDOWN);
    initConstant(env, c, "ENETRESET", ENETRESET);
    initConstant(env, c, "ENETUNREACH", ENETUNREACH);
    initConstant(env, c, "ENFILE", ENFILE);
    initConstant(env, c, "ENOBUFS", ENOBUFS);
    initConstant(env, c, "ENODATA", ENODATA);
    initConstant(env, c, "ENODEV", ENODEV);
    initConstant(env, c, "ENOENT", ENOENT);
    initConstant(env, c, "ENOEXEC", ENOEXEC);
    initConstant(env, c, "ENOLCK", ENOLCK);
    initConstant(env, c, "ENOLINK", ENOLINK);
    initConstant(env, c, "ENOMEM", ENOMEM);
    initConstant(env, c, "ENOMSG", ENOMSG);
    initConstant(env, c, "ENOPROTOOPT", ENOPROTOOPT);
    initConstant(env, c, "ENOSPC", ENOSPC);
    initConstant(env, c, "ENOSR", ENOSR);
    initConstant(env, c, "ENOSTR", ENOSTR);
    initConstant(env, c, "ENOSYS", ENOSYS);
    initConstant(env, c, "ENOTCONN", ENOTCONN);
    initConstant(env, c, "ENOTDIR", ENOTDIR);
    initConstant(env, c, "ENOTEMPTY", ENOTEMPTY);
    initConstant(env, c, "ENOTSOCK", ENOTSOCK);
    initConstant(env, c, "ENOTSUP", ENOTSUP);
    initConstant(env, c, "ENOTTY", ENOTTY);
    initConstant(env, c, "ENXIO", ENXIO);
    initConstant(env, c, "EOPNOTSUPP", EOPNOTSUPP);
    initConstant(env, c, "EOVERFLOW", EOVERFLOW);
    initConstant(env, c, "EPERM", EPERM);
    initConstant(env, c, "EPIPE", EPIPE);
    initConstant(env, c, "EPROTO", EPROTO);
    initConstant(env, c, "EPROTONOSUPPORT", EPROTONOSUPPORT);
    initConstant(env, c, "EPROTOTYPE", EPROTOTYPE);
    initConstant(env, c, "ERANGE", ERANGE);
    initConstant(env, c, "EROFS", EROFS);
    initConstant(env, c, "ESPIPE", ESPIPE);
    initConstant(env, c, "ESRCH", ESRCH);
    initConstant(env, c, "ESTALE", ESTALE);
    initConstant(env, c, "ETIME", ETIME);
    initConstant(env, c, "ETIMEDOUT", ETIMEDOUT);
    initConstant(env, c, "ETXTBSY", ETXTBSY);
    initConstant(env, c, "EWOULDBLOCK", EWOULDBLOCK);
    initConstant(env, c, "EXDEV", EXDEV);
    initConstant(env, c, "EXIT_FAILURE", EXIT_FAILURE);
    initConstant(env, c, "EXIT_SUCCESS", EXIT_SUCCESS);
    initConstant(env, c, "FD_CLOEXEC", FD_CLOEXEC);
    initConstant(env, c, "F_DUPFD", F_DUPFD);
    initConstant(env, c, "F_GETFD", F_GETFD);
    initConstant(env, c, "F_GETFL", F_GETFL);
    initConstant(env, c, "F_GETLK", F_GETLK);
    initConstant(env, c, "F_GETOWN", F_GETOWN);
    initConstant(env, c, "F_OK", F_OK);
    initConstant(env, c, "F_RDLCK", F_RDLCK);
    initConstant(env, c, "F_SETFD", F_SETFD);
    initConstant(env, c, "F_SETFL", F_SETFL);
    initConstant(env, c, "F_SETLK", F_SETLK);
    initConstant(env, c, "F_SETLKW", F_SETLKW);
    initConstant(env, c, "F_SETOWN", F_SETOWN);
    initConstant(env, c, "F_UNLCK", F_UNLCK);
    initConstant(env, c, "F_WRLCK", F_WRLCK);
    initConstant(env, c, "IPPROTO_ICMP", IPPROTO_ICMP);
    initConstant(env, c, "IPPROTO_IP", IPPROTO_IP);
    initConstant(env, c, "IPPROTO_IPV6", IPPROTO_IPV6);
    initConstant(env, c, "IPPROTO_RAW", IPPROTO_RAW);
    initConstant(env, c, "IPPROTO_TCP", IPPROTO_TCP);
    initConstant(env, c, "IPPROTO_UDP", IPPROTO_UDP);
    initConstant(env, c, "MAP_FIXED", MAP_FIXED);
    initConstant(env, c, "MAP_PRIVATE", MAP_PRIVATE);
    initConstant(env, c, "MAP_SHARED", MAP_SHARED);
    initConstant(env, c, "MCL_CURRENT", MCL_CURRENT);
    initConstant(env, c, "MCL_FUTURE", MCL_FUTURE);
    initConstant(env, c, "MSG_CTRUNC", MSG_CTRUNC);
    initConstant(env, c, "MSG_DONTROUTE", MSG_DONTROUTE);
    initConstant(env, c, "MSG_EOR", MSG_EOR);
    initConstant(env, c, "MSG_OOB", MSG_OOB);
    initConstant(env, c, "MSG_PEEK", MSG_PEEK);
    initConstant(env, c, "MSG_TRUNC", MSG_TRUNC);
    initConstant(env, c, "MSG_WAITALL", MSG_WAITALL);
    initConstant(env, c, "MS_ASYNC", MS_ASYNC);
    initConstant(env, c, "MS_INVALIDATE", MS_INVALIDATE);
    initConstant(env, c, "MS_SYNC", MS_SYNC);
    initConstant(env, c, "O_ACCMODE", O_ACCMODE);
    initConstant(env, c, "O_APPEND", O_APPEND);
    initConstant(env, c, "O_CREAT", O_CREAT);
    initConstant(env, c, "O_EXCL", O_EXCL);
    initConstant(env, c, "O_NOCTTY", O_NOCTTY);
    initConstant(env, c, "O_NONBLOCK", O_NONBLOCK);
    initConstant(env, c, "O_RDONLY", O_RDONLY);
    initConstant(env, c, "O_RDWR", O_RDWR);
    initConstant(env, c, "O_SYNC", O_SYNC);
    initConstant(env, c, "O_TRUNC", O_TRUNC);
    initConstant(env, c, "O_WRONLY", O_WRONLY);
    initConstant(env, c, "PROT_EXEC", PROT_EXEC);
    initConstant(env, c, "PROT_NONE", PROT_NONE);
    initConstant(env, c, "PROT_READ", PROT_READ);
    initConstant(env, c, "PROT_WRITE", PROT_WRITE);
    initConstant(env, c, "R_OK", R_OK);
    initConstant(env, c, "SEEK_CUR", SEEK_CUR);
    initConstant(env, c, "SEEK_END", SEEK_END);
    initConstant(env, c, "SEEK_SET", SEEK_SET);
    initConstant(env, c, "SHUT_RD", SHUT_RD);
    initConstant(env, c, "SHUT_RDWR", SHUT_RDWR);
    initConstant(env, c, "SHUT_WR", SHUT_WR);
    initConstant(env, c, "SOCK_DGRAM", SOCK_DGRAM);
    initConstant(env, c, "SOCK_RAW", SOCK_RAW);
    initConstant(env, c, "SOCK_SEQPACKET", SOCK_SEQPACKET);
    initConstant(env, c, "SOCK_STREAM", SOCK_STREAM);
    initConstant(env, c, "STDERR_FILENO", STDERR_FILENO);
    initConstant(env, c, "STDIN_FILENO", STDIN_FILENO);
    initConstant(env, c, "STDOUT_FILENO", STDOUT_FILENO);
    initConstant(env, c, "S_IFBLK", S_IFBLK);
    initConstant(env, c, "S_IFCHR", S_IFCHR);
    initConstant(env, c, "S_IFDIR", S_IFDIR);
    initConstant(env, c, "S_IFIFO", S_IFIFO);
    initConstant(env, c, "S_IFLNK", S_IFLNK);
    initConstant(env, c, "S_IFMT", S_IFMT);
    initConstant(env, c, "S_IFREG", S_IFREG);
    initConstant(env, c, "S_IFSOCK", S_IFSOCK);
    initConstant(env, c, "S_IRGRP", S_IRGRP);
    initConstant(env, c, "S_IROTH", S_IROTH);
    initConstant(env, c, "S_IRUSR", S_IRUSR);
    initConstant(env, c, "S_IRWXG", S_IRWXG);
    initConstant(env, c, "S_IRWXO", S_IRWXO);
    initConstant(env, c, "S_IRWXU", S_IRWXU);
    initConstant(env, c, "S_ISGID", S_ISGID);
    initConstant(env, c, "S_ISUID", S_ISUID);
    initConstant(env, c, "S_ISVTX", S_ISVTX);
    initConstant(env, c, "S_IWGRP", S_IWGRP);
    initConstant(env, c, "S_IWOTH", S_IWOTH);
    initConstant(env, c, "S_IWUSR", S_IWUSR);
    initConstant(env, c, "S_IXGRP", S_IXGRP);
    initConstant(env, c, "S_IXOTH", S_IXOTH);
    initConstant(env, c, "S_IXUSR", S_IXUSR);
    initConstant(env, c, "WCONTINUED", WCONTINUED);
    initConstant(env, c, "WEXITED", WEXITED);
    initConstant(env, c, "WNOHANG", WNOHANG);
    initConstant(env, c, "WNOWAIT", WNOWAIT);
    initConstant(env, c, "WSTOPPED", WSTOPPED);
    initConstant(env, c, "WUNTRACED", WUNTRACED);
    initConstant(env, c, "W_OK", W_OK);
    initConstant(env, c, "X_OK", X_OK);
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(OsConstants, initConstants, "()V"),
};
int register_libcore_io_OsConstants(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "libcore/io/OsConstants", gMethods, NELEM(gMethods));
}
