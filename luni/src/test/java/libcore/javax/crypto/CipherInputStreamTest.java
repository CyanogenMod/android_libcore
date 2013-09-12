/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import junit.framework.TestCase;

public final class CipherInputStreamTest extends TestCase {

    private final byte[] aesKeyBytes = {
            (byte) 0x50, (byte) 0x98, (byte) 0xF2, (byte) 0xC3, (byte) 0x85, (byte) 0x23,
            (byte) 0xA3, (byte) 0x33, (byte) 0x50, (byte) 0x98, (byte) 0xF2, (byte) 0xC3,
            (byte) 0x85, (byte) 0x23, (byte) 0xA3, (byte) 0x33,
    };

    private final byte[] aesIvBytes = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };

    private final byte[] aesCipherText = {
            (byte) 0x2F, (byte) 0x2C, (byte) 0x74, (byte) 0x31, (byte) 0xFF, (byte) 0xCC,
            (byte) 0x28, (byte) 0x7D, (byte) 0x59, (byte) 0xBD, (byte) 0xE5, (byte) 0x0A,
            (byte) 0x30, (byte) 0x7E, (byte) 0x6A, (byte) 0x4A
    };

    private final String plainText = "abcde";
    private SecretKey key;
    private AlgorithmParameterSpec iv;

    @Override protected void setUp() throws Exception {
        key = new SecretKeySpec(aesKeyBytes, "AES");
        iv = new IvParameterSpec(aesIvBytes);
    }

    public void testEncrypt() throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        InputStream in = new CipherInputStream(
                new ByteArrayInputStream(plainText.getBytes("UTF-8")), cipher);
        byte[] bytes = readAll(in);
        assertEquals(Arrays.toString(aesCipherText), Arrays.toString(bytes));
    }

    public void testDecrypt() throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        InputStream in = new CipherInputStream(new ByteArrayInputStream(aesCipherText), cipher);
        byte[] bytes = readAll(in);
        assertEquals(Arrays.toString(plainText.getBytes("UTF-8")), Arrays.toString(bytes));
    }

    public void testSkip() throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        InputStream in = new CipherInputStream(new ByteArrayInputStream(aesCipherText), cipher);
        assertTrue(in.skip(5) >= 0);
    }

    private byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int count;
        byte[] buffer = new byte[1024];
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }

    public void testCipherInputStream_TruncatedInput_Failure() throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        InputStream is = new CipherInputStream(new ByteArrayInputStream(new byte[31]), cipher);
        is.read(new byte[4]);
        is.close();
    }
}
