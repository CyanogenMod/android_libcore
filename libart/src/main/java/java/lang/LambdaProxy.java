/*
 * Copyright (C) 2015 The Android Open Source Project
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

package java.lang;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import libcore.util.EmptyArray;

/**
 * Runtime-only class used as a superclass for lambda proxies (box-object opcode).
 * @hide
 */
public class LambdaProxy {
    // TODO: This should be a type-erased "reference" to an art::lambda::Closure.
    // TODO: only 32-bit on 32-bit systems.
    private long closure;  // native pointer to art::lambda::Closure.
    // The closure is deleted by (native) lambda::BoxTable after it detects the LambdaProxy was GCd.

    @SuppressWarnings("unused")
    protected LambdaProxy() {
    }
}
