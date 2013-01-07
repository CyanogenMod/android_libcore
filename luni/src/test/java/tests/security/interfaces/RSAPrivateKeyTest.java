/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tests.security.interfaces;

import junit.framework.TestCase;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.Provider;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

public class RSAPrivateKeyTest extends TestCase {
    private static final BigInteger SIMPLE_N = BigInteger.valueOf(3233);

    private static final BigInteger SIMPLE_D = BigInteger.valueOf(2753);

    private static RSAPrivateKey getNonCRTKey(Provider p) throws Exception {
        KeyFactory gen = KeyFactory.getInstance("RSA", p);

        return (RSAPrivateKey) gen.generatePrivate(new RSAPrivateKeySpec(SIMPLE_N, SIMPLE_D));
    }

    /**
     * @see java.security.interfaces.RSAPrivateKey#getPrivateExponent()
     */
    public void test_getPrivateExponent() throws Exception {
        for (Provider p : Security.getProviders("KeyFactory.RSA")) {
            RSAPrivateKey key = getNonCRTKey(p);
            assertEquals("invalid private exponent", SIMPLE_D, key.getPrivateExponent());
        }
    }

    public void test_getModulus() throws Exception {
        for (Provider p : Security.getProviders("KeyFactory.RSA")) {
            RSAPrivateKey key = getNonCRTKey(p);
            assertEquals("invalid private exponent", SIMPLE_N, key.getModulus());
        }
    }

    public void test_getEncoded() throws Exception {
        for (Provider p : Security.getProviders("KeyFactory.RSA")) {
            RSAPrivateKey key = getNonCRTKey(p);
            byte[] encoded = key.getEncoded();

            KeyFactory gen = KeyFactory.getInstance("RSA", p);
            RSAPrivateKey key2 = (RSAPrivateKey) gen.generatePrivate(new PKCS8EncodedKeySpec(
                    encoded));
            assertEquals("invalid private exponent", SIMPLE_D, key2.getPrivateExponent());
            assertEquals("invalid modulus", SIMPLE_N, key2.getModulus());

            assertEquals(Arrays.toString(encoded), Arrays.toString(key2.getEncoded()));
        }
    }
}
