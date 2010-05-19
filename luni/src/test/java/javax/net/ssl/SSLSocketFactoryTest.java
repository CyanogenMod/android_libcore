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

package javax.net.ssl;

import java.net.Socket;
import java.net.SocketException;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import junit.framework.TestCase;

public class SSLSocketFactoryTest extends TestCase {
    public void test_SSLSocketFactory_getDefault() {
        SocketFactory sf = SSLSocketFactory.getDefault();
        assertNotNull(sf);
        assertTrue(SSLSocketFactory.class.isAssignableFrom(sf.getClass()));
    }

    public void test_SSLSocketFactory_getDefaultCipherSuites() {
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        String[] cipherSuites = sf.getDefaultCipherSuites();
        assertNotNull(cipherSuites);
        assertTrue(cipherSuites.length != 0);

        // Make sure modifying the result is not observable
        String savedCipherSuite = cipherSuites[0];
        assertNotNull(savedCipherSuite);
        cipherSuites[0] = null;
        assertNotNull(sf.getSupportedCipherSuites()[0]);
        cipherSuites[0] = savedCipherSuite;

        // Make sure all cipherSuites names are expected
        for (String cipherSuite : cipherSuites) {
            assertTrue(StandardNames.CIPHER_SUITES.contains(cipherSuite));
        }
    }

    public void test_SSLSocketFactory_getSupportedCipherSuites() {
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        String[] cipherSuites = sf.getSupportedCipherSuites();
        assertNotNull(cipherSuites);
        assertTrue(cipherSuites.length != 0);

        // Make sure modifying the result is not observable
        String savedCipherSuite = cipherSuites[0];
        assertNotNull(savedCipherSuite);
        cipherSuites[0] = null;
        assertNotNull(sf.getSupportedCipherSuites()[0]);
        cipherSuites[0] = savedCipherSuite;

        // Make sure all cipherSuites names are expected
        Set remainingCipherSuites = new HashSet<String>(StandardNames.CIPHER_SUITES);
        for (String cipherSuite : cipherSuites) {
            assertTrue(remainingCipherSuites.remove(cipherSuite));
        }
        assertEquals(Collections.EMPTY_SET, remainingCipherSuites);

        assertEquals(StandardNames.CIPHER_SUITES.size(), cipherSuites.length);
    }

    public void test_SSLSocketFactory_createSocket() throws Exception {
        try {
            SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            Socket s = sf.createSocket(null, null, -1, false);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            Socket ssl = sf.createSocket(new Socket(), null, -1, false);
            fail();
        } catch (SocketException expected) {
        }

        ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket(0);
        InetSocketAddress sa = (InetSocketAddress) ss.getLocalSocketAddress();
        InetAddress host = sa.getAddress();
        int port = sa.getPort();
        Socket s = new Socket(host, port);
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        Socket ssl = sf.createSocket(s, null, -1, false);
        assertNotNull(ssl);
        assertTrue(SSLSocket.class.isAssignableFrom(ssl.getClass()));
    }
}
