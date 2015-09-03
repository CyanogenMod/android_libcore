/*
 * Copyright (C) 2015 The Android Open Source Project
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

#include <sys/stat.h>
#include <string>

#include <jni.h>
#include "JNIHelp.h"
#include "ScopedUtfChars.h"

extern "C" void Java_libcore_java_io_FileTest_nativeTestFilesWithSurrogatePairs(
    JNIEnv* env, jobject /* clazz */, jstring baseDir) {
  ScopedUtfChars baseDirUtf(env, baseDir);

  std::string base(baseDirUtf.c_str());
  std::string subDir = base + "/dir_\xF0\x93\x80\x80";
  std::string subFile = subDir + "/file_\xF0\x93\x80\x80";

  struct stat sb;
  int ret = stat(subDir.c_str(), &sb);
  if (ret == -1) {
      jniThrowIOException(env, errno);
  }
  if (!S_ISDIR(sb.st_mode)) {
      jniThrowException(env, "java/lang/IllegalStateException", "expected dir");
  }

  ret = stat(subFile.c_str(), &sb);
  if (ret == -1) {
      jniThrowIOException(env, errno);
  }

  if (!S_ISREG(sb.st_mode)) {
      jniThrowException(env, "java/lang/IllegalStateException", "expected file");
  }
}
