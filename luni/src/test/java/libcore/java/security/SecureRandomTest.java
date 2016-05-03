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

package libcore.java.security;

import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Set;

import junit.framework.TestCase;

import dalvik.system.VMRuntime;

public class SecureRandomTest extends TestCase {
    private static final String EXPECTED_PROVIDER = "com.android.org.conscrypt.OpenSSLProvider";

    private static final byte[] STATIC_SEED_BYTES = new byte[] {
            0x0A, (byte) 0xA0, 0x01, 0x10, (byte) 0xFF, (byte) 0xF0, 0x0F
    };

    private static final long STATIC_SEED_LONG = 8506210602917522860L;

    public void test_getInstance() throws Exception {
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            Set<Provider.Service> services = provider.getServices();
            for (Provider.Service service : services) {
                String type = service.getType();
                if (!type.equals("SecureRandom")) {
                    continue;
                }

                String algorithm = service.getAlgorithm();
                try {
                    SecureRandom sr1 = SecureRandom.getInstance(algorithm);
                    assertEquals(algorithm, sr1.getAlgorithm());
                    test_SecureRandom(sr1);

                    // SecureRandom.getInstance(String, Provider)
                    SecureRandom sr2 = SecureRandom.getInstance(algorithm, provider);
                    assertEquals(algorithm, sr2.getAlgorithm());
                    assertEquals(provider, sr2.getProvider());
                    test_SecureRandom(sr2);

                    // SecureRandom.getInstance(String, String)
                    SecureRandom sr3 = SecureRandom.getInstance(algorithm, provider.getName());
                    assertEquals(algorithm, sr3.getAlgorithm());
                    assertEquals(provider, sr3.getProvider());
                    test_SecureRandom(sr3);

                    System.out.println("SecureRandomTest " + algorithm + " and provider "
                            + provider.getName());
                } catch (Exception e) {
                    throw new Exception("Problem testing SecureRandom." + algorithm
                            + ", provider: " + provider.getName(), e);
                }
            }
        }
    }

    private void test_SecureRandom(SecureRandom sr) throws Exception {
        byte[] out1 = new byte[20];
        byte[] out2 = new byte[20];

        sr.nextBytes(out1);
        sr.nextBytes(out2);
        assertFalse(Arrays.equals(out1, out2));

        byte[] seed1 = sr.generateSeed(20);
        byte[] seed2 = sr.generateSeed(20);
        assertFalse(Arrays.equals(seed1, seed2));

        sr.setSeed(STATIC_SEED_BYTES);
        sr.nextBytes(out1);
        sr.nextBytes(out2);
        assertFalse(Arrays.equals(out1, out2));

        sr.setSeed(STATIC_SEED_LONG);
        sr.nextBytes(out1);
        sr.nextBytes(out2);
        assertFalse(Arrays.equals(out1, out2));
    }

    public void testGetCommonInstances_Success() throws Exception {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        assertNotNull(sr);
        assertEquals(EXPECTED_PROVIDER, sr.getProvider().getClass().getName());
    }

    public void testNewConstructors_Success() throws Exception {
        SecureRandom sr1 = new SecureRandom();
        assertNotNull(sr1.getProvider());
        assertEquals(EXPECTED_PROVIDER, sr1.getProvider().getClass().getName());
        test_SecureRandom(sr1);

        SecureRandom sr2 = new SecureRandom(STATIC_SEED_BYTES);
        assertEquals(EXPECTED_PROVIDER, sr2.getProvider().getClass().getName());
        test_SecureRandom(sr2);
    }

    /**
      * http://b/28550092 : Removal of "Crypto" provider in N caused application compatibility
      * issues for callers of SecureRandom. To improve compatibility the provider is not registered
      * as a JCA Provider obtainable via Security.getProvider() but is made available for
      * SecureRandom.getInstance() iff the application targets API <= 23.
      */
    public void testCryptoProvider_withWorkaround_Success() throws Exception {
        // Assert that SecureRandom is still using the default value. Sanity check.
        assertEquals(SecureRandom.DEFAULT_SDK_TARGET_FOR_CRYPTO_PROVIDER_WORKAROUND,
                SecureRandom.getSdkTargetForCryptoProviderWorkaround());

        try {
            // Modify the maximum target SDK to apply the workaround, thereby enabling the
            // workaround for the current SDK and enabling it to be tested.
            SecureRandom.setSdkTargetForCryptoProviderWorkaround(
                    VMRuntime.getRuntime().getTargetSdkVersion());

            // Assert that the crypto provider is not installed...
            assertNull(Security.getProvider("Crypto"));
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
            assertNotNull(sr);
            // ...but we can get a SecureRandom from it...
            assertEquals("org.apache.harmony.security.provider.crypto.CryptoProvider",
                    sr.getProvider().getClass().getName());
            // ...yet it's not installed. So the workaround worked.
            assertNull(Security.getProvider("Crypto"));
        } finally {
            // Reset the target SDK for the workaround to the default / real value.
            SecureRandom.setSdkTargetForCryptoProviderWorkaround(
                    SecureRandom.DEFAULT_SDK_TARGET_FOR_CRYPTO_PROVIDER_WORKAROUND);
        }
    }

    /**
     * http://b/28550092 : Removal of "Crypto" provider in N caused application compatibility
     * issues for callers of SecureRandom. To improve compatibility the provider is not registered
     * as a JCA Provider obtainable via Security.getProvider() but is made available for
     * SecureRandom.getInstance() iff the application targets API <= 23.
     */
    public void testCryptoProvider_withoutWorkaround_Failure() throws Exception {
        // Assert that SecureRandom is still using the default value. Sanity check.
        assertEquals(SecureRandom.DEFAULT_SDK_TARGET_FOR_CRYPTO_PROVIDER_WORKAROUND,
                SecureRandom.getSdkTargetForCryptoProviderWorkaround());

        try {
            // We set the limit SDK for the workaround at the previous one, indicating that the
            // workaround shouldn't be in place.
            SecureRandom.setSdkTargetForCryptoProviderWorkaround(
                    VMRuntime.getRuntime().getTargetSdkVersion() - 1);

            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
            fail("Should throw " + NoSuchProviderException.class.getName());
        } catch(NoSuchProviderException expected) {
            // The workaround doesn't work. As expected.
        } finally {
            // Reset the target SDK for the workaround to the default / real value.
            SecureRandom.setSdkTargetForCryptoProviderWorkaround(
                    SecureRandom.DEFAULT_SDK_TARGET_FOR_CRYPTO_PROVIDER_WORKAROUND);
        }
    }
}
