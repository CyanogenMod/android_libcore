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

package libcore.javax.net.ssl;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import junit.framework.TestCase;
import libcore.java.security.StandardNames;

public class SSLServerSocketTest extends TestCase {

  public void testDefaultConfiguration() throws Exception {
    SSLServerSocket serverSocket =
        (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket();
    StandardNames.assertDefaultCipherSuites(serverSocket.getEnabledCipherSuites());
    StandardNames.assertSupportedCipherSuites(serverSocket.getSupportedCipherSuites());
    StandardNames.assertValidProtocols(serverSocket.getEnabledProtocols());
    StandardNames.assertSupportedProtocols(serverSocket.getSupportedProtocols());
    assertTrue(serverSocket.getEnableSessionCreation());
    assertFalse(serverSocket.getNeedClientAuth());
    assertFalse(serverSocket.getWantClientAuth());
  }
}
