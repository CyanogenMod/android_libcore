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

package libcore.java.lang;

import junit.framework.TestCase;

public final class ArrayStoreExceptionTest extends TestCase {
    public void testArrayStoreException1() throws Exception {
        Object[] array = new String[10];
        Object o = new Exception();
        try {
            array[0] = o;
            fail();
        } catch (ArrayStoreException ex) {
            ex.printStackTrace();
            assertEquals("java.lang.Exception cannot be stored in an array of type java.lang.String[]", ex.getMessage());
        }
    }

    public void testArrayStoreException2() throws Exception {
        Object[] array = makeArray2();
        Object o = new Integer(5);
        try {
            array[0] = o;
            fail();
        } catch (ArrayStoreException ex) {
            assertEquals("java.lang.Integer cannot be stored in an array of type "
                    + "libcore.java.lang.ArrayStoreExceptionTest$Nonce[][]",
                    ex.getMessage());
        }
    }
    private static Object[] makeArray2() {
        // TODO: This is a separate method to work around a bug in dx. <http://b/4065116>
        return new Nonce[10][];
    }

    public void testArrayStoreException3() throws Exception {
        Object[] array = makeArray3();
        Object o = new Nonce[1];
        try {
            array[0] = o;
            fail();
        } catch (ArrayStoreException ex) {
            assertEquals("libcore.java.lang.ArrayStoreExceptionTest$Nonce[] cannot be stored "
                    + "in an array of type java.lang.Float[][]",
                    ex.getMessage());
        }
    }
    private static Object[] makeArray3() {
        // TODO: This is a separate method to work around a bug in dx. <http://b/4065116>
        return new Float[10][];
    }

    /**
     * This class is just used so that we have an example of getting a
     * message that includes an inner class.
     */
    private static class Nonce {
        // This space intentionally left blank.
    }
}
