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

package libcore.javax.security.auth.x500;

import junit.framework.TestCase;
import libcore.java.util.SerializableTester;
import javax.security.auth.x500.X500Principal;

public class X500PrincipalTest extends TestCase {

    public void testSerialization() {
        String expected = "aced0005737200266a617661782e73656375726974792e617574682e7"
                + "83530302e583530305072696e636970616cf90dff3c88b877c703000078707572"
                + "00025b42acf317f8060854e002000078700000006a30683117301506035504031"
                + "30e7777772e676f6f676c652e636f6d31133011060355040a130a476f6f676c65"
                + "20496e63311630140603550407130d4d6f756e7461696e2056696577311330110"
                + "603550408130a43616c69666f726e6961310b300906035504061302555378";
        X500Principal actual = new X500Principal("C=US, "
                                                 + "ST=California, "
                                                 + "L=Mountain View, "
                                                 + "O=Google Inc, "
                                                 + "CN=www.google.com");
        new SerializableTester<X500Principal>(actual, expected).test();
    }
}
