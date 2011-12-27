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

package javax.net.ssl;

import java.net.InetAddress;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.security.auth.x500.X500Principal;

/**
 * A HostnameVerifier consistent with <a
 * href="http://www.ietf.org/rfc/rfc2818.txt">RFC 2818</a>.
 *
 * @hide accessible via HttpsURLConnection.getDefaultHostnameVerifier()
 */
public final class DefaultHostnameVerifier implements HostnameVerifier {
    private static final int ALT_DNS_NAME = 2;
    private static final int ALT_IPA_NAME = 7;

    public final boolean verify(String host, SSLSession session) {
        try {
            Certificate[] certificates = session.getPeerCertificates();
            return verify(host, (X509Certificate) certificates[0]);
        } catch (SSLException e) {
            return false;
        }
    }

    public boolean verify(String host, X509Certificate certificate) {
        return InetAddress.isNumeric(host)
                ? verifyIpAddress(host, certificate)
                : verifyHostName(host, certificate);
    }

    /**
     * Returns true if {@code certificate} matches {@code ipAddress}.
     */
    private boolean verifyIpAddress(String ipAddress, X509Certificate certificate) {
        for (String altName : getSubjectAltNames(certificate, ALT_IPA_NAME)) {
            if (ipAddress.equalsIgnoreCase(altName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code certificate} matches {@code hostName}.
     */
    private boolean verifyHostName(String hostName, X509Certificate certificate) {
        hostName = hostName.toLowerCase(Locale.US);
        boolean hasDns = false;
        for (String altName : getSubjectAltNames(certificate, ALT_DNS_NAME)) {
            hasDns = true;
            if (verifyHostName(hostName, altName)) {
                return true;
            }
        }

        if (!hasDns) {
            X500Principal principal = certificate.getSubjectX500Principal();
            String cn = new DistinguishedNameParser(principal).find("cn");
            if (cn != null) {
                return verifyHostName(hostName, cn);
            }
        }

        return false;
    }

    private List<String> getSubjectAltNames(X509Certificate certificate, int type) {
        List<String> result = new ArrayList<String>();
        try {
            Collection<?> subjectAltNames = certificate.getSubjectAlternativeNames();
            if (subjectAltNames == null) {
                return Collections.emptyList();
            }
            for (Object subjectAltName : subjectAltNames) {
                List<?> entry = (List<?>) subjectAltName;
                if (entry == null || entry.size() < 2) {
                    continue;
                }
                Integer altNameType = (Integer) entry.get(0);
                if (altNameType == null) {
                    continue;
                }
                if (altNameType == type) {
                    String altName = (String) entry.get(1);
                    if (altName != null) {
                        result.add(altName);
                    }
                }
            }
            return result;
        } catch (CertificateParsingException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns true if {@code hostName} matches the name or pattern {@code cn}.
     *
     * @param hostName lowercase host name.
     * @param cn certificate host name. May include wildcards like
     *     {@code *.android.com}.
     */
    public boolean verifyHostName(String hostName, String cn) {
        if (hostName == null || hostName.isEmpty() || cn == null || cn.isEmpty()) {
            return false;
        }

        cn = cn.toLowerCase(Locale.US);
        if (hostName.equals(cn)) {
            return true;
        }

        String[] hostNameParts = hostName.split("\\.");
        String[] cnParts = cn.split("\\.");
        if (hostNameParts.length < cnParts.length) {
          // cn has a '*.'-prefix of hostName: *.Y.X matches Y.X
          return cn.equals("*." + hostName);
        }

        for (int i = cnParts.length - 1; i >= 0; --i) {
            if (hostNameParts[i].equals(cnParts[i])) {
                continue;
            }
            // special *-match: *.Y.X matches Z.Y.X but not W.Z.Y.X
            if (i != 0 || hostNameParts.length != cnParts.length) {
                return false;
            }
            if (cnParts[0].equals("*")) {
                return true;
            }
            // *-component match: f*.com matches foo.com but not bar.com
            return wildcardMatch(hostNameParts[0], cnParts[0]);
        }

        // hostName is a '.'-suffix of cn: Z.Y.X matches X
        return true;
    }

    /**
     * Returns true if {@code cnPart} matches {@code hostPart}.
     *
     * @param hostPart a lowercase host name
     * @param cnPart a lowercase host name pattern.
     */
    private boolean wildcardMatch(String hostPart, String cnPart) {
        int starIndex = cnPart.indexOf('*');
        if (starIndex == -1 || hostPart.length() < cnPart.length() - 1) {
            return false;
        }

        String prefix = cnPart.substring(0,  starIndex);
        String suffix = cnPart.substring(starIndex + 1);
        return hostPart.startsWith(prefix) && hostPart.endsWith(suffix);
    }
}
