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

package java.lang.reflect;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

public final class GenericExceptionsTest extends TestCase {

    public void testGenericExceptionsOfMethodsWithTypeParameters() throws Exception {
        Method method = Thrower.class.getMethod("parameterizedMethod");
        assertEquals(Arrays.<Type>asList(IOException.class),
                Arrays.asList(method.getGenericExceptionTypes()));
    }

    public void testGenericExceptionsOfMethodsWithGenericParameters() throws Exception {
        Method method = Thrower.class.getMethod("genericParameters", List.class);
        assertEquals(Arrays.<Type>asList(IOException.class),
                Arrays.asList(method.getGenericExceptionTypes()));
    }

    public void testGenericExceptionsOfConstructorsWithTypeParameters() throws Exception {
        Method method = Thrower.class.getMethod("parameterizedMethod");
        assertEquals(Arrays.<Type>asList(IOException.class),
                Arrays.asList(method.getGenericExceptionTypes()));
    }

    public void testGenericExceptionsOfConstructorsWithGenericParameters() throws Exception {
        Method method = Thrower.class.getMethod("genericParameters", List.class);
        assertEquals(Arrays.<Type>asList(IOException.class),
                Arrays.asList(method.getGenericExceptionTypes()));
    }

    static class Thrower {
        <T> Thrower() throws IOException {}
        Thrower(List<?> unused) throws IOException {}
        public <T> void parameterizedMethod() throws IOException {}
        public void genericParameters(List<?> unused) throws IOException {}
    }
}
