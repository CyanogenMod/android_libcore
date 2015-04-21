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
 * Copyright (C) 2012 The Android Open Source Project
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

package java.lang.reflect;

import com.android.dex.Dex;
import java.lang.annotation.Annotation;
import libcore.reflect.AnnotationAccess;
import libcore.util.EmptyArray;

/**
 * This class represents methods and constructors.
 * @hide
 */
public final class ArtMethod {
    /* A note on the field order here, it reflects the same field order as laid out by ART. */

    /** Method's declaring class */
    private Class<?> declaringClass;

    /** Short-cut to declaringClass.dexCache.resolvedMethods */
    private ArtMethod[] dexCacheResolvedMethods;

    /** Short-cut to declaringClass.dexCache.resolvedTypes */
    /* package */ Class<?>[] dexCacheResolvedTypes;

    /** Bits encoding access (e.g. public, private) as well as other runtime specific flags */
    private int accessFlags;

    /* Dex file fields. The defining dex file is available via declaringClass.dexCache */

    /** The offset of the code item associated with this method within its defining dex file */
    private int dexCodeItemOffset;

    /** The method index of this method within its defining dex file */
    private int dexMethodIndex;

    /* End of dex file fields. */

    /**
     * Entry within a dispatch table for this method. For static/direct methods the index is
     * into the declaringClass.directMethods, for virtual methods the vtable and for
     * interface methods the ifTable.
     */
    private int methodIndex;

    /** Only created by ART directly. */
    private ArtMethod() {}
}
