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


package org.apache.harmony.security.provider.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.ProviderException;
import libcore.io.Streams;

/**
 * Supplies random bits from /dev/urandom.
 */
public class RandomBitsSupplier implements SHA1_Data {
    private static FileInputStream devURandom;
    static {
        try {
            devURandom = new FileInputStream(new File("/dev/urandom"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    static boolean isServiceAvailable() {
        return (devURandom != null);
    }

    public static byte[] getRandomBits(int byteCount) {
        if (byteCount <= 0) {
            throw new IllegalArgumentException("Too few bytes requested: " + byteCount);
        }
        try {
            byte[] result = new byte[byteCount];
            Streams.readFully(devURandom, result, 0, byteCount);
            return result;
        } catch (Exception ex) {
            throw new ProviderException("Couldn't read " + byteCount + " random bytes", ex);
        }
    }
}
