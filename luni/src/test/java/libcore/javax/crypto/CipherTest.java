/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.javax.crypto;

import com.android.org.bouncycastle.asn1.x509.KeyUsage;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import junit.framework.TestCase;
import libcore.java.security.StandardNames;
import libcore.java.security.TestKeyStore;

public final class CipherTest extends TestCase {

    private static boolean isUnsupported(String algorithm) {
        if (algorithm.equals("RC2")) {
            return true;
        }
        if (algorithm.equals("PBEWITHMD5ANDRC2")) {
            return true;
        }
        if (algorithm.startsWith("PBEWITHSHA1ANDRC2")) {
            return true;
        }
        if (algorithm.equals("PBEWITHSHAAND40BITRC2-CBC")) {
            return true;
        }
        if (algorithm.equals("PBEWITHSHAAND128BITRC2-CBC")) {
            return true;
        }
        if (algorithm.equals("PBEWITHSHAANDTWOFISH-CBC")) {
            return true;
        }
        return false;
    }

    private synchronized static int getEncryptMode(String algorithm) throws Exception {
        if (isWrap(algorithm)) {
            return Cipher.WRAP_MODE;
        }
        return Cipher.ENCRYPT_MODE;
    }

    private synchronized static int getDecryptMode(String algorithm) throws Exception {
        if (isWrap(algorithm)) {
            return Cipher.UNWRAP_MODE;
        }
        return Cipher.DECRYPT_MODE;
    }

    private static String getBaseAlgorithm(String algorithm) {
        if (algorithm.equals("AESWRAP")) {
            return "AES";
        }
        if (algorithm.startsWith("AES/")) {
            return "AES";
        }
        if (algorithm.equals("PBEWITHMD5AND128BITAES-CBC-OPENSSL")) {
            return "AES";
        }
        if (algorithm.equals("PBEWITHMD5AND192BITAES-CBC-OPENSSL")) {
            return "AES";
        }
        if (algorithm.equals("PBEWITHMD5AND256BITAES-CBC-OPENSSL")) {
            return "AES";
        }
        if (algorithm.equals("PBEWITHSHA256AND128BITAES-CBC-BC")) {
            return "AES";
        }
        if (algorithm.equals("PBEWITHSHA256AND192BITAES-CBC-BC")) {
            return "AES";
        }
        if (algorithm.equals("PBEWITHSHA256AND256BITAES-CBC-BC")) {
            return "AES";
        }
        if (algorithm.equals("PBEWITHSHAAND128BITAES-CBC-BC")) {
            return "AES";
        }
        if (algorithm.equals("PBEWITHSHAAND192BITAES-CBC-BC")) {
            return "AES";
        }
        if (algorithm.equals("PBEWITHSHAAND256BITAES-CBC-BC")) {
            return "AES";
        }
        if (algorithm.equals("PBEWITHMD5ANDDES")) {
            return "DES";
        }
        if (algorithm.equals("PBEWITHSHA1ANDDES")) {
            return "DES";
        }
        if (algorithm.equals("DESEDEWRAP")) {
            return "DESEDE";
        }
        if (algorithm.equals("PBEWITHSHAAND2-KEYTRIPLEDES-CBC")) {
            return "DESEDE";
        }
        if (algorithm.equals("PBEWITHSHAAND3-KEYTRIPLEDES-CBC")) {
            return "DESEDE";
        }
        if (algorithm.equals("PBEWITHMD5ANDTRIPLEDES")) {
            return "DESEDE";
        }
        if (algorithm.equals("PBEWITHSHA1ANDDESEDE")) {
            return "DESEDE";
        }
        if (algorithm.equals("RSA/ECB/NOPADDING")) {
            return "RSA";
        }
        if (algorithm.equals("RSA/ECB/PKCS1PADDING")) {
            return "RSA";
        }
        if (algorithm.equals("PBEWITHSHAAND40BITRC4")) {
            return "ARC4";
        }
        if (algorithm.equals("PBEWITHSHAAND128BITRC4")) {
            return "ARC4";
        }
        return algorithm;
    }

    private static boolean isAsymmetric(String algorithm) {
        return getBaseAlgorithm(algorithm).equals("RSA");
    }

    private static boolean isWrap(String algorithm) {
        return algorithm.endsWith("WRAP");
    }

    private static boolean isPBE(String algorithm) {
        return algorithm.startsWith("PBE");
    }

    private static Map<String, Key> ENCRYPT_KEYS = new HashMap<String, Key>();
    private synchronized static Key getEncryptKey(String algorithm) throws Exception {
        Key key = ENCRYPT_KEYS.get(algorithm);
        if (key != null) {
            return key;
        }
        if (algorithm.startsWith("RSA")) {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                                                              RSA_2048_privateExponent);
            key = kf.generatePrivate(keySpec);
        } else if (isPBE(algorithm)) {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
            key = skf.generateSecret(new PBEKeySpec("secret".toCharArray()));
        } else {
            KeyGenerator kg = KeyGenerator.getInstance(getBaseAlgorithm(algorithm));
            key = kg.generateKey();
        }
        ENCRYPT_KEYS.put(algorithm, key);
        return key;
    }

    private static Map<String, Key> DECRYPT_KEYS = new HashMap<String, Key>();
    private synchronized static Key getDecryptKey(String algorithm) throws Exception {
        Key key = DECRYPT_KEYS.get(algorithm);
        if (key != null) {
            return key;
        }
        if (algorithm.startsWith("RSA")) {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus,
                                                            RSA_2048_publicExponent);
            key = kf.generatePublic(keySpec);
        } else {
            assertFalse(algorithm, isAsymmetric(algorithm));
            key = getEncryptKey(algorithm);
        }
        DECRYPT_KEYS.put(algorithm, key);
        return key;
    }

    private static Map<String, Integer> EXPECTED_BLOCK_SIZE = new HashMap<String, Integer>();
    static {
        EXPECTED_BLOCK_SIZE.put("AES", 16);
        EXPECTED_BLOCK_SIZE.put("PBEWITHMD5AND128BITAES-CBC-OPENSSL", 16);
        EXPECTED_BLOCK_SIZE.put("PBEWITHMD5AND192BITAES-CBC-OPENSSL", 16);
        EXPECTED_BLOCK_SIZE.put("PBEWITHMD5AND256BITAES-CBC-OPENSSL", 16);
        EXPECTED_BLOCK_SIZE.put("PBEWITHSHA256AND128BITAES-CBC-BC", 16);
        EXPECTED_BLOCK_SIZE.put("PBEWITHSHA256AND192BITAES-CBC-BC", 16);
        EXPECTED_BLOCK_SIZE.put("PBEWITHSHA256AND256BITAES-CBC-BC", 16);
        EXPECTED_BLOCK_SIZE.put("PBEWITHSHAAND128BITAES-CBC-BC", 16);
        EXPECTED_BLOCK_SIZE.put("PBEWITHSHAAND192BITAES-CBC-BC", 16);
        EXPECTED_BLOCK_SIZE.put("PBEWITHSHAAND256BITAES-CBC-BC", 16);

        if (StandardNames.IS_RI) {
            EXPECTED_BLOCK_SIZE.put("AESWRAP", 16);
        } else {
            EXPECTED_BLOCK_SIZE.put("AESWRAP", 0);
        }

        EXPECTED_BLOCK_SIZE.put("ARC4", 0);
        EXPECTED_BLOCK_SIZE.put("ARCFOUR", 0);

        EXPECTED_BLOCK_SIZE.put("PBEWITHSHAAND40BITRC4", 0);
        EXPECTED_BLOCK_SIZE.put("PBEWITHSHAAND128BITRC4", 0);

        EXPECTED_BLOCK_SIZE.put("BLOWFISH", 8);

        EXPECTED_BLOCK_SIZE.put("DES", 8);
        EXPECTED_BLOCK_SIZE.put("PBEWITHMD5ANDDES", 8);
        EXPECTED_BLOCK_SIZE.put("PBEWITHMD5ANDTRIPLEDES", 8);
        EXPECTED_BLOCK_SIZE.put("PBEWITHSHA1ANDDES", 8);
        EXPECTED_BLOCK_SIZE.put("PBEWITHSHA1ANDDESEDE", 8);

        EXPECTED_BLOCK_SIZE.put("DESEDE", 8);
        EXPECTED_BLOCK_SIZE.put("PBEWITHSHAAND2-KEYTRIPLEDES-CBC", 8);
        EXPECTED_BLOCK_SIZE.put("PBEWITHSHAAND3-KEYTRIPLEDES-CBC", 8);

        if (StandardNames.IS_RI) {
            EXPECTED_BLOCK_SIZE.put("DESEDEWRAP", 8);
        } else {
            EXPECTED_BLOCK_SIZE.put("DESEDEWRAP", 0);
        }

        EXPECTED_BLOCK_SIZE.put("RSA", 0);
    }

    private static int getExpectedBlockSize(String algorithm, Key key) {
        final int firstSlash = algorithm.indexOf('/');
        if (firstSlash != -1) {
            algorithm = algorithm.substring(0, firstSlash);
        }

        if (!StandardNames.IS_RI && "RSA".equals(getBaseAlgorithm(algorithm))) {
            // Must be one less than the modulus size to make sure it fits.
            return getRSAModulusSize(key) - 1;
        }

        Integer expected = EXPECTED_BLOCK_SIZE.get(algorithm);
        assertNotNull(algorithm, expected);
        return expected;
    }

    private static int getRSAModulusSize(Key key) {
        if (key instanceof RSAPrivateKey) {
            RSAPrivateKey rsaKey = (RSAPrivateKey) key;
            return rsaKey.getModulus().bitLength() / 8;
        } else if (key instanceof RSAPublicKey) {
            RSAPublicKey rsaKey = (RSAPublicKey) key;
            return rsaKey.getModulus().bitLength() / 8;
        } else {
            throw new RuntimeException("Unknown RSA key type: " + key.getClass().getName());
        }
    }

    private static Map<String, Integer> EXPECTED_OUTPUT_SIZE = new HashMap<String, Integer>();
    static {
        EXPECTED_OUTPUT_SIZE.put("AES", 16);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHMD5AND128BITAES-CBC-OPENSSL", 16);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHMD5AND192BITAES-CBC-OPENSSL", 16);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHMD5AND256BITAES-CBC-OPENSSL", 16);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHSHA256AND128BITAES-CBC-BC", 16);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHSHA256AND192BITAES-CBC-BC", 16);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHSHA256AND256BITAES-CBC-BC", 16);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHSHAAND128BITAES-CBC-BC", 16);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHSHAAND192BITAES-CBC-BC", 16);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHSHAAND256BITAES-CBC-BC", 16);

        if (StandardNames.IS_RI) {
            EXPECTED_OUTPUT_SIZE.put("AESWRAP", 8);
        } else {
            EXPECTED_OUTPUT_SIZE.put("AESWRAP", -1);
        }

        EXPECTED_OUTPUT_SIZE.put("ARC4", 0);
        EXPECTED_OUTPUT_SIZE.put("ARCFOUR", 0);

        EXPECTED_OUTPUT_SIZE.put("PBEWITHSHAAND40BITRC4", 0);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHSHAAND128BITRC4", 0);

        EXPECTED_OUTPUT_SIZE.put("BLOWFISH", 8);

        EXPECTED_OUTPUT_SIZE.put("DES", 8);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHMD5ANDDES", 8);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHMD5ANDTRIPLEDES", 8);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHSHA1ANDDES", 8);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHSHA1ANDDESEDE", 8);

        EXPECTED_OUTPUT_SIZE.put("DESEDE", 8);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHSHAAND2-KEYTRIPLEDES-CBC", 8);
        EXPECTED_OUTPUT_SIZE.put("PBEWITHSHAAND3-KEYTRIPLEDES-CBC", 8);

        if (StandardNames.IS_RI) {
            EXPECTED_OUTPUT_SIZE.put("DESEDEWRAP", 16);
        } else {
            EXPECTED_OUTPUT_SIZE.put("DESEDEWRAP", -1);
        }
    }
    private static int getExpectedOutputSize(String algorithm, Key key) {
        final int firstSlash = algorithm.indexOf('/');
        if (firstSlash != -1) {
            algorithm = algorithm.substring(0, firstSlash);
        }

        // Output size for RSA depends on the key size being used.
        if ("RSA".equals(getBaseAlgorithm(algorithm))) {
            return getRSAModulusSize(key);
        }

        Integer expected = EXPECTED_OUTPUT_SIZE.get(algorithm);
        assertNotNull(algorithm, expected);
        return expected;
    }

    private static byte[] ORIGINAL_PLAIN_TEXT = new byte[] { 0x0a, 0x0b, 0x0c };
    private static byte[] PKCS1_BLOCK_TYPE_00_PADDED_PLAIN_TEXT = new byte[] {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x0a, 0x0b, 0x0c
    };
    private static byte[] PKCS1_BLOCK_TYPE_01_PADDED_PLAIN_TEXT = new byte[] {
        (byte) 0x00, (byte) 0x01, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x0a, (byte) 0x0b, (byte) 0x0c
    };
    private static byte[] PKCS1_BLOCK_TYPE_02_PADDED_PLAIN_TEXT = new byte[] {
        (byte) 0x00, (byte) 0x02, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x0a, (byte) 0x0b, (byte) 0x0c
    };

    private static byte[] getExpectedPlainText(String algorithm) {
        if (algorithm.equals("RSA/ECB/NOPADDING")) {
            return PKCS1_BLOCK_TYPE_00_PADDED_PLAIN_TEXT;
        }
        return ORIGINAL_PLAIN_TEXT;
    }

    public void test_getInstance() throws Exception {
        final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(errBuffer);

        Set<String> seenBaseCipherNames = new HashSet<String>();
        Set<String> seenCiphersWithModeAndPadding = new HashSet<String>();

        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            Set<Provider.Service> services = provider.getServices();
            for (Provider.Service service : services) {
                String type = service.getType();
                if (!type.equals("Cipher")) {
                    continue;
                }

                String algorithm = service.getAlgorithm();

                /*
                 * Any specific modes and paddings aren't tested directly here,
                 * but we need to make sure we see the bare algorithm from some
                 * provider. We will test each mode specifically when we get the
                 * base cipher.
                 */
                final int firstSlash = algorithm.indexOf('/');
                if (firstSlash == -1) {
                    seenBaseCipherNames.add(algorithm);
                } else {
                    final String baseCipherName = algorithm.substring(0, firstSlash);
                    if (!seenBaseCipherNames.contains(baseCipherName)) {
                        seenCiphersWithModeAndPadding.add(baseCipherName);
                    }
                    continue;
                }

                try {
                    test_Cipher_Algorithm(provider, algorithm);
                } catch (Throwable e) {
                    out.append("Error encountered checking " + algorithm + "\n");
                    e.printStackTrace(out);
                }

                Set<String> modes = StandardNames.getModesForCipher(algorithm);
                if (modes != null) {
                    for (String mode : modes) {
                        Set<String> paddings = StandardNames.getPaddingsForCipher(algorithm);
                        if (paddings != null) {
                            for (String padding : paddings) {
                                final String algorithmName = algorithm + "/" + mode + "/" + padding;
                                try {
                                    test_Cipher_Algorithm(provider, algorithmName);
                                } catch (Throwable e) {
                                    out.append("Error encountered checking " + algorithmName + "\n");
                                    e.printStackTrace(out);
                                }
                            }
                        }
                    }
                }
            }
        }

        seenCiphersWithModeAndPadding.removeAll(seenBaseCipherNames);
        assertEquals("Ciphers seen with mode and padding but not base cipher",
                Collections.EMPTY_SET, seenCiphersWithModeAndPadding);

        out.flush();
        if (errBuffer.size() > 0) {
            throw new Exception("Errors encountered:\n\n" + errBuffer.toString() + "\n\n");
        }
    }

    private void test_Cipher_Algorithm(Provider provider, String algorithm) throws Exception {
        // Cipher.getInstance(String)
        Cipher c1 = Cipher.getInstance(algorithm);
        assertEquals(algorithm, c1.getAlgorithm());
        test_Cipher(c1);

        // Cipher.getInstance(String, Provider)
        Cipher c2 = Cipher.getInstance(algorithm, provider);
        assertEquals(algorithm, c2.getAlgorithm());
        assertEquals(provider, c2.getProvider());
        test_Cipher(c2);

        // KeyGenerator.getInstance(String, String)
        Cipher c3 = Cipher.getInstance(algorithm, provider.getName());
        assertEquals(algorithm, c3.getAlgorithm());
        assertEquals(provider, c3.getProvider());
        test_Cipher(c3);
    }

    private void test_Cipher(Cipher c) throws Exception {
        String algorithm = c.getAlgorithm().toUpperCase(Locale.US);
        if (isUnsupported(algorithm)) {
            return;
        }

        try {
            c.getOutputSize(0);
        } catch (IllegalStateException expected) {
        }

        // TODO: test keys from different factories (e.g. OpenSSLRSAPrivateKey vs JCERSAPrivateKey)
        Key encryptKey = getEncryptKey(algorithm);
        final AlgorithmParameterSpec spec;
        if (isPBE(algorithm)) {
            final byte[] salt = new byte[8];
            new SecureRandom().nextBytes(salt);
            spec = new PBEParameterSpec(salt, 1024);
        } else {
            spec = null;
        }
        c.init(getEncryptMode(algorithm), encryptKey, spec);

        assertEquals("getBlockSize()", getExpectedBlockSize(algorithm, encryptKey),
                c.getBlockSize());

        assertEquals("getOutputSize(0)", getExpectedOutputSize(algorithm, encryptKey),
                c.getOutputSize(0));

        // TODO: test Cipher.getIV()

        // TODO: test Cipher.getParameters()

        assertNull(c.getExemptionMechanism());

        if (isWrap(algorithm)) {
            byte[] cipherText = c.wrap(encryptKey);
            c.init(getDecryptMode(algorithm), getDecryptKey(algorithm));
            int keyType = (isAsymmetric(algorithm)) ? Cipher.PRIVATE_KEY : Cipher.SECRET_KEY;
            Key decryptedKey = c.unwrap(cipherText, encryptKey.getAlgorithm(), keyType);
            assertEquals("encryptKey.getAlgorithm()=" + encryptKey.getAlgorithm()
                         + " decryptedKey.getAlgorithm()=" + decryptedKey.getAlgorithm()
                         + " encryptKey.getEncoded()=" + Arrays.toString(encryptKey.getEncoded())
                         + " decryptedKey.getEncoded()=" + Arrays.toString(decryptedKey.getEncoded()),
                         encryptKey, decryptedKey);
        } else {
            byte[] cipherText = c.doFinal(ORIGINAL_PLAIN_TEXT);
            c.init(getDecryptMode(algorithm), getDecryptKey(algorithm), spec);
            byte[] decryptedPlainText = c.doFinal(cipherText);
            assertEquals(Arrays.toString(getExpectedPlainText(algorithm)),
                         Arrays.toString(decryptedPlainText));
        }
    }

    public void testInputPKCS1Padding() throws Exception {
        testInputPKCS1Padding(PKCS1_BLOCK_TYPE_01_PADDED_PLAIN_TEXT, getEncryptKey("RSA"), getDecryptKey("RSA"));
        try {
            testInputPKCS1Padding(PKCS1_BLOCK_TYPE_02_PADDED_PLAIN_TEXT, getEncryptKey("RSA"), getDecryptKey("RSA"));
            fail();
        } catch (BadPaddingException expected) {
        }
        try {
            testInputPKCS1Padding(PKCS1_BLOCK_TYPE_01_PADDED_PLAIN_TEXT, getDecryptKey("RSA"), getEncryptKey("RSA"));
            fail();
        } catch (BadPaddingException expected) {
        }
        testInputPKCS1Padding(PKCS1_BLOCK_TYPE_02_PADDED_PLAIN_TEXT, getDecryptKey("RSA"), getEncryptKey("RSA"));
    }

    private void testInputPKCS1Padding(byte[] prePaddedPlainText, Key encryptKey, Key decryptKey) throws Exception {
        Cipher encryptCipher = Cipher.getInstance("RSA/ECB/NoPadding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, encryptKey);
        byte[] cipherText = encryptCipher.doFinal(prePaddedPlainText);
        Cipher decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decryptCipher.init(Cipher.DECRYPT_MODE, decryptKey);
        byte[] plainText = decryptCipher.doFinal(cipherText);
        assertEquals(Arrays.toString(ORIGINAL_PLAIN_TEXT),
                     Arrays.toString(plainText));
    }

    public void testOutputPKCS1Padding() throws Exception {
       testOutputPKCS1Padding((byte) 1, getEncryptKey("RSA"), getDecryptKey("RSA"));
       testOutputPKCS1Padding((byte) 2, getDecryptKey("RSA"), getEncryptKey("RSA"));
    }

    private void testOutputPKCS1Padding(byte expectedBlockType, Key encryptKey, Key decryptKey) throws Exception {
        Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, encryptKey);
        byte[] cipherText = encryptCipher.doFinal(ORIGINAL_PLAIN_TEXT);
        Cipher decryptCipher = Cipher.getInstance("RSA/ECB/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, decryptKey);
        byte[] plainText = decryptCipher.doFinal(cipherText);
        assertPadding(encryptKey, expectedBlockType, ORIGINAL_PLAIN_TEXT, plainText);
    }

    private void assertPadding(Key key, byte expectedBlockType, byte[] expectedData,
            byte[] actualDataWithPadding) {
        assertNotNull(actualDataWithPadding);
        assertEquals(getExpectedOutputSize("RSA", key), actualDataWithPadding.length);
        assertEquals(0, actualDataWithPadding[0]);
        byte actualBlockType = actualDataWithPadding[1];
        assertEquals(expectedBlockType, actualBlockType);
        int actualDataOffset = actualDataWithPadding.length - expectedData.length;
        if (actualBlockType == 1) {
            for (int i = 2; i < actualDataOffset - 1; i++) {
                assertEquals((byte) 0xFF, actualDataWithPadding[i]);
            }
        }
        assertEquals(0x00, actualDataWithPadding[actualDataOffset-1]);
        byte[] actualData = new byte[expectedData.length];
        System.arraycopy(actualDataWithPadding, actualDataOffset, actualData, 0, actualData.length);
        assertEquals(Arrays.toString(expectedData), Arrays.toString(actualData));
    }

    public void testCipherInitWithCertificate () throws Exception {
        // no key usage specified, everything is fine
        assertCipherInitWithKeyUsage(0,                         true,  true, true,  true);

        // common case is that encrypt/wrap is prohibited when special usage is specified
        assertCipherInitWithKeyUsage(KeyUsage.digitalSignature, false, true, false, true);
        assertCipherInitWithKeyUsage(KeyUsage.nonRepudiation,   false, true, false, true);
        assertCipherInitWithKeyUsage(KeyUsage.keyAgreement,     false, true, false, true);
        assertCipherInitWithKeyUsage(KeyUsage.keyCertSign,      false, true, false, true);
        assertCipherInitWithKeyUsage(KeyUsage.cRLSign,          false, true, false, true);

        // Note they encipherOnly/decipherOnly don't have to do with
        // ENCRYPT_MODE or DECRYPT_MODE, but restrict usage relative
        // to keyAgreement. There is not a *_MODE option that
        // corresponds to this in Cipher, the RI does not enforce
        // anything in Cipher.
        // http://code.google.com/p/android/issues/detail?id=12955
        assertCipherInitWithKeyUsage(KeyUsage.encipherOnly,     false, true, false, true);
        assertCipherInitWithKeyUsage(KeyUsage.decipherOnly,     false, true, false, true);
        assertCipherInitWithKeyUsage(KeyUsage.keyAgreement | KeyUsage.encipherOnly,
                                                                false, true, false, true);
        assertCipherInitWithKeyUsage(KeyUsage.keyAgreement | KeyUsage.decipherOnly,
                                                                false, true, false, true);

        // except when wrapping a key is specifically allowed or
        assertCipherInitWithKeyUsage(KeyUsage.keyEncipherment,  false, true, true,  true);
        // except when wrapping data encryption is specifically allowed
        assertCipherInitWithKeyUsage(KeyUsage.dataEncipherment, true,  true, false, true);
    }

    private void assertCipherInitWithKeyUsage (int keyUsage,
                                               boolean allowEncrypt,
                                               boolean allowDecrypt,
                                               boolean allowWrap,
                                               boolean allowUnwrap) throws Exception {
        Certificate certificate = certificateWithKeyUsage(keyUsage);
        assertCipherInitWithKeyUsage(certificate, allowEncrypt, Cipher.ENCRYPT_MODE);
        assertCipherInitWithKeyUsage(certificate, allowDecrypt, Cipher.DECRYPT_MODE);
        assertCipherInitWithKeyUsage(certificate, allowWrap,    Cipher.WRAP_MODE);
        assertCipherInitWithKeyUsage(certificate, allowUnwrap,  Cipher.UNWRAP_MODE);
    }

    private void assertCipherInitWithKeyUsage(Certificate certificate,
                                              boolean allowMode,
                                              int mode) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        if (allowMode) {
            cipher.init(mode, certificate);
        } else {
            try {
                cipher.init(mode, certificate);
                String modeString;
                switch (mode) {
                    case Cipher.ENCRYPT_MODE:
                        modeString = "ENCRYPT_MODE";
                        break;
                    case Cipher.DECRYPT_MODE:
                        modeString = "DECRYPT_MODE";
                        break;
                    case Cipher.WRAP_MODE:
                        modeString = "WRAP_MODE";
                        break;
                    case Cipher.UNWRAP_MODE:
                        modeString = "UNWRAP_MODE";
                        break;
                    default:
                        throw new AssertionError("Unknown Cipher.*_MODE " + mode);
                }
                fail("Should have had InvalidKeyException for " + modeString
                     + " for " + certificate);
            } catch (InvalidKeyException expected) {
            }
        }
    }

    private Certificate certificateWithKeyUsage(int keyUsage) throws Exception {
        // note the rare usage of non-zero keyUsage
        return new TestKeyStore.Builder()
                .aliasPrefix("rsa-dsa-ec")
                .keyUsage(keyUsage)
                .build()
                .getPrivateKey("RSA", "RSA").getCertificate();
    }

    /*
     * Test vectors generated with this private key:
     *
     * -----BEGIN RSA PRIVATE KEY-----
     * MIIEpAIBAAKCAQEA4Ec+irjyKE/rnnQv+XSPoRjtmGM8kvUq63ouvg075gMpvnZq
     * 0Q62pRXQ0s/ZvqeTDwwwZTeJn3lYzT6FsB+IGFJNMSWEqUslHjYltUFB7b/uGYgI
     * 4buX/Hy0m56qr2jpyY19DtxTu8D6ADQ1bWMF+7zDxwAUBThqu8hzyw8+90JfPTPf
     * ezFa4DbSoLZq/UdQOxab8247UWJRW3Ff2oPeryxYrrmr+zCXw8yd2dvl7ylsF2E5
     * Ao6KZx5jBW1F9AGI0sQTNJCEXeUsJTTpxrJHjAe9rpKII7YtBmx3cPn2Pz26JH9T
     * CER0e+eqqF2FO4vSRKzsPePImrRkU6tNJMOsaQIDAQABAoIBADd4R3al8XaY9ayW
     * DfuDobZ1ZOZIvQWXz4q4CHGG8macJ6nsvdSA8Bl6gNBzCebGqW+SUzHlf4tKxvTU
     * XtpFojJpwJ/EKMB6Tm7fc4oV3sl/q9Lyu0ehTyDqcvz+TDbgGtp3vRN82NTaELsW
     * LpSkZilx8XX5hfoYjwVsuX7igW9Dq503R2Ekhs2owWGWwwgYqZXshdOEZ3kSZ7O/
     * IfJzcQppJYYldoQcW2cSwS1L0govMpmtt8E12l6VFavadufK8qO+gFUdBzt4vxFi
     * xIrSt/R0OgI47k0lL31efmUzzK5kzLOTYAdaL9HgNOw65c6cQIzL8OJeQRQCFoez
     * 3UdUroECgYEA9UGIS8Nzeyki1BGe9F4t7izUy7dfRVBaFXqlAJ+Zxzot8HJKxGAk
     * MGMy6omBd2NFRl3G3x4KbxQK/ztzluaomUrF2qloc0cv43dJ0U6z4HXmKdvrNYMz
     * im82SdCiZUp6Qv2atr+krE1IHTkLsimwZL3DEcwb4bYxidp8QM3s8rECgYEA6hp0
     * LduIHO23KIyH442GjdekCdFaQ/RF1Td6C1cx3b/KLa8oqOE81cCvzsM0fXSjniNa
     * PNljPydN4rlPkt9DgzkR2enxz1jyfeLgj/RZZMcg0+whOdx8r8kSlTzeyy81Wi4s
     * NaUPrXVMs7IxZkJLo7bjESoriYw4xcFe2yOGkzkCgYBRgo8exv2ZYCmQG68dfjN7
     * pfCvJ+mE6tiVrOYr199O5FoiQInyzBUa880XP84EdLywTzhqLNzA4ANrokGfVFeS
     * YtRxAL6TGYSj76Bb7PFBV03AebOpXEqD5sQ/MhTW3zLVEt4ZgIXlMeYWuD/X3Z0f
     * TiYHwzM9B8VdEH0dOJNYcQKBgQDbT7UPUN6O21P/NMgJMYigUShn2izKBIl3WeWH
     * wkQBDa+GZNWegIPRbBZHiTAfZ6nweAYNg0oq29NnV1toqKhCwrAqibPzH8zsiiL+
     * OVeVxcbHQitOXXSh6ajzDndZufwtY5wfFWc+hOk6XvFQb0MVODw41Fy9GxQEj0ch
     * 3IIyYQKBgQDYEUWTr0FfthLb8ZI3ENVNB0hiBadqO0MZSWjA3/HxHvD2GkozfV/T
     * dBu8lkDkR7i2tsR8OsEgQ1fTsMVbqShr2nP2KSlvX6kUbYl2NX08dR51FIaWpAt0
     * aFyCzjCQLWOdck/yTV4ulAfuNO3tLjtN9lqpvP623yjQe6aQPxZXaA==
     * -----END RSA PRIVATE KEY-----
     *
     */

    private static final BigInteger RSA_2048_modulus = new BigInteger(new byte[] {
        (byte) 0x00, (byte) 0xe0, (byte) 0x47, (byte) 0x3e, (byte) 0x8a, (byte) 0xb8, (byte) 0xf2, (byte) 0x28,
        (byte) 0x4f, (byte) 0xeb, (byte) 0x9e, (byte) 0x74, (byte) 0x2f, (byte) 0xf9, (byte) 0x74, (byte) 0x8f,
        (byte) 0xa1, (byte) 0x18, (byte) 0xed, (byte) 0x98, (byte) 0x63, (byte) 0x3c, (byte) 0x92, (byte) 0xf5,
        (byte) 0x2a, (byte) 0xeb, (byte) 0x7a, (byte) 0x2e, (byte) 0xbe, (byte) 0x0d, (byte) 0x3b, (byte) 0xe6,
        (byte) 0x03, (byte) 0x29, (byte) 0xbe, (byte) 0x76, (byte) 0x6a, (byte) 0xd1, (byte) 0x0e, (byte) 0xb6,
        (byte) 0xa5, (byte) 0x15, (byte) 0xd0, (byte) 0xd2, (byte) 0xcf, (byte) 0xd9, (byte) 0xbe, (byte) 0xa7,
        (byte) 0x93, (byte) 0x0f, (byte) 0x0c, (byte) 0x30, (byte) 0x65, (byte) 0x37, (byte) 0x89, (byte) 0x9f,
        (byte) 0x79, (byte) 0x58, (byte) 0xcd, (byte) 0x3e, (byte) 0x85, (byte) 0xb0, (byte) 0x1f, (byte) 0x88,
        (byte) 0x18, (byte) 0x52, (byte) 0x4d, (byte) 0x31, (byte) 0x25, (byte) 0x84, (byte) 0xa9, (byte) 0x4b,
        (byte) 0x25, (byte) 0x1e, (byte) 0x36, (byte) 0x25, (byte) 0xb5, (byte) 0x41, (byte) 0x41, (byte) 0xed,
        (byte) 0xbf, (byte) 0xee, (byte) 0x19, (byte) 0x88, (byte) 0x08, (byte) 0xe1, (byte) 0xbb, (byte) 0x97,
        (byte) 0xfc, (byte) 0x7c, (byte) 0xb4, (byte) 0x9b, (byte) 0x9e, (byte) 0xaa, (byte) 0xaf, (byte) 0x68,
        (byte) 0xe9, (byte) 0xc9, (byte) 0x8d, (byte) 0x7d, (byte) 0x0e, (byte) 0xdc, (byte) 0x53, (byte) 0xbb,
        (byte) 0xc0, (byte) 0xfa, (byte) 0x00, (byte) 0x34, (byte) 0x35, (byte) 0x6d, (byte) 0x63, (byte) 0x05,
        (byte) 0xfb, (byte) 0xbc, (byte) 0xc3, (byte) 0xc7, (byte) 0x00, (byte) 0x14, (byte) 0x05, (byte) 0x38,
        (byte) 0x6a, (byte) 0xbb, (byte) 0xc8, (byte) 0x73, (byte) 0xcb, (byte) 0x0f, (byte) 0x3e, (byte) 0xf7,
        (byte) 0x42, (byte) 0x5f, (byte) 0x3d, (byte) 0x33, (byte) 0xdf, (byte) 0x7b, (byte) 0x31, (byte) 0x5a,
        (byte) 0xe0, (byte) 0x36, (byte) 0xd2, (byte) 0xa0, (byte) 0xb6, (byte) 0x6a, (byte) 0xfd, (byte) 0x47,
        (byte) 0x50, (byte) 0x3b, (byte) 0x16, (byte) 0x9b, (byte) 0xf3, (byte) 0x6e, (byte) 0x3b, (byte) 0x51,
        (byte) 0x62, (byte) 0x51, (byte) 0x5b, (byte) 0x71, (byte) 0x5f, (byte) 0xda, (byte) 0x83, (byte) 0xde,
        (byte) 0xaf, (byte) 0x2c, (byte) 0x58, (byte) 0xae, (byte) 0xb9, (byte) 0xab, (byte) 0xfb, (byte) 0x30,
        (byte) 0x97, (byte) 0xc3, (byte) 0xcc, (byte) 0x9d, (byte) 0xd9, (byte) 0xdb, (byte) 0xe5, (byte) 0xef,
        (byte) 0x29, (byte) 0x6c, (byte) 0x17, (byte) 0x61, (byte) 0x39, (byte) 0x02, (byte) 0x8e, (byte) 0x8a,
        (byte) 0x67, (byte) 0x1e, (byte) 0x63, (byte) 0x05, (byte) 0x6d, (byte) 0x45, (byte) 0xf4, (byte) 0x01,
        (byte) 0x88, (byte) 0xd2, (byte) 0xc4, (byte) 0x13, (byte) 0x34, (byte) 0x90, (byte) 0x84, (byte) 0x5d,
        (byte) 0xe5, (byte) 0x2c, (byte) 0x25, (byte) 0x34, (byte) 0xe9, (byte) 0xc6, (byte) 0xb2, (byte) 0x47,
        (byte) 0x8c, (byte) 0x07, (byte) 0xbd, (byte) 0xae, (byte) 0x92, (byte) 0x88, (byte) 0x23, (byte) 0xb6,
        (byte) 0x2d, (byte) 0x06, (byte) 0x6c, (byte) 0x77, (byte) 0x70, (byte) 0xf9, (byte) 0xf6, (byte) 0x3f,
        (byte) 0x3d, (byte) 0xba, (byte) 0x24, (byte) 0x7f, (byte) 0x53, (byte) 0x08, (byte) 0x44, (byte) 0x74,
        (byte) 0x7b, (byte) 0xe7, (byte) 0xaa, (byte) 0xa8, (byte) 0x5d, (byte) 0x85, (byte) 0x3b, (byte) 0x8b,
        (byte) 0xd2, (byte) 0x44, (byte) 0xac, (byte) 0xec, (byte) 0x3d, (byte) 0xe3, (byte) 0xc8, (byte) 0x9a,
        (byte) 0xb4, (byte) 0x64, (byte) 0x53, (byte) 0xab, (byte) 0x4d, (byte) 0x24, (byte) 0xc3, (byte) 0xac,
        (byte) 0x69,
    });

    private static final BigInteger RSA_2048_privateExponent = new BigInteger(new byte[] {
        (byte) 0x37, (byte) 0x78, (byte) 0x47, (byte) 0x76, (byte) 0xa5, (byte) 0xf1, (byte) 0x76, (byte) 0x98,
        (byte) 0xf5, (byte) 0xac, (byte) 0x96, (byte) 0x0d, (byte) 0xfb, (byte) 0x83, (byte) 0xa1, (byte) 0xb6,
        (byte) 0x75, (byte) 0x64, (byte) 0xe6, (byte) 0x48, (byte) 0xbd, (byte) 0x05, (byte) 0x97, (byte) 0xcf,
        (byte) 0x8a, (byte) 0xb8, (byte) 0x08, (byte) 0x71, (byte) 0x86, (byte) 0xf2, (byte) 0x66, (byte) 0x9c,
        (byte) 0x27, (byte) 0xa9, (byte) 0xec, (byte) 0xbd, (byte) 0xd4, (byte) 0x80, (byte) 0xf0, (byte) 0x19,
        (byte) 0x7a, (byte) 0x80, (byte) 0xd0, (byte) 0x73, (byte) 0x09, (byte) 0xe6, (byte) 0xc6, (byte) 0xa9,
        (byte) 0x6f, (byte) 0x92, (byte) 0x53, (byte) 0x31, (byte) 0xe5, (byte) 0x7f, (byte) 0x8b, (byte) 0x4a,
        (byte) 0xc6, (byte) 0xf4, (byte) 0xd4, (byte) 0x5e, (byte) 0xda, (byte) 0x45, (byte) 0xa2, (byte) 0x32,
        (byte) 0x69, (byte) 0xc0, (byte) 0x9f, (byte) 0xc4, (byte) 0x28, (byte) 0xc0, (byte) 0x7a, (byte) 0x4e,
        (byte) 0x6e, (byte) 0xdf, (byte) 0x73, (byte) 0x8a, (byte) 0x15, (byte) 0xde, (byte) 0xc9, (byte) 0x7f,
        (byte) 0xab, (byte) 0xd2, (byte) 0xf2, (byte) 0xbb, (byte) 0x47, (byte) 0xa1, (byte) 0x4f, (byte) 0x20,
        (byte) 0xea, (byte) 0x72, (byte) 0xfc, (byte) 0xfe, (byte) 0x4c, (byte) 0x36, (byte) 0xe0, (byte) 0x1a,
        (byte) 0xda, (byte) 0x77, (byte) 0xbd, (byte) 0x13, (byte) 0x7c, (byte) 0xd8, (byte) 0xd4, (byte) 0xda,
        (byte) 0x10, (byte) 0xbb, (byte) 0x16, (byte) 0x2e, (byte) 0x94, (byte) 0xa4, (byte) 0x66, (byte) 0x29,
        (byte) 0x71, (byte) 0xf1, (byte) 0x75, (byte) 0xf9, (byte) 0x85, (byte) 0xfa, (byte) 0x18, (byte) 0x8f,
        (byte) 0x05, (byte) 0x6c, (byte) 0xb9, (byte) 0x7e, (byte) 0xe2, (byte) 0x81, (byte) 0x6f, (byte) 0x43,
        (byte) 0xab, (byte) 0x9d, (byte) 0x37, (byte) 0x47, (byte) 0x61, (byte) 0x24, (byte) 0x86, (byte) 0xcd,
        (byte) 0xa8, (byte) 0xc1, (byte) 0x61, (byte) 0x96, (byte) 0xc3, (byte) 0x08, (byte) 0x18, (byte) 0xa9,
        (byte) 0x95, (byte) 0xec, (byte) 0x85, (byte) 0xd3, (byte) 0x84, (byte) 0x67, (byte) 0x79, (byte) 0x12,
        (byte) 0x67, (byte) 0xb3, (byte) 0xbf, (byte) 0x21, (byte) 0xf2, (byte) 0x73, (byte) 0x71, (byte) 0x0a,
        (byte) 0x69, (byte) 0x25, (byte) 0x86, (byte) 0x25, (byte) 0x76, (byte) 0x84, (byte) 0x1c, (byte) 0x5b,
        (byte) 0x67, (byte) 0x12, (byte) 0xc1, (byte) 0x2d, (byte) 0x4b, (byte) 0xd2, (byte) 0x0a, (byte) 0x2f,
        (byte) 0x32, (byte) 0x99, (byte) 0xad, (byte) 0xb7, (byte) 0xc1, (byte) 0x35, (byte) 0xda, (byte) 0x5e,
        (byte) 0x95, (byte) 0x15, (byte) 0xab, (byte) 0xda, (byte) 0x76, (byte) 0xe7, (byte) 0xca, (byte) 0xf2,
        (byte) 0xa3, (byte) 0xbe, (byte) 0x80, (byte) 0x55, (byte) 0x1d, (byte) 0x07, (byte) 0x3b, (byte) 0x78,
        (byte) 0xbf, (byte) 0x11, (byte) 0x62, (byte) 0xc4, (byte) 0x8a, (byte) 0xd2, (byte) 0xb7, (byte) 0xf4,
        (byte) 0x74, (byte) 0x3a, (byte) 0x02, (byte) 0x38, (byte) 0xee, (byte) 0x4d, (byte) 0x25, (byte) 0x2f,
        (byte) 0x7d, (byte) 0x5e, (byte) 0x7e, (byte) 0x65, (byte) 0x33, (byte) 0xcc, (byte) 0xae, (byte) 0x64,
        (byte) 0xcc, (byte) 0xb3, (byte) 0x93, (byte) 0x60, (byte) 0x07, (byte) 0x5a, (byte) 0x2f, (byte) 0xd1,
        (byte) 0xe0, (byte) 0x34, (byte) 0xec, (byte) 0x3a, (byte) 0xe5, (byte) 0xce, (byte) 0x9c, (byte) 0x40,
        (byte) 0x8c, (byte) 0xcb, (byte) 0xf0, (byte) 0xe2, (byte) 0x5e, (byte) 0x41, (byte) 0x14, (byte) 0x02,
        (byte) 0x16, (byte) 0x87, (byte) 0xb3, (byte) 0xdd, (byte) 0x47, (byte) 0x54, (byte) 0xae, (byte) 0x81,
    });

    private static final BigInteger RSA_2048_publicExponent = new BigInteger(new byte[] {
        (byte) 0x01, (byte) 0x00, (byte) 0x01,
    });

    private static final BigInteger RSA_2048_primeP = new BigInteger(new byte[] {
        (byte) 0x00, (byte) 0xf5, (byte) 0x41, (byte) 0x88, (byte) 0x4b, (byte) 0xc3, (byte) 0x73, (byte) 0x7b,
        (byte) 0x29, (byte) 0x22, (byte) 0xd4, (byte) 0x11, (byte) 0x9e, (byte) 0xf4, (byte) 0x5e, (byte) 0x2d,
        (byte) 0xee, (byte) 0x2c, (byte) 0xd4, (byte) 0xcb, (byte) 0xb7, (byte) 0x5f, (byte) 0x45, (byte) 0x50,
        (byte) 0x5a, (byte) 0x15, (byte) 0x7a, (byte) 0xa5, (byte) 0x00, (byte) 0x9f, (byte) 0x99, (byte) 0xc7,
        (byte) 0x3a, (byte) 0x2d, (byte) 0xf0, (byte) 0x72, (byte) 0x4a, (byte) 0xc4, (byte) 0x60, (byte) 0x24,
        (byte) 0x30, (byte) 0x63, (byte) 0x32, (byte) 0xea, (byte) 0x89, (byte) 0x81, (byte) 0x77, (byte) 0x63,
        (byte) 0x45, (byte) 0x46, (byte) 0x5d, (byte) 0xc6, (byte) 0xdf, (byte) 0x1e, (byte) 0x0a, (byte) 0x6f,
        (byte) 0x14, (byte) 0x0a, (byte) 0xff, (byte) 0x3b, (byte) 0x73, (byte) 0x96, (byte) 0xe6, (byte) 0xa8,
        (byte) 0x99, (byte) 0x4a, (byte) 0xc5, (byte) 0xda, (byte) 0xa9, (byte) 0x68, (byte) 0x73, (byte) 0x47,
        (byte) 0x2f, (byte) 0xe3, (byte) 0x77, (byte) 0x49, (byte) 0xd1, (byte) 0x4e, (byte) 0xb3, (byte) 0xe0,
        (byte) 0x75, (byte) 0xe6, (byte) 0x29, (byte) 0xdb, (byte) 0xeb, (byte) 0x35, (byte) 0x83, (byte) 0x33,
        (byte) 0x8a, (byte) 0x6f, (byte) 0x36, (byte) 0x49, (byte) 0xd0, (byte) 0xa2, (byte) 0x65, (byte) 0x4a,
        (byte) 0x7a, (byte) 0x42, (byte) 0xfd, (byte) 0x9a, (byte) 0xb6, (byte) 0xbf, (byte) 0xa4, (byte) 0xac,
        (byte) 0x4d, (byte) 0x48, (byte) 0x1d, (byte) 0x39, (byte) 0x0b, (byte) 0xb2, (byte) 0x29, (byte) 0xb0,
        (byte) 0x64, (byte) 0xbd, (byte) 0xc3, (byte) 0x11, (byte) 0xcc, (byte) 0x1b, (byte) 0xe1, (byte) 0xb6,
        (byte) 0x31, (byte) 0x89, (byte) 0xda, (byte) 0x7c, (byte) 0x40, (byte) 0xcd, (byte) 0xec, (byte) 0xf2,
        (byte) 0xb1,
    });

    private static final BigInteger RSA_2048_primeQ = new BigInteger(new byte[] {
        (byte) 0x00, (byte) 0xea, (byte) 0x1a, (byte) 0x74, (byte) 0x2d, (byte) 0xdb, (byte) 0x88, (byte) 0x1c,
        (byte) 0xed, (byte) 0xb7, (byte) 0x28, (byte) 0x8c, (byte) 0x87, (byte) 0xe3, (byte) 0x8d, (byte) 0x86,
        (byte) 0x8d, (byte) 0xd7, (byte) 0xa4, (byte) 0x09, (byte) 0xd1, (byte) 0x5a, (byte) 0x43, (byte) 0xf4,
        (byte) 0x45, (byte) 0xd5, (byte) 0x37, (byte) 0x7a, (byte) 0x0b, (byte) 0x57, (byte) 0x31, (byte) 0xdd,
        (byte) 0xbf, (byte) 0xca, (byte) 0x2d, (byte) 0xaf, (byte) 0x28, (byte) 0xa8, (byte) 0xe1, (byte) 0x3c,
        (byte) 0xd5, (byte) 0xc0, (byte) 0xaf, (byte) 0xce, (byte) 0xc3, (byte) 0x34, (byte) 0x7d, (byte) 0x74,
        (byte) 0xa3, (byte) 0x9e, (byte) 0x23, (byte) 0x5a, (byte) 0x3c, (byte) 0xd9, (byte) 0x63, (byte) 0x3f,
        (byte) 0x27, (byte) 0x4d, (byte) 0xe2, (byte) 0xb9, (byte) 0x4f, (byte) 0x92, (byte) 0xdf, (byte) 0x43,
        (byte) 0x83, (byte) 0x39, (byte) 0x11, (byte) 0xd9, (byte) 0xe9, (byte) 0xf1, (byte) 0xcf, (byte) 0x58,
        (byte) 0xf2, (byte) 0x7d, (byte) 0xe2, (byte) 0xe0, (byte) 0x8f, (byte) 0xf4, (byte) 0x59, (byte) 0x64,
        (byte) 0xc7, (byte) 0x20, (byte) 0xd3, (byte) 0xec, (byte) 0x21, (byte) 0x39, (byte) 0xdc, (byte) 0x7c,
        (byte) 0xaf, (byte) 0xc9, (byte) 0x12, (byte) 0x95, (byte) 0x3c, (byte) 0xde, (byte) 0xcb, (byte) 0x2f,
        (byte) 0x35, (byte) 0x5a, (byte) 0x2e, (byte) 0x2c, (byte) 0x35, (byte) 0xa5, (byte) 0x0f, (byte) 0xad,
        (byte) 0x75, (byte) 0x4c, (byte) 0xb3, (byte) 0xb2, (byte) 0x31, (byte) 0x66, (byte) 0x42, (byte) 0x4b,
        (byte) 0xa3, (byte) 0xb6, (byte) 0xe3, (byte) 0x11, (byte) 0x2a, (byte) 0x2b, (byte) 0x89, (byte) 0x8c,
        (byte) 0x38, (byte) 0xc5, (byte) 0xc1, (byte) 0x5e, (byte) 0xdb, (byte) 0x23, (byte) 0x86, (byte) 0x93,
        (byte) 0x39,
    });

    /**
     * Test data is PKCS#1 padded "Android.\n" which can be generated by:
     * echo "Android." | openssl rsautl -inkey rsa.key -sign | openssl rsautl -inkey rsa.key -raw -verify | recode ../x1
     */
    private static final byte[] RSA_2048_Vector1 = new byte[] {
        (byte) 0x00, (byte) 0x01, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x00, (byte) 0x41, (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F,
        (byte) 0x69, (byte) 0x64, (byte) 0x2E, (byte) 0x0A,
    };

    /**
     * This vector is simply "Android.\n" which is too short.
     */
    private static final byte[] TooShort_Vector = new byte[] {
        (byte) 0x41, (byte) 0x6E, (byte) 0x64, (byte) 0x72, (byte) 0x6F, (byte) 0x69,
        (byte) 0x64, (byte) 0x2E, (byte) 0x0A,
    };

    /**
     * This vector is simply "Android.\n" padded with zeros.
     */
    private static final byte[] TooShort_Vector_Zero_Padded = new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x41, (byte) 0x6e, (byte) 0x64, (byte) 0x72, (byte) 0x6f,
        (byte) 0x69, (byte) 0x64, (byte) 0x2e, (byte) 0x0a,
    };

    /**
     * openssl rsautl -raw -sign -inkey rsa.key | recode ../x1 | sed 's/0x/(byte) 0x/g'
     */
    private static final byte[] RSA_Vector1_Encrypt_Private = new byte[] {
        (byte) 0x35, (byte) 0x43, (byte) 0x38, (byte) 0x44, (byte) 0xAD, (byte) 0x3F,
        (byte) 0x97, (byte) 0x02, (byte) 0xFB, (byte) 0x59, (byte) 0x1F, (byte) 0x4A,
        (byte) 0x2B, (byte) 0xB9, (byte) 0x06, (byte) 0xEC, (byte) 0x66, (byte) 0xE6,
        (byte) 0xD2, (byte) 0xC5, (byte) 0x8B, (byte) 0x7B, (byte) 0xE3, (byte) 0x18,
        (byte) 0xBF, (byte) 0x07, (byte) 0xD6, (byte) 0x01, (byte) 0xF9, (byte) 0xD9,
        (byte) 0x89, (byte) 0xC4, (byte) 0xDB, (byte) 0x00, (byte) 0x68, (byte) 0xFF,
        (byte) 0x9B, (byte) 0x43, (byte) 0x90, (byte) 0xF2, (byte) 0xDB, (byte) 0x83,
        (byte) 0xF4, (byte) 0x7E, (byte) 0xC6, (byte) 0x81, (byte) 0x01, (byte) 0x3A,
        (byte) 0x0B, (byte) 0xE5, (byte) 0xED, (byte) 0x08, (byte) 0x73, (byte) 0x3E,
        (byte) 0xE1, (byte) 0x3F, (byte) 0xDF, (byte) 0x1F, (byte) 0x07, (byte) 0x6D,
        (byte) 0x22, (byte) 0x8D, (byte) 0xCC, (byte) 0x4E, (byte) 0xE3, (byte) 0x9A,
        (byte) 0xBC, (byte) 0xCC, (byte) 0x8F, (byte) 0x9E, (byte) 0x9B, (byte) 0x02,
        (byte) 0x48, (byte) 0x00, (byte) 0xAC, (byte) 0x9F, (byte) 0xA4, (byte) 0x8F,
        (byte) 0x87, (byte) 0xA1, (byte) 0xA8, (byte) 0xE6, (byte) 0x9D, (byte) 0xCD,
        (byte) 0x8B, (byte) 0x05, (byte) 0xE9, (byte) 0xD2, (byte) 0x05, (byte) 0x8D,
        (byte) 0xC9, (byte) 0x95, (byte) 0x16, (byte) 0xD0, (byte) 0xCD, (byte) 0x43,
        (byte) 0x25, (byte) 0x8A, (byte) 0x11, (byte) 0x46, (byte) 0xD7, (byte) 0x74,
        (byte) 0x4C, (byte) 0xCF, (byte) 0x58, (byte) 0xF9, (byte) 0xA1, (byte) 0x30,
        (byte) 0x84, (byte) 0x52, (byte) 0xC9, (byte) 0x01, (byte) 0x5F, (byte) 0x24,
        (byte) 0x4C, (byte) 0xB1, (byte) 0x9F, (byte) 0x7D, (byte) 0x12, (byte) 0x38,
        (byte) 0x27, (byte) 0x0F, (byte) 0x5E, (byte) 0xFF, (byte) 0xE0, (byte) 0x55,
        (byte) 0x8B, (byte) 0xA3, (byte) 0xAD, (byte) 0x60, (byte) 0x35, (byte) 0x83,
        (byte) 0x58, (byte) 0xAF, (byte) 0x99, (byte) 0xDE, (byte) 0x3F, (byte) 0x5D,
        (byte) 0x80, (byte) 0x80, (byte) 0xFF, (byte) 0x9B, (byte) 0xDE, (byte) 0x5C,
        (byte) 0xAB, (byte) 0x97, (byte) 0x43, (byte) 0x64, (byte) 0xD9, (byte) 0x9F,
        (byte) 0xFB, (byte) 0x67, (byte) 0x65, (byte) 0xA5, (byte) 0x99, (byte) 0xE7,
        (byte) 0xE6, (byte) 0xEB, (byte) 0x05, (byte) 0x95, (byte) 0xFC, (byte) 0x46,
        (byte) 0x28, (byte) 0x4B, (byte) 0xD8, (byte) 0x8C, (byte) 0xF5, (byte) 0x0A,
        (byte) 0xEB, (byte) 0x1F, (byte) 0x30, (byte) 0xEA, (byte) 0xE7, (byte) 0x67,
        (byte) 0x11, (byte) 0x25, (byte) 0xF0, (byte) 0x44, (byte) 0x75, (byte) 0x74,
        (byte) 0x94, (byte) 0x06, (byte) 0x78, (byte) 0xD0, (byte) 0x21, (byte) 0xF4,
        (byte) 0x3F, (byte) 0xC8, (byte) 0xC4, (byte) 0x4A, (byte) 0x57, (byte) 0xBE,
        (byte) 0x02, (byte) 0x3C, (byte) 0x93, (byte) 0xF6, (byte) 0x95, (byte) 0xFB,
        (byte) 0xD1, (byte) 0x77, (byte) 0x8B, (byte) 0x43, (byte) 0xF0, (byte) 0xB9,
        (byte) 0x7D, (byte) 0xE0, (byte) 0x32, (byte) 0xE1, (byte) 0x72, (byte) 0xB5,
        (byte) 0x62, (byte) 0x3F, (byte) 0x86, (byte) 0xC3, (byte) 0xD4, (byte) 0x5F,
        (byte) 0x5E, (byte) 0x54, (byte) 0x1B, (byte) 0x5B, (byte) 0xE6, (byte) 0x74,
        (byte) 0xA1, (byte) 0x0B, (byte) 0xE5, (byte) 0x18, (byte) 0xD2, (byte) 0x4F,
        (byte) 0x93, (byte) 0xF3, (byte) 0x09, (byte) 0x58, (byte) 0xCE, (byte) 0xF0,
        (byte) 0xA3, (byte) 0x61, (byte) 0xE4, (byte) 0x6E, (byte) 0x46, (byte) 0x45,
        (byte) 0x89, (byte) 0x50, (byte) 0xBD, (byte) 0x03, (byte) 0x3F, (byte) 0x38,
        (byte) 0xDA, (byte) 0x5D, (byte) 0xD0, (byte) 0x1B, (byte) 0x1F, (byte) 0xB1,
        (byte) 0xEE, (byte) 0x89, (byte) 0x59, (byte) 0xC5,
    };

    private static final byte[] RSA_Vector1_ZeroPadded_Encrypted = new byte[] {
        (byte) 0x60, (byte) 0x4a, (byte) 0x12, (byte) 0xa3, (byte) 0xa7, (byte) 0x4a,
        (byte) 0xa4, (byte) 0xbf, (byte) 0x6c, (byte) 0x36, (byte) 0xad, (byte) 0x66,
        (byte) 0xdf, (byte) 0xce, (byte) 0xf1, (byte) 0xe4, (byte) 0x0f, (byte) 0xd4,
        (byte) 0x54, (byte) 0x5f, (byte) 0x03, (byte) 0x15, (byte) 0x4b, (byte) 0x9e,
        (byte) 0xeb, (byte) 0xfe, (byte) 0x9e, (byte) 0x24, (byte) 0xce, (byte) 0x8e,
        (byte) 0xc3, (byte) 0x36, (byte) 0xa5, (byte) 0x76, (byte) 0xf6, (byte) 0x54,
        (byte) 0xb7, (byte) 0x84, (byte) 0x48, (byte) 0x2f, (byte) 0xd4, (byte) 0x45,
        (byte) 0x74, (byte) 0x48, (byte) 0x5f, (byte) 0x08, (byte) 0x4e, (byte) 0x9c,
        (byte) 0x89, (byte) 0xcc, (byte) 0x34, (byte) 0x40, (byte) 0xb1, (byte) 0x5f,
        (byte) 0xa7, (byte) 0x0e, (byte) 0x11, (byte) 0x4b, (byte) 0xb5, (byte) 0x94,
        (byte) 0xbe, (byte) 0x14, (byte) 0xaa, (byte) 0xaa, (byte) 0xe0, (byte) 0x38,
        (byte) 0x1c, (byte) 0xce, (byte) 0x40, (byte) 0x61, (byte) 0xfc, (byte) 0x08,
        (byte) 0xcb, (byte) 0x14, (byte) 0x2b, (byte) 0xa6, (byte) 0x54, (byte) 0xdf,
        (byte) 0x05, (byte) 0x5c, (byte) 0x9b, (byte) 0x4f, (byte) 0x14, (byte) 0x93,
        (byte) 0xb0, (byte) 0x70, (byte) 0xd9, (byte) 0x32, (byte) 0xdc, (byte) 0x24,
        (byte) 0xe0, (byte) 0xae, (byte) 0x48, (byte) 0xfc, (byte) 0x53, (byte) 0xee,
        (byte) 0x7c, (byte) 0x9f, (byte) 0x69, (byte) 0x34, (byte) 0xf4, (byte) 0x76,
        (byte) 0xee, (byte) 0x67, (byte) 0xb2, (byte) 0xa7, (byte) 0x33, (byte) 0x1c,
        (byte) 0x47, (byte) 0xff, (byte) 0x5c, (byte) 0xf0, (byte) 0xb8, (byte) 0x04,
        (byte) 0x2c, (byte) 0xfd, (byte) 0xe2, (byte) 0xb1, (byte) 0x4a, (byte) 0x0a,
        (byte) 0x69, (byte) 0x1c, (byte) 0x80, (byte) 0x2b, (byte) 0xb4, (byte) 0x50,
        (byte) 0x65, (byte) 0x5c, (byte) 0x76, (byte) 0x78, (byte) 0x9a, (byte) 0x0c,
        (byte) 0x05, (byte) 0x62, (byte) 0xf0, (byte) 0xc4, (byte) 0x1c, (byte) 0x38,
        (byte) 0x15, (byte) 0xd0, (byte) 0xe2, (byte) 0x5a, (byte) 0x3d, (byte) 0xb6,
        (byte) 0xe0, (byte) 0x88, (byte) 0x85, (byte) 0xd1, (byte) 0x4f, (byte) 0x7e,
        (byte) 0xfc, (byte) 0x77, (byte) 0x0d, (byte) 0x2a, (byte) 0x45, (byte) 0xd5,
        (byte) 0xf8, (byte) 0x3c, (byte) 0x7b, (byte) 0x2d, (byte) 0x1b, (byte) 0x82,
        (byte) 0xfe, (byte) 0x58, (byte) 0x22, (byte) 0x47, (byte) 0x06, (byte) 0x58,
        (byte) 0x8b, (byte) 0x4f, (byte) 0xfb, (byte) 0x9b, (byte) 0x1c, (byte) 0x70,
        (byte) 0x36, (byte) 0x12, (byte) 0x04, (byte) 0x17, (byte) 0x47, (byte) 0x8a,
        (byte) 0x0a, (byte) 0xec, (byte) 0x12, (byte) 0x3b, (byte) 0xf8, (byte) 0xd2,
        (byte) 0xdc, (byte) 0x3c, (byte) 0xc8, (byte) 0x46, (byte) 0xc6, (byte) 0x51,
        (byte) 0x06, (byte) 0x06, (byte) 0xcb, (byte) 0x84, (byte) 0x67, (byte) 0xb5,
        (byte) 0x68, (byte) 0xd9, (byte) 0x9c, (byte) 0xd4, (byte) 0x16, (byte) 0x5c,
        (byte) 0xb4, (byte) 0xe2, (byte) 0x55, (byte) 0xe6, (byte) 0x3a, (byte) 0x73,
        (byte) 0x01, (byte) 0x1d, (byte) 0x6f, (byte) 0x30, (byte) 0x31, (byte) 0x59,
        (byte) 0x8b, (byte) 0x2f, (byte) 0x4c, (byte) 0xe7, (byte) 0x86, (byte) 0x4c,
        (byte) 0x39, (byte) 0x4e, (byte) 0x67, (byte) 0x3b, (byte) 0x22, (byte) 0x9b,
        (byte) 0x85, (byte) 0x5a, (byte) 0xc3, (byte) 0x29, (byte) 0xaf, (byte) 0x8c,
        (byte) 0x7c, (byte) 0x59, (byte) 0x4a, (byte) 0x24, (byte) 0xfa, (byte) 0xba,
        (byte) 0x55, (byte) 0x40, (byte) 0x13, (byte) 0x64, (byte) 0xd8, (byte) 0xcb,
        (byte) 0x4b, (byte) 0x98, (byte) 0x3f, (byte) 0xae, (byte) 0x20, (byte) 0xfd,
        (byte) 0x8a, (byte) 0x50, (byte) 0x73, (byte) 0xe4,
    };

    public void testRSA_ECB_NoPadding_Private_OnlyDoFinal_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);

        final PrivateKey privKey = kf.generatePrivate(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually decrypting with private keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, privKey);
        byte[] encrypted = c.doFinal(RSA_2048_Vector1);
        assertTrue("Encrypted should match expected",
                Arrays.equals(RSA_Vector1_Encrypt_Private, encrypted));

        c.init(Cipher.DECRYPT_MODE, privKey);
        encrypted = c.doFinal(RSA_2048_Vector1);
        assertTrue("Encrypted should match expected",
                Arrays.equals(RSA_Vector1_Encrypt_Private, encrypted));
    }

    public void testRSA_ECB_NoPadding_Private_UpdateThenEmptyDoFinal_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);

        final PrivateKey privKey = kf.generatePrivate(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually decrypting with private keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, privKey);
        c.update(RSA_2048_Vector1);
        byte[] encrypted = c.doFinal();
        assertTrue("Encrypted should match expected",
                Arrays.equals(RSA_Vector1_Encrypt_Private, encrypted));

        c.init(Cipher.DECRYPT_MODE, privKey);
        c.update(RSA_2048_Vector1);
        encrypted = c.doFinal();
        assertTrue("Encrypted should match expected",
                Arrays.equals(RSA_Vector1_Encrypt_Private, encrypted));
    }

    public void testRSA_ECB_NoPadding_Private_SingleByteUpdateThenEmptyDoFinal_Success()
            throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);

        final PrivateKey privKey = kf.generatePrivate(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually decrypting with private keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, privKey);
        int i;
        for (i = 0; i < RSA_2048_Vector1.length / 2; i++) {
            c.update(RSA_2048_Vector1, i, 1);
        }
        byte[] encrypted = c.doFinal(RSA_2048_Vector1, i, RSA_2048_Vector1.length - i);
        assertTrue("Encrypted should match expected",
                Arrays.equals(RSA_Vector1_Encrypt_Private, encrypted));

        c.init(Cipher.DECRYPT_MODE, privKey);
        for (i = 0; i < RSA_2048_Vector1.length / 2; i++) {
            c.update(RSA_2048_Vector1, i, 1);
        }
        encrypted = c.doFinal(RSA_2048_Vector1, i, RSA_2048_Vector1.length - i);
        assertTrue("Encrypted should match expected",
                Arrays.equals(RSA_Vector1_Encrypt_Private, encrypted));
    }

    public void testRSA_ECB_NoPadding_Private_OnlyDoFinalWithOffset_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);
        final PrivateKey privKey = kf.generatePrivate(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually decrypting with private keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, privKey);
        byte[] encrypted = new byte[RSA_Vector1_Encrypt_Private.length];
        final int encryptLen = c
                .doFinal(RSA_2048_Vector1, 0, RSA_2048_Vector1.length, encrypted, 0);
        assertEquals("Encrypted size should match expected", RSA_Vector1_Encrypt_Private.length,
                encryptLen);
        assertTrue("Encrypted should match expected",
                Arrays.equals(RSA_Vector1_Encrypt_Private, encrypted));

        c.init(Cipher.DECRYPT_MODE, privKey);
        final int decryptLen = c
                .doFinal(RSA_2048_Vector1, 0, RSA_2048_Vector1.length, encrypted, 0);
        assertEquals("Encrypted size should match expected", RSA_Vector1_Encrypt_Private.length,
                decryptLen);
        assertTrue("Encrypted should match expected",
                Arrays.equals(RSA_Vector1_Encrypt_Private, encrypted));
    }

    public void testRSA_ECB_NoPadding_Public_OnlyDoFinal_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus, RSA_2048_publicExponent);

        final PublicKey privKey = kf.generatePublic(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually encrypting with public keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, privKey);
        byte[] encrypted = c.doFinal(RSA_Vector1_Encrypt_Private);
        assertTrue("Encrypted should match expected",
                Arrays.equals(RSA_2048_Vector1, encrypted));

        c.init(Cipher.DECRYPT_MODE, privKey);
        encrypted = c.doFinal(RSA_Vector1_Encrypt_Private);
        assertTrue("Encrypted should match expected",
                Arrays.equals(RSA_2048_Vector1, encrypted));
    }

    public void testRSA_ECB_NoPadding_Public_OnlyDoFinalWithOffset_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus, RSA_2048_publicExponent);

        final PublicKey pubKey = kf.generatePublic(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually encrypting with public keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, pubKey);
        byte[] encrypted = new byte[RSA_2048_Vector1.length];
        final int encryptLen = c.doFinal(RSA_Vector1_Encrypt_Private, 0,
                RSA_Vector1_Encrypt_Private.length, encrypted, 0);
        assertEquals("Encrypted size should match expected", RSA_2048_Vector1.length, encryptLen);
        assertTrue("Encrypted should match expected", Arrays.equals(RSA_2048_Vector1, encrypted));

        c.init(Cipher.DECRYPT_MODE, pubKey);
        final int decryptLen = c.doFinal(RSA_Vector1_Encrypt_Private, 0,
                RSA_Vector1_Encrypt_Private.length, encrypted, 0);
        assertEquals("Encrypted size should match expected", RSA_2048_Vector1.length, decryptLen);
        assertTrue("Encrypted should match expected", Arrays.equals(RSA_2048_Vector1, encrypted));
    }

    public void testRSA_ECB_NoPadding_Public_UpdateThenEmptyDoFinal_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus, RSA_2048_publicExponent);

        final PublicKey privKey = kf.generatePublic(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually encrypting with public keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, privKey);
        c.update(RSA_Vector1_Encrypt_Private);
        byte[] encrypted = c.doFinal();
        assertTrue("Encrypted should match expected", Arrays.equals(RSA_2048_Vector1, encrypted));

        c.init(Cipher.DECRYPT_MODE, privKey);
        c.update(RSA_Vector1_Encrypt_Private);
        encrypted = c.doFinal();
        assertTrue("Encrypted should match expected", Arrays.equals(RSA_2048_Vector1, encrypted));
    }

    public void testRSA_ECB_NoPadding_Public_SingleByteUpdateThenEmptyDoFinal_Success()
            throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus, RSA_2048_publicExponent);

        final PublicKey privKey = kf.generatePublic(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually encrypting with public keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, privKey);
        int i;
        for (i = 0; i < RSA_Vector1_Encrypt_Private.length / 2; i++) {
            c.update(RSA_Vector1_Encrypt_Private, i, 1);
        }
        byte[] encrypted = c.doFinal(RSA_Vector1_Encrypt_Private, i, RSA_2048_Vector1.length - i);
        assertTrue("Encrypted should match expected", Arrays.equals(RSA_2048_Vector1, encrypted));

        c.init(Cipher.DECRYPT_MODE, privKey);
        for (i = 0; i < RSA_Vector1_Encrypt_Private.length / 2; i++) {
            c.update(RSA_Vector1_Encrypt_Private, i, 1);
        }
        encrypted = c.doFinal(RSA_Vector1_Encrypt_Private, i, RSA_2048_Vector1.length - i);
        assertTrue("Encrypted should match expected", Arrays.equals(RSA_2048_Vector1, encrypted));
    }

    public void testRSA_ECB_NoPadding_Public_TooSmall_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus, RSA_2048_publicExponent);

        final PublicKey privKey = kf.generatePublic(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually encrypting with public keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, privKey);
        byte[] encrypted = c.doFinal(TooShort_Vector);
        assertTrue("Encrypted should match expected",
                Arrays.equals(RSA_Vector1_ZeroPadded_Encrypted, encrypted));

        c.init(Cipher.DECRYPT_MODE, privKey);
        encrypted = c.doFinal(TooShort_Vector);
        assertTrue("Encrypted should match expected",
                Arrays.equals(RSA_Vector1_ZeroPadded_Encrypted, encrypted));
    }

    public void testRSA_ECB_NoPadding_Private_TooSmall_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);

        final PrivateKey privKey = kf.generatePrivate(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually encrypting with public keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, privKey);
        byte[] encrypted = c.doFinal(RSA_Vector1_ZeroPadded_Encrypted);
        assertTrue("Encrypted should match expected",
                Arrays.equals(TooShort_Vector_Zero_Padded, encrypted));

        c.init(Cipher.DECRYPT_MODE, privKey);
        encrypted = c.doFinal(RSA_Vector1_ZeroPadded_Encrypted);
        assertTrue("Encrypted should match expected",
                Arrays.equals(TooShort_Vector_Zero_Padded, encrypted));
    }

    public void testRSA_ECB_NoPadding_Private_CombinedUpdateAndDoFinal_TooBig_Failure()
            throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);

        final PrivateKey privKey = kf.generatePrivate(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually encrypting with public keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, privKey);
        c.update(RSA_Vector1_ZeroPadded_Encrypted);

        try {
            c.doFinal(RSA_Vector1_ZeroPadded_Encrypted);
            fail("Should have error when block size is too big.");
        } catch (IllegalBlockSizeException success) {
        }
    }

    public void testRSA_ECB_NoPadding_Private_UpdateInAndOutPlusDoFinal_TooBig_Failure()
            throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);

        final PrivateKey privKey = kf.generatePrivate(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually encrypting with public keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, privKey);

        byte[] output = new byte[RSA_2048_Vector1.length];
        c.update(RSA_Vector1_ZeroPadded_Encrypted, 0, RSA_Vector1_ZeroPadded_Encrypted.length,
                output);

        try {
            c.doFinal(RSA_Vector1_ZeroPadded_Encrypted);
            fail("Should have error when block size is too big.");
        } catch (IllegalBlockSizeException success) {
        }
    }

    public void testRSA_ECB_NoPadding_Private_OnlyDoFinal_TooBig_Failure() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);

        final PrivateKey privKey = kf.generatePrivate(keySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");

        /*
         * You're actually encrypting with public keys, but there is no
         * distinction made here. It's all keyed off of what kind of key you're
         * using. ENCRYPT_MODE and DECRYPT_MODE are the same.
         */
        c.init(Cipher.ENCRYPT_MODE, privKey);

        byte[] tooBig_Vector = new byte[RSA_Vector1_ZeroPadded_Encrypted.length * 2];
        System.arraycopy(RSA_Vector1_ZeroPadded_Encrypted, 0, tooBig_Vector, 0,
                RSA_Vector1_ZeroPadded_Encrypted.length);
        System.arraycopy(RSA_Vector1_ZeroPadded_Encrypted, 0, tooBig_Vector,
                RSA_Vector1_ZeroPadded_Encrypted.length, RSA_Vector1_ZeroPadded_Encrypted.length);

        try {
            c.doFinal(tooBig_Vector);
            fail("Should have error when block size is too big.");
        } catch (IllegalBlockSizeException success) {
        }
    }

    public void testRSA_ECB_NoPadding_GetBlockSize_Success() throws Exception {
        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
        assertEquals("RSA is not a block cipher and should return block size of 0",
                0, c.getBlockSize());

        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent);
        final PublicKey pubKey = kf.generatePublic(pubKeySpec);
        c.init(Cipher.ENCRYPT_MODE, pubKey);

        assertEquals("RSA is not a block cipher and should return block size of 0",
                0, c.getBlockSize());
    }

    public void testRSA_ECB_NoPadding_GetOutputSize_NoInit_Failure() throws Exception {
        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
        try {
            c.getOutputSize(RSA_2048_Vector1.length);
            fail("Should throw IllegalStateException if getOutputSize is called before init");
        } catch (IllegalStateException success) {
        }
    }

    public void testRSA_ECB_NoPadding_GetOutputSize_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent);
        final PublicKey pubKey = kf.generatePublic(pubKeySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, pubKey);

        final int modulusInBytes = RSA_2048_modulus.bitLength() / 8;
        assertEquals("Output size should be equal to modulus size",
                modulusInBytes, c.getOutputSize(RSA_2048_Vector1.length));

        assertEquals("Output size should be equal to modulus size",
                modulusInBytes, c.getOutputSize(RSA_2048_Vector1.length * 2));

        assertEquals("Output size should be equal to modulus size",
                modulusInBytes, c.getOutputSize(0));
    }

    public void testRSA_ECB_NoPadding_GetIV_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent);
        final PublicKey pubKey = kf.generatePublic(pubKeySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
        assertNull("ECB mode has no IV and should be null", c.getIV());

        c.init(Cipher.ENCRYPT_MODE, pubKey);

        assertNull("ECB mode has no IV and should be null", c.getIV());
    }

    public void testRSA_ECB_NoPadding_GetParameters_NoneProvided_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent);
        final PublicKey pubKey = kf.generatePublic(pubKeySpec);

        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
        assertNull("Parameters should be null", c.getParameters());
    }

    /*
     * Test vector generation:
     * openssl rand -hex 16
     * echo '3d4f8970b1f27537f40a39298a41555f' | sed 's/\(..\)/(byte) 0x\1, /g'
     */
    private static final byte[] AES_128_KEY = new byte[] {
            (byte) 0x3d, (byte) 0x4f, (byte) 0x89, (byte) 0x70, (byte) 0xb1, (byte) 0xf2,
            (byte) 0x75, (byte) 0x37, (byte) 0xf4, (byte) 0x0a, (byte) 0x39, (byte) 0x29,
            (byte) 0x8a, (byte) 0x41, (byte) 0x55, (byte) 0x5f,
    };

    /*
     * Test key generation:
     * openssl rand -hex 24
     * echo '5a7a3d7e40b64ed996f7afa15f97fd595e27db6af428e342' | sed 's/\(..\)/(byte) 0x\1, /g'
     */
    private static final byte[] AES_192_KEY = new byte[] {
            (byte) 0x5a, (byte) 0x7a, (byte) 0x3d, (byte) 0x7e, (byte) 0x40, (byte) 0xb6,
            (byte) 0x4e, (byte) 0xd9, (byte) 0x96, (byte) 0xf7, (byte) 0xaf, (byte) 0xa1,
            (byte) 0x5f, (byte) 0x97, (byte) 0xfd, (byte) 0x59, (byte) 0x5e, (byte) 0x27,
            (byte) 0xdb, (byte) 0x6a, (byte) 0xf4, (byte) 0x28, (byte) 0xe3, (byte) 0x42,
    };

    /*
     * Test key generation:
     * openssl rand -hex 32
     * echo 'ec53c6d51d2c4973585fb0b8e51cd2e39915ff07a1837872715d6121bf861935' | sed 's/\(..\)/(byte) 0x\1, /g'
     */
    private static final byte[] AES_256_KEY = new byte[] {
            (byte) 0xec, (byte) 0x53, (byte) 0xc6, (byte) 0xd5, (byte) 0x1d, (byte) 0x2c,
            (byte) 0x49, (byte) 0x73, (byte) 0x58, (byte) 0x5f, (byte) 0xb0, (byte) 0xb8,
            (byte) 0xe5, (byte) 0x1c, (byte) 0xd2, (byte) 0xe3, (byte) 0x99, (byte) 0x15,
            (byte) 0xff, (byte) 0x07, (byte) 0xa1, (byte) 0x83, (byte) 0x78, (byte) 0x72,
            (byte) 0x71, (byte) 0x5d, (byte) 0x61, (byte) 0x21, (byte) 0xbf, (byte) 0x86,
            (byte) 0x19, (byte) 0x35,
    };

    private static final byte[][] AES_KEYS = new byte[][] {
            AES_128_KEY, AES_192_KEY, AES_256_KEY,
    };

    private static final String[] AES_MODES = new String[] {
            "AES/ECB",
            "AES/CBC",
            "AES/CFB",
            "AES/CTR",
            "AES/OFB",
    };

    /*
     * Test vector creation:
     * echo -n 'Hello, world!' | recode ../x1 | sed 's/0x/(byte) 0x/g'
     */
    private static final byte[] AES_128_ECB_PKCS5Padding_TestVector_1_Plaintext = new byte[] {
            (byte) 0x48, (byte) 0x65, (byte) 0x6C, (byte) 0x6C, (byte) 0x6F, (byte) 0x2C,
            (byte) 0x20, (byte) 0x77, (byte) 0x6F, (byte) 0x72, (byte) 0x6C, (byte) 0x64,
            (byte) 0x21,
    };

    /*
     * Test vector creation:
     * openssl enc -aes-128-ecb -K 3d4f8970b1f27537f40a39298a41555f -in blah|openssl enc -aes-128-ecb -K 3d4f8970b1f27537f40a39298a41555f -nopad -d|recode ../x1 | sed 's/0x/(byte) 0x/g'
     */
    private static final byte[] AES_128_ECB_PKCS5Padding_TestVector_1_Plaintext_Padded = new byte[] {
            (byte) 0x48, (byte) 0x65, (byte) 0x6C, (byte) 0x6C, (byte) 0x6F, (byte) 0x2C,
            (byte) 0x20, (byte) 0x77, (byte) 0x6F, (byte) 0x72, (byte) 0x6C, (byte) 0x64,
            (byte) 0x21, (byte) 0x03, (byte) 0x03, (byte) 0x03
    };

    /*
     * Test vector generation:
     * openssl enc -aes-128-ecb -K 3d4f8970b1f27537f40a39298a41555f -in blah|recode ../x1 | sed 's/0x/(byte) 0x/g'
     */
    private static final byte[] AES_128_ECB_PKCS5Padding_TestVector_1_Encrypted = new byte[] {
            (byte) 0x65, (byte) 0x3E, (byte) 0x86, (byte) 0xFB, (byte) 0x05, (byte) 0x5A,
            (byte) 0x52, (byte) 0xEA, (byte) 0xDD, (byte) 0x08, (byte) 0xE7, (byte) 0x48,
            (byte) 0x33, (byte) 0x01, (byte) 0xFC, (byte) 0x5A,
    };

    /*
     * Test key generation:
     * openssl rand -hex 16
     * echo 'ceaa31952dfd3d0f5af4b2042ba06094' | sed 's/\(..\)/(byte) 0x\1, /g'
     */
    private static final byte[] AES_256_CBC_PKCS5Padding_TestVector_1_IV = new byte[] {
            (byte) 0xce, (byte) 0xaa, (byte) 0x31, (byte) 0x95, (byte) 0x2d, (byte) 0xfd,
            (byte) 0x3d, (byte) 0x0f, (byte) 0x5a, (byte) 0xf4, (byte) 0xb2, (byte) 0x04,
            (byte) 0x2b, (byte) 0xa0, (byte) 0x60, (byte) 0x94,
    };

    /*
     * Test vector generation:
     * echo -n 'I only regret that I have but one test to write.' | recode ../x1 | sed 's/0x/(byte) 0x/g'
     */
    private static final byte[] AES_256_CBC_PKCS5Padding_TestVector_1_Plaintext = new byte[] {
            (byte) 0x49, (byte) 0x20, (byte) 0x6F, (byte) 0x6E, (byte) 0x6C, (byte) 0x79,
            (byte) 0x20, (byte) 0x72, (byte) 0x65, (byte) 0x67, (byte) 0x72, (byte) 0x65,
            (byte) 0x74, (byte) 0x20, (byte) 0x74, (byte) 0x68, (byte) 0x61, (byte) 0x74,
            (byte) 0x20, (byte) 0x49, (byte) 0x20, (byte) 0x68, (byte) 0x61, (byte) 0x76,
            (byte) 0x65, (byte) 0x20, (byte) 0x62, (byte) 0x75, (byte) 0x74, (byte) 0x20,
            (byte) 0x6F, (byte) 0x6E, (byte) 0x65, (byte) 0x20, (byte) 0x74, (byte) 0x65,
            (byte) 0x73, (byte) 0x74, (byte) 0x20, (byte) 0x74, (byte) 0x6F, (byte) 0x20,
            (byte) 0x77, (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x65, (byte) 0x2E
    };

    /*
     * Test vector generation:
     * echo -n 'I only regret that I have but one test to write.' | openssl enc -aes-256-cbc -K ec53c6d51d2c4973585fb0b8e51cd2e39915ff07a1837872715d6121bf861935 -iv ceaa31952dfd3d0f5af4b2042ba06094 | openssl enc -aes-256-cbc -K ec53c6d51d2c4973585fb0b8e51cd2e39915ff07a1837872715d6121bf861935 -iv ceaa31952dfd3d0f5af4b2042ba06094 -d -nopad | recode ../x1 | sed 's/0x/(byte) 0x/g'
     */
    private static final byte[] AES_256_CBC_PKCS5Padding_TestVector_1_Plaintext_Padded = new byte[] {
            (byte) 0x49, (byte) 0x20, (byte) 0x6F, (byte) 0x6E, (byte) 0x6C, (byte) 0x79,
            (byte) 0x20, (byte) 0x72, (byte) 0x65, (byte) 0x67, (byte) 0x72, (byte) 0x65,
            (byte) 0x74, (byte) 0x20, (byte) 0x74, (byte) 0x68, (byte) 0x61, (byte) 0x74,
            (byte) 0x20, (byte) 0x49, (byte) 0x20, (byte) 0x68, (byte) 0x61, (byte) 0x76,
            (byte) 0x65, (byte) 0x20, (byte) 0x62, (byte) 0x75, (byte) 0x74, (byte) 0x20,
            (byte) 0x6F, (byte) 0x6E, (byte) 0x65, (byte) 0x20, (byte) 0x74, (byte) 0x65,
            (byte) 0x73, (byte) 0x74, (byte) 0x20, (byte) 0x74, (byte) 0x6F, (byte) 0x20,
            (byte) 0x77, (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x65, (byte) 0x2E,
            (byte) 0x10, (byte) 0x10, (byte) 0x10, (byte) 0x10, (byte) 0x10, (byte) 0x10,
            (byte) 0x10, (byte) 0x10, (byte) 0x10, (byte) 0x10, (byte) 0x10, (byte) 0x10,
            (byte) 0x10, (byte) 0x10, (byte) 0x10, (byte) 0x10
    };

    /*
     * Test vector generation:
     * echo -n 'I only regret that I have but one test to write.' | openssl enc -aes-256-cbc -K ec53c6d51d2c4973585fb0b8e51cd2e39915ff07a1837872715d6121bf861935 -iv ceaa31952dfd3d0f5af4b2042ba06094 | recode ../x1 | sed 's/0x/(byte) 0x/g'
     */
    private static final byte[] AES_256_CBC_PKCS5Padding_TestVector_1_Ciphertext = new byte[] {
            (byte) 0x90, (byte) 0x65, (byte) 0xDD, (byte) 0xAF, (byte) 0x7A, (byte) 0xCE,
            (byte) 0xAE, (byte) 0xBF, (byte) 0xE8, (byte) 0xF6, (byte) 0x9E, (byte) 0xDB,
            (byte) 0xEA, (byte) 0x65, (byte) 0x28, (byte) 0xC4, (byte) 0x9A, (byte) 0x28,
            (byte) 0xEA, (byte) 0xA3, (byte) 0x95, (byte) 0x2E, (byte) 0xFF, (byte) 0xF1,
            (byte) 0xA0, (byte) 0xCA, (byte) 0xC2, (byte) 0xA4, (byte) 0x65, (byte) 0xCD,
            (byte) 0xBF, (byte) 0xCE, (byte) 0x9E, (byte) 0xF1, (byte) 0x57, (byte) 0xF6,
            (byte) 0x32, (byte) 0x2E, (byte) 0x8F, (byte) 0x93, (byte) 0x2E, (byte) 0xAE,
            (byte) 0x41, (byte) 0x33, (byte) 0x54, (byte) 0xD0, (byte) 0xEF, (byte) 0x8C,
            (byte) 0x52, (byte) 0x14, (byte) 0xAC, (byte) 0x2D, (byte) 0xD5, (byte) 0xA4,
            (byte) 0xF9, (byte) 0x20, (byte) 0x77, (byte) 0x25, (byte) 0x91, (byte) 0x3F,
            (byte) 0xD1, (byte) 0xB9, (byte) 0x00, (byte) 0x3E
    };

    private static class CipherTestParam {
        public final String mode;

        public final byte[] key;

        public final byte[] iv;

        public final byte[] plaintext;

        public final byte[] ciphertext;

        public final byte[] plaintextPadded;

        public CipherTestParam(String mode, byte[] key, byte[] iv, byte[] plaintext,
                byte[] plaintextPadded, byte[] ciphertext) {
            this.mode = mode;
            this.key = key;
            this.iv = iv;
            this.plaintext = plaintext;
            this.plaintextPadded = plaintextPadded;
            this.ciphertext = ciphertext;
        }
    }

    private static List<CipherTestParam> CIPHER_TEST_PARAMS = new ArrayList<CipherTestParam>();
    static {
        CIPHER_TEST_PARAMS.add(new CipherTestParam("AES/ECB", AES_128_KEY,
                null,
                AES_128_ECB_PKCS5Padding_TestVector_1_Plaintext,
                AES_128_ECB_PKCS5Padding_TestVector_1_Plaintext_Padded,
                AES_128_ECB_PKCS5Padding_TestVector_1_Encrypted));
        CIPHER_TEST_PARAMS.add(new CipherTestParam("AES/CBC", AES_256_KEY,
                AES_256_CBC_PKCS5Padding_TestVector_1_IV,
                AES_256_CBC_PKCS5Padding_TestVector_1_Plaintext,
                AES_256_CBC_PKCS5Padding_TestVector_1_Plaintext_Padded,
                AES_256_CBC_PKCS5Padding_TestVector_1_Ciphertext));
    }

    public void testCipher_Success() throws Exception {
        final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(errBuffer);
        for (CipherTestParam p : CIPHER_TEST_PARAMS) {
            try {
                checkCipher(p);
            } catch (Exception e) {
                out.append("Error encountered checking " + p.mode + ", keySize="
                        + (p.key.length * 8) + "\n");
                e.printStackTrace(out);
            }
        }
        out.flush();
        if (errBuffer.size() > 0) {
            throw new Exception("Errors encountered:\n\n" + errBuffer.toString() + "\n\n");
        }
    }

    private void checkCipher(CipherTestParam p) throws Exception {
        SecretKey key = new SecretKeySpec(p.key, "AES");
        Cipher c = Cipher.getInstance(p.mode + "/PKCS5Padding");
        AlgorithmParameterSpec spec = null;
        if (p.iv != null) {
            spec = new IvParameterSpec(p.iv);
        }
        c.init(Cipher.ENCRYPT_MODE, key, spec);

        final byte[] actualCiphertext = c.doFinal(p.plaintext);
        assertTrue(Arrays.equals(p.ciphertext, actualCiphertext));

        c.init(Cipher.DECRYPT_MODE, key, spec);

        final byte[] actualPlaintext = c.doFinal(p.ciphertext);
        assertTrue(Arrays.equals(p.plaintext, actualPlaintext));

        Cipher cNoPad = Cipher.getInstance(p.mode + "/NoPadding");
        cNoPad.init(Cipher.DECRYPT_MODE, key, spec);

        final byte[] actualPlaintextPadded = cNoPad.doFinal(p.ciphertext);
        assertTrue(Arrays.equals(p.plaintextPadded, actualPlaintextPadded));
    }

    public void testCipher_ShortBlock_Failure() throws Exception {
        final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(errBuffer);
        for (CipherTestParam p : CIPHER_TEST_PARAMS) {
            try {
                checkCipher_ShortBlock_Failure(p);
            } catch (Exception e) {
                out.append("Error encountered checking " + p.mode + ", keySize="
                        + (p.key.length * 8) + "\n");
                e.printStackTrace(out);
            }
        }
        out.flush();
        if (errBuffer.size() > 0) {
            throw new Exception("Errors encountered:\n\n" + errBuffer.toString() + "\n\n");
        }
    }

    private void checkCipher_ShortBlock_Failure(CipherTestParam p) throws Exception {
        SecretKey key = new SecretKeySpec(p.key, "AES");
        Cipher c = Cipher.getInstance(p.mode + "/NoPadding");
        if (c.getBlockSize() == 0) {
            return;
        }

        c.init(Cipher.ENCRYPT_MODE, key);
        try {
            c.doFinal(new byte[] { 0x01, 0x02, 0x03 });
            fail("Should throw IllegalBlockSizeException on wrong-sized block");
        } catch (IllegalBlockSizeException expected) {
        }
    }

    public void testAES_ECB_PKCS5Padding_ShortBuffer_Failure() throws Exception {
        SecretKey key = new SecretKeySpec(AES_128_KEY, "AES");
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, key);

        final byte[] fragmentOutput = c.update(AES_128_ECB_PKCS5Padding_TestVector_1_Plaintext);
        if (fragmentOutput != null) {
            assertEquals(0, fragmentOutput.length);
        }

        // Provide null buffer.
        {
            try {
                c.doFinal(null, 0);
                fail("Should throw NullPointerException on null output buffer");
            } catch (NullPointerException expected) {
            } catch (IllegalArgumentException expected) {
            }
        }

        // Provide short buffer.
        {
            final byte[] output = new byte[c.getBlockSize() - 1];
            try {
                c.doFinal(output, 0);
                fail("Should throw ShortBufferException on short output buffer");
            } catch (ShortBufferException expected) {
            }
        }

        // Start 1 byte into output buffer.
        {
            final byte[] output = new byte[c.getBlockSize()];
            try {
                c.doFinal(output, 1);
                fail("Should throw ShortBufferException on short output buffer");
            } catch (ShortBufferException expected) {
            }
        }

        // Should keep data for real output buffer
        {
            final byte[] output = new byte[c.getBlockSize()];
            assertEquals(AES_128_ECB_PKCS5Padding_TestVector_1_Encrypted.length, c.doFinal(output, 0));
            assertTrue(Arrays.equals(AES_128_ECB_PKCS5Padding_TestVector_1_Encrypted, output));
        }
    }

    public void testAES_ECB_NoPadding_IncrementalUpdate_Success() throws Exception {
        SecretKey key = new SecretKeySpec(AES_128_KEY, "AES");
        Cipher c = Cipher.getInstance("AES/ECB/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key);

        for (int i = 0; i < AES_128_ECB_PKCS5Padding_TestVector_1_Plaintext_Padded.length - 1; i++) {
            final byte[] outputFragment = c.update(AES_128_ECB_PKCS5Padding_TestVector_1_Plaintext_Padded, i, 1);
            if (outputFragment != null) {
                assertEquals(0, outputFragment.length);
            }
        }

        final byte[] output = c.doFinal(AES_128_ECB_PKCS5Padding_TestVector_1_Plaintext_Padded,
                AES_128_ECB_PKCS5Padding_TestVector_1_Plaintext_Padded.length - 1, 1);
        assertNotNull(output);
        assertEquals(AES_128_ECB_PKCS5Padding_TestVector_1_Plaintext_Padded.length, output.length);

        assertTrue(Arrays.equals(AES_128_ECB_PKCS5Padding_TestVector_1_Encrypted, output));
    }

    private static final byte[] AES_IV_ZEROES = new byte[] {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };

    public void testAES_ECB_NoPadding_IvParameters_Failure() throws Exception {
        SecretKey key = new SecretKeySpec(AES_128_KEY, "AES");
        Cipher c = Cipher.getInstance("AES/ECB/NoPadding");

        AlgorithmParameterSpec spec = new IvParameterSpec(AES_IV_ZEROES);
        try {
            c.init(Cipher.ENCRYPT_MODE, key, spec);
            fail("Should not accept an IV in ECB mode");
        } catch (InvalidAlgorithmParameterException expected) {
        }
    }
}
