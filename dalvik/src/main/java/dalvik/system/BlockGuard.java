/*
 * Copyright (C) 2010 The Android Open Source Project
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

package dalvik.system;

import org.apache.harmony.luni.platform.IFileSystem;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Mechanism to let threads set restrictions on what code is allowed
 * to do in their thread.
 *
 * <p>This is meant for applications to prevent certain blocking
 * operations from running on their main event loop (or "UI") threads.
 *
 * <p>Note that this is all best-effort to catch most accidental mistakes
 * and isn't intended to be a perfect mechanism, nor provide any sort of
 * security.
 *
 * @hide
 */
public final class BlockGuard {

    public static final int DISALLOW_DISK_WRITE = 0x01;
    public static final int DISALLOW_DISK_READ = 0x02;
    public static final int DISALLOW_NETWORK = 0x04;
    public static final int PASS_RESTRICTIONS_VIA_RPC = 0x08;
    public static final int PENALTY_LOG = 0x10;
    public static final int PENALTY_DIALOG = 0x20;
    public static final int PENALTY_DEATH = 0x40;

    public interface Policy {
        /**
         * Called on disk writes.
         */
        void onWriteToDisk();

        /**
         * Called on disk writes.
         */
        void onReadFromDisk();

        /**
         * Called on network operations.
         */
        void onNetwork();

        /**
         * Returns the policy bitmask, for shipping over Binder calls
         * to remote threads/processes and reinstantiating the policy
         * there.  The bits in the mask are from the DISALLOW_* and
         * PENALTY_* constants.
         */
        int getPolicyMask();
    }

    public final class BlockGuardPolicyException extends RuntimeException {
        // bitmask of DISALLOW_*, PENALTY_*, etc flags
        public final int mPolicyState;
        public final int mPolicyViolated;

        public BlockGuardPolicyException(int policyState, int policyViolated) {
            mPolicyState = policyState;
            mPolicyViolated = policyViolated;
        }

        // TODO: toString() and stringify the bitmasks above
    }

    /**
     * The default, permissive policy that doesn't prevent any operations.
     */
    public static Policy LAX_POLICY = new Policy() {
            public void onWriteToDisk() {}
            public void onReadFromDisk() {}
            public void onNetwork() {}
            public int getPolicyMask() {
                return 0;
            }
        };

    private static ThreadLocal<Policy> threadPolicy = new ThreadLocal<Policy>() {
        @Override protected Policy initialValue() {
            return LAX_POLICY;
        }
    };

    /**
     * Get the current thread's policy.
     *
     * @returns the current thread's policy.  Never returns null.
     *     Will return the LAX_POLICY instance if nothing else is set.
     */
    public static Policy getThreadPolicy() {
        return threadPolicy.get();
    }

    /**
     * Sets the current thread's block guard policy.
     *
     * @param policy policy to set.  May not be null.  Use the public LAX_POLICY
     *   if you want to unset the active policy.
     */
    public static void setThreadPolicy(Policy policy) {
        if (policy == null) {
            throw new NullPointerException("policy == null");
        }
        threadPolicy.set(policy);
    }

    private BlockGuard() {}

    /**
     * A filesystem wrapper that calls the policy check functions
     * on reads and writes.
     */
    public static class WrappedFileSystem implements IFileSystem {
        private final IFileSystem mFileSystem;

        public WrappedFileSystem(IFileSystem fileSystem) {
            mFileSystem = fileSystem;
        }

        public long read(int fileDescriptor, byte[] bytes, int offset, int length)
                throws IOException {
            BlockGuard.getThreadPolicy().onReadFromDisk();
            return mFileSystem.read(fileDescriptor, bytes, offset, length);
        }

        public long write(int fileDescriptor, byte[] bytes, int offset, int length)
                throws IOException {
            BlockGuard.getThreadPolicy().onWriteToDisk();
            return mFileSystem.write(fileDescriptor, bytes, offset, length);
        }

        public long readv(int fileDescriptor, int[] addresses, int[] offsets,
                          int[] lengths, int size) throws IOException {
            BlockGuard.getThreadPolicy().onReadFromDisk();
            return mFileSystem.readv(fileDescriptor, addresses, offsets, lengths, size);
        }

        public long writev(int fileDescriptor, int[] addresses, int[] offsets,
                           int[] lengths, int size) throws IOException {
            BlockGuard.getThreadPolicy().onWriteToDisk();
            return mFileSystem.writev(fileDescriptor, addresses, offsets, lengths, size);
        }

        public long readDirect(int fileDescriptor, int address, int offset,
                               int length) throws IOException {
            BlockGuard.getThreadPolicy().onReadFromDisk();
            return mFileSystem.readDirect(fileDescriptor, address, offset, length);
        }

        public long writeDirect(int fileDescriptor, int address, int offset,
                                int length) throws IOException {
            BlockGuard.getThreadPolicy().onWriteToDisk();
            return mFileSystem.writeDirect(fileDescriptor, address, offset, length);
        }

        public boolean lock(int fileDescriptor, long start, long length, int type,
                            boolean waitFlag) throws IOException {
            return mFileSystem.lock(fileDescriptor, start, length, type, waitFlag);
        }

        public void unlock(int fileDescriptor, long start, long length)
                throws IOException {
            mFileSystem.unlock(fileDescriptor, start, length);
        }

        public long seek(int fileDescriptor, long offset, int whence)
                throws IOException {
            return mFileSystem.seek(fileDescriptor, offset, whence);
        }

        public void fflush(int fileDescriptor, boolean metadata)
                throws IOException {
            BlockGuard.getThreadPolicy().onWriteToDisk();
            mFileSystem.fflush(fileDescriptor, metadata);
        }

        public void close(int fileDescriptor) throws IOException {
            mFileSystem.close(fileDescriptor);
        }

        public void truncate(int fileDescriptor, long size) throws IOException {
            BlockGuard.getThreadPolicy().onWriteToDisk();
            mFileSystem.truncate(fileDescriptor, size);
        }

        public int getAllocGranularity() throws IOException {
            return mFileSystem.getAllocGranularity();
        }

        public int open(byte[] fileName, int mode) throws FileNotFoundException {
            BlockGuard.getThreadPolicy().onReadFromDisk();
            if (mode != 0) {  // 0 is read-only
                BlockGuard.getThreadPolicy().onWriteToDisk();
            }
            return mFileSystem.open(fileName, mode);
        }

        public long transfer(int fileHandler, FileDescriptor socketDescriptor,
                             long offset, long count) throws IOException {
            return mFileSystem.transfer(fileHandler, socketDescriptor, offset, count);
        }

        public int ioctlAvailable(int fileDescriptor) throws IOException {
            return mFileSystem.ioctlAvailable(fileDescriptor);
        }
    }
}
