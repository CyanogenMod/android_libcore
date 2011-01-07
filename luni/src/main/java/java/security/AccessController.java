/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2008 The Android Open Source Project
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

package java.security;

/**
 * Legacy security code; this class exists for compatibility only.
 */
public final class AccessController {

    private AccessController() {
    }

    /**
     * Calls {@code action.run()}.
     */
    public static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    /**
     * Calls {@code action.run()}.
     */
    public static <T> T doPrivileged(PrivilegedAction<T> action, AccessControlContext context) {
        return action.run();
    }

    /**
     * Calls {@code action.run()}.
     */
    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        try {
            return action.run();
        } catch (RuntimeException e) {
            throw e; // so we don't wrap RuntimeExceptions with PrivilegedActionException
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        }
    }

    /**
     * Calls {@code action.run()}.
     */
    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action, AccessControlContext context) throws PrivilegedActionException {
        return doPrivileged(action);
    }

    /**
     * Calls {@code action.run()}.
     */
    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action) {
        return action.run();
    }

    /**
     * Calls {@code action.run()}.
     */
    public static <T> T doPrivilegedWithCombiner(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        return doPrivileged(action);
    }

    /**
     * Does nothing.
     */
    public static void checkPermission(Permission permission) throws AccessControlException {
    }

    /**
     * Returns array of ProtectionDomains from the classes residing on the stack
     * of the current thread, up to and including the caller of the nearest
     * privileged frame. Reflection frames are skipped. The returned array is
     * never null and never contains null elements, meaning that bootstrap
     * classes are effectively ignored.
     */
    private static native ProtectionDomain[] getStackDomains();

    /**
     * Returns the {@code AccessControlContext} for the current {@code Thread}
     * including the inherited access control context of the thread that spawned
     * the current thread (recursively).
     * <p>
     * The returned context may be used to perform access checks at a later
     * point in time, possibly by another thread.
     *
     * @return the {@code AccessControlContext} for the current {@code Thread}
     * @see Thread#currentThread
     */
    public static AccessControlContext getContext() {
        // TODO: just return null?
        return new AccessControlContext(getStackDomains());
    }
}
