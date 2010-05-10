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

import java.security.Provider;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import junit.framework.TestCase;

public class SSLContextTest extends TestCase {

    public void test_SSLContext_getInstance() throws Exception {
        try {
            SSLContext.getInstance(null);
            fail();
        } catch (NullPointerException e) {
        }
        assertNotNull(SSLContext.getInstance("SSL"));
        assertNotNull(SSLContext.getInstance("SSLv3"));
        assertNotNull(SSLContext.getInstance("TLS"));
        assertNotNull(SSLContext.getInstance("TLSv1"));

        assertNotSame(SSLContext.getInstance("TLS"),
                      SSLContext.getInstance("TLS"));

        try {
            SSLContext.getInstance(null, (String) null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            SSLContext.getInstance(null, "");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            SSLContext.getInstance("TLS", (String) null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            SSLContext.getInstance(null, TestSSLContext.PROVIDER_NAME);
            fail();
        } catch (NullPointerException e) {
        }
    }

    public void test_SSLContext_getProtocol() throws Exception {
        assertProtocolExistsForName("SSL");
        assertProtocolExistsForName("TLS");
    }

    private void assertProtocolExistsForName(String protocolName) throws Exception {
        String protocol = SSLContext.getInstance(protocolName).getProtocol();
        assertNotNull(protocol);
        assertEquals(protocolName, protocol);
    }

    public void test_SSLContext_getProvider() throws Exception {
        Provider provider = SSLContext.getInstance("TLS").getProvider();
        assertNotNull(provider);
        assertEquals(TestSSLContext.PROVIDER_NAME, provider.getName());
    }

    public void test_SSLContext_init() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, null);
    }

    public void test_SSLContext_getSocketFactory() throws Exception {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.getSocketFactory();
            fail();
        } catch (IllegalStateException e) {
        }
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, null);
        SocketFactory sf = sslContext.getSocketFactory();
        assertNotNull(sf);
        assertTrue(SSLSocketFactory.class.isAssignableFrom(sf.getClass()));
    }

    public void test_SSLContext_getServerSocketFactory() throws Exception {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.getServerSocketFactory();
            fail();
        } catch (IllegalStateException e) {
        }
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, null);
        ServerSocketFactory ssf = sslContext.getServerSocketFactory();
        assertNotNull(ssf);
        assertTrue(SSLServerSocketFactory.class.isAssignableFrom(ssf.getClass()));
    }

    public void test_SSLContext_createSSLEngine() throws Exception {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.createSSLEngine();
            fail();
        } catch (IllegalStateException e) {
        }
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.createSSLEngine(null, -1);
            fail();
        } catch (IllegalStateException e) {
        }
        {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            SSLEngine se = sslContext.createSSLEngine();
            assertNotNull(se);
        }
        {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            SSLEngine se = sslContext.createSSLEngine(null, -1);
            assertNotNull(se);
        }
    }

    public void test_SSLContext_getServerSessionContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        SSLSessionContext sessionContext = sslContext.getServerSessionContext();
        assertNotNull(sessionContext);

        assertNotSame(SSLContext.getInstance("TLS").getServerSessionContext(),
                      sessionContext);
    }

    public void test_SSLContext_getClientSessionContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        SSLSessionContext sessionContext = sslContext.getClientSessionContext();
        assertNotNull(sessionContext);

        assertNotSame(SSLContext.getInstance("TLS").getClientSessionContext(),
                      sessionContext);
    }

    public void test_SSLContextTest_TestSSLContext_create() {
        TestSSLContext testContext = TestSSLContext.create();
        assertNotNull(testContext);
        assertNotNull(testContext.keyStore);
        assertNull(testContext.keyStorePassword);
        assertNotNull(testContext.publicAlias);
        assertNotNull(testContext.privateAlias);
        assertNotNull(testContext.sslContext);
        assertNotNull(testContext.serverSocket);
        assertNotNull(testContext.host);
        assertTrue(testContext.port != 0);
    }
}
