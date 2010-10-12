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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CloseGuard is a mechanism for flagging implict finalizer cleanup of
 * resources that should have been cleaned up by explicit close
 * methods (aka "explicit termination methods" in Effective Java).
 * <p>
 * A simple example: <pre>   {@code
 *   class Foo {
 *
 *       private final CloseGuard guard = CloseGuard.get();
 *
 *       ...
 *
 *       public Foo() {
 *           ...;
 *           guard.open("cleanup");
 *       }
 *
 *       public void cleanup() {
 *          guard.close();
 *          ...;
 *       }
 *
 *       protected void finalize() throws Throwable {
 *           try {
 *               if (guard != null) {
 *                   guard.warnIfOpen();
 *               }
 *               cleanup();
 *           } finally {
 *               super.finalize();
 *           }
 *       }
 *   }
 * }</pre>
 *
 * In usage where the resource to be explictly cleaned up are
 * allocated after object construction, CloseGuard can protection can
 * be deferred. For example: <pre>   {@code
 *   class Bar {
 *
 *       private final CloseGuard guard = CloseGuard.getUnopened();
 *
 *       ...
 *
 *       public Bar() {
 *           ...;
 *       }
 *
 *       public void connect() {
 *          ...;
 *          guard.open("cleanup");
 *       }
 *
 *       public void cleanup() {
 *          guard.close();
 *          ...;
 *       }
 *
 *       protected void finalize() throws Throwable {
 *           try {
 *               if (guard != null) {
 *                   guard.warnIfOpen();
 *               }
 *               cleanup();
 *           } finally {
 *               super.finalize();
 *           }
 *       }
 *   }
 * }</pre>
 *
 * When used in a constructor calls to {@code open} should occur at
 * the end of the constructor since an exception that would cause
 * abrupt termination of the constructor will mean that the user will
 * not have a reference to the object to cleanup explicitly. When used
 * in a method, the call to {@code open} should occur just after
 * resource acquisition.
 *
 * <p>
 *
 * Note that the null check on {@code guard} in the finalizer is to
 * cover cases where a constructor throws an exception causing the
 * {@code guard} to be uninitialized.
 *
 * @hide
 */
public final class CloseGuard {

    /**
     * Instance used when CloseGuard is disabled to avoid allocation.
     */
    private static final CloseGuard NOOP = new CloseGuard();

    /**
     * Returns a CloseGuard instance. If CloseGuard is enabled, {@code
     * #open(String)} can be used to set up the instance to warn on
     * failure to close. If CloseGuard is disabled, a non-null no-op
     * instanace is returned.
     */
    public static CloseGuard get() {
        if (!enabled()) {
            return NOOP;
        }
        return new CloseGuard();
    }

    private static boolean enabled() {
        boolean enabled = true;  // TODO replace compile time with runtime check
        return enabled;
    }

    private CloseGuard() {}

    /**
     * If CloseGuard is enabled, {@code open} initializes the instance
     * with a warning that the caller should have explictly called the
     * {@code closer} method instead of relying on finalization.
     *
     * @param closer non-null name of explict termination method
     * @throws NullPointerException if closer is null, regardless of
     * whether or not CloseGuard is enabled
     */
    public void open(String closer) {
        // always perform the check for valid API usage...
        if (closer == null) {
            throw new NullPointerException("closer == null");
        }
        // ...but avoid allocating an allocationSite if disabled
        if (!enabled()) {
            return;
        }
        String message = "Explicit termination method '" + closer + "' not called";
        allocationSite = new Throwable(message);
    }

    private Throwable allocationSite;

    /**
     * Marks this CloseGuard instance as closed to avoid warnings on
     * finalization.
     */
    public void close() {
        allocationSite = null;
    }

    /**
     * If CloseGuard is enabled, logs a warning if the caller did not
     * properly cleanup by calling an explicit close method
     * before finalization. If CloseGuard is disable, no action is
     * performed.
     */
    public void warnIfOpen() {
        if (allocationSite == null) {
            return;
        }

        String message =
                ("A resource was acquired at attached stack trace but never released. "
                 + "See java.io.Closeable for information on avoiding resource leaks.");

        Logger.global.log(Level.WARNING, message, allocationSite);
    }
}
