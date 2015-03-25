/*
 * Copyright (C) 2015 The Android Open Source Project
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

package libcore.net;

import libcore.net.url.FtpURLConnection;

/**
 * Network security policy for this process/application.
 *
 * <p>Network stacks/components are expected to honor this policy. Components which can use the
 * Android framework API should be accessing this policy via the framework's
 * {@code android.security.NetworkSecurityPolicy} instead of via this class.
 *
 * <p>The policy currently consists of a single flag: whether cleartext network traffic is
 * permitted. See {@link #isCleartextTrafficPermitted()}.
 */
public class NetworkSecurityPolicy {

    private static volatile boolean cleartextTrafficPermitted = true;

    /**
     * Returns whether cleartext network traffic (e.g. HTTP, FTP, XMPP, IMAP, SMTP -- without TLS or
     * STARTTLS) is permitted for this process.
     *
     * <p>When cleartext network traffic is not permitted, the platform's components (e.g. HTTP
     * stacks, {@code WebView}, {@code MediaPlayer}) will refuse this process's requests to use
     * cleartext traffic. Third-party libraries are encouraged to do the same.
     *
     * <p>This flag is honored on a best effort basis because it's impossible to prevent all
     * cleartext traffic from an application given the level of access provided to applications on
     * Android. For example, there's no expectation that {@link java.net.Socket} API will honor this
     * flag. Luckily, most network traffic from apps is handled by higher-level network stacks which
     * can be made to honor this flag. Platform-provided network stacks (e.g. HTTP and FTP) honor
     * this flag from day one, and well-established third-party network stacks will eventually
     * honor it.
     *
     * <p>See {@link FtpURLConnection} for an example of honoring this flag.
     */
    public static boolean isCleartextTrafficPermitted() {
        return cleartextTrafficPermitted;
    }

    /**
     * Sets whether cleartext network traffic (e.g. HTTP, FTP, XMPP, IMAP, SMTP -- without TLS or
     * STARTTLS) is permitted for this process.
     *
     * @see #isCleartextTrafficPermitted()
     */
    public static void setCleartextTrafficPermitted(boolean permitted) {
        cleartextTrafficPermitted = permitted;
    }
}
