/*
 * Copyright (C) 2013 The Android Open Source Project
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
package libcore.java.lang;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

import dalvik.system.PathClassLoader;

public class ClassTest extends TestCase {

    interface Foo {
        public void foo();
    }

    interface ParameterizedFoo<T> {
        public void foo(T param);
    }

    interface ParameterizedBar<T> extends ParameterizedFoo<T> {
        public void bar(T param);
    }

    interface ParameterizedBaz extends ParameterizedFoo<String> {

    }

    public void test_getGenericSuperclass_nullReturnCases() {
        // Should always return null for interfaces.
        assertNull(Foo.class.getGenericSuperclass());
        assertNull(ParameterizedFoo.class.getGenericSuperclass());
        assertNull(ParameterizedBar.class.getGenericSuperclass());
        assertNull(ParameterizedBaz.class.getGenericSuperclass());

        assertNull(Object.class.getGenericSuperclass());
        assertNull(void.class.getGenericSuperclass());
        assertNull(int.class.getGenericSuperclass());
    }

    public void test_getGenericSuperclass_returnsObjectForArrays() {
        assertSame(Object.class, (new Integer[0]).getClass().getGenericSuperclass());
    }

    public void test_b28833829() throws Exception {
        File f = File.createTempFile("temp_b28833829", ".dex");
        try (InputStream is =
            getClass().getClassLoader().getResourceAsStream("TestBug28833829.dex");
            OutputStream os = new FileOutputStream(f)) {
            byte[] buffer = new byte[8192];
            int bytesRead = 0;
            while ((bytesRead = is.read(buffer)) >= 0) {
                os.write(buffer, 0, bytesRead);
            }
        }

        PathClassLoader pcl = new PathClassLoader(f.getAbsolutePath(), null);
        Class<?> cl = pcl.loadClass(
            "libcore.java.lang.TestBadInnerClass_Outer$ClassTestBadInnerClass_InnerClass");

        // Note that getName() and getSimpleName() are inconsistent here because for
        // inner classes,  the latter is fetched directly from the InnerClass
        // annotation in the dex file. We do not perform any sort of consistency
        // checks with the class name or the enclosing class name. Unfortunately, applications
        // have come to rely on this behaviour.
        assertEquals("libcore.java.lang.TestBadInnerClass_Outer$ClassTestBadInnerClass_InnerClass",
            cl.getName());
        assertEquals("TestBadInnerClass_InnerXXXXX", cl.getSimpleName());
    }

    interface A {
        public static String name = "A";
    }
    interface B {
        public static String name = "B";
    }
    class X implements A { }
    class Y extends X implements B { }
    public void test_getField() {
        try {
            assertEquals(A.class.getField("name"), X.class.getField("name"));
        } catch (NoSuchFieldException e) {
            fail("Got exception");
        }
        try {
            assertEquals(B.class.getField("name"), Y.class.getField("name"));
        } catch (NoSuchFieldException e) {
            fail("Got exception");
        }
    }

    interface C {
        void foo();
    }
    interface D extends C {
        void foo();
    }
    abstract class Z implements D { }

    public void test_getMethod() {
      try {
          assertEquals(Z.class.getMethod("foo"), D.class.getMethod("foo"));
      } catch (NoSuchMethodException e) {
          fail("Got exception");
      }
    }
}
