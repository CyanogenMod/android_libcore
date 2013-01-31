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

package libcore.java.security.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import junit.framework.TestCase;

public class CertificateFactoryTest extends TestCase {

    private static final String VALID_CERTIFICATE_PEM =
            "-----BEGIN CERTIFICATE-----\n"
            + "MIIDITCCAoqgAwIBAgIQL9+89q6RUm0PmqPfQDQ+mjANBgkqhkiG9w0BAQUFADBM\n"
            + "MQswCQYDVQQGEwJaQTElMCMGA1UEChMcVGhhd3RlIENvbnN1bHRpbmcgKFB0eSkg\n"
            + "THRkLjEWMBQGA1UEAxMNVGhhd3RlIFNHQyBDQTAeFw0wOTEyMTgwMDAwMDBaFw0x\n"
            + "MTEyMTgyMzU5NTlaMGgxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlh\n"
            + "MRYwFAYDVQQHFA1Nb3VudGFpbiBWaWV3MRMwEQYDVQQKFApHb29nbGUgSW5jMRcw\n"
            + "FQYDVQQDFA53d3cuZ29vZ2xlLmNvbTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkC\n"
            + "gYEA6PmGD5D6htffvXImttdEAoN4c9kCKO+IRTn7EOh8rqk41XXGOOsKFQebg+jN\n"
            + "gtXj9xVoRaELGYW84u+E593y17iYwqG7tcFR39SDAqc9BkJb4SLD3muFXxzW2k6L\n"
            + "05vuuWciKh0R73mkszeK9P4Y/bz5RiNQl/Os/CRGK1w7t0UCAwEAAaOB5zCB5DAM\n"
            + "BgNVHRMBAf8EAjAAMDYGA1UdHwQvMC0wK6ApoCeGJWh0dHA6Ly9jcmwudGhhd3Rl\n"
            + "LmNvbS9UaGF3dGVTR0NDQS5jcmwwKAYDVR0lBCEwHwYIKwYBBQUHAwEGCCsGAQUF\n"
            + "BwMCBglghkgBhvhCBAEwcgYIKwYBBQUHAQEEZjBkMCIGCCsGAQUFBzABhhZodHRw\n"
            + "Oi8vb2NzcC50aGF3dGUuY29tMD4GCCsGAQUFBzAChjJodHRwOi8vd3d3LnRoYXd0\n"
            + "ZS5jb20vcmVwb3NpdG9yeS9UaGF3dGVfU0dDX0NBLmNydDANBgkqhkiG9w0BAQUF\n"
            + "AAOBgQCfQ89bxFApsb/isJr/aiEdLRLDLE5a+RLizrmCUi3nHX4adpaQedEkUjh5\n"
            + "u2ONgJd8IyAPkU0Wueru9G2Jysa9zCRo1kNbzipYvzwY4OA8Ys+WAi0oR1A04Se6\n"
            + "z5nRUP8pJcA2NhUzUnC+MY+f6H/nEQyNv4SgQhqAibAxWEEHXw==\n"
            + "-----END CERTIFICATE-----\n";

    private static final String INVALID_CERTIFICATE_PEM =
            "-----BEGIN CERTIFICATE-----\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n"
            + "AAAAAAAA\n"
            + "-----END CERTIFICATE-----";

    public void test_generateCertificate() throws Exception {
        Provider[] providers = Security.getProviders("CertificateFactory.X509");
        for (Provider p : providers) {
            CertificateFactory cf = CertificateFactory.getInstance("X509", p);
            try {
                test_generateCertificate(cf);
                test_generateCertificate_InputStream_Offset_Correct(cf);
                test_generateCertificate_InputStream_Empty(cf);
            } catch (Exception e) {
                throw new Exception("Problem testing " + p.getName(), e);
            }
        }
    }

    private void test_generateCertificate(CertificateFactory cf) throws Exception {
        byte[] valid = VALID_CERTIFICATE_PEM.getBytes();
        Certificate c = cf.generateCertificate(new ByteArrayInputStream(valid));
        assertNotNull(c);

        try {
            byte[] invalid = INVALID_CERTIFICATE_PEM.getBytes();
            cf.generateCertificate(new ByteArrayInputStream(invalid));
            fail();
        } catch (CertificateException expected) {
        }
    }

    private void test_generateCertificate_InputStream_Empty(CertificateFactory cf) throws Exception {
        try {
            Certificate c = cf.generateCertificate(new ByteArrayInputStream(new byte[0]));
            if (!"BC".equals(cf.getProvider().getName())) {
                fail("should throw CertificateException: " + cf.getProvider().getName());
            }
            assertNull(c);
        } catch (CertificateException e) {
            if ("BC".equals(cf.getProvider().getName())) {
                fail("should return null: " + cf.getProvider().getName());
            }
        }
    }

    private void test_generateCertificate_InputStream_Offset_Correct(CertificateFactory cf)
            throws Exception {
        byte[] valid = VALID_CERTIFICATE_PEM.getBytes();

        byte[] doubleCertificateData = new byte[valid.length * 2];
        System.arraycopy(valid, 0, doubleCertificateData, 0, valid.length);
        System.arraycopy(valid, 0, doubleCertificateData, valid.length, valid.length);
        MeasuredInputStream certStream = new MeasuredInputStream(new ByteArrayInputStream(
                doubleCertificateData));
        Certificate certificate = cf.generateCertificate(certStream);
        assertNotNull(certificate);
        assertEquals(valid.length, certStream.getCount());
    }

    /**
     * Proxy that counts the number of bytes read from an InputStream.
     */
    private static class MeasuredInputStream extends InputStream {
        private long mCount = 0;

        private long mMarked = 0;

        private InputStream mStream;

        public MeasuredInputStream(InputStream is) {
            mStream = is;
        }

        public long getCount() {
            return mCount;
        }

        @Override
        public int read() throws IOException {
            int nextByte = mStream.read();
            mCount++;
            return nextByte;
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            int count = mStream.read(buffer);
            mCount += count;
            return count;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = mStream.read(buffer, offset, length);
            mCount += count;
            return count;
        }

        @Override
        public long skip(long byteCount) throws IOException {
            long count = mStream.skip(byteCount);
            mCount += count;
            return count;
        }

        @Override
        public int available() throws IOException {
            return mStream.available();
        }

        @Override
        public void close() throws IOException {
            mStream.close();
        }

        @Override
        public void mark(int readlimit) {
            mMarked = mCount;
            mStream.mark(readlimit);
        }

        @Override
        public boolean markSupported() {
            return mStream.markSupported();
        }

        @Override
        public synchronized void reset() throws IOException {
            mCount = mMarked;
            mStream.reset();
        }
    }
}
