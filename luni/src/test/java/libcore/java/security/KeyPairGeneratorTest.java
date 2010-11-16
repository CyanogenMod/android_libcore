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
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;

public class KeyPairGeneratorTest extends TestCase {

    public void test_getInstance() throws Exception {
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            Set<Provider.Service> services = provider.getServices();
            for (Provider.Service service : services) {
                String type = service.getType();
                if (!type.equals("KeyPairGenerator")) {
                    continue;
                }
                String algorithm = service.getAlgorithm();
                try {
                    // KeyPairGenerator.getInstance(String)
                    KeyPairGenerator kpg1 = KeyPairGenerator.getInstance(algorithm);
                    assertEquals(algorithm, kpg1.getAlgorithm());
                    test_KeyPairGenerator(kpg1);

                    // KeyPairGenerator.getInstance(String, Provider)
                    KeyPairGenerator kpg2 = KeyPairGenerator.getInstance(algorithm, provider);
                    assertEquals(algorithm, kpg2.getAlgorithm());
                    assertEquals(provider, kpg2.getProvider());
                    test_KeyPairGenerator(kpg2);

                    // KeyPairGenerator.getInstance(String, String)
                    KeyPairGenerator kpg3 = KeyPairGenerator.getInstance(algorithm,
                                                                        provider.getName());
                    assertEquals(algorithm, kpg3.getAlgorithm());
                    assertEquals(provider, kpg3.getProvider());
                    test_KeyPairGenerator(kpg3);
                } catch (Exception e) {
                    throw new Exception("Problem testing KeyPairGenerator." + algorithm, e);
                }
            }
        }
    }

    private static final Map<String, List<Integer>> KEY_SIZES
            = new HashMap<String, List<Integer>>();
    private static void putKeySize(String algorithm, int keySize) {
        algorithm = algorithm.toUpperCase();
        List<Integer> keySizes = KEY_SIZES.get(algorithm);
        if (keySizes == null) {
            keySizes = new ArrayList<Integer>();
            KEY_SIZES.put(algorithm, keySizes);
        }
        keySizes.add(keySize);
    }
    private static List<Integer> getKeySizes(String algorithm) throws Exception {
        algorithm = algorithm.toUpperCase();
        List<Integer> keySizes = KEY_SIZES.get(algorithm);
        if (keySizes == null) {
            throw new Exception("Unknown key sizes for KeyPairGenerator." + algorithm);
        }
        return keySizes;
    }
    static {
        putKeySize("DSA", 512);
        putKeySize("DSA", 512+64);
        putKeySize("DSA", 1024);
        putKeySize("RSA", 512);
        putKeySize("DH", 512);
        putKeySize("DH", 512+64);
        putKeySize("DH", 1024);
        putKeySize("DiffieHellman", 512);
        putKeySize("DiffieHellman", 512+64);
        putKeySize("DiffieHellman", 1024);
        putKeySize("EC", 256);
    }

    private void test_KeyPairGenerator(KeyPairGenerator kpg) throws Exception {
        // without a call to initialize
        test_KeyPair(kpg, kpg.genKeyPair());
        test_KeyPair(kpg, kpg.generateKeyPair());

        String algorithm = kpg.getAlgorithm();
        List<Integer> keySizes = getKeySizes(algorithm);
        for (int keySize : keySizes) {
            kpg.initialize(keySize);
            test_KeyPair(kpg, kpg.genKeyPair());
            test_KeyPair(kpg, kpg.generateKeyPair());

            kpg.initialize(keySize, (SecureRandom) null);
            test_KeyPair(kpg, kpg.genKeyPair());
            test_KeyPair(kpg, kpg.generateKeyPair());

            kpg.initialize(keySize, new SecureRandom());
            test_KeyPair(kpg, kpg.genKeyPair());
            test_KeyPair(kpg, kpg.generateKeyPair());
        }
    }

    private void test_KeyPair(KeyPairGenerator kpg, KeyPair kp) throws Exception {
        assertNotNull(kp);
        test_Key(kpg, kp.getPrivate());
        test_Key(kpg, kp.getPublic());
    }

    private void test_Key(KeyPairGenerator kpg, Key k) throws Exception {
        String expectedAlgorithm = kpg.getAlgorithm().toUpperCase();
        if (StandardNames.IS_RI && expectedAlgorithm.equals("DIFFIEHELLMAN")) {
            expectedAlgorithm = "DH";
        }
        assertEquals(expectedAlgorithm, k.getAlgorithm().toUpperCase());
        assertNotNull(k.getEncoded());
        assertNotNull(k.getFormat());
    }
}
