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

package java.util.logging;

import java.io.Serializable;
import java.security.BasicPermission;
import java.security.Guard;

/**
 * Legacy security code; this class exists for compatibility only.
 */
public final class LoggingPermission extends BasicPermission implements Guard, Serializable {

    // for serialization compatibility with J2SE 1.4.2
    private static final long serialVersionUID = 63564341580231582L;

    /**
     * Legacy security code; this class exists for compatibility only.
     */
    public LoggingPermission(String name, String actions) {
        super(name, actions);
        if (!"control".equals(name)) {
            throw new IllegalArgumentException("name must be \"control\"");
        }
        if (actions != null && !actions.isEmpty()) {
            throw new IllegalArgumentException("actions != null && !actions.isEmpty()");
        }
    }
}
