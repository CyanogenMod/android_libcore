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
 * File information returned by fstatvfs(2) and statvfs(2).
 *
 * @hide
 */
public final class StructStatVfs extends libcore.io.StructStatVfs {
  public StructStatVfs(long f_bsize, long f_frsize, long f_blocks, long f_bfree, long f_bavail,
                       long f_files, long f_ffree, long f_favail,
                       long f_fsid, long f_flag, long f_namemax) {
    super(f_bsize, f_frsize, f_blocks, f_bfree, f_bavail,
          f_files, f_ffree, f_favail,
          f_fsid, f_flag, f_namemax);
  }
}
