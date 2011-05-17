/*
 * Copyright (C) 2011 The Android Open Source Project
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import junit.framework.TestCase;
import libcore.java.security.TestKeyStore;

public class TrustedCertificateStoreTest extends TestCase {

    private static final File DIR_TEMP = new File(System.getProperty("java.io.tmpdir"));
    private static final File DIR_TEST = new File(DIR_TEMP, "test");
    private static final File DIR_SYSTEM = new File(DIR_TEST, "system");
    private static final File DIR_ADDED = new File(DIR_TEST, "added");
    private static final File DIR_DELETED = new File(DIR_TEST, "removed");

    private static final X509Certificate CA1 = TestKeyStore.getClient().getRootCertificate("RSA");
    private static final X509Certificate CA2
            = TestKeyStore.getClientCA2().getRootCertificate("RSA");

    private static final KeyStore.PrivateKeyEntry PRIVATE
            = TestKeyStore.getServer().getPrivateKey("RSA", "RSA");
    private static final X509Certificate[] CHAIN = (X509Certificate[])PRIVATE.getCertificateChain();

    private static final X509Certificate CA3_WITH_CA1_SUBJECT
            = new TestKeyStore.Builder()
                .aliasPrefix("unused")
                .subject(CA1.getSubjectX500Principal())
                .ca(true)
                .build().getRootCertificate("RSA");


    private static final String ALIAS_SYSTEM_CA1 = alias(false, CA1, 0);
    private static final String ALIAS_SYSTEM_CA2 = alias(false, CA2, 0);
    private static final String ALIAS_USER_CA1 = alias(true, CA1, 0);
    private static final String ALIAS_USER_CA2 = alias(true, CA2, 0);

    private static final String ALIAS_SYSTEM_CHAIN0 = alias(false, CHAIN[0], 0);
    private static final String ALIAS_SYSTEM_CHAIN1 = alias(false, CHAIN[1], 0);
    private static final String ALIAS_SYSTEM_CHAIN2 = alias(false, CHAIN[2], 0);
    private static final String ALIAS_USER_CHAIN0 = alias(true, CHAIN[0], 0);
    private static final String ALIAS_USER_CHAIN1 = alias(true, CHAIN[1], 0);
    private static final String ALIAS_USER_CHAIN2 = alias(true, CHAIN[2], 0);

    private static final String ALIAS_SYSTEM_CA3 = alias(false, CA3_WITH_CA1_SUBJECT, 0);
    private static final String ALIAS_SYSTEM_CA3_COLLISION
            = alias(false, CA3_WITH_CA1_SUBJECT, 1);
    private static final String ALIAS_USER_CA3 = alias(true, CA3_WITH_CA1_SUBJECT, 0);
    private static final String ALIAS_USER_CA3_COLLISION
            = alias(true, CA3_WITH_CA1_SUBJECT, 1);

    private TrustedCertificateStore store;

    @Override protected void setUp() {
        setupStore();
    }

    private void setupStore() {
        DIR_SYSTEM.mkdirs();
        createStore();
    }

    private void createStore() {
        store = new TrustedCertificateStore(DIR_SYSTEM, DIR_ADDED, DIR_DELETED);
    }

    @Override protected void tearDown() {
        cleanStore();
    }

    private void cleanStore() {
        for (File dir : new File[] { DIR_SYSTEM, DIR_ADDED, DIR_DELETED, DIR_TEST }) {
            File[] files = dir.listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                assertTrue(file.delete());
            }
        }
        store = null;
    }

    private void resetStore() {
        cleanStore();
        setupStore();
    }

    public void testEmptyDirectories() throws Exception {
        assertEmpty();
    }

    public void testOneSystemOneDeleted() throws Exception {
        install(CA1, ALIAS_SYSTEM_CA1);
        store.deleteCertificateEntry(ALIAS_SYSTEM_CA1);
        assertEmpty();
        assertDeleted(CA1, ALIAS_SYSTEM_CA1);
    }

    public void testTwoSystemTwoDeleted() throws Exception {
        install(CA1, ALIAS_SYSTEM_CA1);
        store.deleteCertificateEntry(ALIAS_SYSTEM_CA1);
        install(CA2, ALIAS_SYSTEM_CA2);
        store.deleteCertificateEntry(ALIAS_SYSTEM_CA2);
        assertEmpty();
        assertDeleted(CA1, ALIAS_SYSTEM_CA1);
        assertDeleted(CA2, ALIAS_SYSTEM_CA2);
    }

    public void testPartialFileIsIgnored() throws Exception {
        File file = file(ALIAS_SYSTEM_CA1);
        OutputStream os = new FileOutputStream(file);
        os.write(0);
        os.close();
        assertTrue(file.exists());
        assertEmpty();
        assertTrue(file.exists());
    }

    private void assertEmpty() throws Exception {
        try {
            store.getCertificate(null);
            fail();
        } catch (NullPointerException expected) {
        }
        assertNull(store.getCertificate(""));

        try {
            store.getCreationDate(null);
            fail();
        } catch (NullPointerException expected) {
        }
        assertNull(store.getCreationDate(""));

        Set<String> s = store.aliases();
        assertNotNull(s);
        assertTrue(s.isEmpty());
        assertAliases();

        try {
            store.containsAlias(null);
            fail();
        } catch (NullPointerException expected) {
        }
        assertFalse(store.containsAlias(""));

        assertNull(store.getCertificateAlias(null));
        assertNull(store.getCertificateAlias(CA1));

        try {
            store.isTrustAnchor(null);
            fail();
        } catch (NullPointerException expected) {
        }
        assertFalse(store.isTrustAnchor(CA1));

        try {
            store.findIssuer(null);
            fail();
        } catch (NullPointerException expected) {
        }
        assertNull(store.findIssuer(CA1));

        try {
            store.installCertificate(null);
            fail();
        } catch (NullPointerException expected) {
        }

        store.deleteCertificateEntry(null);
        store.deleteCertificateEntry("");

        String[] userFiles = DIR_ADDED.list();
        assertTrue(userFiles == null || userFiles.length == 0);
    }

    public void testTwoSystem() throws Exception {
        testTwo(CA1, ALIAS_SYSTEM_CA1,
                CA2, ALIAS_SYSTEM_CA2);
    }

    public void testTwoUser() throws Exception {
        testTwo(CA1, ALIAS_USER_CA1,
                CA2, ALIAS_USER_CA2);
    }

    public void testOneSystemOneUser() throws Exception {
        testTwo(CA1, ALIAS_SYSTEM_CA1,
                CA2, ALIAS_USER_CA2);
    }

    public void testTwoSystemSameSubject() throws Exception {
        testTwo(CA1, ALIAS_SYSTEM_CA1,
                CA3_WITH_CA1_SUBJECT, ALIAS_SYSTEM_CA3_COLLISION);
    }

    public void testTwoUserSameSubject() throws Exception {
        testTwo(CA1, ALIAS_USER_CA1,
                CA3_WITH_CA1_SUBJECT, ALIAS_USER_CA3_COLLISION);

        store.deleteCertificateEntry(ALIAS_USER_CA1);
        assertDeleted(CA1, ALIAS_USER_CA1);
        assertTombstone(ALIAS_USER_CA1);
        assertRootCA(CA3_WITH_CA1_SUBJECT, ALIAS_USER_CA3_COLLISION);
        assertAliases(ALIAS_USER_CA3_COLLISION);

        store.deleteCertificateEntry(ALIAS_USER_CA3_COLLISION);
        assertDeleted(CA3_WITH_CA1_SUBJECT, ALIAS_USER_CA3_COLLISION);
        assertNoTombstone(ALIAS_USER_CA3_COLLISION);
        assertNoTombstone(ALIAS_USER_CA1);
        assertEmpty();
    }

    public void testOneSystemOneUserSameSubject() throws Exception {
        testTwo(CA1, ALIAS_SYSTEM_CA1,
                CA3_WITH_CA1_SUBJECT, ALIAS_USER_CA3);
        testTwo(CA1, ALIAS_USER_CA1,
                CA3_WITH_CA1_SUBJECT, ALIAS_SYSTEM_CA3);
    }

    private void testTwo(X509Certificate x1, String alias1,
                         X509Certificate x2, String alias2) {
        install(x1, alias1);
        install(x2, alias2);
        assertRootCA(x1, alias1);
        assertRootCA(x2, alias2);
        assertAliases(alias1, alias2);
    }


    public void testOneSystemOneUserOneDeleted() throws Exception {
        install(CA1, ALIAS_SYSTEM_CA1);
        store.installCertificate(CA2);
        store.deleteCertificateEntry(ALIAS_SYSTEM_CA1);
        assertDeleted(CA1, ALIAS_SYSTEM_CA1);
        assertRootCA(CA2, ALIAS_USER_CA2);
        assertAliases(ALIAS_USER_CA2);
    }

    public void testOneSystemOneUserOneDeletedSameSubject() throws Exception {
        install(CA1, ALIAS_SYSTEM_CA1);
        store.installCertificate(CA3_WITH_CA1_SUBJECT);
        store.deleteCertificateEntry(ALIAS_SYSTEM_CA1);
        assertDeleted(CA1, ALIAS_SYSTEM_CA1);
        assertRootCA(CA3_WITH_CA1_SUBJECT, ALIAS_USER_CA3);
        assertAliases(ALIAS_USER_CA3);
    }

    public void testUserMaskingSystem() throws Exception {
        install(CA1, ALIAS_SYSTEM_CA1);
        install(CA1, ALIAS_USER_CA1);
        assertMasked(CA1, ALIAS_SYSTEM_CA1);
        assertRootCA(CA1, ALIAS_USER_CA1);
        assertAliases(ALIAS_SYSTEM_CA1, ALIAS_USER_CA1);
    }

    public void testChain() throws Exception {
        testChain(ALIAS_SYSTEM_CHAIN1, ALIAS_SYSTEM_CHAIN2);
        testChain(ALIAS_SYSTEM_CHAIN1, ALIAS_USER_CHAIN2);
        testChain(ALIAS_USER_CHAIN1, ALIAS_SYSTEM_CA1);
        testChain(ALIAS_USER_CHAIN1, ALIAS_USER_CHAIN2);
    }

    private void testChain(String alias1, String alias2) throws Exception {
        install(CHAIN[1], alias1);
        install(CHAIN[2], alias2);
        assertIntermediateCA(CHAIN[1], alias1);
        assertRootCA(CHAIN[2], alias2);
        assertAliases(alias1, alias2);
        assertEquals(CHAIN[2], store.findIssuer(CHAIN[1]));
        assertEquals(CHAIN[1], store.findIssuer(CHAIN[0]));
        resetStore();
    }

    public void testMissingSystemDirectory() {
        cleanStore();
        try {
            createStore();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    public void testWithExistingUserDirectories() throws Exception {
        DIR_ADDED.mkdirs();
        DIR_DELETED.mkdirs();
        install(CA1, ALIAS_SYSTEM_CA1);
        assertRootCA(CA1, ALIAS_SYSTEM_CA1);
        assertAliases(ALIAS_SYSTEM_CA1);
    }

    public void testIsTrustAnchorWithReissuedCA() throws Exception {
        PublicKey publicKey = PRIVATE.getCertificate().getPublicKey();
        PrivateKey privateKey = PRIVATE.getPrivateKey();
        String name = "CN=CA4";
        X509Certificate ca1 = TestKeyStore.createCA(publicKey, privateKey, name);
        Thread.sleep(1 * 1000); // wait to ensure CAs vary by expiration
        X509Certificate ca2 = TestKeyStore.createCA(publicKey, privateKey, name);
        assertFalse(ca1.equals(ca2));

        String systemAlias = alias(false, ca1, 0);
        install(ca1, systemAlias);
        assertRootCA(ca1, systemAlias);
        assertTrue(store.isTrustAnchor(ca2));
        assertEquals(ca1, store.findIssuer(ca2));
        resetStore();

        String userAlias = alias(true, ca1, 0);
        store.installCertificate(ca1);
        assertRootCA(ca1, userAlias);
        assertTrue(store.isTrustAnchor(ca2));
        assertEquals(ca1, store.findIssuer(ca2));
        resetStore();
    }

    public void testInstallEmpty() throws Exception {
        store.installCertificate(CA1);
        assertRootCA(CA1, ALIAS_USER_CA1);
        assertAliases(ALIAS_USER_CA1);

        // reinstalling should not change anything
        store.installCertificate(CA1);
        assertRootCA(CA1, ALIAS_USER_CA1);
        assertAliases(ALIAS_USER_CA1);
    }

    public void testInstallEmptySystemExists() throws Exception {
        install(CA1, ALIAS_SYSTEM_CA1);
        assertRootCA(CA1, ALIAS_SYSTEM_CA1);
        assertAliases(ALIAS_SYSTEM_CA1);

        // reinstalling should not affect system CA
        store.installCertificate(CA1);
        assertRootCA(CA1, ALIAS_SYSTEM_CA1);
        assertAliases(ALIAS_SYSTEM_CA1);

    }

    public void testInstallEmptyDeletedSystemExists() throws Exception {
        install(CA1, ALIAS_SYSTEM_CA1);
        store.deleteCertificateEntry(ALIAS_SYSTEM_CA1);
        assertEmpty();
        assertDeleted(CA1, ALIAS_SYSTEM_CA1);

        // installing should restore deleted system CA
        store.installCertificate(CA1);
        assertRootCA(CA1, ALIAS_SYSTEM_CA1);
        assertAliases(ALIAS_SYSTEM_CA1);
    }

    public void testDeleteEmpty() throws Exception {
        store.deleteCertificateEntry(ALIAS_SYSTEM_CA1);
        assertEmpty();
        assertDeleted(CA1, ALIAS_SYSTEM_CA1);
    }

    public void testDeleteUser() throws Exception {
        store.installCertificate(CA1);
        assertRootCA(CA1, ALIAS_USER_CA1);
        assertAliases(ALIAS_USER_CA1);

        store.deleteCertificateEntry(ALIAS_USER_CA1);
        assertEmpty();
        assertDeleted(CA1, ALIAS_USER_CA1);
        assertNoTombstone(ALIAS_USER_CA1);
    }

    public void testDeleteSystem() throws Exception {
        install(CA1, ALIAS_SYSTEM_CA1);
        assertRootCA(CA1, ALIAS_SYSTEM_CA1);
        assertAliases(ALIAS_SYSTEM_CA1);

        store.deleteCertificateEntry(ALIAS_SYSTEM_CA1);
        assertEmpty();
        assertDeleted(CA1, ALIAS_SYSTEM_CA1);

        // deleting again should not change anything
        store.deleteCertificateEntry(ALIAS_SYSTEM_CA1);
        assertEmpty();
        assertDeleted(CA1, ALIAS_SYSTEM_CA1);
    }

    private void assertRootCA(X509Certificate x, String alias) {
        assertIntermediateCA(x, alias);
        assertEquals(x, store.findIssuer(x));
    }

    private void assertTrusted(X509Certificate x, String alias) {
        assertEquals(x, store.getCertificate(alias));
        assertEquals(file(alias).lastModified(), store.getCreationDate(alias).getTime());
        assertTrue(store.containsAlias(alias));
        assertTrue(store.isTrustAnchor(x));
    }

    private void assertIntermediateCA(X509Certificate x, String alias) {
        assertTrusted(x, alias);
        assertEquals(alias, store.getCertificateAlias(x));
    }

    private void assertMasked(X509Certificate x, String alias) {
        assertTrusted(x, alias);
        assertFalse(alias.equals(store.getCertificateAlias(x)));
    }

    private void assertDeleted(X509Certificate x, String alias) {
        assertNull(store.getCertificate(alias));
        assertFalse(store.containsAlias(alias));
        assertNull(store.getCertificateAlias(x));
        assertFalse(store.isTrustAnchor(x));
    }

    private void assertTombstone(String alias) {
        assertTrue(TrustedCertificateStore.isUser(alias));
        File file = file(alias);
        assertTrue(file.exists());
        assertEquals(0, file.length());
    }

    private void assertNoTombstone(String alias) {
        assertTrue(TrustedCertificateStore.isUser(alias));
        assertFalse(file(alias).exists());
    }

    private void assertAliases(String... aliases) {
        Set<String> expected = new HashSet<String>(Arrays.asList(aliases));
        Set<String> actual = new HashSet<String>();
        for (String alias : store.aliases()) {
            if (TrustedCertificateStore.isSystem(alias) || TrustedCertificateStore.isUser(alias)) {
                actual.add(alias);
            } else {
                throw new AssertionError(alias);
            }
        }
        assertEquals(expected, actual);
    }

    /**
     * format a certificate alias
     */
    private static String alias(boolean user, X509Certificate x, int index) {
        String prefix = user ? "user:" : "system:";

        X500Principal subject = x.getSubjectX500Principal();
        int intHash = NativeCrypto.X509_NAME_hash_old(subject);
        String strHash = IntegralToString.intToHexString(intHash, false, 8);

        return prefix + strHash + '.' + index;
    }

    /**
     * Install certificate under specified alias
     */
    private static void install(X509Certificate x, String alias) {
        try {
            File file = file(alias);
            file.getParentFile().mkdirs();
            OutputStream out = new FileOutputStream(file);
            out.write(x.getEncoded());
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compute file for an alias
     */
    private static File file(String alias) {
        File dir;
        if (TrustedCertificateStore.isSystem(alias)) {
            dir = DIR_SYSTEM;
        } else if (TrustedCertificateStore.isUser(alias)) {
            dir = DIR_ADDED;
        } else {
            throw new IllegalArgumentException(alias);
        }

        int index = alias.lastIndexOf(":");
        if (index == -1) {
            throw new IllegalArgumentException(alias);
        }
        String filename = alias.substring(index+1);

        return new File(dir, filename);
    }
}
