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

package libcore.java.security.cert;

import tests.support.resource.Support_Resources;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import libcore.java.security.StandardNames;

public class X509CRLTest extends TestCase {
    private Provider[] mX509Providers;

    private static final String CERT_RSA = "x509/cert-rsa.der";

    private static final String CERT_DSA = "x509/cert-dsa.der";

    private static final String CRL_RSA = "x509/crl-rsa.der";

    private static final String CRL_RSA_DSA = "x509/crl-rsa-dsa.der";

    private static final String CRL_UNSUPPORTED = "x509/crl-unsupported.der";

    private static final String CRL_RSA_DATES = "x509/crl-rsa-dates.txt";

    private static final String CRL_RSA_DSA_DATES = "x509/crl-rsa-dsa-dates.txt";

    private static final String CRL_RSA_SIG = "x509/crl-rsa-sig.der";

    private static final String CRL_RSA_TBS = "x509/crl-rsa-tbs.der";

    private final X509Certificate getCertificate(CertificateFactory f, String name)
            throws Exception {
        final InputStream is = Support_Resources.getStream(name);
        assertNotNull("File does not exist: " + name, is);
        try {
            return (X509Certificate) f.generateCertificate(is);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    private final X509CRL getCRL(CertificateFactory f, String name) throws Exception {
        final InputStream is = Support_Resources.getStream(name);
        assertNotNull("File does not exist: " + name, is);
        try {
            return (X509CRL) f.generateCRL(is);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    private byte[] getResourceAsBytes(String name) throws Exception {
        final InputStream ris = Support_Resources.getStream(name);
        try {
            DataInputStream dis = new DataInputStream(ris);
            byte[] buf = new byte[ris.available()];
            dis.readFully(buf);
            return buf;
        } finally {
            try {
                ris.close();
            } catch (IOException ignored) {
            }
        }
    }

    private Map<String, Date> getCrlDates(String name) throws Exception {
        Map<String, Date> dates = new HashMap<String, Date>();
        final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm:ss yyyy zzz");

        final InputStream ris = Support_Resources.getStream(name);
        try {

            final BufferedReader buf = new BufferedReader(new InputStreamReader(ris));

            String line;
            while ((line = buf.readLine()) != null) {
                int index = line.indexOf('=');
                String key = line.substring(0, index);
                final Date value = sdf.parse(line.substring(index + 1));
                dates.put(key, value);
            }

            return dates;
        } finally {
            try {
                ris.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void test_Provider() throws Exception {
        final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(errBuffer);

        for (Provider p : mX509Providers) {
            try {
                CertificateFactory f = CertificateFactory.getInstance("X.509", p);
                isRevoked(f);
                getType(f);
                getEncoded(f);
                getVersion(f);
                hasUnsupportedCriticalExtension(f);
                getSignature(f);
                getTBSCertList(f);
                getRevokedCertificates(f);
                getThisUpdateNextUpdate(f);
                getSigAlgName(f);
                getSigAlgOID(f);
                test_toString(f);
                test_equals(f);
            } catch (Throwable e) {
                out.append("Error encountered checking " + p.getName() + "\n");
                e.printStackTrace(out);
            }
        }

        out.flush();
        if (errBuffer.size() > 0) {
            throw new Exception("Errors encountered:\n\n" + errBuffer.toString() + "\n\n");
        }
    }

    private void getType(CertificateFactory f) throws Exception {
        CRL crlRsa = getCRL(f, CRL_RSA);

        assertEquals("X.509", crlRsa.getType());
    }

    private void isRevoked(CertificateFactory f) throws Exception {
        X509Certificate rsaCert = getCertificate(f, CERT_RSA);
        X509Certificate dsaCert = getCertificate(f, CERT_DSA);
        X509CRL crlRsa = getCRL(f, CRL_RSA);
        X509CRL crlRsaDsa = getCRL(f, CRL_RSA_DSA);

        assertTrue(crlRsa.isRevoked(rsaCert));
        assertFalse(crlRsa.isRevoked(dsaCert));

        assertTrue(crlRsaDsa.isRevoked(rsaCert));
        assertTrue(crlRsaDsa.isRevoked(dsaCert));

        try {
            assertFalse(crlRsa.isRevoked(null));
            if ("BC".equals(f.getProvider().getName())) {
                fail("BouncyCastle throws on null input");
            }
        } catch (NullPointerException e) {
            if (!"BC".equals(f.getProvider().getName())) {
                fail("Should not throw on null input");
            }
        }
    }

    private void getThisUpdateNextUpdate(CertificateFactory f) throws Exception {
        {
            X509CRL crl = getCRL(f, CRL_RSA);
            Map<String, Date> dates = getCrlDates(CRL_RSA_DATES);

            Date lastUpdate = dates.get("lastUpdate");
            Date nextUpdate = dates.get("nextUpdate");

            assertNotNull(lastUpdate);
            assertNotNull(nextUpdate);

            assertEquals(lastUpdate, crl.getThisUpdate());
            assertEquals(nextUpdate, crl.getNextUpdate());
        }

        {
            X509CRL crl = getCRL(f, CRL_RSA_DSA);
            Map<String, Date> dates = getCrlDates(CRL_RSA_DSA_DATES);

            Date lastUpdate = dates.get("lastUpdate");
            Date nextUpdate = dates.get("nextUpdate");

            assertNotNull(lastUpdate);
            assertNotNull(nextUpdate);

            assertEquals(lastUpdate, crl.getThisUpdate());
            assertEquals(nextUpdate, crl.getNextUpdate());
        }
    }

    private void getSigAlgName(CertificateFactory f) throws Exception {
        X509CRL crlRsa = getCRL(f, CRL_RSA);

        String actual = crlRsa.getSigAlgName().toUpperCase(Locale.US);

        // Bouncycastle is broken
        if ("BC".equals(f.getProvider().getName())) {
            assertEquals("1.2.840.113549.1.1.5", actual);
        } else {
            assertEquals("SHA1WITHRSA", actual);
        }
    }

    private void getSigAlgOID(CertificateFactory f) throws Exception {
        X509CRL crlRsa = getCRL(f, CRL_RSA);

        assertEquals("1.2.840.113549.1.1.5", crlRsa.getSigAlgOID());
    }

    private void getVersion(CertificateFactory f) throws Exception {
        X509CRL crlRsa = getCRL(f, CRL_RSA);

        assertEquals(1, crlRsa.getVersion());
    }

    private void hasUnsupportedCriticalExtension(CertificateFactory f) throws Exception {
        X509CRL crlRsa = getCRL(f, CRL_RSA);
        assertFalse(crlRsa.hasUnsupportedCriticalExtension());

        X509CRL unsupportedCrl = getCRL(f, CRL_UNSUPPORTED);
        assertTrue(unsupportedCrl.hasUnsupportedCriticalExtension());
    }

    private void getSignature(CertificateFactory f) throws Exception {
        X509CRL crlRsa = getCRL(f, CRL_RSA);
        byte[] expected = getResourceAsBytes(CRL_RSA_SIG);

        assertEquals(Arrays.toString(expected), Arrays.toString(crlRsa.getSignature()));
    }

    private void getTBSCertList(CertificateFactory f) throws Exception {
        X509CRL crlRsa = getCRL(f, CRL_RSA);
        byte[] expected = getResourceAsBytes(CRL_RSA_TBS);

        assertEquals(Arrays.toString(expected), Arrays.toString(crlRsa.getTBSCertList()));
    }

    private void getEncoded(CertificateFactory f) throws Exception {
        X509CRL crlRsa = getCRL(f, CRL_RSA);

        byte[] crlRsaBytes = getResourceAsBytes(CRL_RSA);

        assertEquals(Arrays.toString(crlRsa.getEncoded()), Arrays.toString(crlRsaBytes));
    }

    private void assertRsaCrl(CertificateFactory f, X509CRLEntry rsaEntry) throws Exception {
        X509Certificate rsaCert = getCertificate(f, CERT_RSA);
        Map<String, Date> dates = getCrlDates(CRL_RSA_DSA_DATES);
        Date expectedDate = dates.get("lastUpdate");

        assertEquals(rsaCert.getSerialNumber(), rsaEntry.getSerialNumber());
        assertEquals(expectedDate, rsaEntry.getRevocationDate());
        assertNull(rsaEntry.getCertificateIssuer());
        assertFalse(rsaEntry.hasExtensions());
        assertNull(rsaEntry.getCriticalExtensionOIDs());
        assertNull(rsaEntry.getNonCriticalExtensionOIDs());
    }

    private void assertDsaCrl(CertificateFactory f, X509CRLEntry dsaEntry) throws Exception {
        X509Certificate dsaCert = getCertificate(f, CERT_DSA);
        Map<String, Date> dates = getCrlDates(CRL_RSA_DSA_DATES);
        Date expectedDate = dates.get("lastUpdate");

        assertEquals(dsaCert.getSerialNumber(), dsaEntry.getSerialNumber());
        assertEquals(expectedDate, dsaEntry.getRevocationDate());
        assertNull(dsaEntry.getCertificateIssuer());
        assertTrue(dsaEntry.hasExtensions());
        /* TODO: get the OID */
        assertNotNull(dsaEntry.getCriticalExtensionOIDs());
        /* TODO: get the OID */
        assertNotNull(dsaEntry.getNonCriticalExtensionOIDs());
    }

    private void getRevokedCertificates(CertificateFactory f) throws Exception {
        X509CRL crlRsa = getCRL(f, CRL_RSA);
        X509Certificate rsaCert = getCertificate(f, CERT_RSA);
        X509Certificate dsaCert = getCertificate(f, CERT_DSA);

        Set<? extends X509CRLEntry> entries = crlRsa.getRevokedCertificates();
        assertEquals(1, entries.size());
        for (X509CRLEntry e : entries) {
            assertRsaCrl(f, e);
        }

        X509CRL crlRsaDsa = getCRL(f, CRL_RSA_DSA);
        Set<? extends X509CRLEntry> entries2 = crlRsaDsa.getRevokedCertificates();
        assertEquals(2, entries2.size());
        assertRsaCrl(f, crlRsaDsa.getRevokedCertificate(rsaCert));
        assertDsaCrl(f, crlRsaDsa.getRevokedCertificate(dsaCert));
    }

    private void test_toString(CertificateFactory f) throws Exception {
        X509CRL crl1 = getCRL(f, CRL_RSA);
        X509CRL crl2 = getCRL(f, CRL_RSA);

        X509CRL crlRsaDsa = getCRL(f, CRL_RSA_DSA);

        assertNotNull(crl1);

        assertNotNull(crlRsaDsa);

        assertEquals(crl1.toString(), crl2.toString());

        assertFalse(crl1.toString().equals(crlRsaDsa.toString()));
    }

    private void test_equals(CertificateFactory f) throws Exception {
        X509CRL crl1 = getCRL(f, CRL_RSA);
        X509CRL crl2 = getCRL(f, CRL_RSA);
        X509Certificate rsaCert = getCertificate(f, CERT_RSA);

        X509CRL crlRsaDsa = getCRL(f, CRL_RSA_DSA);

        assertEquals(crl1, crl2);
        assertFalse(crl1.equals(crlRsaDsa));

        X509CRLEntry entry1 = crl1.getRevokedCertificate(rsaCert);
        assertNotNull(entry1);
        X509CRLEntry entry2 = crl2.getRevokedCertificate(rsaCert);
        assertNotNull(entry2);

        assertEquals(entry1, entry2);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mX509Providers = Security.getProviders("CertificateFactory.X509");
    }
}
