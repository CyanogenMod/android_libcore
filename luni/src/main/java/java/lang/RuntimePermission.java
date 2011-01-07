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

import java.security.BasicPermission;

/**
 * Legacy security code; this class exists for compatibility only.
 */
public final class RuntimePermission extends BasicPermission {

    private static final long serialVersionUID = 7399184964622342223L;

    /**
     * Creates an instance of {@code RuntimePermission} with the specified name.
     *
     * @param permissionName
     *            the name of the new permission.
     */
    public RuntimePermission(String permissionName) {
        super(permissionName);
    }

    /**
     * Creates an instance of {@code RuntimePermission} with the specified name
     * and action list. The action list is ignored.
     *
     * @param name
     *            the name of the new permission.
     * @param actions
     *            ignored.
     */
    public RuntimePermission(String name, String actions) {
        super(name, actions);
    }
}
