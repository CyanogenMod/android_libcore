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

import java.io.Serializable;
import java.security.SecureRandomSpi;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OpenSSLRandom extends SecureRandomSpi implements Serializable {
    private static final long serialVersionUID = 8506210602917522860L;

    private transient int state;
    private static final int UNSEEDED = 0;
    private static final int SEEDED = 1;

    public OpenSSLRandom() {
        state = UNSEEDED;
    }

    /**
     * Generates a invocation-specific seed to be mixed into the
     * Linux PRNG.
     */
    private void generateSeed() {
        try {
            ByteArrayOutputStream seedBuffer = new ByteArrayOutputStream();
            DataOutputStream seedBufferOut =
                    new DataOutputStream(seedBuffer);
            seedBufferOut.writeLong(System.currentTimeMillis());
            seedBufferOut.writeLong(System.nanoTime());
            seedBufferOut.close();
            NativeCrypto.RAND_seed(seedBuffer.toByteArray());
            NativeCrypto.RAND_load_file("/dev/urandom", 1024);
            state = SEEDED;
        } catch (IOException e) {
            throw new SecurityException("Failed to generate seed", e);
        }
    }

    @Override
    protected void engineSetSeed(byte[] seed) {
        NativeCrypto.RAND_seed(seed);
        state = SEEDED;
    }

    @Override
    protected void engineNextBytes(byte[] bytes) {
        if (state == UNSEEDED)
            generateSeed();
        NativeCrypto.RAND_bytes(bytes);
    }

    @Override
    protected byte[] engineGenerateSeed(int numBytes) {
        byte[] output = new byte[numBytes];
        if (state == UNSEEDED)
            generateSeed();
        NativeCrypto.RAND_bytes(output);
        return output;
    }
}
