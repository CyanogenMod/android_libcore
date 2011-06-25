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

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketException;
import java.nio.charset.Charsets;

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

    private static final boolean LOGI = false;

    // TODO: refactor class name to something more generic, since its scope is
    // growing beyond just blocking/logging.

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

    public static class SocketTags {
        public int statsTag = -1;
        public int statsUid = -1;
    }

    public static class BlockGuardPolicyException extends RuntimeException {
        // bitmask of DISALLOW_*, PENALTY_*, etc flags
        private final int mPolicyState;
        private final int mPolicyViolated;
        private final String mMessage;   // may be null

        public BlockGuardPolicyException(int policyState, int policyViolated) {
            this(policyState, policyViolated, null);
        }

        public BlockGuardPolicyException(int policyState, int policyViolated, String message) {
            mPolicyState = policyState;
            mPolicyViolated = policyViolated;
            mMessage = message;
            fillInStackTrace();
        }

        public int getPolicy() {
            return mPolicyState;
        }

        public int getPolicyViolation() {
            return mPolicyViolated;
        }

        public String getMessage() {
            // Note: do not change this format casually.  It's
            // somewhat unfortunately Parceled and passed around
            // Binder calls and parsed back into an Exception by
            // Android's StrictMode.  This was the least invasive
            // option and avoided a gross mix of Java Serialization
            // combined with Parcels.
            return "policy=" + mPolicyState + " violation=" + mPolicyViolated +
                    (mMessage == null ? "" : (" msg=" + mMessage));
        }
    }

    /**
     * The default, permissive policy that doesn't prevent any operations.
     */
    public static final Policy LAX_POLICY = new Policy() {
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

    private static ThreadLocal<SocketTags> threadSocketTags = new ThreadLocal<SocketTags>() {
        @Override
        protected SocketTags initialValue() {
            return new SocketTags();
        }
    };

    /**
     * Get the current thread's policy.
     *
     * @return the current thread's policy.  Never returns null.
     *     Will return the LAX_POLICY instance if nothing else is set.
     */
    public static Policy getThreadPolicy() {
        return threadPolicy.get();
    }

    public static void setThreadSocketStatsTag(int tag) {
        threadSocketTags.get().statsTag = tag;
    }

    public static void setThreadSocketStatsUid(int uid) {
        threadSocketTags.get().statsUid = uid;
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

    public static void tagSocketFd(FileDescriptor fd) throws SocketException {
        final SocketTags options = threadSocketTags.get();
        if (LOGI) {
            System.logI("tagSocket(" + fd.getInt$() + ") with statsTag="
                    + options.statsTag + ", statsUid=" + options.statsUid);
        }
        try {
            // TODO: skip tagging when options would be no-op
            internalTagSocketFd(fd, options.statsTag, options.statsUid);
        } catch (IOException e) {
            throw new SocketException("Problem tagging socket", e);
        }
    }

    public static void untagSocketFd(FileDescriptor fd) throws SocketException {
        if (LOGI) {
            System.logI("untagSocket(" + fd.getInt$() + ")");
        }
        try {
            internalUnTagSocketFd(fd);
        } catch (IOException e) {
            throw new SocketException("Problem untagging socket", e);
        }
    }

    private static void internalTagSocketFd(FileDescriptor fd, int tag, int uid)
            throws IOException {
        final int fdNum = fd.getInt$();
        if (fdNum == -1 || (tag == -1 && uid == -1)) return;

        String cmd = "t " + fdNum;
        if (tag == -1) {
            // Case where just the uid needs adjusting. But probaly the caller
            // will want to track his own name here, just in case.
            cmd += " 0";
        } else {
            cmd += " " + tagToKernel(tag);
        }
        if (uid != -1) {
            cmd += " " + uid;
        }
        internalModuleCtrl(cmd);
    }

    private static void internalUnTagSocketFd(FileDescriptor fd) throws IOException {
        final int fdNum = fd.getInt$();
        if (fdNum == -1) return;
        String cmd = "u " + fdNum;
        internalModuleCtrl(cmd);
    }

    /**
     * Sends commands to the kernel netfilter module.
     *
     * @param cmd command string for the qtaguid netfilter module. May not be null.
     *   <p>Supports:
     *     <ul><li>tag a socket:<br>
     *        <code>t <i>sock_fd</i> <i>acct_tag</i> [<i>uid_in_case_caller_is_acting_on_behalf_of</i>]</code><br>
     *     <code>*_tag</code> defaults to default_policy_tag_from_uid(uid_of_caller)<br>
     *     <code>acct_tag</code> is either 0 or greater that 2^32.<br>
     *     <code>uid_*</code> is only settable by priviledged UIDs (DownloadManager,...)
     *     </li>
     *     <li>untag a socket, preserving counters:<br>
     *       <code>u <i>sock_fd</i></code>
     *     </li></ul>
     *   <p>Notes:<br>
     *   <ul><li><i>sock_fd</i> is withing the callers process space.</li>
     *   <li><i>*_tag</i> are 64bit values</li></ul>
     *
     */
    private static void internalModuleCtrl(String cmd) throws IOException {
        final FileOutputStream procOut;
        // TODO: Use something like
        //  android.os.SystemProperties.getInt("persist.bandwidth.enable", 0)
        // to see if tagging should happen or not.
        try {
            procOut = new FileOutputStream("/proc/net/xt_qtaguid/ctrl");
        } catch (FileNotFoundException e) {
            if (LOGI) {
                System.logI("Can't talk to kernel module:" + e);
            }
            return;
        }
        try {
            procOut.write(cmd.getBytes(Charsets.US_ASCII));
        } finally {
            procOut.close();
        }
    }

    /**
     * Convert {@link Integer} tag to {@code /proc/} format. Assumes unsigned
     * base-10 format like {@code 2147483647}. Currently strips signed bit to
     * avoid using {@link BigInteger}.
     */
    public static String tagToKernel(int tag) {
        // TODO: eventually write in hex, since thats what proc exports
        // TODO: migrate to direct integer instead of odd shifting
        return Long.toString((((long) tag) << 32) & 0x7FFFFFFF00000000L);
    }

    /**
     * Convert {@code /proc/} tag format to {@link Integer}. Assumes incoming
     * format like {@code 0x7fffffff00000000}.
     */
    public static int kernelToTag(String string) {
        // TODO: migrate to direct integer instead of odd shifting
        return (int) (Long.decode(string) >> 32);
    }

    private BlockGuard() {}
}
