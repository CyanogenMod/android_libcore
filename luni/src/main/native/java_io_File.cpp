/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#define LOG_TAG "File"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "JniException.h"
#include "LocalArray.h"
#include "ScopedFd.h"
#include "ScopedLocalRef.h"
#include "ScopedPrimitiveArray.h"
#include "ScopedUtfChars.h"
#include "StaticAssert.h"
#include "readlink.h"
#include "toStringArray.h"

#include <string>
#include <vector>

#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/vfs.h>
#include <time.h>
#include <unistd.h>
#include <utime.h>

static jboolean File_deleteImpl(JNIEnv* env, jclass, jstring javaPath) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }
    return (remove(path.c_str()) == 0);
}

static bool doStat(JNIEnv* env, jstring javaPath, struct stat& sb) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }
    return (stat(path.c_str(), &sb) == 0);
}

static jlong File_lengthImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct stat sb;
    if (!doStat(env, javaPath, sb)) {
        // The RI returns 0 on error. (Even for errors like EACCES or ELOOP.)
        return 0;
    }
    return sb.st_size;
}

static jlong File_lastModifiedImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct stat sb;
    if (!doStat(env, javaPath, sb)) {
        return 0;
    }
    return static_cast<jlong>(sb.st_mtime) * 1000L;
}

static jboolean File_isDirectoryImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct stat sb;
    return (doStat(env, javaPath, sb) && S_ISDIR(sb.st_mode));
}

static jboolean File_isFileImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct stat sb;
    return (doStat(env, javaPath, sb) && S_ISREG(sb.st_mode));
}

static jboolean doAccess(JNIEnv* env, jstring javaPath, int mode) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }
    return (access(path.c_str(), mode) == 0);
}

static jboolean File_existsImpl(JNIEnv* env, jclass, jstring javaPath) {
    return doAccess(env, javaPath, F_OK);
}

static jboolean File_canExecuteImpl(JNIEnv* env, jclass, jstring javaPath) {
    return doAccess(env, javaPath, X_OK);
}

static jboolean File_canReadImpl(JNIEnv* env, jclass, jstring javaPath) {
    return doAccess(env, javaPath, R_OK);
}

static jboolean File_canWriteImpl(JNIEnv* env, jclass, jstring javaPath) {
    return doAccess(env, javaPath, W_OK);
}

static jstring File_readlink(JNIEnv* env, jclass, jstring javaPath) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return NULL;
    }

    std::string result;
    if (!readlink(path.c_str(), result)) {
        jniThrowIOException(env, errno);
        return NULL;
    }
    return env->NewStringUTF(result.c_str());
}

static jstring File_realpath(JNIEnv* env, jclass, jstring javaPath) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return NULL;
    }

    extern bool realpath(const char* path, std::string& resolved);
    std::string result;
    if (!realpath(path.c_str(), result)) {
        jniThrowIOException(env, errno);
        return NULL;
    }
    return env->NewStringUTF(result.c_str());
}

static jboolean File_setLastModifiedImpl(JNIEnv* env, jclass, jstring javaPath, jlong ms) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }

    // We want to preserve the access time.
    struct stat sb;
    if (stat(path.c_str(), &sb) == -1) {
        return JNI_FALSE;
    }

    // TODO: we could get microsecond resolution with utimes(3), "legacy" though it is.
    utimbuf times;
    times.actime = sb.st_atime;
    times.modtime = static_cast<time_t>(ms / 1000);
    return (utime(path.c_str(), &times) == 0);
}

static jboolean doChmod(JNIEnv* env, jstring javaPath, mode_t mask, bool set) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }

    struct stat sb;
    if (stat(path.c_str(), &sb) == -1) {
        return JNI_FALSE;
    }
    mode_t newMode = set ? (sb.st_mode | mask) : (sb.st_mode & ~mask);
    return (chmod(path.c_str(), newMode) == 0);
}

static jboolean File_setExecutableImpl(JNIEnv* env, jclass, jstring javaPath,
        jboolean set, jboolean ownerOnly) {
    return doChmod(env, javaPath, ownerOnly ? S_IXUSR : (S_IXUSR | S_IXGRP | S_IXOTH), set);
}

static jboolean File_setReadableImpl(JNIEnv* env, jclass, jstring javaPath,
        jboolean set, jboolean ownerOnly) {
    return doChmod(env, javaPath, ownerOnly ? S_IRUSR : (S_IRUSR | S_IRGRP | S_IROTH), set);
}

static jboolean File_setWritableImpl(JNIEnv* env, jclass, jstring javaPath,
        jboolean set, jboolean ownerOnly) {
    return doChmod(env, javaPath, ownerOnly ? S_IWUSR : (S_IWUSR | S_IWGRP | S_IWOTH), set);
}

static bool doStatFs(JNIEnv* env, jstring javaPath, struct statfs& sb) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }

    int rc = statfs(path.c_str(), &sb);
    return (rc != -1);
}

static jlong File_getFreeSpaceImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct statfs sb;
    if (!doStatFs(env, javaPath, sb)) {
        return 0;
    }
    STATIC_ASSERT(sizeof(sb.f_bfree) == sizeof(jlong), statfs_not_64_bit);
    return sb.f_bfree * sb.f_bsize; // free block count * block size in bytes.
}

static jlong File_getTotalSpaceImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct statfs sb;
    if (!doStatFs(env, javaPath, sb)) {
        return 0;
    }
    STATIC_ASSERT(sizeof(sb.f_blocks) == sizeof(jlong), statfs_not_64_bit);
    return sb.f_blocks * sb.f_bsize; // total block count * block size in bytes.
}

static jlong File_getUsableSpaceImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct statfs sb;
    if (!doStatFs(env, javaPath, sb)) {
        return 0;
    }
    STATIC_ASSERT(sizeof(sb.f_bavail) == sizeof(jlong), statfs_not_64_bit);
    return sb.f_bavail * sb.f_bsize; // non-root free block count * block size in bytes.
}

// Iterates over the filenames in the given directory.
class ScopedReaddir {
public:
    ScopedReaddir(const char* path) {
        mDirStream = opendir(path);
        mIsBad = (mDirStream == NULL);
    }

    ~ScopedReaddir() {
        if (mDirStream != NULL) {
            closedir(mDirStream);
        }
    }

    // Returns the next filename, or NULL.
    const char* next() {
        dirent* result = NULL;
        int rc = readdir_r(mDirStream, &mEntry, &result);
        if (rc != 0) {
            mIsBad = true;
            return NULL;
        }
        return (result != NULL) ? result->d_name : NULL;
    }

    // Has an error occurred on this stream?
    bool isBad() const {
        return mIsBad;
    }

private:
    DIR* mDirStream;
    dirent mEntry;
    bool mIsBad;

    // Disallow copy and assignment.
    ScopedReaddir(const ScopedReaddir&);
    void operator=(const ScopedReaddir&);
};

typedef std::vector<std::string> DirEntries;

// Reads the directory referred to by 'pathBytes', adding each directory entry
// to 'entries'.
static bool readDirectory(JNIEnv* env, jstring javaPath, DirEntries& entries) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return false;
    }

    ScopedReaddir dir(path.c_str());
    if (dir.isBad()) {
        return false;
    }
    const char* filename;
    while ((filename = dir.next()) != NULL) {
        if (strcmp(filename, ".") != 0 && strcmp(filename, "..") != 0) {
            // TODO: this hides allocation failures from us. Push directory iteration up into Java?
            entries.push_back(filename);
        }
    }
    return true;
}

static jobjectArray File_listImpl(JNIEnv* env, jclass, jstring javaPath) {
    // Read the directory entries into an intermediate form.
    DirEntries entries;
    if (!readDirectory(env, javaPath, entries)) {
        return NULL;
    }
    // Translate the intermediate form into a Java String[].
    return toStringArray(env, entries);
}

static jboolean File_mkdirImpl(JNIEnv* env, jclass, jstring javaPath) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }

    // On Android, we don't want default permissions to allow global access.
    return (mkdir(path.c_str(), S_IRWXU) == 0);
}

static jboolean File_createNewFileImpl(JNIEnv* env, jclass, jstring javaPath) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }

    // On Android, we don't want default permissions to allow global access.
    ScopedFd fd(open(path.c_str(), O_CREAT | O_EXCL, 0600));
    if (fd.get() != -1) {
        // We created a new file. Success!
        return JNI_TRUE;
    }
    if (errno == EEXIST) {
        // The file already exists.
        return JNI_FALSE;
    }
    jniThrowIOException(env, errno);
    return JNI_FALSE; // Ignored by Java; keeps the C++ compiler happy.
}

static jboolean File_renameToImpl(JNIEnv* env, jclass, jstring javaOldPath, jstring javaNewPath) {
    ScopedUtfChars oldPath(env, javaOldPath);
    if (oldPath.c_str() == NULL) {
        return JNI_FALSE;
    }

    ScopedUtfChars newPath(env, javaNewPath);
    if (newPath.c_str() == NULL) {
        return JNI_FALSE;
    }

    return (rename(oldPath.c_str(), newPath.c_str()) == 0);
}

static void File_symlink(JNIEnv* env, jclass, jstring javaOldPath, jstring javaNewPath) {
    ScopedUtfChars oldPath(env, javaOldPath);
    if (oldPath.c_str() == NULL) {
        return;
    }

    ScopedUtfChars newPath(env, javaNewPath);
    if (newPath.c_str() == NULL) {
        return;
    }

    if (symlink(oldPath.c_str(), newPath.c_str()) == -1) {
        jniThrowIOException(env, errno);
    }
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(File, canExecuteImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(File, canReadImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(File, canWriteImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(File, createNewFileImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(File, deleteImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(File, existsImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(File, getFreeSpaceImpl, "(Ljava/lang/String;)J"),
    NATIVE_METHOD(File, getTotalSpaceImpl, "(Ljava/lang/String;)J"),
    NATIVE_METHOD(File, getUsableSpaceImpl, "(Ljava/lang/String;)J"),
    NATIVE_METHOD(File, isDirectoryImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(File, isFileImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(File, lastModifiedImpl, "(Ljava/lang/String;)J"),
    NATIVE_METHOD(File, lengthImpl, "(Ljava/lang/String;)J"),
    NATIVE_METHOD(File, listImpl, "(Ljava/lang/String;)[Ljava/lang/String;"),
    NATIVE_METHOD(File, mkdirImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(File, readlink, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(File, realpath, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(File, renameToImpl, "(Ljava/lang/String;Ljava/lang/String;)Z"),
    NATIVE_METHOD(File, setExecutableImpl, "(Ljava/lang/String;ZZ)Z"),
    NATIVE_METHOD(File, setLastModifiedImpl, "(Ljava/lang/String;J)Z"),
    NATIVE_METHOD(File, setReadableImpl, "(Ljava/lang/String;ZZ)Z"),
    NATIVE_METHOD(File, setWritableImpl, "(Ljava/lang/String;ZZ)Z"),
    NATIVE_METHOD(File, symlink, "(Ljava/lang/String;Ljava/lang/String;)V"),
};
int register_java_io_File(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "java/io/File", gMethods, NELEM(gMethods));
}
