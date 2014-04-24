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

package android.system;

/**
 * File information returned by fstat(2), lstat(2), and stat(2). Corresponds to C's
 * {@code struct stat} from
 * <a href="http://www.opengroup.org/onlinepubs/000095399/basedefs/sys/stat.h.html">&lt;stat.h&gt;</a>
 *
 * @hide
 */
public final class StructStat extends libcore.io.StructStat {
  public StructStat(long st_dev, long st_ino, int st_mode, long st_nlink, int st_uid, int st_gid,
                    long st_rdev, long st_size, long st_atime, long st_mtime, long st_ctime,
                    long st_blksize, long st_blocks) {
    super(st_dev, st_ino, st_mode, st_nlink, st_uid, st_gid,
          st_rdev, st_size, st_atime, st_mtime, st_ctime,
          st_blksize, st_blocks);
  }
}
