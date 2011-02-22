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

package java.lang;

import dalvik.system.VMStack;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FilePermission;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.net.InetAddress;
import java.net.SocketPermission;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;
import java.security.Security;
import java.security.SecurityPermission;
import java.util.PropertyPermission;

/**
 * Legacy security code; this class exists for compatibility only.
 *
 * <p>Security managers do <strong>not</strong> provide a
 * secure environment for executing untrusted code. Untrusted code cannot be
 * safely isolated within the Dalvik VM.
 */
public class SecurityManager {
    /**
     * Flag to indicate whether a security check is in progress.
     *
     * @deprecated Use {@link #checkPermission}
     */
    @Deprecated
    protected boolean inCheck;

    /**
     * Constructs a new {@code SecurityManager} instance.
     */
    public SecurityManager() {
    }

    /**
     * Does nothing.
     */
    public void checkAccept(String host, int port) {
    }

    /**
     * Does nothing.
     */
    public void checkAccess(Thread thread) {
    }

    /**
     * Does nothing.
     */
    public void checkAccess(ThreadGroup group) {
    }

    /**
     * Does nothing.
     */
    public void checkConnect(String host, int port) {
    }

    /**
     * Does nothing.
     */
    public void checkConnect(String host, int port, Object context) {
    }

    /**
     * Does nothing.
     */
    public void checkCreateClassLoader() {
    }

    /**
     * Does nothing.
     */
    public void checkDelete(String file) {
    }

    /**
     * Does nothing.
     */
    public void checkExec(String cmd) {
    }

    /**
     * Does nothing.
     */
    public void checkExit(int status) {
    }

    /**
     * Does nothing.
     */
    public void checkLink(String libName) {
    }

    /**
     * Does nothing.
     */
    public void checkListen(int port) {
    }

    /**
     * Does nothing.
     */
    public void checkMemberAccess(Class<?> cls, int type) {
    }

    /**
     * Does nothing.
     */
    public void checkMulticast(InetAddress maddr) {
    }

    /**
     * Does nothing.
     * @deprecated use {@link #checkMulticast(java.net.InetAddress)}
     */
    @Deprecated
    public void checkMulticast(InetAddress maddr, byte ttl) {
    }

    /**
     * Does nothing.
     */
    public void checkPackageAccess(String packageName) {
    }

    /**
     * Does nothing.
     */
    public void checkPackageDefinition(String packageName) {
    }

    /**
     * Does nothing.
     */
    public void checkPropertiesAccess() {
    }

    /**
     * Does nothing.
     */
    public void checkPropertyAccess(String key) {
    }

    /**
     * Does nothing.
     */
    public void checkRead(FileDescriptor fd) {
    }

    /**
     * Does nothing.
     */
    public void checkRead(String file) {
    }

    /**
     * Does nothing.
     */
    public void checkRead(String file, Object context) {
    }

    /**
     * Does nothing.
     */
    public void checkSecurityAccess(String target) {
    }

    /**
     * Does nothing.
     */
    public void checkSetFactory() {
    }

    /**
     * Returns true.
     */
    public boolean checkTopLevelWindow(Object window) {
        return true;
    }

    /**
     * Does nothing.
     */
    public void checkSystemClipboardAccess() {
    }

    /**
     * Does nothing.
     */
    public void checkAwtEventQueueAccess() {
    }

    /**
     * Does nothing.
     */
    public void checkPrintJobAccess() {
    }

    /**
     * Does nothing.
     */
    public void checkWrite(FileDescriptor fd) {
    }

    /**
     * Does nothing.
     */
    public void checkWrite(String file) {
    }

    /**
     * Indicates if this security manager is currently checking something.
     *
     * @return {@code true} if this security manager is executing a security
     *         check method; {@code false} otherwise.
     * @deprecated Use {@link #checkPermission}.
     */
    @Deprecated
    public boolean getInCheck() {
        return inCheck;
    }

    /**
     * Returns an array containing one entry for each method in the current
     * execution stack. Each entry is the {@code java.lang.Class} which
     * represents the class in which the method is defined.
     *
     * @return all classes in the execution stack.
     */
    @SuppressWarnings("unchecked")
    protected Class[] getClassContext() {
        return VMStack.getClasses(-1, false);
    }

    /**
     * Returns the class loader of the first class in the execution stack whose
     * class loader is not a system class loader.
     *
     * @return the most recent non-system class loader.
     * @deprecated Use {@link #checkPermission}.
     */
    @Deprecated
    protected ClassLoader currentClassLoader() {
        /*
         * First, check if AllPermission is allowed. If so, then we are
         * effectively running in an unsafe environment, so just answer null
         * (==> everything is a system class).
         */
        try {
            checkPermission(new AllPermission());
            return null;
        } catch (SecurityException ex) {
        }

        /*
         * Now, check if there are any non-system class loaders in the stack up
         * to the first privileged method (or the end of the stack.
         */
        Class<?>[] classes = VMStack.getClasses(-1, true);
        return classes.length > 0 ? classes[0].getClassLoaderImpl() : null;
    }

    /**
     * Returns the index in the call stack of the first class whose class loader
     * is not a system class loader.
     *
     * @return the frame index of the first method whose class was loaded by a
     *         non-system class loader.
     * @deprecated Use {@link #checkPermission}.
     */
    @Deprecated
    protected int classLoaderDepth() {
        /*
         * First, check if AllPermission is allowed. If so, then we are
         * effectively running in an unsafe environment, so just answer -1 (==>
         * everything is a system class).
         */
        try {
            checkPermission(new AllPermission());
            return -1;
        } catch (SecurityException ex) {
        }

        /*
         * Now, check if there are any non-system class loaders in the stack up
         * to the first privileged method (or the end of the stack.
         */
        Class<?>[] classes = VMStack.getClasses(-1, true);
        return classes.length > 0 ? 0 : -1;
    }

    /**
     * Returns null.
     * @deprecated Use {@link #checkPermission}.
     */
    @Deprecated
    protected Class<?> currentLoadedClass() {
        return null;
    }

    /**
     * Returns the index in the call stack of the first method which is
     * contained in the class with the specified name. Returns -1 if no methods
     * from this class are in the stack.
     *
     * @param name
     *            the name of the class to look for.
     * @return the frame index of the first method found is contained in the
     *         class identified by {@code name}.
     * @deprecated Use {@link #checkPermission}.
     */
    @Deprecated
    protected int classDepth(String name) {
        Class<?>[] classes = VMStack.getClasses(-1, false);
        for (int i = 0; i < classes.length; i++) {
            if (classes[i].getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Indicates whether there is a method in the call stack from the class with
     * the specified name.
     *
     * @param name
     *            the name of the class to look for.
     * @return {@code true} if a method from the class identified by {@code
     *         name} is executing; {@code false} otherwise.
     * @deprecated Use {@link #checkPermission}.
     */
    @Deprecated
    protected boolean inClass(String name) {
        return classDepth(name) != -1;
    }

    /**
     * Indicates whether there is a method in the call stack from a class which
     * was defined by a non-system class loader.
     *
     * @return {@code true} if a method from a class that was defined by a
     *         non-system class loader is executing; {@code false} otherwise.
     * @deprecated Use {@link #checkPermission}
     */
    @Deprecated
    protected boolean inClassLoader() {
        return currentClassLoader() != null;
    }

    /**
     * Returns the thread group which should be used to instantiate new threads.
     * By default, this is the same as the thread group of the thread running
     * this method.
     *
     * @return ThreadGroup the thread group to create new threads in.
     */
    public ThreadGroup getThreadGroup() {
        return Thread.currentThread().getThreadGroup();
    }

    /**
     * Returns an object which encapsulates the security state of the current
     * point in the execution.
     *
     * @return an object that encapsulates information about the current
     *         execution environment.
     */
    public Object getSecurityContext() {
        return AccessController.getContext();
    }

    /**
     * Does nothing.
     */
    public void checkPermission(Permission permission) {
    }

    /**
     * Does nothing.
     */
    public void checkPermission(Permission permission, Object context) {
    }
}
