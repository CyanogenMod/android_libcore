/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.security;

import java.util.ArrayList;
import java.util.List;


/**
 * An AccessControlContext is used to make system resource access decisions
 * based on the context it encapsulates.
 *
 * <p>More specifically, it encapsulates a context and
 * has a single method, <code>checkPermission</code>,
 * that is equivalent to the <code>checkPermission</code> method
 * in the AccessController class, with one difference: The AccessControlContext
 * <code>checkPermission</code> method makes access decisions based on the
 * context it encapsulates,
 * rather than that of the current execution thread.
 *
 * <p>Thus, the purpose of AccessControlContext is for those situations where
 * a security check that should be made within a given context
 * actually needs to be done from within a
 * <i>different</i> context (for example, from within a worker thread).
 *
 * <p> An AccessControlContext is created by calling the
 * <code>AccessController.getContext</code> method.
 * The <code>getContext</code> method takes a "snapshot"
 * of the current calling context, and places
 * it in an AccessControlContext object, which it returns. A sample call is
 * the following:
 *
 * <pre>
 *   AccessControlContext acc = AccessController.getContext()
 * </pre>
 *
 * <p>
 * Code within a different context can subsequently call the
 * <code>checkPermission</code> method on the
 * previously-saved AccessControlContext object. A sample call is the
 * following:
 *
 * <pre>
 *   acc.checkPermission(permission)
 * </pre>
 *
 * @see AccessController
 *
 * @author Roland Schemers
 */

public final class AccessControlContext {

    /**
     * Create an AccessControlContext with the given array of ProtectionDomains.
     * Context must not be null. Duplicate domains will be removed from the
     * context.
     *
     * @param context the ProtectionDomains associated with this context.
     * The non-duplicate domains are copied from the array. Subsequent
     * changes to the array will not affect this AccessControlContext.
     * @throws NullPointerException if <code>context</code> is <code>null</code>
     */
    public AccessControlContext(ProtectionDomain context[]) {
    }

    /**
     * Create a new <code>AccessControlContext</code> with the given
     * <code>AccessControlContext</code> and <code>DomainCombiner</code>.
     * This constructor associates the provided
     * <code>DomainCombiner</code> with the provided
     * <code>AccessControlContext</code>.
     *
     * <p>
     *
     * @param acc the <code>AccessControlContext</code> associated
     *          with the provided <code>DomainCombiner</code>.
     *
     * @param combiner the <code>DomainCombiner</code> to be associated
     *          with the provided <code>AccessControlContext</code>.
     *
     * @exception NullPointerException if the provided
     *          <code>context</code> is <code>null</code>.
     *
     * @exception SecurityException if a security manager is installed and the
     *          caller does not have the "createAccessControlContext"
     *          {@link SecurityPermission}
     * @since 1.3
     */
    public AccessControlContext(AccessControlContext acc,
                                DomainCombiner combiner) {
    }


    /**
     * Get the <code>DomainCombiner</code> associated with this
     * <code>AccessControlContext</code>.
     *
     * <p>
     *
     * @return the <code>DomainCombiner</code> associated with this
     *          <code>AccessControlContext</code>, or <code>null</code>
     *          if there is none.
     *
     * @exception SecurityException if a security manager is installed and
     *          the caller does not have the "getDomainCombiner"
     *          {@link SecurityPermission}
     * @since 1.3
     */
    public DomainCombiner getDomainCombiner() {
      return null;
    }

    /**
     * Determines whether the access request indicated by the
     * specified permission should be allowed or denied, based on
     * the security policy currently in effect, and the context in
     * this object. The request is allowed only if every ProtectionDomain
     * in the context implies the permission. Otherwise the request is
     * denied.
     *
     * <p>
     * This method quietly returns if the access request
     * is permitted, or throws a suitable AccessControlException otherwise.
     *
     * @param perm the requested permission.
     *
     * @exception AccessControlException if the specified permission
     * is not permitted, based on the current security policy and the
     * context encapsulated by this object.
     * @exception NullPointerException if the permission to check for is null.
     */
    public void checkPermission(Permission perm)
        throws AccessControlException {
    }

}
