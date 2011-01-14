/*
 * Copyright (C) 2011 The Android Open Source Project
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

package test;

/**
 * Class used as part of the class loading tests. This class uses other
 * classes that should have come from the same jar/dex file. Each test
 * method in this class is called from the same-named method in
 * {@code DexClassLoaderTest}.
 */
public class Test2 {
    /**
     * Simple sameness checker, to avoid pulling in JUnit as a dependency.
     */
    public static void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            throw new RuntimeException(
                "EXPECTED: " + expected + "; ACTUAL: " + actual);
        }
    }

    /**
     * Simple sameness checker, to avoid pulling in JUnit as a dependency.
     */
    public static void assertSame(int expected, int actual) {
        if (expected != actual) {
            throw new RuntimeException(
                "EXPECTED: " + expected + "; ACTUAL: " + actual);
        }
    }

    /**
     * Test that an instance of a sibling class can be constructed.
     */
    public static void test_constructor() {
        new Target();
    }

    public static void test_callStaticMethod() {
        assertSame("blort", Target.blort());
    }

    public static void test_getStaticVariable() {
        Target.setStaticVariable(22);
        assertSame(22, Target.staticVariable);
    }

    public static void test_callInstanceMethod() {
        Target target = new Target();
        assertSame("zorch", target.zorch());
    }

    public static void test_getInstanceVariable() {
        Target target = new Target();
        target.setInstanceVariable(10098);
        assertSame(10098, target.instanceVariable);
    }
}
