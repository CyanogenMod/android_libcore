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

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import junit.framework.TestCase;
import libcore.util.SerializationTester;

public final class SerializationTest extends TestCase {

    // http://b/4471249
    public void testSerializeFieldMadeTransient() throws Exception {
        // Does ObjectStreamClass have the right idea?
        ObjectStreamClass osc = ObjectStreamClass.lookup(FieldMadeTransient.class);
        ObjectStreamField[] fields = osc.getFields();
        assertEquals(1, fields.length);
        assertEquals("nonTransientInt", fields[0].getName());
        assertEquals(int.class, fields[0].getType());

        // this was created by serializing a FieldMadeTransient with a non-0 transientInt
        String s = "aced0005737200346c6962636f72652e6a6176612e696f2e53657269616c697a6174696f6e54657"
                + "374244669656c644d6164655472616e7369656e74000000000000000002000149000c7472616e736"
                + "9656e74496e747870abababab";
        FieldMadeTransient deserialized = (FieldMadeTransient) SerializationTester.deserializeHex(s);
        assertEquals(0, deserialized.transientInt);
    }

    static class FieldMadeTransient implements Serializable {
        private static final long serialVersionUID = 0L;
        private transient int transientInt;
        private int nonTransientInt;
    }

    public void testSerializeFieldMadeStatic() throws Exception {
        // Does ObjectStreamClass have the right idea?
        ObjectStreamClass osc = ObjectStreamClass.lookup(FieldMadeStatic.class);
        ObjectStreamField[] fields = osc.getFields();
        assertEquals(0, fields.length);

        // This was created by serializing a FieldMadeStatic with a non-static staticInt
        String s = "aced0005737200316c6962636f72652e6a6176612e696f2e53657269616c697a6174696f6e54657"
                + "374244669656c644d6164655374617469630000000000000000020001490009737461746963496e7"
                + "47870000022b8";
        FieldMadeStatic deserialized = (FieldMadeStatic) SerializationTester.deserializeHex(s);
        // The field data is simply ignored if it is static.
        assertEquals(9999, deserialized.staticInt);
    }

    static class FieldMadeStatic implements Serializable {
        private static final long serialVersionUID = 0L;
        // private int staticInt = 8888;
        private static int staticInt = 9999;
    }

    // We can serialize an object that has an unserializable field providing it is null.
    public void testDeserializeNullUnserializableField() throws Exception {
        // This was created by creating a new SerializableContainer and not setting the
        // unserializable field. A canned serialized form is used so we can tell if the static
        // initializers were executed during deserialization.
        // SerializationTester.serializeHex(new SerializableContainer());
        String s = "aced0005737200376c6962636f72652e6a6176612e696f2e53657269616c697a6174696f6e54657"
                + "3742453657269616c697a61626c65436f6e7461696e657200000000000000000200014c000e756e7"
                + "3657269616c697a61626c657400334c6c6962636f72652f6a6176612f696f2f53657269616c697a6"
                + "174696f6e546573742457617353657269616c697a61626c653b787070";

        serializableContainerInitializedFlag = false;
        wasSerializableInitializedFlag = false;

        SerializableContainer sc = (SerializableContainer) SerializationTester.deserializeHex(s);
        assertNull(sc.unserializable);

        // Confirm the container was initialized, but the class for the null field was not.
        assertTrue(serializableContainerInitializedFlag);
        assertFalse(wasSerializableInitializedFlag);
    }

    public static boolean serializableContainerInitializedFlag = false;

    static class SerializableContainer implements Serializable {
        private static final long serialVersionUID = 0L;
        private Object unserializable = null;

        static {
            serializableContainerInitializedFlag = true;
        }
    }

    // We must not serialize an object that has a non-null unserializable field.
    public void testSerializeUnserializableField() throws Exception {
        SerializableContainer sc = new SerializableContainer();
        sc.unserializable = new WasSerializable();
        try {
            SerializationTester.serializeHex(sc);
            fail();
        } catch (NotSerializableException expected) {
        }
    }

    // It must not be possible to deserialize an object if a field is no longer serializable.
    public void testDeserializeUnserializableField() throws Exception {
        // This was generated by creating a SerializableContainer and setting the unserializable
        // field to a WasSerializable when it was still Serializable. A canned serialized form is
        // used so we can tell if the static initializers were executed during deserialization.
        // SerializableContainer sc = new SerializableContainer();
        // sc.unserializable = new WasSerializable();
        // SerializationTester.serializeHex(sc);
        String s = "aced0005737200376c6962636f72652e6a6176612e696f2e53657269616c697a6174696f6e54657"
                + "3742453657269616c697a61626c65436f6e7461696e657200000000000000000200014c000e756e7"
                + "3657269616c697a61626c657400124c6a6176612f6c616e672f4f626a6563743b7870737200316c6"
                + "962636f72652e6a6176612e696f2e53657269616c697a6174696f6e5465737424576173536572696"
                + "16c697a61626c65000000000000000002000149000169787000000000";

        serializableContainerInitializedFlag = false;
        wasSerializableInitializedFlag = false;
        try {
            SerializationTester.deserializeHex(s);
            fail();
        } catch (InvalidClassException expected) {
        }
        // Confirm neither the container nor the contained class was initialized.
        assertFalse(serializableContainerInitializedFlag);
        assertFalse(wasSerializableInitializedFlag);
    }

    public void testSerialVersionUidChange() throws Exception {
        // this was created by serializing a SerialVersionUidChanged with serialVersionUID = 0L
        String s = "aced0005737200396c6962636f72652e6a6176612e696f2e53657269616c697a6174696f6e54657"
                + "3742453657269616c56657273696f6e5569644368616e67656400000000000000000200014900016"
                + "1787000000003";
        try {
            SerializationTester.deserializeHex(s);
            fail();
        } catch (InvalidClassException expected) {
        }
    }

    static class SerialVersionUidChanged implements Serializable {
        private static final long serialVersionUID = 1L; // was 0L
        private int a;
    }

    public void testMissingSerialVersionUid() throws Exception {
        // this was created by serializing a FieldsChanged with one int field named 'a'
        String s = "aced00057372002f6c6962636f72652e6a6176612e696f2e53657269616c697a6174696f6e54657"
                + "374244669656c64734368616e6765643bcfb934e310fa1c02000149000161787000000003";
        try {
            SerializationTester.deserializeHex(s);
            fail();
        } catch (InvalidClassException expected) {
        }
    }

    static class FieldsChanged implements Serializable {
        private int b; // was 'a'
    }
}
