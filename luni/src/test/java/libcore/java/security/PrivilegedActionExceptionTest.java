/*
 * Copyright 2016 The Android Open Source Project
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

package libcore.java.security;

import junit.framework.TestCase;

import java.security.PrivilegedActionException;

public class PrivilegedActionExceptionTest extends TestCase {

    /**
     * PrivilegedActionException's constructor argument may be rethrown by getException() or
     * getCause().
     * b/31360928
     */
    public void testGetException() {
        Exception e = new Exception();
        PrivilegedActionException pae = new PrivilegedActionException(e);

        assertSame(e, pae.getException());
        assertSame(e, pae.getCause());
    }
}
