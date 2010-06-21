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

package java.security;

import java.security.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.TestSSLContext;
import junit.framework.TestCase;

public class ProviderTest extends TestCase {

    public void test_Provider_getServices() throws Exception {

        // build set of expected algorithms
        Map<String,Set<String>> remaining
                = new HashMap<String,Set<String>>(StandardNames.PROVIDER_ALGORITHMS);
        for (Entry<String,Set<String>> entry : remaining.entrySet()) {
            entry.setValue(new HashSet<String>(entry.getValue()));
        }

        List<String> extra = new ArrayList();
        List<String> missing = new ArrayList();

        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            String providerName = provider.getName();
            // ignore BouncyCastle provider if it is installed on the RI
            if (StandardNames.IS_RI && providerName.equals("BC")) {
                continue;
            }
            Set<Provider.Service> services = provider.getServices();
            assertNotNull(services);
            assertFalse(services.isEmpty());

            for (Provider.Service service : services) {
                String type = service.getType();
                String algorithm = service.getAlgorithm().toUpperCase();
                String className = service.getClassName();
                if (false) {
                    System.out.println(providerName
                                       + " " + type
                                       + " " + algorithm
                                       + " " + className);
                }

                // remove from remaining, assert unknown if missing
                Set<String> algorithms = remaining.get(type);
                if (algorithms == null || !algorithms.remove(algorithm)) {
                    // seems to be missing, but sometimes the same
                    // algorithm is available from multiple providers
                    // (e.g. KeyFactory RSA is available from
                    // SunRsaSign and SunJSSE), so double check in
                    // original source before giving error
                    if (!(StandardNames.PROVIDER_ALGORITHMS.containsKey(type)
                            && StandardNames.PROVIDER_ALGORITHMS.get(type).contains(algorithm))) {
                        extra.add("Unknown " + type + " " + algorithm + "\n");
                    }
                }
                if (algorithms != null && algorithms.isEmpty()) {
                    remaining.remove(type);
                }

                // make sure class exists and can be initialized
                try {
                    assertNotNull(Class.forName(className,
                                                true,
                                                provider.getClass().getClassLoader()));
                } catch (ClassNotFoundException e) {
                    missing.add(className);
                }
            }
        }

        // assert that we don't have any extra in the implementation
        Collections.sort(extra); // sort so that its grouped by type
        assertEquals(Collections.EMPTY_LIST, extra);

        // assert that we don't have any missing in the implementation
        assertEquals(Collections.EMPTY_MAP, remaining);

        // assert that we don't have any missing classes
        Collections.sort(missing); // sort it for readability
        assertEquals(Collections.EMPTY_LIST, missing);
    }
}
