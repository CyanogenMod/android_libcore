/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.apache.harmony.xnet.provider.jsse;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import junit.framework.TestCase;

public class OpenSSLSignatureTest extends TestCase {
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

    private static final byte[] Vector1Data = new byte[] {
            (byte) 0x41, (byte) 0x6e, (byte) 0x64, (byte) 0x72, (byte) 0x6f, (byte) 0x69, (byte) 0x64, (byte) 0x2e,
            (byte) 0x0a,
    };

    private static final byte[] SHA1withRSA_Vector1Signature = {
            (byte) 0x6d, (byte) 0x5b, (byte) 0xff, (byte) 0x68, (byte) 0xda, (byte) 0x18, (byte) 0x98, (byte) 0x72,
            (byte) 0x5c, (byte) 0x1f, (byte) 0x46, (byte) 0x51, (byte) 0x77, (byte) 0x15, (byte) 0x11, (byte) 0xcb,
            (byte) 0xe0, (byte) 0xb9, (byte) 0x3b, (byte) 0x7d, (byte) 0xf5, (byte) 0x96, (byte) 0x98, (byte) 0x24,
            (byte) 0x85, (byte) 0x9d, (byte) 0x3e, (byte) 0xed, (byte) 0x9b, (byte) 0xb2, (byte) 0x8a, (byte) 0x91,
            (byte) 0xfb, (byte) 0xf6, (byte) 0x85, (byte) 0x64, (byte) 0x74, (byte) 0x18, (byte) 0xb5, (byte) 0x1c,
            (byte) 0xb3, (byte) 0x8d, (byte) 0x99, (byte) 0x0d, (byte) 0xdf, (byte) 0xaa, (byte) 0xa6, (byte) 0xa1,
            (byte) 0xc3, (byte) 0xb6, (byte) 0x25, (byte) 0xb3, (byte) 0x06, (byte) 0xe0, (byte) 0xef, (byte) 0x28,
            (byte) 0xb0, (byte) 0x4d, (byte) 0x50, (byte) 0xc7, (byte) 0x75, (byte) 0x39, (byte) 0xb9, (byte) 0x2c,
            (byte) 0x47, (byte) 0xb5, (byte) 0xe2, (byte) 0x96, (byte) 0xf8, (byte) 0xf6, (byte) 0xcb, (byte) 0xa0,
            (byte) 0x58, (byte) 0xc9, (byte) 0x3e, (byte) 0xd5, (byte) 0xfc, (byte) 0x26, (byte) 0xd9, (byte) 0x55,
            (byte) 0x73, (byte) 0x39, (byte) 0x75, (byte) 0xb3, (byte) 0xb0, (byte) 0x0a, (byte) 0x5f, (byte) 0x5e,
            (byte) 0x3b, (byte) 0x4a, (byte) 0x2e, (byte) 0xb1, (byte) 0x0e, (byte) 0x7d, (byte) 0xe5, (byte) 0xcc,
            (byte) 0x04, (byte) 0x2c, (byte) 0xd1, (byte) 0x0a, (byte) 0x32, (byte) 0xaa, (byte) 0xd9, (byte) 0x8d,
            (byte) 0x1f, (byte) 0xcb, (byte) 0xe3, (byte) 0x7f, (byte) 0x63, (byte) 0x12, (byte) 0xb1, (byte) 0x98,
            (byte) 0x46, (byte) 0x46, (byte) 0x07, (byte) 0xd9, (byte) 0x49, (byte) 0xd2, (byte) 0xbf, (byte) 0xb5,
            (byte) 0xbc, (byte) 0xbb, (byte) 0xfd, (byte) 0x1c, (byte) 0xd7, (byte) 0x11, (byte) 0x94, (byte) 0xaa,
            (byte) 0x5f, (byte) 0x7b, (byte) 0xb2, (byte) 0x0c, (byte) 0x5d, (byte) 0x94, (byte) 0x53, (byte) 0x5e,
            (byte) 0x81, (byte) 0x5c, (byte) 0xbb, (byte) 0x1d, (byte) 0x4f, (byte) 0x30, (byte) 0xcd, (byte) 0xf8,
            (byte) 0xd7, (byte) 0xa5, (byte) 0xfa, (byte) 0x5e, (byte) 0xe0, (byte) 0x19, (byte) 0x3f, (byte) 0xa4,
            (byte) 0xaa, (byte) 0x56, (byte) 0x4e, (byte) 0xec, (byte) 0xeb, (byte) 0xee, (byte) 0xa2, (byte) 0x6c,
            (byte) 0xc9, (byte) 0x4f, (byte) 0xc2, (byte) 0xcc, (byte) 0x2a, (byte) 0xbc, (byte) 0x5b, (byte) 0x09,
            (byte) 0x10, (byte) 0x73, (byte) 0x61, (byte) 0x0c, (byte) 0x04, (byte) 0xb6, (byte) 0xb7, (byte) 0x2c,
            (byte) 0x37, (byte) 0xd2, (byte) 0xca, (byte) 0x2d, (byte) 0x54, (byte) 0xf2, (byte) 0xf7, (byte) 0x77,
            (byte) 0xe1, (byte) 0xba, (byte) 0x9f, (byte) 0x29, (byte) 0x07, (byte) 0xa2, (byte) 0x74, (byte) 0xc6,
            (byte) 0xe9, (byte) 0x1e, (byte) 0xde, (byte) 0xd7, (byte) 0x9c, (byte) 0x4b, (byte) 0xb7, (byte) 0x66,
            (byte) 0x52, (byte) 0xe8, (byte) 0xac, (byte) 0xf6, (byte) 0x76, (byte) 0xab, (byte) 0x16, (byte) 0x82,
            (byte) 0x96, (byte) 0x87, (byte) 0x40, (byte) 0x0f, (byte) 0xad, (byte) 0x2d, (byte) 0x46, (byte) 0xa6,
            (byte) 0x28, (byte) 0x04, (byte) 0x13, (byte) 0xc2, (byte) 0xce, (byte) 0x50, (byte) 0x56, (byte) 0x6d,
            (byte) 0xbe, (byte) 0x0c, (byte) 0x91, (byte) 0xd0, (byte) 0x8e, (byte) 0x80, (byte) 0x9e, (byte) 0x91,
            (byte) 0x8f, (byte) 0x62, (byte) 0xb3, (byte) 0x57, (byte) 0xd6, (byte) 0xae, (byte) 0x53, (byte) 0x91,
            (byte) 0x83, (byte) 0xe9, (byte) 0x38, (byte) 0x77, (byte) 0x8f, (byte) 0x20, (byte) 0xdd, (byte) 0x13,
            (byte) 0x7d, (byte) 0x15, (byte) 0x44, (byte) 0x7e, (byte) 0xb5, (byte) 0x00, (byte) 0xd6, (byte) 0x45,
    };

    private static final byte[] Vector2Data = new byte[] {
        (byte) 0x54, (byte) 0x68, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x69, (byte) 0x73, (byte) 0x20,
        (byte) 0x61, (byte) 0x20, (byte) 0x73, (byte) 0x69, (byte) 0x67, (byte) 0x6e, (byte) 0x65, (byte) 0x64,
        (byte) 0x20, (byte) 0x6d, (byte) 0x65, (byte) 0x73, (byte) 0x73, (byte) 0x61, (byte) 0x67, (byte) 0x65,
        (byte) 0x20, (byte) 0x66, (byte) 0x72, (byte) 0x6f, (byte) 0x6d, (byte) 0x20, (byte) 0x4b, (byte) 0x65,
        (byte) 0x6e, (byte) 0x6e, (byte) 0x79, (byte) 0x20, (byte) 0x52, (byte) 0x6f, (byte) 0x6f, (byte) 0x74,
        (byte) 0x2e, (byte) 0x0a,
    };

    private static final byte[] SHA1withRSA_Vector2Signature = new byte[] {
        (byte) 0x2e, (byte) 0xa6, (byte) 0x33, (byte) 0xd1, (byte) 0x9d, (byte) 0xfc, (byte) 0x4e, (byte) 0x27,
        (byte) 0xb3, (byte) 0xa8, (byte) 0x9a, (byte) 0xf2, (byte) 0x48, (byte) 0x62, (byte) 0x15, (byte) 0xa2,
        (byte) 0xce, (byte) 0x5f, (byte) 0x2b, (byte) 0x0e, (byte) 0xc5, (byte) 0x26, (byte) 0xba, (byte) 0xd9,
        (byte) 0x0f, (byte) 0x60, (byte) 0xeb, (byte) 0xf0, (byte) 0xd5, (byte) 0x5c, (byte) 0x6b, (byte) 0x23,
        (byte) 0x11, (byte) 0x95, (byte) 0xa4, (byte) 0xbd, (byte) 0x11, (byte) 0x68, (byte) 0xe7, (byte) 0x3a,
        (byte) 0x37, (byte) 0x3d, (byte) 0x79, (byte) 0xb8, (byte) 0x4f, (byte) 0xe9, (byte) 0xa1, (byte) 0x88,
        (byte) 0xfb, (byte) 0xa9, (byte) 0x8b, (byte) 0x34, (byte) 0xa1, (byte) 0xe0, (byte) 0xca, (byte) 0x11,
        (byte) 0xdd, (byte) 0xd0, (byte) 0x83, (byte) 0x7f, (byte) 0xc1, (byte) 0x0b, (byte) 0x16, (byte) 0x61,
        (byte) 0xac, (byte) 0x09, (byte) 0xa2, (byte) 0xdd, (byte) 0x40, (byte) 0x5b, (byte) 0x8c, (byte) 0x7a,
        (byte) 0xb2, (byte) 0xb4, (byte) 0x02, (byte) 0x7c, (byte) 0xd4, (byte) 0x9a, (byte) 0xe6, (byte) 0xa5,
        (byte) 0x1a, (byte) 0x27, (byte) 0x77, (byte) 0x70, (byte) 0xe3, (byte) 0xe3, (byte) 0x71, (byte) 0xc7,
        (byte) 0x59, (byte) 0xc7, (byte) 0x9f, (byte) 0xb8, (byte) 0xef, (byte) 0xe7, (byte) 0x15, (byte) 0x02,
        (byte) 0x0d, (byte) 0x70, (byte) 0xdc, (byte) 0x2c, (byte) 0xe9, (byte) 0xf7, (byte) 0x63, (byte) 0x2a,
        (byte) 0xb5, (byte) 0xee, (byte) 0x9f, (byte) 0x29, (byte) 0x56, (byte) 0x86, (byte) 0x99, (byte) 0xb3,
        (byte) 0x0f, (byte) 0xe5, (byte) 0x1f, (byte) 0x76, (byte) 0x22, (byte) 0x3b, (byte) 0x7f, (byte) 0xa9,
        (byte) 0x9e, (byte) 0xd4, (byte) 0xc4, (byte) 0x83, (byte) 0x5d, (byte) 0x57, (byte) 0xcc, (byte) 0x37,
        (byte) 0xcb, (byte) 0x9a, (byte) 0x9e, (byte) 0x73, (byte) 0x44, (byte) 0x93, (byte) 0xb4, (byte) 0xf1,
        (byte) 0x6b, (byte) 0x98, (byte) 0xa0, (byte) 0x57, (byte) 0xbb, (byte) 0x5e, (byte) 0x8f, (byte) 0x89,
        (byte) 0x5b, (byte) 0x97, (byte) 0x26, (byte) 0xe4, (byte) 0xd0, (byte) 0x51, (byte) 0x0a, (byte) 0x5a,
        (byte) 0xb7, (byte) 0x12, (byte) 0x1a, (byte) 0x6d, (byte) 0xb0, (byte) 0x79, (byte) 0x30, (byte) 0x51,
        (byte) 0x83, (byte) 0x2e, (byte) 0xe2, (byte) 0x7a, (byte) 0x67, (byte) 0x66, (byte) 0xd3, (byte) 0x95,
        (byte) 0xca, (byte) 0xfc, (byte) 0xcb, (byte) 0x92, (byte) 0x79, (byte) 0x32, (byte) 0x26, (byte) 0x86,
        (byte) 0xe1, (byte) 0x0d, (byte) 0xd8, (byte) 0x19, (byte) 0xfa, (byte) 0x65, (byte) 0x37, (byte) 0xc9,
        (byte) 0x4c, (byte) 0x2a, (byte) 0xe1, (byte) 0x42, (byte) 0xc7, (byte) 0xd4, (byte) 0xb7, (byte) 0xeb,
        (byte) 0x1f, (byte) 0xc3, (byte) 0x53, (byte) 0x64, (byte) 0x6f, (byte) 0x2b, (byte) 0x78, (byte) 0x18,
        (byte) 0x03, (byte) 0xda, (byte) 0x8d, (byte) 0x62, (byte) 0x24, (byte) 0x70, (byte) 0xab, (byte) 0xe6,
        (byte) 0x16, (byte) 0x13, (byte) 0x24, (byte) 0x6b, (byte) 0x5f, (byte) 0xd3, (byte) 0xec, (byte) 0xc1,
        (byte) 0x58, (byte) 0x64, (byte) 0xbd, (byte) 0x30, (byte) 0x98, (byte) 0x5e, (byte) 0x33, (byte) 0xce,
        (byte) 0x87, (byte) 0x64, (byte) 0x14, (byte) 0x07, (byte) 0x85, (byte) 0x43, (byte) 0x3e, (byte) 0x9f,
        (byte) 0x27, (byte) 0x9f, (byte) 0x63, (byte) 0x66, (byte) 0x9d, (byte) 0x26, (byte) 0x19, (byte) 0xc0,
        (byte) 0x02, (byte) 0x08, (byte) 0x15, (byte) 0xcb, (byte) 0xb4, (byte) 0xaa, (byte) 0x4a, (byte) 0xc8,
        (byte) 0xc0, (byte) 0x09, (byte) 0x15, (byte) 0x7d, (byte) 0x8a, (byte) 0x21, (byte) 0xbc, (byte) 0xa3,
    };

    private static final byte[] SHA256withRSA_Vector2Signature = new byte[] {
        (byte) 0x18, (byte) 0x6e, (byte) 0x31, (byte) 0x1f, (byte) 0x1d, (byte) 0x44, (byte) 0x09, (byte) 0x3e,
        (byte) 0xa0, (byte) 0xc4, (byte) 0x3d, (byte) 0xb4, (byte) 0x1b, (byte) 0xf2, (byte) 0xd8, (byte) 0xa4,
        (byte) 0x59, (byte) 0xab, (byte) 0xb5, (byte) 0x37, (byte) 0x28, (byte) 0xb8, (byte) 0x94, (byte) 0x6b,
        (byte) 0x6f, (byte) 0x13, (byte) 0x54, (byte) 0xff, (byte) 0xac, (byte) 0x15, (byte) 0x84, (byte) 0xd0,
        (byte) 0xc9, (byte) 0x15, (byte) 0x5b, (byte) 0x69, (byte) 0x05, (byte) 0xf1, (byte) 0x44, (byte) 0xfd,
        (byte) 0xde, (byte) 0xe8, (byte) 0xb4, (byte) 0x12, (byte) 0x59, (byte) 0x9e, (byte) 0x4c, (byte) 0x0b,
        (byte) 0xd5, (byte) 0x49, (byte) 0x33, (byte) 0x28, (byte) 0xe0, (byte) 0xcb, (byte) 0x87, (byte) 0x85,
        (byte) 0xd8, (byte) 0x18, (byte) 0x6f, (byte) 0xfe, (byte) 0xa2, (byte) 0x23, (byte) 0x82, (byte) 0xf0,
        (byte) 0xe5, (byte) 0x39, (byte) 0x1b, (byte) 0x8c, (byte) 0x93, (byte) 0x11, (byte) 0x49, (byte) 0x72,
        (byte) 0x2a, (byte) 0x5b, (byte) 0x25, (byte) 0xff, (byte) 0x4e, (byte) 0x88, (byte) 0x70, (byte) 0x9d,
        (byte) 0x9d, (byte) 0xff, (byte) 0xe2, (byte) 0xc0, (byte) 0x7e, (byte) 0xc8, (byte) 0x03, (byte) 0x40,
        (byte) 0xbe, (byte) 0x44, (byte) 0x09, (byte) 0xeb, (byte) 0x9e, (byte) 0x8e, (byte) 0x88, (byte) 0xe4,
        (byte) 0x98, (byte) 0x82, (byte) 0x06, (byte) 0xa4, (byte) 0x9d, (byte) 0x63, (byte) 0x88, (byte) 0x65,
        (byte) 0xa3, (byte) 0x8e, (byte) 0x0d, (byte) 0x22, (byte) 0xf3, (byte) 0x33, (byte) 0xf2, (byte) 0x40,
        (byte) 0xe8, (byte) 0x91, (byte) 0x67, (byte) 0x72, (byte) 0x29, (byte) 0x1c, (byte) 0x08, (byte) 0xff,
        (byte) 0x54, (byte) 0xa0, (byte) 0xcc, (byte) 0xad, (byte) 0x84, (byte) 0x88, (byte) 0x4b, (byte) 0x3b,
        (byte) 0xef, (byte) 0xf9, (byte) 0x5e, (byte) 0xb3, (byte) 0x41, (byte) 0x6a, (byte) 0xbd, (byte) 0x94,
        (byte) 0x16, (byte) 0x7d, (byte) 0x9d, (byte) 0x53, (byte) 0x77, (byte) 0xf1, (byte) 0x6a, (byte) 0x95,
        (byte) 0x57, (byte) 0xad, (byte) 0x65, (byte) 0x9d, (byte) 0x75, (byte) 0x95, (byte) 0xf6, (byte) 0x6a,
        (byte) 0xd2, (byte) 0x88, (byte) 0xea, (byte) 0x5b, (byte) 0xa2, (byte) 0x94, (byte) 0x8f, (byte) 0x5e,
        (byte) 0x84, (byte) 0x18, (byte) 0x19, (byte) 0x46, (byte) 0x83, (byte) 0x0b, (byte) 0x6d, (byte) 0x5b,
        (byte) 0xb9, (byte) 0xdb, (byte) 0xa4, (byte) 0xe5, (byte) 0x17, (byte) 0x02, (byte) 0x9e, (byte) 0x11,
        (byte) 0xed, (byte) 0xd9, (byte) 0x7b, (byte) 0x83, (byte) 0x87, (byte) 0x89, (byte) 0xf3, (byte) 0xe4,
        (byte) 0xbf, (byte) 0x0e, (byte) 0xe8, (byte) 0xdc, (byte) 0x55, (byte) 0x9c, (byte) 0xf7, (byte) 0xc9,
        (byte) 0xc3, (byte) 0xe2, (byte) 0x2c, (byte) 0xf7, (byte) 0x8c, (byte) 0xaa, (byte) 0x17, (byte) 0x1f,
        (byte) 0xd1, (byte) 0xc7, (byte) 0x74, (byte) 0xc7, (byte) 0x8e, (byte) 0x1c, (byte) 0x5b, (byte) 0xd2,
        (byte) 0x31, (byte) 0x74, (byte) 0x43, (byte) 0x9a, (byte) 0x52, (byte) 0xbf, (byte) 0x89, (byte) 0xc5,
        (byte) 0xb4, (byte) 0x80, (byte) 0x6a, (byte) 0x9e, (byte) 0x05, (byte) 0xdb, (byte) 0xbb, (byte) 0x07,
        (byte) 0x8c, (byte) 0x08, (byte) 0x61, (byte) 0xba, (byte) 0xa4, (byte) 0xbc, (byte) 0x80, (byte) 0x3a,
        (byte) 0xdd, (byte) 0x3b, (byte) 0x1a, (byte) 0x8c, (byte) 0x21, (byte) 0xd8, (byte) 0xa3, (byte) 0xc0,
        (byte) 0xc7, (byte) 0xd1, (byte) 0x08, (byte) 0xe1, (byte) 0x34, (byte) 0x99, (byte) 0xc0, (byte) 0xcf,
        (byte) 0x80, (byte) 0xff, (byte) 0xfa, (byte) 0x07, (byte) 0xef, (byte) 0x5c, (byte) 0x45, (byte) 0xe5,
    };

    private static final byte[] SHA384withRSA_Vector2Signature = new byte[] {
        (byte) 0xaf, (byte) 0xf7, (byte) 0x7a, (byte) 0xc2, (byte) 0xbb, (byte) 0xb8, (byte) 0xbd, (byte) 0xe3,
        (byte) 0x42, (byte) 0xaa, (byte) 0x16, (byte) 0x8a, (byte) 0x52, (byte) 0x6c, (byte) 0x99, (byte) 0x66,
        (byte) 0x08, (byte) 0xbe, (byte) 0x15, (byte) 0xd9, (byte) 0x7c, (byte) 0x60, (byte) 0x2c, (byte) 0xac,
        (byte) 0x4d, (byte) 0x4c, (byte) 0xf4, (byte) 0xdf, (byte) 0xbc, (byte) 0x16, (byte) 0x58, (byte) 0x0a,
        (byte) 0x4e, (byte) 0xde, (byte) 0x8d, (byte) 0xb3, (byte) 0xbd, (byte) 0x03, (byte) 0x4e, (byte) 0x23,
        (byte) 0x40, (byte) 0xa5, (byte) 0x80, (byte) 0xae, (byte) 0x83, (byte) 0xb4, (byte) 0x0f, (byte) 0x99,
        (byte) 0x44, (byte) 0xc3, (byte) 0x5e, (byte) 0xdb, (byte) 0x59, (byte) 0x1d, (byte) 0xea, (byte) 0x7b,
        (byte) 0x4d, (byte) 0xf3, (byte) 0xd2, (byte) 0xad, (byte) 0xbd, (byte) 0x21, (byte) 0x9f, (byte) 0x8e,
        (byte) 0x87, (byte) 0x8f, (byte) 0x12, (byte) 0x13, (byte) 0x33, (byte) 0xf1, (byte) 0xc0, (byte) 0x9d,
        (byte) 0xe7, (byte) 0xec, (byte) 0x6e, (byte) 0xad, (byte) 0xea, (byte) 0x5d, (byte) 0x69, (byte) 0xbb,
        (byte) 0xab, (byte) 0x5b, (byte) 0xd8, (byte) 0x55, (byte) 0x56, (byte) 0xc8, (byte) 0xda, (byte) 0x81,
        (byte) 0x41, (byte) 0xfb, (byte) 0xd3, (byte) 0x11, (byte) 0x6c, (byte) 0x97, (byte) 0xa7, (byte) 0xc3,
        (byte) 0xf1, (byte) 0x31, (byte) 0xbf, (byte) 0xbe, (byte) 0x3f, (byte) 0xdb, (byte) 0x35, (byte) 0x85,
        (byte) 0xb7, (byte) 0xb0, (byte) 0x75, (byte) 0x7f, (byte) 0xaf, (byte) 0xfb, (byte) 0x65, (byte) 0x61,
        (byte) 0xc7, (byte) 0x0e, (byte) 0x63, (byte) 0xb5, (byte) 0x7d, (byte) 0x95, (byte) 0xe9, (byte) 0x16,
        (byte) 0x9d, (byte) 0x6a, (byte) 0x00, (byte) 0x9f, (byte) 0x5e, (byte) 0xcd, (byte) 0xff, (byte) 0xa6,
        (byte) 0xbc, (byte) 0x71, (byte) 0xf2, (byte) 0x2c, (byte) 0xd3, (byte) 0x68, (byte) 0xb9, (byte) 0x3f,
        (byte) 0xaa, (byte) 0x06, (byte) 0xf1, (byte) 0x9c, (byte) 0x7e, (byte) 0xca, (byte) 0x4a, (byte) 0xfe,
        (byte) 0xb1, (byte) 0x73, (byte) 0x19, (byte) 0x80, (byte) 0x05, (byte) 0xa6, (byte) 0x85, (byte) 0x14,
        (byte) 0xda, (byte) 0x7a, (byte) 0x16, (byte) 0x7a, (byte) 0xc2, (byte) 0x46, (byte) 0x57, (byte) 0xa7,
        (byte) 0xc0, (byte) 0xbf, (byte) 0xcd, (byte) 0xdc, (byte) 0x2f, (byte) 0x64, (byte) 0xf6, (byte) 0x6d,
        (byte) 0xdc, (byte) 0xcb, (byte) 0x5a, (byte) 0x29, (byte) 0x95, (byte) 0x1c, (byte) 0xfe, (byte) 0xf2,
        (byte) 0xda, (byte) 0x7e, (byte) 0xcb, (byte) 0x26, (byte) 0x12, (byte) 0xc6, (byte) 0xb0, (byte) 0xba,
        (byte) 0x84, (byte) 0x9b, (byte) 0x4f, (byte) 0xba, (byte) 0x1b, (byte) 0x78, (byte) 0x25, (byte) 0xb8,
        (byte) 0x8f, (byte) 0x2e, (byte) 0x51, (byte) 0x5f, (byte) 0x9e, (byte) 0xfc, (byte) 0x40, (byte) 0xbc,
        (byte) 0x85, (byte) 0xcd, (byte) 0x86, (byte) 0x7f, (byte) 0x88, (byte) 0xc5, (byte) 0xaa, (byte) 0x2b,
        (byte) 0x78, (byte) 0xb1, (byte) 0x9c, (byte) 0x51, (byte) 0x9a, (byte) 0xe1, (byte) 0xe1, (byte) 0xc0,
        (byte) 0x40, (byte) 0x47, (byte) 0xcb, (byte) 0xa4, (byte) 0xb7, (byte) 0x6c, (byte) 0x31, (byte) 0xf2,
        (byte) 0xc8, (byte) 0x9a, (byte) 0xad, (byte) 0x0b, (byte) 0xd3, (byte) 0xf6, (byte) 0x85, (byte) 0x9a,
        (byte) 0x8f, (byte) 0x4f, (byte) 0xc9, (byte) 0xd8, (byte) 0x33, (byte) 0x7c, (byte) 0x45, (byte) 0x30,
        (byte) 0xea, (byte) 0x17, (byte) 0xd3, (byte) 0xe3, (byte) 0x90, (byte) 0x2c, (byte) 0xda, (byte) 0xde,
        (byte) 0x41, (byte) 0x17, (byte) 0x3f, (byte) 0x08, (byte) 0xb9, (byte) 0x34, (byte) 0xc0, (byte) 0xd1,
    };

    private static final byte[] SHA512withRSA_Vector2Signature = new byte[] {
        (byte) 0x19, (byte) 0xe2, (byte) 0xe5, (byte) 0xf3, (byte) 0x18, (byte) 0x83, (byte) 0xec, (byte) 0xf0,
        (byte) 0xab, (byte) 0x50, (byte) 0x05, (byte) 0x4b, (byte) 0x5f, (byte) 0x22, (byte) 0xfc, (byte) 0x82,
        (byte) 0x6d, (byte) 0xca, (byte) 0xe7, (byte) 0xbe, (byte) 0x23, (byte) 0x94, (byte) 0xfa, (byte) 0xf9,
        (byte) 0xa4, (byte) 0x8a, (byte) 0x95, (byte) 0x4d, (byte) 0x14, (byte) 0x08, (byte) 0x8b, (byte) 0x5e,
        (byte) 0x03, (byte) 0x1b, (byte) 0x74, (byte) 0xde, (byte) 0xc1, (byte) 0x45, (byte) 0x9c, (byte) 0xce,
        (byte) 0x1d, (byte) 0xac, (byte) 0xab, (byte) 0xd3, (byte) 0xa8, (byte) 0xc3, (byte) 0xca, (byte) 0x67,
        (byte) 0x80, (byte) 0xf6, (byte) 0x03, (byte) 0x46, (byte) 0x65, (byte) 0x77, (byte) 0x59, (byte) 0xbb,
        (byte) 0xb8, (byte) 0x83, (byte) 0xee, (byte) 0xc2, (byte) 0x3e, (byte) 0x78, (byte) 0xdd, (byte) 0x89,
        (byte) 0xcd, (byte) 0x9b, (byte) 0x78, (byte) 0x35, (byte) 0xa9, (byte) 0x09, (byte) 0xc8, (byte) 0x77,
        (byte) 0xdd, (byte) 0xd3, (byte) 0xa0, (byte) 0x64, (byte) 0xb0, (byte) 0x74, (byte) 0x48, (byte) 0x51,
        (byte) 0x4f, (byte) 0xa0, (byte) 0xae, (byte) 0x33, (byte) 0xb3, (byte) 0x28, (byte) 0xb0, (byte) 0xa8,
        (byte) 0x78, (byte) 0x8f, (byte) 0xa2, (byte) 0x32, (byte) 0xa6, (byte) 0x0a, (byte) 0xaa, (byte) 0x09,
        (byte) 0xb5, (byte) 0x8d, (byte) 0x4c, (byte) 0x44, (byte) 0x46, (byte) 0xb4, (byte) 0xd2, (byte) 0x06,
        (byte) 0x6b, (byte) 0x8c, (byte) 0x51, (byte) 0x6e, (byte) 0x9c, (byte) 0xfa, (byte) 0x1f, (byte) 0x94,
        (byte) 0x3e, (byte) 0x19, (byte) 0x9c, (byte) 0x63, (byte) 0xfe, (byte) 0xa9, (byte) 0x9a, (byte) 0xe3,
        (byte) 0x6c, (byte) 0x82, (byte) 0x64, (byte) 0x5f, (byte) 0xca, (byte) 0xc2, (byte) 0x8d, (byte) 0x66,
        (byte) 0xbe, (byte) 0x12, (byte) 0x6e, (byte) 0xb6, (byte) 0x35, (byte) 0x6d, (byte) 0xaa, (byte) 0xed,
        (byte) 0x4b, (byte) 0x50, (byte) 0x08, (byte) 0x1c, (byte) 0xbf, (byte) 0x07, (byte) 0x70, (byte) 0x78,
        (byte) 0xc0, (byte) 0xbb, (byte) 0xc5, (byte) 0x8d, (byte) 0x6c, (byte) 0x8d, (byte) 0x35, (byte) 0xff,
        (byte) 0x04, (byte) 0x81, (byte) 0xd8, (byte) 0xf4, (byte) 0xd2, (byte) 0x4a, (byte) 0xc3, (byte) 0x05,
        (byte) 0x23, (byte) 0xcb, (byte) 0xeb, (byte) 0x20, (byte) 0xb1, (byte) 0xd4, (byte) 0x2d, (byte) 0xd8,
        (byte) 0x7a, (byte) 0xd4, (byte) 0x7e, (byte) 0xf6, (byte) 0xa9, (byte) 0xe8, (byte) 0x72, (byte) 0x69,
        (byte) 0xfe, (byte) 0xab, (byte) 0x54, (byte) 0x4d, (byte) 0xd1, (byte) 0xf4, (byte) 0x6b, (byte) 0x83,
        (byte) 0x31, (byte) 0x17, (byte) 0xed, (byte) 0x26, (byte) 0xe9, (byte) 0xd2, (byte) 0x5b, (byte) 0xad,
        (byte) 0x42, (byte) 0x42, (byte) 0xa5, (byte) 0x8f, (byte) 0x98, (byte) 0x7c, (byte) 0x1b, (byte) 0x5c,
        (byte) 0x8e, (byte) 0x88, (byte) 0x56, (byte) 0x20, (byte) 0x8e, (byte) 0x48, (byte) 0xf9, (byte) 0x4d,
        (byte) 0x82, (byte) 0x91, (byte) 0xcb, (byte) 0xc8, (byte) 0x1c, (byte) 0x7c, (byte) 0xa5, (byte) 0x69,
        (byte) 0x1b, (byte) 0x40, (byte) 0xc2, (byte) 0x4c, (byte) 0x25, (byte) 0x16, (byte) 0x4f, (byte) 0xfa,
        (byte) 0x09, (byte) 0xeb, (byte) 0xf5, (byte) 0x6c, (byte) 0x55, (byte) 0x3c, (byte) 0x6e, (byte) 0xf7,
        (byte) 0xc0, (byte) 0xc1, (byte) 0x34, (byte) 0xd1, (byte) 0x53, (byte) 0xa3, (byte) 0x69, (byte) 0x64,
        (byte) 0xee, (byte) 0xf4, (byte) 0xf9, (byte) 0xc7, (byte) 0x96, (byte) 0x60, (byte) 0x84, (byte) 0x87,
        (byte) 0xb4, (byte) 0xc7, (byte) 0x3c, (byte) 0x26, (byte) 0xa7, (byte) 0x3a, (byte) 0xbf, (byte) 0x95,
    };

    private static final byte[] MD5withRSA_Vector2Signature = new byte[] {
        (byte) 0x04, (byte) 0x17, (byte) 0x83, (byte) 0x10, (byte) 0xe2, (byte) 0x6e, (byte) 0xdf, (byte) 0xa9,
        (byte) 0xae, (byte) 0xd2, (byte) 0xdc, (byte) 0x5f, (byte) 0x70, (byte) 0x1d, (byte) 0xaf, (byte) 0x54,
        (byte) 0xc0, (byte) 0x5f, (byte) 0x0b, (byte) 0x2c, (byte) 0xe6, (byte) 0xd0, (byte) 0x00, (byte) 0x18,
        (byte) 0x4c, (byte) 0xf6, (byte) 0x8f, (byte) 0x18, (byte) 0x10, (byte) 0x74, (byte) 0x90, (byte) 0x99,
        (byte) 0xa9, (byte) 0x90, (byte) 0x3c, (byte) 0x5a, (byte) 0x38, (byte) 0xd3, (byte) 0x3d, (byte) 0x48,
        (byte) 0xcf, (byte) 0x31, (byte) 0xaf, (byte) 0x12, (byte) 0x98, (byte) 0xfb, (byte) 0x66, (byte) 0xe8,
        (byte) 0x58, (byte) 0xec, (byte) 0xca, (byte) 0xe1, (byte) 0x42, (byte) 0xf9, (byte) 0x84, (byte) 0x17,
        (byte) 0x6f, (byte) 0x4c, (byte) 0x3e, (byte) 0xc4, (byte) 0x40, (byte) 0xc6, (byte) 0x70, (byte) 0xb0,
        (byte) 0x38, (byte) 0xf3, (byte) 0x47, (byte) 0xeb, (byte) 0x6f, (byte) 0xcb, (byte) 0xea, (byte) 0x21,
        (byte) 0x41, (byte) 0xf3, (byte) 0xa0, (byte) 0x3e, (byte) 0x42, (byte) 0xad, (byte) 0xa5, (byte) 0xad,
        (byte) 0x5d, (byte) 0x2c, (byte) 0x1a, (byte) 0x8e, (byte) 0x3e, (byte) 0xb3, (byte) 0xa5, (byte) 0x78,
        (byte) 0x3d, (byte) 0x56, (byte) 0x09, (byte) 0x93, (byte) 0xc9, (byte) 0x93, (byte) 0xd3, (byte) 0xd2,
        (byte) 0x9a, (byte) 0xc5, (byte) 0xa5, (byte) 0x2e, (byte) 0xb2, (byte) 0xd8, (byte) 0x37, (byte) 0xc7,
        (byte) 0x13, (byte) 0x1a, (byte) 0x0b, (byte) 0xda, (byte) 0x50, (byte) 0x28, (byte) 0x6d, (byte) 0x47,
        (byte) 0x65, (byte) 0x52, (byte) 0xcd, (byte) 0xe7, (byte) 0xec, (byte) 0x57, (byte) 0x00, (byte) 0x41,
        (byte) 0x34, (byte) 0x28, (byte) 0xb9, (byte) 0x8b, (byte) 0x03, (byte) 0x41, (byte) 0xb6, (byte) 0xd5,
        (byte) 0xa8, (byte) 0xef, (byte) 0xd3, (byte) 0xdd, (byte) 0x80, (byte) 0xd5, (byte) 0x69, (byte) 0xe4,
        (byte) 0xf0, (byte) 0x4d, (byte) 0xa4, (byte) 0x7d, (byte) 0x60, (byte) 0x2f, (byte) 0xef, (byte) 0x79,
        (byte) 0x07, (byte) 0x75, (byte) 0xeb, (byte) 0xf7, (byte) 0x4b, (byte) 0x43, (byte) 0x41, (byte) 0xdb,
        (byte) 0x33, (byte) 0xad, (byte) 0x9c, (byte) 0x7b, (byte) 0x78, (byte) 0x83, (byte) 0x34, (byte) 0x77,
        (byte) 0xe4, (byte) 0x80, (byte) 0xbe, (byte) 0xe6, (byte) 0x6f, (byte) 0xdd, (byte) 0xac, (byte) 0xa5,
        (byte) 0x37, (byte) 0xcf, (byte) 0xb5, (byte) 0x44, (byte) 0x11, (byte) 0x77, (byte) 0x96, (byte) 0x45,
        (byte) 0xf9, (byte) 0xae, (byte) 0x48, (byte) 0xa6, (byte) 0xbe, (byte) 0x30, (byte) 0x32, (byte) 0xeb,
        (byte) 0x43, (byte) 0x6f, (byte) 0x66, (byte) 0x39, (byte) 0x57, (byte) 0xf8, (byte) 0xe6, (byte) 0x60,
        (byte) 0x31, (byte) 0xd0, (byte) 0xfc, (byte) 0xcf, (byte) 0x9f, (byte) 0xe5, (byte) 0x3d, (byte) 0xcf,
        (byte) 0xbd, (byte) 0x7b, (byte) 0x13, (byte) 0x20, (byte) 0xce, (byte) 0x11, (byte) 0xfd, (byte) 0xe5,
        (byte) 0xff, (byte) 0x90, (byte) 0x85, (byte) 0xdf, (byte) 0xca, (byte) 0x3d, (byte) 0xd9, (byte) 0x44,
        (byte) 0x16, (byte) 0xc2, (byte) 0x32, (byte) 0x28, (byte) 0xc7, (byte) 0x01, (byte) 0x6d, (byte) 0xea,
        (byte) 0xcb, (byte) 0x0d, (byte) 0x85, (byte) 0x08, (byte) 0x6f, (byte) 0xcb, (byte) 0x41, (byte) 0x6a,
        (byte) 0x3c, (byte) 0x0f, (byte) 0x3d, (byte) 0x38, (byte) 0xb5, (byte) 0x61, (byte) 0xc5, (byte) 0x64,
        (byte) 0x64, (byte) 0x81, (byte) 0x4c, (byte) 0xcd, (byte) 0xd1, (byte) 0x6a, (byte) 0x87, (byte) 0x28,
        (byte) 0x02, (byte) 0xaf, (byte) 0x8f, (byte) 0x59, (byte) 0xe5, (byte) 0x67, (byte) 0x25, (byte) 0x00,
    };

    public void testGetCommonInstances_Success() throws Exception {
        assertNotNull(OpenSSLSignature.getInstance("SHA1withRSA"));
        assertNotNull(OpenSSLSignature.getInstance("SHA256withRSA"));
        assertNotNull(OpenSSLSignature.getInstance("SHA384withRSA"));
        assertNotNull(OpenSSLSignature.getInstance("SHA512withRSA"));
        assertNotNull(OpenSSLSignature.getInstance("MD5withRSA"));
        assertNotNull(OpenSSLSignature.getInstance("SHA1withDSA"));
    }

    public void testVerify_SHA1withRSA_Key_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus, RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA1withRSA");
        sig.initVerify(pubKey);
        sig.update(Vector1Data);

        assertTrue("Signature must match expected signature",
                sig.verify(SHA1withRSA_Vector1Signature));
    }

    public void testVerify_SHA256withRSA_Key_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus, RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA256withRSA");
        sig.initVerify(pubKey);
        sig.update(Vector2Data);

        assertTrue("Signature must match expected signature",
                sig.verify(SHA256withRSA_Vector2Signature));
    }

    public void testVerify_SHA384withRSA_Key_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus, RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA384withRSA");
        sig.initVerify(pubKey);
        sig.update(Vector2Data);

        assertTrue("Signature must match expected signature",
                sig.verify(SHA384withRSA_Vector2Signature));
    }

    public void testVerify_SHA512withRSA_Key_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus, RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA512withRSA");
        sig.initVerify(pubKey);
        sig.update(Vector2Data);

        assertTrue("Signature must match expected signature",
                sig.verify(SHA512withRSA_Vector2Signature));
    }

    public void testVerify_MD5withRSA_Key_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus, RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("MD5withRSA");
        sig.initVerify(pubKey);
        sig.update(Vector2Data);

        assertTrue("Signature must match expected signature",
                sig.verify(MD5withRSA_Vector2Signature));
    }

    public void testVerify_SHA1withRSA_Key_InitSignThenInitVerify_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);

        RSAPrivateKeySpec privKeySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);
        PrivateKey privKey = kf.generatePrivate(privKeySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA1withRSA");

        // Start a signing operation
        sig.initSign(privKey);
        sig.update(Vector2Data);

        // Switch to verify
        sig.initVerify(pubKey);
        sig.update(Vector1Data);

        assertTrue("Signature must match expected signature",
                sig.verify(SHA1withRSA_Vector1Signature));
    }

    public void testVerify_SHA1withRSA_Key_TwoMessages_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus, RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA1withRSA");
        sig.initVerify(pubKey);

        sig.update(Vector1Data);
        assertTrue("First signature must match expected signature",
                sig.verify(SHA1withRSA_Vector1Signature));

        sig.update(Vector2Data);
        assertTrue("Second signature must match expected signature",
                sig.verify(SHA1withRSA_Vector2Signature));
    }

    public void testVerify_SHA1withRSA_Key_WrongExpectedSignature_Failure() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(RSA_2048_modulus, RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA1withRSA");
        sig.initVerify(pubKey);
        sig.update(Vector1Data);

        assertFalse("Signature should fail to verify", sig.verify(SHA1withRSA_Vector2Signature));
    }

    public void testSign_SHA1withRSA_CrtKeyWithPublicExponent_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent, RSA_2048_privateExponent, null, null, null, null, null);
        PrivateKey privKey = kf.generatePrivate(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA1withRSA");
        sig.initSign(privKey);
        sig.update(Vector1Data);

        byte[] signature = sig.sign();
        assertNotNull("Signature must not be null", signature);
        assertTrue("Signature should match expected",
                Arrays.equals(signature, SHA1withRSA_Vector1Signature));

        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);
        sig.initVerify(pubKey);
        sig.update(Vector1Data);
        assertTrue("Signature must verify correctly", sig.verify(signature));
    }

    public void testSign_SHA1withRSA_CrtKey_NoPrivateExponent_Failure() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent, null, RSA_2048_primeP, RSA_2048_primeQ, null, null, null);
        PrivateKey privKey = kf.generatePrivate(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA1withRSA");

        try {
            sig.initSign(privKey);
            fail("Should throw error when private exponent is not available");
        } catch (InvalidKeyException e) {
            // success
        }
    }

    public void testSign_SHA1withRSA_CrtKey_NoModulus_Failure() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(null, RSA_2048_publicExponent,
                RSA_2048_privateExponent, RSA_2048_primeP, RSA_2048_primeQ, null, null, null);
        PrivateKey privKey = kf.generatePrivate(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA1withRSA");

        try {
            sig.initSign(privKey);
            fail("Should throw error when modulus is not available");
        } catch (InvalidKeyException e) {
            // success
        }
    }

    public void testSign_SHA1withRSA_Key_EmptyKey_Failure() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(null, null);
        PrivateKey privKey = kf.generatePrivate(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA1withRSA");

        try {
            sig.initSign(privKey);
            fail("Should throw error when key is empty");
        } catch (InvalidKeyException e) {
            // success
        }
    }

    public void testSign_SHA1withRSA_Key_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);
        PrivateKey privKey = kf.generatePrivate(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA1withRSA");
        sig.initSign(privKey);
        sig.update(Vector1Data);

        byte[] signature = sig.sign();
        assertNotNull("Signature must not be null", signature);
        assertTrue("Signature should match expected",
                Arrays.equals(signature, SHA1withRSA_Vector1Signature));

        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);
        sig.initVerify(pubKey);
        sig.update(Vector1Data);
        assertTrue("Signature must verify correctly", sig.verify(signature));
    }

    public void testSign_SHA256withRSA_Key_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);
        PrivateKey privKey = kf.generatePrivate(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA256withRSA");
        sig.initSign(privKey);
        sig.update(Vector2Data);

        byte[] signature = sig.sign();
        assertNotNull("Signature must not be null", signature);
        assertTrue("Signature should match expected",
                Arrays.equals(signature, SHA256withRSA_Vector2Signature));

        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);
        sig.initVerify(pubKey);
        sig.update(Vector2Data);
        assertTrue("Signature must verify correctly", sig.verify(signature));
    }

    public void testSign_SHA384withRSA_Key_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);
        PrivateKey privKey = kf.generatePrivate(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA384withRSA");
        sig.initSign(privKey);
        sig.update(Vector2Data);

        byte[] signature = sig.sign();
        assertNotNull("Signature must not be null", signature);
        assertTrue("Signature should match expected",
                Arrays.equals(signature, SHA384withRSA_Vector2Signature));

        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);
        sig.initVerify(pubKey);
        sig.update(Vector2Data);
        assertTrue("Signature must verify correctly", sig.verify(signature));
    }

    public void testSign_SHA512withRSA_Key_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);
        PrivateKey privKey = kf.generatePrivate(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA512withRSA");
        sig.initSign(privKey);
        sig.update(Vector2Data);

        byte[] signature = sig.sign();
        assertNotNull("Signature must not be null", signature);
        assertTrue("Signature should match expected",
                Arrays.equals(signature, SHA512withRSA_Vector2Signature));

        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);
        sig.initVerify(pubKey);
        sig.update(Vector2Data);
        assertTrue("Signature must verify correctly", sig.verify(signature));
    }

    public void testSign_MD5withRSA_Key_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSA_2048_modulus,
                RSA_2048_privateExponent);
        PrivateKey privKey = kf.generatePrivate(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("MD5withRSA");
        sig.initSign(privKey);
        sig.update(Vector2Data);

        byte[] signature = sig.sign();
        assertNotNull("Signature must not be null", signature);
        assertTrue("Signature should match expected",
                Arrays.equals(signature, MD5withRSA_Vector2Signature));

        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(RSA_2048_modulus,
                RSA_2048_publicExponent);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);
        sig.initVerify(pubKey);
        sig.update(Vector2Data);
        assertTrue("Signature must verify correctly", sig.verify(signature));
    }

    /*
     * These tests were generated with this DSA private key:
     *
     * -----BEGIN DSA PRIVATE KEY-----
     * MIIBugIBAAKBgQCeYcKJ73epThNnZB8JAf4kE1Pgt5CoTnb+iYJ/esU8TgwgVTCV
     * QoXhQH0njwcN6NyZ77MHlDTWfP+cvmnT60Q3UO9J+OJb2NEQhJfq46UcwE5pynA9
     * eLkW5f5hXYpasyxhtgE70AF8Mo3h82kOi1jGzwCU+EkqS+raAP9L0L5AIwIVAL/u
     * qg8SNFBy+GAT2PFBARClL1dfAoGAd9R6EsyBfn7rOvvmhm1aEB2tqU+5A10hGuQw
     * lXWOzV7RvQpF7uf3a2UCYNAurz28B90rjjPAk4DZK6dxV3a8jrng1/QjjUEal08s
     * G9VLZuj60lANF6s0MT2kiNiOqKduFwO3D2h8ZHuSuGPkmmcYgSfUCxNI031O9qiP
     * VhctCFECgYAz7i1DhjRGUkCdYQd5tVaI42lhXOV71MTYPbuFOIxTL/hny7Z0PZWR
     * A1blmYE6vrArDEhzpmRvDJZSIMzMfJjUIGu1KO73zpo9siK0xY0/sw5r3QC9txP2
     * 2Mv3BUIl5TLrs9outQJ0VMwldY2fElgCLWcSVkH44qZwWir1cq+cIwIUEGPDardb
     * pNvWlWgTDD6a6ZTby+M=
     * -----END DSA PRIVATE KEY-----
     *
     */

    private static final BigInteger DSA_priv = new BigInteger(new byte[] {
        (byte) 0x10, (byte) 0x63, (byte) 0xc3, (byte) 0x6a, (byte) 0xb7, (byte) 0x5b, (byte) 0xa4, (byte) 0xdb,
        (byte) 0xd6, (byte) 0x95, (byte) 0x68, (byte) 0x13, (byte) 0x0c, (byte) 0x3e, (byte) 0x9a, (byte) 0xe9,
        (byte) 0x94, (byte) 0xdb, (byte) 0xcb, (byte) 0xe3,
    });

    private static final BigInteger DSA_pub = new BigInteger(new byte[] {
        (byte) 0x33, (byte) 0xee, (byte) 0x2d, (byte) 0x43, (byte) 0x86, (byte) 0x34, (byte) 0x46, (byte) 0x52,
        (byte) 0x40, (byte) 0x9d, (byte) 0x61, (byte) 0x07, (byte) 0x79, (byte) 0xb5, (byte) 0x56, (byte) 0x88,
        (byte) 0xe3, (byte) 0x69, (byte) 0x61, (byte) 0x5c, (byte) 0xe5, (byte) 0x7b, (byte) 0xd4, (byte) 0xc4,
        (byte) 0xd8, (byte) 0x3d, (byte) 0xbb, (byte) 0x85, (byte) 0x38, (byte) 0x8c, (byte) 0x53, (byte) 0x2f,
        (byte) 0xf8, (byte) 0x67, (byte) 0xcb, (byte) 0xb6, (byte) 0x74, (byte) 0x3d, (byte) 0x95, (byte) 0x91,
        (byte) 0x03, (byte) 0x56, (byte) 0xe5, (byte) 0x99, (byte) 0x81, (byte) 0x3a, (byte) 0xbe, (byte) 0xb0,
        (byte) 0x2b, (byte) 0x0c, (byte) 0x48, (byte) 0x73, (byte) 0xa6, (byte) 0x64, (byte) 0x6f, (byte) 0x0c,
        (byte) 0x96, (byte) 0x52, (byte) 0x20, (byte) 0xcc, (byte) 0xcc, (byte) 0x7c, (byte) 0x98, (byte) 0xd4,
        (byte) 0x20, (byte) 0x6b, (byte) 0xb5, (byte) 0x28, (byte) 0xee, (byte) 0xf7, (byte) 0xce, (byte) 0x9a,
        (byte) 0x3d, (byte) 0xb2, (byte) 0x22, (byte) 0xb4, (byte) 0xc5, (byte) 0x8d, (byte) 0x3f, (byte) 0xb3,
        (byte) 0x0e, (byte) 0x6b, (byte) 0xdd, (byte) 0x00, (byte) 0xbd, (byte) 0xb7, (byte) 0x13, (byte) 0xf6,
        (byte) 0xd8, (byte) 0xcb, (byte) 0xf7, (byte) 0x05, (byte) 0x42, (byte) 0x25, (byte) 0xe5, (byte) 0x32,
        (byte) 0xeb, (byte) 0xb3, (byte) 0xda, (byte) 0x2e, (byte) 0xb5, (byte) 0x02, (byte) 0x74, (byte) 0x54,
        (byte) 0xcc, (byte) 0x25, (byte) 0x75, (byte) 0x8d, (byte) 0x9f, (byte) 0x12, (byte) 0x58, (byte) 0x02,
        (byte) 0x2d, (byte) 0x67, (byte) 0x12, (byte) 0x56, (byte) 0x41, (byte) 0xf8, (byte) 0xe2, (byte) 0xa6,
        (byte) 0x70, (byte) 0x5a, (byte) 0x2a, (byte) 0xf5, (byte) 0x72, (byte) 0xaf, (byte) 0x9c, (byte) 0x23,
    });

    private static final BigInteger DSA_P = new BigInteger(new byte[] {
        (byte) 0x00, (byte) 0x9e, (byte) 0x61, (byte) 0xc2, (byte) 0x89, (byte) 0xef, (byte) 0x77, (byte) 0xa9,
        (byte) 0x4e, (byte) 0x13, (byte) 0x67, (byte) 0x64, (byte) 0x1f, (byte) 0x09, (byte) 0x01, (byte) 0xfe,
        (byte) 0x24, (byte) 0x13, (byte) 0x53, (byte) 0xe0, (byte) 0xb7, (byte) 0x90, (byte) 0xa8, (byte) 0x4e,
        (byte) 0x76, (byte) 0xfe, (byte) 0x89, (byte) 0x82, (byte) 0x7f, (byte) 0x7a, (byte) 0xc5, (byte) 0x3c,
        (byte) 0x4e, (byte) 0x0c, (byte) 0x20, (byte) 0x55, (byte) 0x30, (byte) 0x95, (byte) 0x42, (byte) 0x85,
        (byte) 0xe1, (byte) 0x40, (byte) 0x7d, (byte) 0x27, (byte) 0x8f, (byte) 0x07, (byte) 0x0d, (byte) 0xe8,
        (byte) 0xdc, (byte) 0x99, (byte) 0xef, (byte) 0xb3, (byte) 0x07, (byte) 0x94, (byte) 0x34, (byte) 0xd6,
        (byte) 0x7c, (byte) 0xff, (byte) 0x9c, (byte) 0xbe, (byte) 0x69, (byte) 0xd3, (byte) 0xeb, (byte) 0x44,
        (byte) 0x37, (byte) 0x50, (byte) 0xef, (byte) 0x49, (byte) 0xf8, (byte) 0xe2, (byte) 0x5b, (byte) 0xd8,
        (byte) 0xd1, (byte) 0x10, (byte) 0x84, (byte) 0x97, (byte) 0xea, (byte) 0xe3, (byte) 0xa5, (byte) 0x1c,
        (byte) 0xc0, (byte) 0x4e, (byte) 0x69, (byte) 0xca, (byte) 0x70, (byte) 0x3d, (byte) 0x78, (byte) 0xb9,
        (byte) 0x16, (byte) 0xe5, (byte) 0xfe, (byte) 0x61, (byte) 0x5d, (byte) 0x8a, (byte) 0x5a, (byte) 0xb3,
        (byte) 0x2c, (byte) 0x61, (byte) 0xb6, (byte) 0x01, (byte) 0x3b, (byte) 0xd0, (byte) 0x01, (byte) 0x7c,
        (byte) 0x32, (byte) 0x8d, (byte) 0xe1, (byte) 0xf3, (byte) 0x69, (byte) 0x0e, (byte) 0x8b, (byte) 0x58,
        (byte) 0xc6, (byte) 0xcf, (byte) 0x00, (byte) 0x94, (byte) 0xf8, (byte) 0x49, (byte) 0x2a, (byte) 0x4b,
        (byte) 0xea, (byte) 0xda, (byte) 0x00, (byte) 0xff, (byte) 0x4b, (byte) 0xd0, (byte) 0xbe, (byte) 0x40,
        (byte) 0x23,
    });

    private static final BigInteger DSA_Q = new BigInteger(new byte[] {
        (byte) 0x00, (byte) 0xbf, (byte) 0xee, (byte) 0xaa, (byte) 0x0f, (byte) 0x12, (byte) 0x34, (byte) 0x50,
        (byte) 0x72, (byte) 0xf8, (byte) 0x60, (byte) 0x13, (byte) 0xd8, (byte) 0xf1, (byte) 0x41, (byte) 0x01,
        (byte) 0x10, (byte) 0xa5, (byte) 0x2f, (byte) 0x57, (byte) 0x5f,
    });

    private static final BigInteger DSA_G = new BigInteger(new byte[] {
        (byte) 0x77, (byte) 0xd4, (byte) 0x7a, (byte) 0x12, (byte) 0xcc, (byte) 0x81, (byte) 0x7e, (byte) 0x7e,
        (byte) 0xeb, (byte) 0x3a, (byte) 0xfb, (byte) 0xe6, (byte) 0x86, (byte) 0x6d, (byte) 0x5a, (byte) 0x10,
        (byte) 0x1d, (byte) 0xad, (byte) 0xa9, (byte) 0x4f, (byte) 0xb9, (byte) 0x03, (byte) 0x5d, (byte) 0x21,
        (byte) 0x1a, (byte) 0xe4, (byte) 0x30, (byte) 0x95, (byte) 0x75, (byte) 0x8e, (byte) 0xcd, (byte) 0x5e,
        (byte) 0xd1, (byte) 0xbd, (byte) 0x0a, (byte) 0x45, (byte) 0xee, (byte) 0xe7, (byte) 0xf7, (byte) 0x6b,
        (byte) 0x65, (byte) 0x02, (byte) 0x60, (byte) 0xd0, (byte) 0x2e, (byte) 0xaf, (byte) 0x3d, (byte) 0xbc,
        (byte) 0x07, (byte) 0xdd, (byte) 0x2b, (byte) 0x8e, (byte) 0x33, (byte) 0xc0, (byte) 0x93, (byte) 0x80,
        (byte) 0xd9, (byte) 0x2b, (byte) 0xa7, (byte) 0x71, (byte) 0x57, (byte) 0x76, (byte) 0xbc, (byte) 0x8e,
        (byte) 0xb9, (byte) 0xe0, (byte) 0xd7, (byte) 0xf4, (byte) 0x23, (byte) 0x8d, (byte) 0x41, (byte) 0x1a,
        (byte) 0x97, (byte) 0x4f, (byte) 0x2c, (byte) 0x1b, (byte) 0xd5, (byte) 0x4b, (byte) 0x66, (byte) 0xe8,
        (byte) 0xfa, (byte) 0xd2, (byte) 0x50, (byte) 0x0d, (byte) 0x17, (byte) 0xab, (byte) 0x34, (byte) 0x31,
        (byte) 0x3d, (byte) 0xa4, (byte) 0x88, (byte) 0xd8, (byte) 0x8e, (byte) 0xa8, (byte) 0xa7, (byte) 0x6e,
        (byte) 0x17, (byte) 0x03, (byte) 0xb7, (byte) 0x0f, (byte) 0x68, (byte) 0x7c, (byte) 0x64, (byte) 0x7b,
        (byte) 0x92, (byte) 0xb8, (byte) 0x63, (byte) 0xe4, (byte) 0x9a, (byte) 0x67, (byte) 0x18, (byte) 0x81,
        (byte) 0x27, (byte) 0xd4, (byte) 0x0b, (byte) 0x13, (byte) 0x48, (byte) 0xd3, (byte) 0x7d, (byte) 0x4e,
        (byte) 0xf6, (byte) 0xa8, (byte) 0x8f, (byte) 0x56, (byte) 0x17, (byte) 0x2d, (byte) 0x08, (byte) 0x51,
    });

    /**
     * A possible signature using SHA1withDSA of Vector2Data. Note that DSS is
     * randomized, so this won't be the exact signature you'll get out of
     * another signing operation unless you use a fixed RNG.
     */
    public static final byte[] SHA1withDSA_Vector2Signature = new byte[] {
        (byte) 0x30, (byte) 0x2d, (byte) 0x02, (byte) 0x15, (byte) 0x00, (byte) 0x88, (byte) 0xef, (byte) 0xac,
        (byte) 0x2b, (byte) 0x8b, (byte) 0xe2, (byte) 0x61, (byte) 0xc6, (byte) 0x2b, (byte) 0xea, (byte) 0xd5,
        (byte) 0x96, (byte) 0xbc, (byte) 0xb0, (byte) 0xa1, (byte) 0x30, (byte) 0x0c, (byte) 0x1f, (byte) 0xed,
        (byte) 0x11, (byte) 0x02, (byte) 0x14, (byte) 0x15, (byte) 0xc4, (byte) 0xfc, (byte) 0x82, (byte) 0x6f,
        (byte) 0x17, (byte) 0xdc, (byte) 0x87, (byte) 0x82, (byte) 0x75, (byte) 0x23, (byte) 0xd4, (byte) 0x58,
        (byte) 0xdc, (byte) 0x73, (byte) 0x3d, (byte) 0xf3, (byte) 0x51, (byte) 0xc0, (byte) 0x57,
    };

    public void testSign_SHA1withDSA_Key_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("DSA");
        DSAPrivateKeySpec keySpec = new DSAPrivateKeySpec(DSA_priv, DSA_P, DSA_Q, DSA_G);
        PrivateKey privKey = kf.generatePrivate(keySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA1withDSA");
        sig.initSign(privKey);
        sig.update(Vector2Data);

        byte[] signature = sig.sign();
        assertNotNull("Signature must not be null", signature);

        DSAPublicKeySpec pubKeySpec = new DSAPublicKeySpec(DSA_pub, DSA_P, DSA_Q, DSA_G);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);
        sig.initVerify(pubKey);
        sig.update(Vector2Data);
        assertTrue("Signature must verify correctly", sig.verify(signature));
    }

    public void testVerify_SHA1withDSA_Key_Success() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("DSA");
        DSAPublicKeySpec pubKeySpec = new DSAPublicKeySpec(DSA_pub, DSA_P, DSA_Q, DSA_G);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);

        OpenSSLSignature sig = OpenSSLSignature.getInstance("SHA1withDSA");
        sig.initVerify(pubKey);
        sig.update(Vector2Data);
        assertTrue("Signature must verify correctly", sig.verify(SHA1withDSA_Vector2Signature));
    }
}
