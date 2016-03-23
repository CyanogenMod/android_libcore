/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package libcore.java.util;

import java.util.Arrays;

public class ArraysTest extends junit.framework.TestCase {

    /**
     * java.util.Arrays#setAll(int[], java.util.function.IntUnaryOperator)
     */
    public void test_setAll$I() {
        int[] list = new int[3];
        list[0] = 0;
        list[1] = 1;
        list[2] = 2;

        Arrays.setAll(list, x -> x + 1);
        assertEquals(1, list[0]);
        assertEquals(2, list[1]);
        assertEquals(3, list[2]);

        try {
            Arrays.setAll(list, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Arrays.setAll((int[]) null, (x -> x + 1));
            fail();
        } catch (NullPointerException expected) {
        }
    }

    /**
     * java.util.Arrays#parallelSetAll(int[], java.util.function.IntUnaryOperator)
     */
    public void test_parallelSetAll$I() {
        int[] list = new int[3];
        list[0] = 0;
        list[1] = 1;
        list[2] = 2;

        Arrays.parallelSetAll(list, x -> x + 1);
        assertEquals(1, list[0]);
        assertEquals(2, list[1]);
        assertEquals(3, list[2]);

        try {
            Arrays.parallelSetAll(list, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Arrays.parallelSetAll((int[]) null, (x -> x + 1));
            fail();
        } catch (NullPointerException expected) {
        }
    }

    /**
     * java.util.Arrays#setAll(long[], java.util.function.IntToLongFunction)
     */
    public void test_setAll$L() {
        long[] list = new long[3];
        list[0] = 0;
        list[1] = 1;
        list[2] = 2;

        Arrays.setAll(list, x -> x + 1);
        assertEquals(1, list[0]);
        assertEquals(2, list[1]);
        assertEquals(3, list[2]);

        try {
            Arrays.setAll(list, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Arrays.setAll((long[]) null, (x -> x + 1));
            fail();
        } catch (NullPointerException expected) {
        }
    }

    /**
     * java.util.Arrays#parallelSetAll(long[], java.util.function.IntToLongFunction)
     */
    public void test_parallelSetAll$L() {
        long[] list = new long[3];
        list[0] = 0;
        list[1] = 1;
        list[2] = 2;

        Arrays.parallelSetAll(list, x -> x + 1);
        assertEquals(1, list[0]);
        assertEquals(2, list[1]);
        assertEquals(3, list[2]);

        try {
            Arrays.parallelSetAll(list, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Arrays.parallelSetAll((long[]) null, (x -> x + 1));
            fail();
        } catch (NullPointerException expected) {
        }
    }

    /**
     * java.util.Arrays#setAll(double[], java.util.function.IntToDoubleFunction)
     */
    public void test_setAll$D() {
        double[] list = new double[3];
        list[0] = 0.0d;
        list[1] = 1.0d;
        list[2] = 2.0d;

        Arrays.setAll(list, x -> x + 0.5);
        assertEquals(0.5d, list[0]);
        assertEquals(1.5d, list[1]);
        assertEquals(2.5d, list[2]);

        try {
            Arrays.setAll(list, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Arrays.setAll((double[]) null, x -> x + 0.5);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    /**
     * java.util.Arrays#parallelSetAll(double[], java.util.function.IntToDoubleFunction)
     */
    public void test_parallelSetAll$D() {
        double[] list = new double[3];
        list[0] = 0.0d;
        list[1] = 1.0d;
        list[2] = 2.0d;

        Arrays.parallelSetAll(list, x -> x + 0.5);
        assertEquals(0.5d, list[0]);
        assertEquals(1.5d, list[1]);
        assertEquals(2.5d, list[2]);

        try {
            Arrays.parallelSetAll(list, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Arrays.parallelSetAll((double[]) null, x -> x + 0.5);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    /**
     * java.util.Array#setAll(T[], java.util.function.IntFunction<\? extends T>)
     */
    public void test_setAll$T() {
        String[] strings = new String[3];
        strings[0] = "a";
        strings[0] = "b";
        strings[0] = "c";

        Arrays.setAll(strings, x -> "a" + x);
        assertEquals("a0", strings[0]);
        assertEquals("a1", strings[1]);
        assertEquals("a2", strings[2]);

        try {
            Arrays.setAll(strings, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Arrays.setAll((String[]) null, x -> "a" + x);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    /**
     * java.util.Array#parallelSetAll(T[], java.util.function.IntFunction<\? extends T>)
     */
    public void test_parallelSetAll$T() {
        String[] strings = new String[3];
        strings[0] = "a";
        strings[0] = "b";
        strings[0] = "c";

        Arrays.parallelSetAll(strings, x -> "a" + x);
        assertEquals("a0", strings[0]);
        assertEquals("a1", strings[1]);
        assertEquals("a2", strings[2]);

        try {
            Arrays.parallelSetAll(strings, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Arrays.parallelSetAll((String[]) null, x -> "a" + x);
            fail();
        } catch (NullPointerException expected) {
        }
    }
}
