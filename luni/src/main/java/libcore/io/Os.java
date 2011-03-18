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

import java.io.FileDescriptor;

public interface Os {
    public boolean access(String path, int mode) throws ErrnoException;
    public String[] environ();
    public void fdatasync(FileDescriptor fd) throws ErrnoException;
    public StructStat fstat(FileDescriptor fd) throws ErrnoException;
    public void fsync(FileDescriptor fd) throws ErrnoException;
    public void ftruncate(FileDescriptor fd, long length) throws ErrnoException;
    public String getenv(String name);
    public boolean isatty(FileDescriptor fd);
    public long lseek(FileDescriptor fd, long offset, int whence) throws ErrnoException;
    public StructStat lstat(String path) throws ErrnoException;
    public StructStat stat(String path) throws ErrnoException;
    public String strerror(int errno);
    public long sysconf(int name);
}
