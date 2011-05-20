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

package libcore.java.io;

import java.io.Serializable;
import junit.framework.TestCase;
import libcore.java.util.SerializableTester;

public final class SerializationTest extends TestCase {

    // http://b/4471249
    public void testSerializeFieldMadeTransient() throws Exception {
        // this was created by serializing a FieldMadeTransient with a non-0 transientInt
        String s = "aced0005737200346c6962636f72652e6a6176612e696f2e53657269616c697a6174696f6e54657"
                + "374244669656c644d6164655472616e7369656e74000000000000000002000149000c7472616e736"
                + "9656e74496e747870abababab";
        FieldMadeTransient deserialized = (FieldMadeTransient) SerializableTester.deserializeHex(s);
        assertEquals(0, deserialized.transientInt);
    }

    static class FieldMadeTransient implements Serializable {
        private static final long serialVersionUID = 0L;
        private transient int transientInt;
    }
}
