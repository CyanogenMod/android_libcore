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

#include <errno.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

static jint OsConstants_get_AF_INET(JNIEnv*, jclass) { return AF_INET; }
static jint OsConstants_get_AF_INET6(JNIEnv*, jclass) { return AF_INET6; }
static jint OsConstants_get_AF_UNIX(JNIEnv*, jclass) { return AF_UNIX; }
static jint OsConstants_get_AF_UNSPEC(JNIEnv*, jclass) { return AF_UNSPEC; }
static jint OsConstants_get_E2BIG(JNIEnv*, jclass) { return E2BIG; }
static jint OsConstants_get_EACCES(JNIEnv*, jclass) { return EACCES; }
static jint OsConstants_get_EADDRINUSE(JNIEnv*, jclass) { return EADDRINUSE; }
static jint OsConstants_get_EADDRNOTAVAIL(JNIEnv*, jclass) { return EADDRNOTAVAIL; }
static jint OsConstants_get_EAFNOSUPPORT(JNIEnv*, jclass) { return EAFNOSUPPORT; }
static jint OsConstants_get_EAGAIN(JNIEnv*, jclass) { return EAGAIN; }
static jint OsConstants_get_EALREADY(JNIEnv*, jclass) { return EALREADY; }
static jint OsConstants_get_EBADF(JNIEnv*, jclass) { return EBADF; }
static jint OsConstants_get_EBADMSG(JNIEnv*, jclass) { return EBADMSG; }
static jint OsConstants_get_EBUSY(JNIEnv*, jclass) { return EBUSY; }
static jint OsConstants_get_ECANCELED(JNIEnv*, jclass) { return ECANCELED; }
static jint OsConstants_get_ECHILD(JNIEnv*, jclass) { return ECHILD; }
static jint OsConstants_get_ECONNABORTED(JNIEnv*, jclass) { return ECONNABORTED; }
static jint OsConstants_get_ECONNREFUSED(JNIEnv*, jclass) { return ECONNREFUSED; }
static jint OsConstants_get_ECONNRESET(JNIEnv*, jclass) { return ECONNRESET; }
static jint OsConstants_get_EDEADLK(JNIEnv*, jclass) { return EDEADLK; }
static jint OsConstants_get_EDESTADDRREQ(JNIEnv*, jclass) { return EDESTADDRREQ; }
static jint OsConstants_get_EDOM(JNIEnv*, jclass) { return EDOM; }
static jint OsConstants_get_EDQUOT(JNIEnv*, jclass) { return EDQUOT; }
static jint OsConstants_get_EEXIST(JNIEnv*, jclass) { return EEXIST; }
static jint OsConstants_get_EFAULT(JNIEnv*, jclass) { return EFAULT; }
static jint OsConstants_get_EFBIG(JNIEnv*, jclass) { return EFBIG; }
static jint OsConstants_get_EHOSTUNREACH(JNIEnv*, jclass) { return EHOSTUNREACH; }
static jint OsConstants_get_EIDRM(JNIEnv*, jclass) { return EIDRM; }
static jint OsConstants_get_EILSEQ(JNIEnv*, jclass) { return EILSEQ; }
static jint OsConstants_get_EINPROGRESS(JNIEnv*, jclass) { return EINPROGRESS; }
static jint OsConstants_get_EINTR(JNIEnv*, jclass) { return EINTR; }
static jint OsConstants_get_EINVAL(JNIEnv*, jclass) { return EINVAL; }
static jint OsConstants_get_EIO(JNIEnv*, jclass) { return EIO; }
static jint OsConstants_get_EISCONN(JNIEnv*, jclass) { return EISCONN; }
static jint OsConstants_get_EISDIR(JNIEnv*, jclass) { return EISDIR; }
static jint OsConstants_get_ELOOP(JNIEnv*, jclass) { return ELOOP; }
static jint OsConstants_get_EMFILE(JNIEnv*, jclass) { return EMFILE; }
static jint OsConstants_get_EMLINK(JNIEnv*, jclass) { return EMLINK; }
static jint OsConstants_get_EMSGSIZE(JNIEnv*, jclass) { return EMSGSIZE; }
static jint OsConstants_get_EMULTIHOP(JNIEnv*, jclass) { return EMULTIHOP; }
static jint OsConstants_get_ENAMETOOLONG(JNIEnv*, jclass) { return ENAMETOOLONG; }
static jint OsConstants_get_ENETDOWN(JNIEnv*, jclass) { return ENETDOWN; }
static jint OsConstants_get_ENETRESET(JNIEnv*, jclass) { return ENETRESET; }
static jint OsConstants_get_ENETUNREACH(JNIEnv*, jclass) { return ENETUNREACH; }
static jint OsConstants_get_ENFILE(JNIEnv*, jclass) { return ENFILE; }
static jint OsConstants_get_ENOBUFS(JNIEnv*, jclass) { return ENOBUFS; }
static jint OsConstants_get_ENODATA(JNIEnv*, jclass) { return ENODATA; }
static jint OsConstants_get_ENODEV(JNIEnv*, jclass) { return ENODEV; }
static jint OsConstants_get_ENOENT(JNIEnv*, jclass) { return ENOENT; }
static jint OsConstants_get_ENOEXEC(JNIEnv*, jclass) { return ENOEXEC; }
static jint OsConstants_get_ENOLCK(JNIEnv*, jclass) { return ENOLCK; }
static jint OsConstants_get_ENOLINK(JNIEnv*, jclass) { return ENOLINK; }
static jint OsConstants_get_ENOMEM(JNIEnv*, jclass) { return ENOMEM; }
static jint OsConstants_get_ENOMSG(JNIEnv*, jclass) { return ENOMSG; }
static jint OsConstants_get_ENOPROTOOPT(JNIEnv*, jclass) { return ENOPROTOOPT; }
static jint OsConstants_get_ENOSPC(JNIEnv*, jclass) { return ENOSPC; }
static jint OsConstants_get_ENOSR(JNIEnv*, jclass) { return ENOSR; }
static jint OsConstants_get_ENOSTR(JNIEnv*, jclass) { return ENOSTR; }
static jint OsConstants_get_ENOSYS(JNIEnv*, jclass) { return ENOSYS; }
static jint OsConstants_get_ENOTCONN(JNIEnv*, jclass) { return ENOTCONN; }
static jint OsConstants_get_ENOTDIR(JNIEnv*, jclass) { return ENOTDIR; }
static jint OsConstants_get_ENOTEMPTY(JNIEnv*, jclass) { return ENOTEMPTY; }
static jint OsConstants_get_ENOTSOCK(JNIEnv*, jclass) { return ENOTSOCK; }
static jint OsConstants_get_ENOTSUP(JNIEnv*, jclass) { return ENOTSUP; }
static jint OsConstants_get_ENOTTY(JNIEnv*, jclass) { return ENOTTY; }
static jint OsConstants_get_ENXIO(JNIEnv*, jclass) { return ENXIO; }
static jint OsConstants_get_EOPNOTSUPP(JNIEnv*, jclass) { return EOPNOTSUPP; }
static jint OsConstants_get_EOVERFLOW(JNIEnv*, jclass) { return EOVERFLOW; }
static jint OsConstants_get_EPERM(JNIEnv*, jclass) { return EPERM; }
static jint OsConstants_get_EPIPE(JNIEnv*, jclass) { return EPIPE; }
static jint OsConstants_get_EPROTO(JNIEnv*, jclass) { return EPROTO; }
static jint OsConstants_get_EPROTONOSUPPORT(JNIEnv*, jclass) { return EPROTONOSUPPORT; }
static jint OsConstants_get_EPROTOTYPE(JNIEnv*, jclass) { return EPROTOTYPE; }
static jint OsConstants_get_ERANGE(JNIEnv*, jclass) { return ERANGE; }
static jint OsConstants_get_EROFS(JNIEnv*, jclass) { return EROFS; }
static jint OsConstants_get_ESPIPE(JNIEnv*, jclass) { return ESPIPE; }
static jint OsConstants_get_ESRCH(JNIEnv*, jclass) { return ESRCH; }
static jint OsConstants_get_ESTALE(JNIEnv*, jclass) { return ESTALE; }
static jint OsConstants_get_ETIME(JNIEnv*, jclass) { return ETIME; }
static jint OsConstants_get_ETIMEDOUT(JNIEnv*, jclass) { return ETIMEDOUT; }
static jint OsConstants_get_ETXTBSY(JNIEnv*, jclass) { return ETXTBSY; }
static jint OsConstants_get_EWOULDBLOCK(JNIEnv*, jclass) { return EWOULDBLOCK; }
static jint OsConstants_get_EXDEV(JNIEnv*, jclass) { return EXDEV; }
static jint OsConstants_get_EXIT_FAILURE(JNIEnv*, jclass) { return EXIT_FAILURE; }
static jint OsConstants_get_EXIT_SUCCESS(JNIEnv*, jclass) { return EXIT_SUCCESS; }
static jint OsConstants_get_FD_CLOEXEC(JNIEnv*, jclass) { return FD_CLOEXEC; }
static jint OsConstants_get_F_DUPFD(JNIEnv*, jclass) { return F_DUPFD; }
static jint OsConstants_get_F_GETFD(JNIEnv*, jclass) { return F_GETFD; }
static jint OsConstants_get_F_GETFL(JNIEnv*, jclass) { return F_GETFL; }
static jint OsConstants_get_F_GETLK(JNIEnv*, jclass) { return F_GETLK; }
static jint OsConstants_get_F_GETOWN(JNIEnv*, jclass) { return F_GETOWN; }
static jint OsConstants_get_F_OK(JNIEnv*, jclass) { return F_OK; }
static jint OsConstants_get_F_RDLCK(JNIEnv*, jclass) { return F_RDLCK; }
static jint OsConstants_get_F_SETFD(JNIEnv*, jclass) { return F_SETFD; }
static jint OsConstants_get_F_SETFL(JNIEnv*, jclass) { return F_SETFL; }
static jint OsConstants_get_F_SETLK(JNIEnv*, jclass) { return F_SETLK; }
static jint OsConstants_get_F_SETLKW(JNIEnv*, jclass) { return F_SETLKW; }
static jint OsConstants_get_F_SETOWN(JNIEnv*, jclass) { return F_SETOWN; }
static jint OsConstants_get_F_UNLCK(JNIEnv*, jclass) { return F_UNLCK; }
static jint OsConstants_get_F_WRLCK(JNIEnv*, jclass) { return F_WRLCK; }
static jint OsConstants_get_IPPROTO_ICMP(JNIEnv*, jclass) { return IPPROTO_ICMP; }
static jint OsConstants_get_IPPROTO_IP(JNIEnv*, jclass) { return IPPROTO_IP; }
static jint OsConstants_get_IPPROTO_IPV6(JNIEnv*, jclass) { return IPPROTO_IPV6; }
static jint OsConstants_get_IPPROTO_RAW(JNIEnv*, jclass) { return IPPROTO_RAW; }
static jint OsConstants_get_IPPROTO_TCP(JNIEnv*, jclass) { return IPPROTO_TCP; }
static jint OsConstants_get_IPPROTO_UDP(JNIEnv*, jclass) { return IPPROTO_UDP; }
static jint OsConstants_get_MAP_FIXED(JNIEnv*, jclass) { return MAP_FIXED; }
static jint OsConstants_get_MAP_PRIVATE(JNIEnv*, jclass) { return MAP_PRIVATE; }
static jint OsConstants_get_MAP_SHARED(JNIEnv*, jclass) { return MAP_SHARED; }
static jint OsConstants_get_MCL_CURRENT(JNIEnv*, jclass) { return MCL_CURRENT; }
static jint OsConstants_get_MCL_FUTURE(JNIEnv*, jclass) { return MCL_FUTURE; }
static jint OsConstants_get_MSG_CTRUNC(JNIEnv*, jclass) { return MSG_CTRUNC; }
static jint OsConstants_get_MSG_DONTROUTE(JNIEnv*, jclass) { return MSG_DONTROUTE; }
static jint OsConstants_get_MSG_EOR(JNIEnv*, jclass) { return MSG_EOR; }
static jint OsConstants_get_MSG_OOB(JNIEnv*, jclass) { return MSG_OOB; }
static jint OsConstants_get_MSG_PEEK(JNIEnv*, jclass) { return MSG_PEEK; }
static jint OsConstants_get_MSG_TRUNC(JNIEnv*, jclass) { return MSG_TRUNC; }
static jint OsConstants_get_MSG_WAITALL(JNIEnv*, jclass) { return MSG_WAITALL; }
static jint OsConstants_get_MS_ASYNC(JNIEnv*, jclass) { return MS_ASYNC; }
static jint OsConstants_get_MS_INVALIDATE(JNIEnv*, jclass) { return MS_INVALIDATE; }
static jint OsConstants_get_MS_SYNC(JNIEnv*, jclass) { return MS_SYNC; }
static jint OsConstants_get_O_ACCMODE(JNIEnv*, jclass) { return O_ACCMODE; }
static jint OsConstants_get_O_APPEND(JNIEnv*, jclass) { return O_APPEND; }
static jint OsConstants_get_O_CREAT(JNIEnv*, jclass) { return O_CREAT; }
static jint OsConstants_get_O_EXCL(JNIEnv*, jclass) { return O_EXCL; }
static jint OsConstants_get_O_NOCTTY(JNIEnv*, jclass) { return O_NOCTTY; }
static jint OsConstants_get_O_NONBLOCK(JNIEnv*, jclass) { return O_NONBLOCK; }
static jint OsConstants_get_O_RDONLY(JNIEnv*, jclass) { return O_RDONLY; }
static jint OsConstants_get_O_RDWR(JNIEnv*, jclass) { return O_RDWR; }
static jint OsConstants_get_O_SYNC(JNIEnv*, jclass) { return O_SYNC; }
static jint OsConstants_get_O_TRUNC(JNIEnv*, jclass) { return O_TRUNC; }
static jint OsConstants_get_O_WRONLY(JNIEnv*, jclass) { return O_WRONLY; }
static jint OsConstants_get_PROT_EXEC(JNIEnv*, jclass) { return PROT_EXEC; }
static jint OsConstants_get_PROT_NONE(JNIEnv*, jclass) { return PROT_NONE; }
static jint OsConstants_get_PROT_READ(JNIEnv*, jclass) { return PROT_READ; }
static jint OsConstants_get_PROT_WRITE(JNIEnv*, jclass) { return PROT_WRITE; }
static jint OsConstants_get_R_OK(JNIEnv*, jclass) { return R_OK; }
static jint OsConstants_get_SEEK_CUR(JNIEnv*, jclass) { return SEEK_CUR; }
static jint OsConstants_get_SEEK_END(JNIEnv*, jclass) { return SEEK_END; }
static jint OsConstants_get_SEEK_SET(JNIEnv*, jclass) { return SEEK_SET; }
static jint OsConstants_get_SHUT_RD(JNIEnv*, jclass) { return SHUT_RD; }
static jint OsConstants_get_SHUT_RDWR(JNIEnv*, jclass) { return SHUT_RDWR; }
static jint OsConstants_get_SHUT_WR(JNIEnv*, jclass) { return SHUT_WR; }
static jint OsConstants_get_SOCK_DGRAM(JNIEnv*, jclass) { return SOCK_DGRAM; }
static jint OsConstants_get_SOCK_RAW(JNIEnv*, jclass) { return SOCK_RAW; }
static jint OsConstants_get_SOCK_SEQPACKET(JNIEnv*, jclass) { return SOCK_SEQPACKET; }
static jint OsConstants_get_SOCK_STREAM(JNIEnv*, jclass) { return SOCK_STREAM; }
static jint OsConstants_get_STDERR_FILENO(JNIEnv*, jclass) { return STDERR_FILENO; }
static jint OsConstants_get_STDIN_FILENO(JNIEnv*, jclass) { return STDIN_FILENO; }
static jint OsConstants_get_STDOUT_FILENO(JNIEnv*, jclass) { return STDOUT_FILENO; }
static jint OsConstants_get_S_IFBLK(JNIEnv*, jclass) { return S_IFBLK; }
static jint OsConstants_get_S_IFCHR(JNIEnv*, jclass) { return S_IFCHR; }
static jint OsConstants_get_S_IFDIR(JNIEnv*, jclass) { return S_IFDIR; }
static jint OsConstants_get_S_IFIFO(JNIEnv*, jclass) { return S_IFIFO; }
static jint OsConstants_get_S_IFLNK(JNIEnv*, jclass) { return S_IFLNK; }
static jint OsConstants_get_S_IFMT(JNIEnv*, jclass) { return S_IFMT; }
static jint OsConstants_get_S_IFREG(JNIEnv*, jclass) { return S_IFREG; }
static jint OsConstants_get_S_IFSOCK(JNIEnv*, jclass) { return S_IFSOCK; }
static jint OsConstants_get_S_IRGRP(JNIEnv*, jclass) { return S_IRGRP; }
static jint OsConstants_get_S_IROTH(JNIEnv*, jclass) { return S_IROTH; }
static jint OsConstants_get_S_IRUSR(JNIEnv*, jclass) { return S_IRUSR; }
static jint OsConstants_get_S_IRWXG(JNIEnv*, jclass) { return S_IRWXG; }
static jint OsConstants_get_S_IRWXO(JNIEnv*, jclass) { return S_IRWXO; }
static jint OsConstants_get_S_IRWXU(JNIEnv*, jclass) { return S_IRWXU; }
static jint OsConstants_get_S_ISGID(JNIEnv*, jclass) { return S_ISGID; }
static jint OsConstants_get_S_ISUID(JNIEnv*, jclass) { return S_ISUID; }
static jint OsConstants_get_S_ISVTX(JNIEnv*, jclass) { return S_ISVTX; }
static jint OsConstants_get_S_IWGRP(JNIEnv*, jclass) { return S_IWGRP; }
static jint OsConstants_get_S_IWOTH(JNIEnv*, jclass) { return S_IWOTH; }
static jint OsConstants_get_S_IWUSR(JNIEnv*, jclass) { return S_IWUSR; }
static jint OsConstants_get_S_IXGRP(JNIEnv*, jclass) { return S_IXGRP; }
static jint OsConstants_get_S_IXOTH(JNIEnv*, jclass) { return S_IXOTH; }
static jint OsConstants_get_S_IXUSR(JNIEnv*, jclass) { return S_IXUSR; }
static jint OsConstants_get_WCONTINUED(JNIEnv*, jclass) { return WCONTINUED; }
static jint OsConstants_get_WEXITED(JNIEnv*, jclass) { return WEXITED; }
static jint OsConstants_get_WNOHANG(JNIEnv*, jclass) { return WNOHANG; }
static jint OsConstants_get_WNOWAIT(JNIEnv*, jclass) { return WNOWAIT; }
static jint OsConstants_get_WSTOPPED(JNIEnv*, jclass) { return WSTOPPED; }
static jint OsConstants_get_WUNTRACED(JNIEnv*, jclass) { return WUNTRACED; }
static jint OsConstants_get_W_OK(JNIEnv*, jclass) { return W_OK; }
static jint OsConstants_get_X_OK(JNIEnv*, jclass) { return X_OK; }

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(OsConstants, get_AF_INET, "()I"),
    NATIVE_METHOD(OsConstants, get_AF_INET6, "()I"),
    NATIVE_METHOD(OsConstants, get_AF_UNIX, "()I"),
    NATIVE_METHOD(OsConstants, get_AF_UNSPEC, "()I"),
    NATIVE_METHOD(OsConstants, get_E2BIG, "()I"),
    NATIVE_METHOD(OsConstants, get_EACCES, "()I"),
    NATIVE_METHOD(OsConstants, get_EADDRINUSE, "()I"),
    NATIVE_METHOD(OsConstants, get_EADDRNOTAVAIL, "()I"),
    NATIVE_METHOD(OsConstants, get_EAFNOSUPPORT, "()I"),
    NATIVE_METHOD(OsConstants, get_EAGAIN, "()I"),
    NATIVE_METHOD(OsConstants, get_EALREADY, "()I"),
    NATIVE_METHOD(OsConstants, get_EBADF, "()I"),
    NATIVE_METHOD(OsConstants, get_EBADMSG, "()I"),
    NATIVE_METHOD(OsConstants, get_EBUSY, "()I"),
    NATIVE_METHOD(OsConstants, get_ECANCELED, "()I"),
    NATIVE_METHOD(OsConstants, get_ECHILD, "()I"),
    NATIVE_METHOD(OsConstants, get_ECONNABORTED, "()I"),
    NATIVE_METHOD(OsConstants, get_ECONNREFUSED, "()I"),
    NATIVE_METHOD(OsConstants, get_ECONNRESET, "()I"),
    NATIVE_METHOD(OsConstants, get_EDEADLK, "()I"),
    NATIVE_METHOD(OsConstants, get_EDESTADDRREQ, "()I"),
    NATIVE_METHOD(OsConstants, get_EDOM, "()I"),
    NATIVE_METHOD(OsConstants, get_EDQUOT, "()I"),
    NATIVE_METHOD(OsConstants, get_EEXIST, "()I"),
    NATIVE_METHOD(OsConstants, get_EFAULT, "()I"),
    NATIVE_METHOD(OsConstants, get_EFBIG, "()I"),
    NATIVE_METHOD(OsConstants, get_EHOSTUNREACH, "()I"),
    NATIVE_METHOD(OsConstants, get_EIDRM, "()I"),
    NATIVE_METHOD(OsConstants, get_EILSEQ, "()I"),
    NATIVE_METHOD(OsConstants, get_EINPROGRESS, "()I"),
    NATIVE_METHOD(OsConstants, get_EINTR, "()I"),
    NATIVE_METHOD(OsConstants, get_EINVAL, "()I"),
    NATIVE_METHOD(OsConstants, get_EIO, "()I"),
    NATIVE_METHOD(OsConstants, get_EISCONN, "()I"),
    NATIVE_METHOD(OsConstants, get_EISDIR, "()I"),
    NATIVE_METHOD(OsConstants, get_ELOOP, "()I"),
    NATIVE_METHOD(OsConstants, get_EMFILE, "()I"),
    NATIVE_METHOD(OsConstants, get_EMLINK, "()I"),
    NATIVE_METHOD(OsConstants, get_EMSGSIZE, "()I"),
    NATIVE_METHOD(OsConstants, get_EMULTIHOP, "()I"),
    NATIVE_METHOD(OsConstants, get_ENAMETOOLONG, "()I"),
    NATIVE_METHOD(OsConstants, get_ENETDOWN, "()I"),
    NATIVE_METHOD(OsConstants, get_ENETRESET, "()I"),
    NATIVE_METHOD(OsConstants, get_ENETUNREACH, "()I"),
    NATIVE_METHOD(OsConstants, get_ENFILE, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOBUFS, "()I"),
    NATIVE_METHOD(OsConstants, get_ENODATA, "()I"),
    NATIVE_METHOD(OsConstants, get_ENODEV, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOENT, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOEXEC, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOLCK, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOLINK, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOMEM, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOMSG, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOPROTOOPT, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOSPC, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOSR, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOSTR, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOSYS, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOTCONN, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOTDIR, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOTEMPTY, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOTSOCK, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOTSUP, "()I"),
    NATIVE_METHOD(OsConstants, get_ENOTTY, "()I"),
    NATIVE_METHOD(OsConstants, get_ENXIO, "()I"),
    NATIVE_METHOD(OsConstants, get_EOPNOTSUPP, "()I"),
    NATIVE_METHOD(OsConstants, get_EOVERFLOW, "()I"),
    NATIVE_METHOD(OsConstants, get_EPERM, "()I"),
    NATIVE_METHOD(OsConstants, get_EPIPE, "()I"),
    NATIVE_METHOD(OsConstants, get_EPROTO, "()I"),
    NATIVE_METHOD(OsConstants, get_EPROTONOSUPPORT, "()I"),
    NATIVE_METHOD(OsConstants, get_EPROTOTYPE, "()I"),
    NATIVE_METHOD(OsConstants, get_ERANGE, "()I"),
    NATIVE_METHOD(OsConstants, get_EROFS, "()I"),
    NATIVE_METHOD(OsConstants, get_ESPIPE, "()I"),
    NATIVE_METHOD(OsConstants, get_ESRCH, "()I"),
    NATIVE_METHOD(OsConstants, get_ESTALE, "()I"),
    NATIVE_METHOD(OsConstants, get_ETIME, "()I"),
    NATIVE_METHOD(OsConstants, get_ETIMEDOUT, "()I"),
    NATIVE_METHOD(OsConstants, get_ETXTBSY, "()I"),
    NATIVE_METHOD(OsConstants, get_EWOULDBLOCK, "()I"),
    NATIVE_METHOD(OsConstants, get_EXDEV, "()I"),
    NATIVE_METHOD(OsConstants, get_EXIT_FAILURE, "()I"),
    NATIVE_METHOD(OsConstants, get_EXIT_SUCCESS, "()I"),
    NATIVE_METHOD(OsConstants, get_FD_CLOEXEC, "()I"),
    NATIVE_METHOD(OsConstants, get_F_DUPFD, "()I"),
    NATIVE_METHOD(OsConstants, get_F_GETFD, "()I"),
    NATIVE_METHOD(OsConstants, get_F_GETFL, "()I"),
    NATIVE_METHOD(OsConstants, get_F_GETLK, "()I"),
    NATIVE_METHOD(OsConstants, get_F_GETOWN, "()I"),
    NATIVE_METHOD(OsConstants, get_F_OK, "()I"),
    NATIVE_METHOD(OsConstants, get_F_RDLCK, "()I"),
    NATIVE_METHOD(OsConstants, get_F_SETFD, "()I"),
    NATIVE_METHOD(OsConstants, get_F_SETFL, "()I"),
    NATIVE_METHOD(OsConstants, get_F_SETLK, "()I"),
    NATIVE_METHOD(OsConstants, get_F_SETLKW, "()I"),
    NATIVE_METHOD(OsConstants, get_F_SETOWN, "()I"),
    NATIVE_METHOD(OsConstants, get_F_UNLCK, "()I"),
    NATIVE_METHOD(OsConstants, get_F_WRLCK, "()I"),
    NATIVE_METHOD(OsConstants, get_IPPROTO_ICMP, "()I"),
    NATIVE_METHOD(OsConstants, get_IPPROTO_IP, "()I"),
    NATIVE_METHOD(OsConstants, get_IPPROTO_IPV6, "()I"),
    NATIVE_METHOD(OsConstants, get_IPPROTO_RAW, "()I"),
    NATIVE_METHOD(OsConstants, get_IPPROTO_TCP, "()I"),
    NATIVE_METHOD(OsConstants, get_IPPROTO_UDP, "()I"),
    NATIVE_METHOD(OsConstants, get_MAP_FIXED, "()I"),
    NATIVE_METHOD(OsConstants, get_MAP_PRIVATE, "()I"),
    NATIVE_METHOD(OsConstants, get_MAP_SHARED, "()I"),
    NATIVE_METHOD(OsConstants, get_MCL_CURRENT, "()I"),
    NATIVE_METHOD(OsConstants, get_MCL_FUTURE, "()I"),
    NATIVE_METHOD(OsConstants, get_MSG_CTRUNC, "()I"),
    NATIVE_METHOD(OsConstants, get_MSG_DONTROUTE, "()I"),
    NATIVE_METHOD(OsConstants, get_MSG_EOR, "()I"),
    NATIVE_METHOD(OsConstants, get_MSG_OOB, "()I"),
    NATIVE_METHOD(OsConstants, get_MSG_PEEK, "()I"),
    NATIVE_METHOD(OsConstants, get_MSG_TRUNC, "()I"),
    NATIVE_METHOD(OsConstants, get_MSG_WAITALL, "()I"),
    NATIVE_METHOD(OsConstants, get_MS_ASYNC, "()I"),
    NATIVE_METHOD(OsConstants, get_MS_INVALIDATE, "()I"),
    NATIVE_METHOD(OsConstants, get_MS_SYNC, "()I"),
    NATIVE_METHOD(OsConstants, get_O_ACCMODE, "()I"),
    NATIVE_METHOD(OsConstants, get_O_APPEND, "()I"),
    NATIVE_METHOD(OsConstants, get_O_CREAT, "()I"),
    NATIVE_METHOD(OsConstants, get_O_EXCL, "()I"),
    NATIVE_METHOD(OsConstants, get_O_NOCTTY, "()I"),
    NATIVE_METHOD(OsConstants, get_O_NONBLOCK, "()I"),
    NATIVE_METHOD(OsConstants, get_O_RDONLY, "()I"),
    NATIVE_METHOD(OsConstants, get_O_RDWR, "()I"),
    NATIVE_METHOD(OsConstants, get_O_SYNC, "()I"),
    NATIVE_METHOD(OsConstants, get_O_TRUNC, "()I"),
    NATIVE_METHOD(OsConstants, get_O_WRONLY, "()I"),
    NATIVE_METHOD(OsConstants, get_PROT_EXEC, "()I"),
    NATIVE_METHOD(OsConstants, get_PROT_NONE, "()I"),
    NATIVE_METHOD(OsConstants, get_PROT_READ, "()I"),
    NATIVE_METHOD(OsConstants, get_PROT_WRITE, "()I"),
    NATIVE_METHOD(OsConstants, get_R_OK, "()I"),
    NATIVE_METHOD(OsConstants, get_SEEK_CUR, "()I"),
    NATIVE_METHOD(OsConstants, get_SEEK_END, "()I"),
    NATIVE_METHOD(OsConstants, get_SEEK_SET, "()I"),
    NATIVE_METHOD(OsConstants, get_SHUT_RD, "()I"),
    NATIVE_METHOD(OsConstants, get_SHUT_RDWR, "()I"),
    NATIVE_METHOD(OsConstants, get_SHUT_WR, "()I"),
    NATIVE_METHOD(OsConstants, get_SOCK_DGRAM, "()I"),
    NATIVE_METHOD(OsConstants, get_SOCK_RAW, "()I"),
    NATIVE_METHOD(OsConstants, get_SOCK_SEQPACKET, "()I"),
    NATIVE_METHOD(OsConstants, get_SOCK_STREAM, "()I"),
    NATIVE_METHOD(OsConstants, get_STDERR_FILENO, "()I"),
    NATIVE_METHOD(OsConstants, get_STDIN_FILENO, "()I"),
    NATIVE_METHOD(OsConstants, get_STDOUT_FILENO, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IFBLK, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IFCHR, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IFDIR, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IFIFO, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IFLNK, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IFMT, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IFREG, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IFSOCK, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IRGRP, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IROTH, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IRUSR, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IRWXG, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IRWXO, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IRWXU, "()I"),
    NATIVE_METHOD(OsConstants, get_S_ISGID, "()I"),
    NATIVE_METHOD(OsConstants, get_S_ISUID, "()I"),
    NATIVE_METHOD(OsConstants, get_S_ISVTX, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IWGRP, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IWOTH, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IWUSR, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IXGRP, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IXOTH, "()I"),
    NATIVE_METHOD(OsConstants, get_S_IXUSR, "()I"),
    NATIVE_METHOD(OsConstants, get_WCONTINUED, "()I"),
    NATIVE_METHOD(OsConstants, get_WEXITED, "()I"),
    NATIVE_METHOD(OsConstants, get_WNOHANG, "()I"),
    NATIVE_METHOD(OsConstants, get_WNOWAIT, "()I"),
    NATIVE_METHOD(OsConstants, get_WSTOPPED, "()I"),
    NATIVE_METHOD(OsConstants, get_WUNTRACED, "()I"),
    NATIVE_METHOD(OsConstants, get_W_OK, "()I"),
    NATIVE_METHOD(OsConstants, get_X_OK, "()I"),
};
int register_libcore_io_OsConstants(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "libcore/io/OsConstants", gMethods, NELEM(gMethods));
}
