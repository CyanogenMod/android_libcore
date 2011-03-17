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

package libcore.io;

public final class OsConstants {
    private OsConstants() { }

    public static boolean S_ISBLK(int mode) { return (mode & S_IFMT) == S_IFBLK; }
    public static boolean S_ISCHR(int mode) { return (mode & S_IFMT) == S_IFCHR; }
    public static boolean S_ISDIR(int mode) { return (mode & S_IFMT) == S_IFDIR; }
    public static boolean S_ISFIFO(int mode) { return (mode & S_IFMT) == S_IFIFO; }
    public static boolean S_ISREG(int mode) { return (mode & S_IFMT) == S_IFREG; }
    public static boolean S_ISLNK(int mode) { return (mode & S_IFMT) == S_IFLNK; }
    public static boolean S_ISSOCK(int mode) { return (mode & S_IFMT) == S_IFSOCK; }

    public static int WEXITSTATUS(int status) { return (status & 0xff00) >> 8; }
    public static boolean WCOREDUMP(int status) { return (status & 0x80) != 0; }
    public static int WTERMSIG(int status) { return status & 0x7f; }
    public static int WSTOPSIG(int status) { return WEXITSTATUS(status); }
    public static boolean WIFEXITED(int status) { return (WTERMSIG(status) == 0); }
    public static boolean WIFSTOPPED(int status) { return (WTERMSIG(status) == 0x7f); }
    public static boolean WIFSIGNALED(int status) { return (WTERMSIG(status + 1) >= 2); }

    public static final int AF_INET = placeholder();
    public static final int AF_INET6 = placeholder();
    public static final int AF_UNIX = placeholder();
    public static final int AF_UNSPEC = placeholder();
    public static final int E2BIG = placeholder();
    public static final int EACCES = placeholder();
    public static final int EADDRINUSE = placeholder();
    public static final int EADDRNOTAVAIL = placeholder();
    public static final int EAFNOSUPPORT = placeholder();
    public static final int EAGAIN = placeholder();
    public static final int EALREADY = placeholder();
    public static final int EBADF = placeholder();
    public static final int EBADMSG = placeholder();
    public static final int EBUSY = placeholder();
    public static final int ECANCELED = placeholder();
    public static final int ECHILD = placeholder();
    public static final int ECONNABORTED = placeholder();
    public static final int ECONNREFUSED = placeholder();
    public static final int ECONNRESET = placeholder();
    public static final int EDEADLK = placeholder();
    public static final int EDESTADDRREQ = placeholder();
    public static final int EDOM = placeholder();
    public static final int EDQUOT = placeholder();
    public static final int EEXIST = placeholder();
    public static final int EFAULT = placeholder();
    public static final int EFBIG = placeholder();
    public static final int EHOSTUNREACH = placeholder();
    public static final int EIDRM = placeholder();
    public static final int EILSEQ = placeholder();
    public static final int EINPROGRESS = placeholder();
    public static final int EINTR = placeholder();
    public static final int EINVAL = placeholder();
    public static final int EIO = placeholder();
    public static final int EISCONN = placeholder();
    public static final int EISDIR = placeholder();
    public static final int ELOOP = placeholder();
    public static final int EMFILE = placeholder();
    public static final int EMLINK = placeholder();
    public static final int EMSGSIZE = placeholder();
    public static final int EMULTIHOP = placeholder();
    public static final int ENAMETOOLONG = placeholder();
    public static final int ENETDOWN = placeholder();
    public static final int ENETRESET = placeholder();
    public static final int ENETUNREACH = placeholder();
    public static final int ENFILE = placeholder();
    public static final int ENOBUFS = placeholder();
    public static final int ENODATA = placeholder();
    public static final int ENODEV = placeholder();
    public static final int ENOENT = placeholder();
    public static final int ENOEXEC = placeholder();
    public static final int ENOLCK = placeholder();
    public static final int ENOLINK = placeholder();
    public static final int ENOMEM = placeholder();
    public static final int ENOMSG = placeholder();
    public static final int ENOPROTOOPT = placeholder();
    public static final int ENOSPC = placeholder();
    public static final int ENOSR = placeholder();
    public static final int ENOSTR = placeholder();
    public static final int ENOSYS = placeholder();
    public static final int ENOTCONN = placeholder();
    public static final int ENOTDIR = placeholder();
    public static final int ENOTEMPTY = placeholder();
    public static final int ENOTSOCK = placeholder();
    public static final int ENOTSUP = placeholder();
    public static final int ENOTTY = placeholder();
    public static final int ENXIO = placeholder();
    public static final int EOPNOTSUPP = placeholder();
    public static final int EOVERFLOW = placeholder();
    public static final int EPERM = placeholder();
    public static final int EPIPE = placeholder();
    public static final int EPROTO = placeholder();
    public static final int EPROTONOSUPPORT = placeholder();
    public static final int EPROTOTYPE = placeholder();
    public static final int ERANGE = placeholder();
    public static final int EROFS = placeholder();
    public static final int ESPIPE = placeholder();
    public static final int ESRCH = placeholder();
    public static final int ESTALE = placeholder();
    public static final int ETIME = placeholder();
    public static final int ETIMEDOUT = placeholder();
    public static final int ETXTBSY = placeholder();
    public static final int EWOULDBLOCK = placeholder();
    public static final int EXDEV = placeholder();
    public static final int EXIT_FAILURE = placeholder();
    public static final int EXIT_SUCCESS = placeholder();
    public static final int FD_CLOEXEC = placeholder();
    public static final int F_DUPFD = placeholder();
    public static final int F_GETFD = placeholder();
    public static final int F_GETFL = placeholder();
    public static final int F_GETLK = placeholder();
    public static final int F_GETOWN = placeholder();
    public static final int F_OK = placeholder();
    public static final int F_RDLCK = placeholder();
    public static final int F_SETFD = placeholder();
    public static final int F_SETFL = placeholder();
    public static final int F_SETLK = placeholder();
    public static final int F_SETLKW = placeholder();
    public static final int F_SETOWN = placeholder();
    public static final int F_UNLCK = placeholder();
    public static final int F_WRLCK = placeholder();
    public static final int IPPROTO_ICMP = placeholder();
    public static final int IPPROTO_IP = placeholder();
    public static final int IPPROTO_IPV6 = placeholder();
    public static final int IPPROTO_RAW = placeholder();
    public static final int IPPROTO_TCP = placeholder();
    public static final int IPPROTO_UDP = placeholder();
    public static final int MAP_FIXED = placeholder();
    public static final int MAP_PRIVATE = placeholder();
    public static final int MAP_SHARED = placeholder();
    public static final int MCL_CURRENT = placeholder();
    public static final int MCL_FUTURE = placeholder();
    public static final int MSG_CTRUNC = placeholder();
    public static final int MSG_DONTROUTE = placeholder();
    public static final int MSG_EOR = placeholder();
    public static final int MSG_OOB = placeholder();
    public static final int MSG_PEEK = placeholder();
    public static final int MSG_TRUNC = placeholder();
    public static final int MSG_WAITALL = placeholder();
    public static final int MS_ASYNC = placeholder();
    public static final int MS_INVALIDATE = placeholder();
    public static final int MS_SYNC = placeholder();
    public static final int O_ACCMODE = placeholder();
    public static final int O_APPEND = placeholder();
    public static final int O_CREAT = placeholder();
    public static final int O_EXCL = placeholder();
    public static final int O_NOCTTY = placeholder();
    public static final int O_NONBLOCK = placeholder();
    public static final int O_RDONLY = placeholder();
    public static final int O_RDWR = placeholder();
    public static final int O_SYNC = placeholder();
    public static final int O_TRUNC = placeholder();
    public static final int O_WRONLY = placeholder();
    public static final int PROT_EXEC = placeholder();
    public static final int PROT_NONE = placeholder();
    public static final int PROT_READ = placeholder();
    public static final int PROT_WRITE = placeholder();
    public static final int R_OK = placeholder();
    public static final int SEEK_CUR = placeholder();
    public static final int SEEK_END = placeholder();
    public static final int SEEK_SET = placeholder();
    public static final int SHUT_RD = placeholder();
    public static final int SHUT_RDWR = placeholder();
    public static final int SHUT_WR = placeholder();
    public static final int SOCK_DGRAM = placeholder();
    public static final int SOCK_RAW = placeholder();
    public static final int SOCK_SEQPACKET = placeholder();
    public static final int SOCK_STREAM = placeholder();
    public static final int STDERR_FILENO = placeholder();
    public static final int STDIN_FILENO = placeholder();
    public static final int STDOUT_FILENO = placeholder();
    public static final int S_IFBLK = placeholder();
    public static final int S_IFCHR = placeholder();
    public static final int S_IFDIR = placeholder();
    public static final int S_IFIFO = placeholder();
    public static final int S_IFLNK = placeholder();
    public static final int S_IFMT = placeholder();
    public static final int S_IFREG = placeholder();
    public static final int S_IFSOCK = placeholder();
    public static final int S_IRGRP = placeholder();
    public static final int S_IROTH = placeholder();
    public static final int S_IRUSR = placeholder();
    public static final int S_IRWXG = placeholder();
    public static final int S_IRWXO = placeholder();
    public static final int S_IRWXU = placeholder();
    public static final int S_ISGID = placeholder();
    public static final int S_ISUID = placeholder();
    public static final int S_ISVTX = placeholder();
    public static final int S_IWGRP = placeholder();
    public static final int S_IWOTH = placeholder();
    public static final int S_IWUSR = placeholder();
    public static final int S_IXGRP = placeholder();
    public static final int S_IXOTH = placeholder();
    public static final int S_IXUSR = placeholder();
    public static final int WCONTINUED = placeholder();
    public static final int WEXITED = placeholder();
    public static final int WNOHANG = placeholder();
    public static final int WNOWAIT = placeholder();
    public static final int WSTOPPED = placeholder();
    public static final int WUNTRACED = placeholder();
    public static final int W_OK = placeholder();
    public static final int X_OK = placeholder();

    private static native void initConstants();

    // A hack to avoid these constants being inlined by javac...
    private static int placeholder() { return 0; }
    // ...because we want to initialize them at runtime.
    static {
        initConstants();
    }
}
