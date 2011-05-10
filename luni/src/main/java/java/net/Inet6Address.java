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

package java.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.util.Arrays;
import java.util.Enumeration;
import static libcore.io.OsConstants.*;

/**
 * An IPv6 address. See {@link InetAddress}.
 */
public final class Inet6Address extends InetAddress {

    private static final long serialVersionUID = 6880410070516793377L;

    /**
     * @hide
     */
    public static final InetAddress ANY =
            new Inet6Address(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, null, 0);

    /**
     * @hide
     */
    public static final InetAddress LOOPBACK =
            new Inet6Address(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
                    "localhost", 0);

    boolean scope_id_set;
    int scope_id;

    boolean scope_ifname_set;
    String ifname;

    /**
     * Constructs an {@code InetAddress} representing the {@code address} and
     * {@code name} and {@code scope_id}.
     *
     * @param address
     *            the network address.
     * @param name
     *            the name associated with the address.
     * @param scope_id
     *            the scope id for link- or site-local addresses.
     */
    Inet6Address(byte[] ipaddress, String hostName, int scope_id) {
        super(AF_INET6, ipaddress, hostName);
        this.scope_id = scope_id;
        this.scope_id_set = (scope_id != 0);
    }

    /**
     * Constructs an IPv6 address according to the given {@code host}, {@code
     * addr} and {@code scope_id}.
     *
     * @param host
     *            the host name associated with the address.
     * @param addr
     *            the network address.
     * @param scope_id
     *            the scope id for link- or site-local addresses.
     * @return the Inet6Address instance representing the IP address.
     * @throws UnknownHostException
     *             if the address is null or has an invalid length.
     */
    public static Inet6Address getByAddress(String host, byte[] addr, int scope_id)
            throws UnknownHostException {
        if (addr == null || addr.length != 16) {
            throw new UnknownHostException("Not an IPv6 address: " + Arrays.toString(addr));
        }
        if (scope_id < 0) {
            scope_id = 0;
        }
        // TODO: should we clone 'addr'?
        return new Inet6Address(addr, host, scope_id);
    }

    /**
     * Gets an IPv6 address instance according to the given {@code host},
     * {@code addr} and {@code nif}. {@code scope_id} is set according to the
     * given {@code nif} and the {@code addr} type (for example site-local or
     * link-local).
     *
     * @param host
     *            the hostname associated with the address.
     * @param addr
     *            the network address.
     * @param nif
     *            the network interface that this address is associated with.
     * @return the Inet6Address instance representing the IP address.
     * @throws UnknownHostException
     *             if the address is {@code null} or has an invalid length or
     *             the interface doesn't have a numeric scope id for the given
     *             address type.
     */
    public static Inet6Address getByAddress(String host, byte[] addr,
            NetworkInterface nif) throws UnknownHostException {

        Inet6Address address = Inet6Address.getByAddress(host, addr, 0);

        // if nif is null, nothing needs to be set.
        if (nif == null) {
            return address;
        }

        // find the first address which matches the type addr,
        // then set the scope_id and ifname.
        Enumeration<InetAddress> addressList = nif.getInetAddresses();
        while (addressList.hasMoreElements()) {
            InetAddress ia = addressList.nextElement();
            if (ia.getAddress().length == 16) {
                Inet6Address v6ia = (Inet6Address) ia;
                boolean isSameType = v6ia.compareLocalType(address);
                if (isSameType) {
                    address.scope_id_set = true;
                    address.scope_id = v6ia.scope_id;
                    address.scope_ifname_set = true;
                    address.ifname = nif.getName();
                    break;
                }
            }
        }
        // if no address matches the type of addr, throws an
        // UnknownHostException.
        if (!address.scope_id_set) {
            throw new UnknownHostException("Scope id not found for address: " + Arrays.toString(addr));
        }
        return address;
    }

    /**
     * Returns {@code true} if one of following cases applies:
     * <p>
     * <ol>
     *  <li>both addresses are site local</li>
     *  <li>both addresses are link local</li>
     *  <li>{@code ia} is neither site local nor link local</li>
     * </ol>
     */
    private boolean compareLocalType(Inet6Address ia) {
        if (ia.isSiteLocalAddress() && isSiteLocalAddress()) {
            return true;
        }
        if (ia.isLinkLocalAddress() && isLinkLocalAddress()) {
            return true;
        }
        if (!ia.isSiteLocalAddress() && !ia.isLinkLocalAddress()) {
            return true;
        }
        return false;
    }

    /**
     * Returns whether this address is an IP multicast address or not.
     */
    @Override public boolean isMulticastAddress() {
        // Multicast addresses are prefixed with 11111111 (255)
        return ipaddress[0] == -1;
    }

    /**
     * Returns true if this address is the unspecified wildcard address "::".
     */
    @Override public boolean isAnyLocalAddress() {
        return Arrays.equals(ipaddress, Inet6Address.ANY.ipaddress);
    }

    /**
     * Returns whether this address is the loopback address or not. The only
     * valid IPv6 loopback address is "::1".
     */
    @Override public boolean isLoopbackAddress() {
        return Arrays.equals(ipaddress, Inet6Address.LOOPBACK.ipaddress);
    }

    /**
     * Returns whether this address is a link-local address or not.
     */
    @Override public boolean isLinkLocalAddress() {
        // the first 10 bits need to be 1111111010 (1018)
        return (ipaddress[0] == -2) && ((ipaddress[1] & 255) >>> 6) == 2;
    }

    /**
     * Returns whether this address is a site-local address or not.
     */
    @Override public boolean isSiteLocalAddress() {
        // the first 10 bits need to be 1111111011 (1019)
        return (ipaddress[0] == -2) && ((ipaddress[1] & 255) >>> 6) == 3;
    }

    /**
     * Returns whether this address is a global multicast address or not. A
     * valid IPv6 global multicast address is 11111111xxxx1110 or FF0E hex.
     */
    @Override public boolean isMCGlobal() {
        // the first byte should be 0xFF and the lower 4 bits
        // of the second byte should be 0xE
        return (ipaddress[0] == -1) && (ipaddress[1] & 15) == 14;
    }

    /**
     * Returns whether this address is a node-local multicast address or not.
     */
    @Override public boolean isMCNodeLocal() {
        // the first byte should be 0xFF and the lower 4 bits
        // of the second byte should be 0x1
        return (ipaddress[0] == -1) && (ipaddress[1] & 15) == 1;
    }

    /**
     * Returns whether this address is a link-local multicast address or not.
     */
    @Override public boolean isMCLinkLocal() {
        // the first byte should be 0xFF and the lower 4 bits
        // of the second byte should be 0x2
        return (ipaddress[0] == -1) && (ipaddress[1] & 15) == 2;
    }

    /**
     * Returns whether this address is a site-local multicast address or not.
     */
    @Override public boolean isMCSiteLocal() {
        // the first byte should be 0xFF and the lower 4 bits
        // of the second byte should be 0x5
        return (ipaddress[0] == -1) && (ipaddress[1] & 15) == 5;
    }

    /**
     * Returns whether this address is a organization-local multicast address or
     * not.
     */
    @Override public boolean isMCOrgLocal() {
        // the first byte should be 0xFF and the lower 4 bits
        // of the second byte should be 0x8
        return (ipaddress[0] == -1) && (ipaddress[1] & 15) == 8;
    }

    /**
     * Returns the scope id if this address is scoped to an interface, 0 otherwise.
     */
    public int getScopeId() {
        return scope_id_set ? scope_id : 0;
    }

    /**
     * Returns the network interface if this address is instanced with a scoped
     * network interface, null otherwise.
     */
    public NetworkInterface getScopedInterface() {
        try {
            return scope_ifname_set ? NetworkInterface.getByName(ifname) : null;
        } catch (SocketException ex) {
            return null;
        }
    }

    /**
     * Returns whether this address is an IPv4-compatible address or not.
     */
    public boolean isIPv4CompatibleAddress() {
        for (int i = 0; i < 12; i++) {
            if (ipaddress[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("ipaddress", byte[].class),
        new ObjectStreamField("scope_id", int.class),
        new ObjectStreamField("scope_id_set", boolean.class),
        new ObjectStreamField("scope_ifname_set", boolean.class),
        new ObjectStreamField("ifname", String.class),
    };

    private void writeObject(ObjectOutputStream stream) throws IOException {
        ObjectOutputStream.PutField fields = stream.putFields();
        if (ipaddress == null) {
            fields.put("ipaddress", null);
        } else {
            fields.put("ipaddress", ipaddress);
        }

        fields.put("scope_id", scope_id);
        fields.put("scope_id_set", scope_id_set);
        fields.put("scope_ifname_set", scope_ifname_set);
        fields.put("ifname", ifname);
        stream.writeFields();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        ipaddress = (byte[]) fields.get("ipaddress", null);
        scope_id = fields.get("scope_id", 0);
        scope_id_set = fields.get("scope_id_set", false);
        ifname = (String) fields.get("ifname", null);
        scope_ifname_set = fields.get("scope_ifname_set", false);
    }

    @Override public String toString() {
        if (ifname != null) {
            return super.toString() + "%" + ifname;
        }
        if (scope_id != 0) {
            return super.toString() + "%" + scope_id;
        }
        return super.toString();
    }
}
