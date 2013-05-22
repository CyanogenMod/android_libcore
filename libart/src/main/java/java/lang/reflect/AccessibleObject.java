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

package java.lang.reflect;

import java.lang.annotation.Annotation;

/**
 * The superclass of fields, constructors and methods. Reflective operations
 * like {@link Field#set} and {@link Method#invoke} will fail with an {@link
 * IllegalAccessException} when the caller doesn't satisfy the target's access
 * modifier (either public, protected, package-private or private) and the
 * <i>accessible</i> flag is false. Prevent the exception by setting the
 * <i>accessible</i> flag to true with {@link #setAccessible(boolean)
 * setAccessible(true)}.
 *
 * <p>On Android releases up to and including Android 4.0 (Ice Cream Sandwich),
 * the <i>accessible</i> flag is false by default. On releases after
 * Android 4.0, the <i>accessible</i> flag is true by default and cannot be set
 * to false.
 */
// STOPSHIP 'After 4.0' is a guess; identify the release in which 'accessible' was true by default
public class AccessibleObject implements AnnotatedElement {
    protected AccessibleObject() {
    }

    /**
     * Returns true if this object is accessible without access checks.
     */
    public boolean isAccessible() {
        return true; // always!
    }

    /**
     * Attempts to set the accessible flag. Setting this to true prevents {@code
     * IllegalAccessExceptions}.
     */
    public void setAccessible(boolean flag) {
    }

    /**
     * Attempts to set the accessible flag for all objects in {@code objects}.
     * Setting this to true prevents {@code IllegalAccessExceptions}.
     */
    public static void setAccessible(AccessibleObject[] objects, boolean flag) {
    }

    @Override public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        throw new UnsupportedOperationException();
    }

    @Override public Annotation[] getDeclaredAnnotations() {
        throw new UnsupportedOperationException();
    }

    @Override public Annotation[] getAnnotations() {
        // for all but Class, getAnnotations == getDeclaredAnnotations
        return getDeclaredAnnotations();
    }

    @Override public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        throw new UnsupportedOperationException();
    }
}
