/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.security;

import java.util.Random;
import org.apache.harmony.security.fortress.Engine;
import org.apache.harmony.security.fortress.Services;
import org.apache.harmony.security.provider.crypto.SHA1PRNG_SecureRandomImpl;

/**
 * {@code SecureRandom} is an engine class which is capable of generating
 * cryptographically secure pseudo-random numbers.
 */
public class SecureRandom extends Random {

    private static final long serialVersionUID = 4940670005562187L;

    // The service name.
    private static final String SERVICE = "SecureRandom";

    // Used to access common engine functionality
    private static final Engine ENGINE = new Engine(SERVICE);

    private final Provider provider;

    private final SecureRandomSpi secureRandomSpi;

    private final String algorithm;

    // Internal SecureRandom used for getSeed(int)
    private static volatile SecureRandom internalSecureRandom;

    /**
     * Constructs a new instance of {@code SecureRandom}. An implementation for
     * the highest-priority provider is returned. The constructed instance will
     * not have been seeded.
     */
    public SecureRandom() {
        super(0);
        Services.refresh();
        Provider.Service service = Services.getSecureRandomService();
        if (service == null) {
            this.provider = null;
            this.secureRandomSpi = new SHA1PRNG_SecureRandomImpl();
            this.algorithm = "SHA1PRNG";
        } else {
            try {
                this.provider = service.getProvider();
                this.secureRandomSpi = (SecureRandomSpi)service.newInstance(null);
                this.algorithm = service.getAlgorithm();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Constructs a new instance of {@code SecureRandom}. An implementation for
     * the highest-priority provider is returned. The constructed instance will
     * be seeded with the parameter.
     *
     * @param seed
     *            the seed for this generator.
     */
    public SecureRandom(byte[] seed) {
        this();
        setSeed(seed);
    }

    /**
     * Constructs a new instance of {@code SecureRandom} using the given
     * implementation from the specified provider.
     *
     * @param secureRandomSpi
     *            the implementation.
     * @param provider
     *            the security provider.
     */
    protected SecureRandom(SecureRandomSpi secureRandomSpi,
                           Provider provider) {
        this(secureRandomSpi, provider, "unknown");
    }

    // Constructor
    private SecureRandom(SecureRandomSpi secureRandomSpi,
                         Provider provider,
                         String algorithm) {
        super(0);
        this.provider = provider;
        this.algorithm = algorithm;
        this.secureRandomSpi = secureRandomSpi;
    }

    /**
     * Returns a new instance of {@code SecureRandom} that utilizes the
     * specified algorithm.
     *
     * @param algorithm
     *            the name of the algorithm to use.
     * @return a new instance of {@code SecureRandom} that utilizes the
     *         specified algorithm.
     * @throws NoSuchAlgorithmException
     *             if the specified algorithm is not available.
     * @throws NullPointerException
     *             if {@code algorithm} is {@code null}.
     */
    public static SecureRandom getInstance(String algorithm)
                                throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException();
        }
        Engine.SpiAndProvider sap = ENGINE.getInstance(algorithm, null);
        return new SecureRandom((SecureRandomSpi) sap.spi, sap.provider,
                                algorithm);
    }

    /**
     * Returns a new instance of {@code SecureRandom} that utilizes the
     * specified algorithm from the specified provider.
     *
     * @param algorithm
     *            the name of the algorithm to use.
     * @param provider
     *            the name of the provider.
     * @return a new instance of {@code SecureRandom} that utilizes the
     *         specified algorithm from the specified provider.
     * @throws NoSuchAlgorithmException
     *             if the specified algorithm is not available.
     * @throws NoSuchProviderException
     *             if the specified provider is not available.
     * @throws NullPointerException
     *             if {@code algorithm} is {@code null}.
     * @throws IllegalArgumentException if {@code provider == null || provider.isEmpty()}
     */
    public static SecureRandom getInstance(String algorithm, String provider)
                                throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException();
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException(provider);
        }
        return getInstance(algorithm, p);
    }

    /**
     * Returns a new instance of {@code SecureRandom} that utilizes the
     * specified algorithm from the specified provider.
     *
     * @param algorithm
     *            the name of the algorithm to use.
     * @param provider
     *            the security provider.
     * @return a new instance of {@code SecureRandom} that utilizes the
     *         specified algorithm from the specified provider.
     * @throws NoSuchAlgorithmException
     *             if the specified algorithm is not available.
     * @throws NullPointerException
     *             if {@code algorithm} is {@code null}.
     * @throws IllegalArgumentException if {@code provider == null}
     */
    public static SecureRandom getInstance(String algorithm, Provider provider)
                                throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException();
        }
        if (algorithm == null) {
            throw new NullPointerException();
        }
        Object spi = ENGINE.getInstance(algorithm, provider, null);
        return new SecureRandom((SecureRandomSpi) spi, provider, algorithm);
    }

    /**
     * Returns the provider associated with this {@code SecureRandom}.
     *
     * @return the provider associated with this {@code SecureRandom}.
     */
    public final Provider getProvider() {
        return provider;
    }

    /**
     * Returns the name of the algorithm of this {@code SecureRandom}.
     *
     * @return the name of the algorithm of this {@code SecureRandom}.
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Reseeds this {@code SecureRandom} instance with the specified {@code
     * seed}. The seed of this {@code SecureRandom} instance is supplemented,
     * not replaced.
     *
     * @param seed
     *            the new seed.
     */
    public synchronized void setSeed(byte[] seed) {
        secureRandomSpi.engineSetSeed(seed);
    }

    /**
     * Reseeds this this {@code SecureRandom} instance with the eight bytes
     * described by the representation of the given {@code long seed}. The seed
     * of this {@code SecureRandom} instance is supplemented, not replaced.
     *
     * @param seed
     *            the new seed.
     */
    @Override
    public void setSeed(long seed) {
        if (seed == 0) {    // skip call from Random
            return;
        }
        byte[] byteSeed = {
                (byte)((seed >> 56) & 0xFF),
                (byte)((seed >> 48) & 0xFF),
                (byte)((seed >> 40) & 0xFF),
                (byte)((seed >> 32) & 0xFF),
                (byte)((seed >> 24) & 0xFF),
                (byte)((seed >> 16) & 0xFF),
                (byte)((seed >> 8) & 0xFF),
                (byte)((seed) & 0xFF)
        };
        setSeed(byteSeed);
    }

    /**
     * Generates and stores random bytes in the given {@code byte[]} for each
     * array element.
     *
     * @param bytes
     *            the {@code byte[]} to be filled with random bytes.
     */
    @Override
    public synchronized void nextBytes(byte[] bytes) {
        secureRandomSpi.engineNextBytes(bytes);
    }

    /**
     * Generates and returns an {@code int} containing the specified number of
     * random bits (right justified, with leading zeros).
     *
     * @param numBits
     *            number of bits to be generated. An input value should be in
     *            the range [0, 32].
     * @return an {@code int} containing the specified number of random bits.
     */
    @Override
    protected final int next(int numBits) {
        if (numBits < 0) {
            numBits = 0;
        } else {
            if (numBits > 32) {
                numBits = 32;
            }
        }
        int bytes = (numBits+7)/8;
        byte[] next = new byte[bytes];
        int ret = 0;

        nextBytes(next);
        for (int i = 0; i < bytes; i++) {
            ret = (next[i] & 0xFF) | (ret << 8);
        }
        ret = ret >>> (bytes*8 - numBits);
        return ret;
    }

    /**
     * Generates and returns the specified number of seed bytes, computed using
     * the seed generation algorithm used by this {@code SecureRandom}.
     *
     * @param numBytes
     *            the number of seed bytes.
     * @return the seed bytes
     */
    public static byte[] getSeed(int numBytes) {
        SecureRandom result = internalSecureRandom;
        if (result == null) {
            // single-check idiom
            internalSecureRandom = result = new SecureRandom();
        }
        return result.generateSeed(numBytes);
    }

    /**
     * Generates and returns the specified number of seed bytes, computed using
     * the seed generation algorithm used by this {@code SecureRandom}.
     *
     * @param numBytes
     *            the number of seed bytes.
     * @return the seed bytes.
     */
    public byte[] generateSeed(int numBytes) {
        return secureRandomSpi.engineGenerateSeed(numBytes);
    }

}
