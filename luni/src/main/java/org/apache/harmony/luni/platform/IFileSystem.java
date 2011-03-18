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

package org.apache.harmony.luni.platform;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface IFileSystem {
    public long read(int fileDescriptor, byte[] bytes, int offset, int length)
            throws IOException;

    public long write(int fileDescriptor, byte[] bytes, int offset, int length)
            throws IOException;

    public long readv(int fileDescriptor, int[] addresses, int[] offsets,
            int[] lengths, int size) throws IOException;

    public long writev(int fileDescriptor, int[] addresses, int[] offsets,
            int[] lengths, int size) throws IOException;

    // Required to support direct byte buffers
    public long readDirect(int fileDescriptor, int address, int offset,
            int length) throws IOException;

    public long writeDirect(int fileDescriptor, int address, int offset,
            int length) throws IOException;

    public boolean lock(int fileDescriptor, long start, long length, boolean shared,
            boolean waitFlag) throws IOException;

    public void unlock(int fileDescriptor, long start, long length)
            throws IOException;

    public long seek(int fileDescriptor, long offset, int whence)
            throws IOException;

    public void truncate(int fileDescriptor, long size) throws IOException;

    public int open(String path, int mode) throws FileNotFoundException;

    public long transfer(int fileHandler, FileDescriptor socketDescriptor,
            long offset, long count) throws IOException;

    public int ioctlAvailable(FileDescriptor fileDescriptor) throws IOException;

    public static class SeekPipeException extends IOException {
        public SeekPipeException(String message) {
            super(message);
        }
    }

}
