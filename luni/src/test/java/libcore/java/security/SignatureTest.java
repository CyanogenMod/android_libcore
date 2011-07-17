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

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
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

    private static final byte[] PK_BYTES = hexToBytes(
            "30819f300d06092a864886f70d010101050003818d0030818902818100cd769d178f61475fce3001"
            + "2604218320c77a427121d3b41dd76756c8fc0c428cd15cb754adc85466f47547b1c85623d9c17fc6"
            + "4f202fca21099caf99460c824ad657caa8c2db34996838d32623c4f23c8b6a4e6698603901262619"
            + "4840e0896b1a6ec4f6652484aad04569bb6a885b822a10d700224359c632dc7324520cbb3d020301"
            + "0001");
    private static final byte[] CONTENT = hexToBytes(
            "f2fa9d73656e00fa01edc12e73656e2e7670632e6432004867268c46dd95030b93ce7260423e5c00"
            + "fabd4d656d6265727300fa018dc12e73656e2e7670632e643100d7c258dc00fabd44657669636573"
            + "00faa54b65797300fa02b5c12e4d2e4b009471968cc68835f8a68dde10f53d19693d480de767e5fb"
            + "976f3562324006372300fabdfd04e1f51ef3aa00fa8d00000001a203e202859471968cc68835f8a6"
            + "8dde10f53d19693d480de767e5fb976f356232400637230002bab504e1f51ef5810002c29d28463f"
            + "0003da8d000001e201eaf2fa9d73656e00fa01edc12e73656e2e7670632e6432004867268c46dd95"
            + "030b93ce7260423e5c00fabd4d656d6265727300fa018dc12e73656e2e7670632e643100d7c258dc"
            + "00fabd4465766963657300faa54b65797300fa02b5c12e4d2e4b009471968cc68835f8a68dde10f5"
            + "3d19693d480de767e5fb976f3562324006372300fabdfd04e1f51ef3aa000003e202859471968cc6"
            + "8835f8a68dde10f53d19693d480de767e5fb976f3562324006372300000000019a0a9530819f300d"
            + "06092a864886f70d010101050003818d0030818902818100cd769d178f61475fce30012604218320"
            + "c77a427121d3b41dd76756c8fc0c428cd15cb754adc85466f47547b1c85623d9c17fc64f202fca21"
            + "099caf99460c824ad657caa8c2db34996838d32623c4f23c8b6a4e66986039012626194840e0896b"
            + "1a6ec4f6652484aad04569bb6a885b822a10d700224359c632dc7324520cbb3d020301000100");
    private static final byte[] SIGNATURE = hexToBytes(
            "b4016456148cd2e9f580470aad63d19c1fee52b38c9dcb5b4d61a7ca369a7277497775d106d86394"
            + "a69229184333b5a3e6261d5bcebdb02530ca9909f4d790199eae7c140f7db39dee2232191bdf0bfb"
            + "34fdadc44326b9b3f3fa828652bab07f0362ac141c8c3784ebdec44e0b156a5e7bccdc81a56fe954"
            + "56ac8c0e4ae12d97");

    private static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                  + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    // http://code.google.com/p/android/issues/detail?id=18566
    // http://b/5038554
    public void test18566() throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(PK_BYTES);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey pk = keyFactory.generatePublic(keySpec);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pk);
        sig.update(CONTENT);
        assertTrue(sig.verify(SIGNATURE));
    }
}
