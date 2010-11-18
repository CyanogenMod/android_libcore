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

package libcore.java.security;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;

public class SignatureTest extends TestCase {

    // 20 bytes for DSA
    private final byte[] DATA = new byte[20];

    public void test_getInstance() throws Exception {
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            Set<Provider.Service> services = provider.getServices();
            for (Provider.Service service : services) {
                String type = service.getType();
                if (!type.equals("Signature")) {
                    continue;
                }
                String algorithm = service.getAlgorithm();
                try {
                    KeyPair kp = keyPair(algorithm);
                    // Signature.getInstance(String)
                    Signature sig1 = Signature.getInstance(algorithm);
                    assertEquals(algorithm, sig1.getAlgorithm());
                    test_Signature(sig1, kp);

                    // Signature.getInstance(String, Provider)
                    Signature sig2 = Signature.getInstance(algorithm, provider);
                    assertEquals(algorithm, sig2.getAlgorithm());
                    assertEquals(provider, sig2.getProvider());
                    test_Signature(sig2, kp);

                    // Signature.getInstance(String, String)
                    Signature sig3 = Signature.getInstance(algorithm, provider.getName());
                    assertEquals(algorithm, sig3.getAlgorithm());
                    assertEquals(provider, sig3.getProvider());
                    test_Signature(sig3, kp);
                } catch (Exception e) {
                    throw new Exception("Problem testing Signature." + algorithm, e);
                }
            }
        }
    }

    private final Map<String, KeyPair> keypairAlgorithmToInstance
            = new HashMap<String, KeyPair>();

    private KeyPair keyPair(String sigAlgorithm) throws Exception {
        if (sigAlgorithm.endsWith("Encryption")) {
            sigAlgorithm = sigAlgorithm.substring(0, sigAlgorithm.length()-"Encryption".length());
        }

        String kpAlgorithm;
        // note ECDSA must be before DSA
        if (sigAlgorithm.endsWith("ECDSA")) {
            kpAlgorithm = "EC";
        } else if (sigAlgorithm.endsWith("DSA")) {
            kpAlgorithm = "DSA";
        } else if (sigAlgorithm.endsWith("RSA")) {
            kpAlgorithm = "RSA";
        } else {
            throw new Exception("Unknown KeyPair algorithm for Signature algorithm "
                                + sigAlgorithm);
        }

        KeyPair kp = keypairAlgorithmToInstance.get(kpAlgorithm);
        if (kp == null) {
            kp = KeyPairGenerator.getInstance(kpAlgorithm).generateKeyPair();
            keypairAlgorithmToInstance.put(sigAlgorithm, kp);
        }
        return kp;
    }

    private void test_Signature(Signature sig, KeyPair keyPair) throws Exception {
        sig.initSign(keyPair.getPrivate());
        sig.update(DATA);
        byte[] signature = sig.sign();
        assertNotNull(signature);
        assertTrue(signature.length > 0);

        sig.initVerify(keyPair.getPublic());
        sig.update(DATA);
        assertTrue(sig.verify(signature));
    }
}
