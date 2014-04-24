package java.nio;

import android.system.StructGroupReq;
import android.system.StructGroupSourceReq;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.MembershipKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import libcore.io.IoBridge;

/**
 * A helper class for {@link DatagramChannelImpl} that keeps track of multicast group
 * memberships. This class is not threadsafe, and relies on the DatagramChannelImpl to synchronize.
 *
 * <p>See <a href="http://tools.ietf.org/html/rfc3678">RFC 3678</a> for context and terminology.
 */
final class MulticastMembershipHandler {

  private final DatagramChannelImpl channel;
  private final Map<Id, Membership> memberships = new HashMap<Id, Membership>();

  MulticastMembershipHandler(DatagramChannelImpl channel) {
    this.channel = channel;
  }

  /**
   * The implementation for
   * {@link java.nio.channels.MulticastChannel#join(InetAddress, NetworkInterface)}.
   */
  public MembershipKeyImpl addAnySourceMembership(
      NetworkInterface networkInterface, InetAddress groupAddress) throws SocketException {

    validateMulticastGroupArgs(groupAddress, networkInterface);
    assertChannelOpen();

    Id id = new Id(networkInterface, groupAddress);
    Membership membership = memberships.get(id);
    if (membership != null) {
      return membership.getAnySourceMembershipKey();
    }

    // No existing membership found. Attempt to join.
    StructGroupReq groupReq = makeGroupReq(groupAddress, networkInterface);
    IoBridge.setSocketOption(channel.getFD(), IoBridge.JAVA_MCAST_JOIN_GROUP, groupReq);

    // Record the membership and return the key.
    membership = Membership.createAnySource(channel, networkInterface, groupAddress);
    memberships.put(id, membership);
    return membership.getAnySourceMembershipKey();
  }

  /**
   * The implementation for
   * {@link java.nio.channels.MulticastChannel#join(InetAddress, NetworkInterface, InetAddress)}.
   */
  public MembershipKeyImpl addSourceSpecificMembership(
      NetworkInterface networkInterface, InetAddress groupAddress, InetAddress sourceAddress)
      throws SocketException {

    validateMulticastGroupArgs(groupAddress, networkInterface);
    validateSourceAddress(sourceAddress);
    validateAddressProtocolTheSame(groupAddress, sourceAddress);
    assertChannelOpen();

    Id id = new Id(networkInterface, groupAddress);
    Membership membership = memberships.get(id);
    if (membership != null) {
      MembershipKeyImpl existingMembershipKey =
          membership.getSourceSpecificMembershipKey(sourceAddress);
      if (existingMembershipKey != null) {
        return existingMembershipKey;
      }
    }

    // No existing membership found. Attempt to join.
    IoBridge.setSocketOption(channel.getFD(), IoBridge.JAVA_MCAST_JOIN_SOURCE_GROUP,
        makeGroupSourceReq(groupAddress, networkInterface, sourceAddress));

    if (membership == null) {
      // Record the membership and return the key.
      membership = Membership.createSourceSpecific(
          channel, networkInterface, groupAddress, sourceAddress);
      memberships.put(id, membership);
      return membership.getSourceSpecificMembershipKey(sourceAddress);
    } else {
      // Add a new source to the existing membership.
      return membership.addSource(sourceAddress);
    }
  }

  /**
   * The implementation for {@link MembershipKey#drop()}.
   */
  public void dropMembership(MembershipKeyImpl membershipKey) {
    // For compatibility with the RI, this is one case where the membershipKey can no longer be
    // valid.
    if (!membershipKey.isValid()) {
      return;
    }
    if (membershipKey.channel() != this.channel) {
      throw new AssertionError("Bad membership key");
    }
    assertChannelOpen();

    Id id = createId(membershipKey);
    Membership membership = memberships.get(id);
    if (membership == null) {
      throw new AssertionError("Bad membership key" + membershipKey);
    }

    if (!membership.isSourceSpecific()) {
      try {
        StructGroupReq groupReq =
            makeGroupReq(membershipKey.group(), membershipKey.networkInterface());
        IoBridge.setSocketOption(channel.getFD(), IoBridge.JAVA_MCAST_LEAVE_GROUP, groupReq);
      } catch (SocketException e) {
        // TODO: Obtain opinion on how to report this, throw this or if it is safe to ignore.
        throw new IllegalStateException(e);
      }
      memberships.remove(id);
    } else {
      StructGroupSourceReq groupSourceReq = makeGroupSourceReq(membershipKey.group(),
          membershipKey.networkInterface(), membershipKey.sourceAddress());

      try {
        IoBridge.setSocketOption(
            channel.getFD(), IoBridge.JAVA_MCAST_LEAVE_SOURCE_GROUP,
            groupSourceReq);
      } catch (SocketException e) {
        // TODO: Obtain opinion on how to report this, throw this or if it is safe to ignore.
        throw new IllegalStateException(e);
      }

      boolean isLast = membership.removeSource(membershipKey.sourceAddress());
      if (isLast) {
        memberships.remove(id);
      }
    }
    membershipKey.invalidate();
  }

  /**
   * The implementation for {@link MembershipKey#block(java.net.InetAddress)}.
   */
  public void block(MembershipKeyImpl membershipKey, InetAddress sourceAddress)
      throws SocketException {
    validateMembershipKey(membershipKey);
    validateSourceAddress(sourceAddress);
    validateAddressProtocolTheSame(membershipKey.group(), sourceAddress);
    assertChannelOpen();

    Membership membership = getMembershipForKey(membershipKey);
    if (membership == null) {
      throw new AssertionError("Bad membership key" + membershipKey);
    }

    if (membership.isBlocked(sourceAddress)) {
      return;
    }

    IoBridge.setSocketOption(channel.getFD(), IoBridge.JAVA_MCAST_BLOCK_SOURCE,
        makeGroupSourceReq(
            membershipKey.group(), membershipKey.networkInterface(), sourceAddress));

    membership.block(sourceAddress);
  }

  /**
   * The implementation for {@link MembershipKey#unblock(java.net.InetAddress)}.
   */
  public void unblock(MembershipKeyImpl membershipKey, InetAddress sourceAddress) {
    validateMembershipKey(membershipKey);
    validateSourceAddress(sourceAddress);
    validateAddressProtocolTheSame(membershipKey.group(), sourceAddress);
    assertChannelOpen();

    Membership membership = getMembershipForKey(membershipKey);
    if (membership == null) {
      throw new AssertionError("Bad membership key" + membershipKey);
    }

    if (!membership.isBlocked(sourceAddress)) {
      throw new IllegalStateException(
          "sourceAddress " + sourceAddress + " is not blocked for " + membership.debugId());
    }

    try {
      IoBridge.setSocketOption(channel.getFD(), IoBridge.JAVA_MCAST_UNBLOCK_SOURCE,
          makeGroupSourceReq(membershipKey.group(), membershipKey.networkInterface(),
              sourceAddress));
    } catch (SocketException e) {
      throw new IllegalStateException(e);
    }

    membership.unblock(sourceAddress);
  }

  private Membership getMembershipForKey(MembershipKey membershipKey) {
    Id id = createId(membershipKey);
    Membership membership = memberships.get(id);
    if (membership == null) {
      throw new AssertionError("No membership found for id " + id);
    }
    return membership;
  }

  private void assertChannelOpen() {
    if (!channel.isOpen()) {
      throw new AssertionError("Channel is closed");
    }
  }

  private void validateMembershipKey(MembershipKeyImpl membershipKey) {
    if (membershipKey.channel() != this.channel) {
      throw new AssertionError("Invalid or bad membership key");
    }
    if (!membershipKey.isValid()) {
      throw new IllegalStateException("Membership key is no longer valid: " + membershipKey);
    }
  }

  private static Id createId(MembershipKey membershipKey) {
    return new Id(membershipKey.networkInterface(), membershipKey.group());
  }

  private static void validateSourceAddress(InetAddress sourceAddress) {
    if (sourceAddress.isAnyLocalAddress()) {
      throw new IllegalArgumentException(
          "sourceAddress must not be a wildcard address, is " + sourceAddress);
    }
    if (sourceAddress.isMulticastAddress()) {
      throw new IllegalArgumentException(
          "sourceAddress must be a unicast address, is " + sourceAddress);
    }
  }

  private static void validateMulticastGroupArgs(
      InetAddress groupAddress, NetworkInterface networkInterface) throws SocketException {

    if (groupAddress == null) {
      // RI throws NullPointerException.
      throw new NullPointerException("groupAddress == null");
    }
    if (networkInterface == null) {
      // RI throws NullPointerException.
      throw new NullPointerException("networkInterface == null");
    }
    if (!networkInterface.isLoopback() && !networkInterface.supportsMulticast()) {
      throw new IllegalArgumentException(
          "networkInterface " + networkInterface + " does not support multicast");
    }
    if (!groupAddress.isMulticastAddress()) {
      throw new IllegalArgumentException("Not a multicast group: " + groupAddress);
    }
  }

  private static void validateAddressProtocolTheSame(
      InetAddress groupAddress, InetAddress sourceAddress) {

    if (groupAddress.getClass() != sourceAddress.getClass()) {
      throw new IllegalArgumentException("Mixed address types not permitted: groupAddress: " +
          groupAddress + ", sourceAddress: " + sourceAddress);
    }
  }

  private static StructGroupSourceReq makeGroupSourceReq(
      InetAddress gsr_group, NetworkInterface networkInterface, InetAddress gsr_source) {
    int gsr_interface = (networkInterface != null) ? networkInterface.getIndex() : 0;
    return new StructGroupSourceReq(gsr_interface, gsr_group, gsr_source);
  }

  private static StructGroupReq makeGroupReq(InetAddress gr_group,
      NetworkInterface networkInterface) {
    int gr_interface = (networkInterface != null) ? networkInterface.getIndex() : 0;
    return new StructGroupReq(gr_interface, gr_group);
  }

  /**
   * Membership information associated with an {@link Id}. A membership can be one of two types:
   * "source-specific" and "any-source". The two types a mutually exclusive for a given Id.
   */
  static final class Membership {

    private final DatagramChannelImpl channel;
    private final InetAddress groupAddress;
    private final NetworkInterface networkInterface;

    // Any-source membership key. Mutually exclusive with sourceSpecificMembershipKeys.
    private final MembershipKeyImpl anySourceMembershipKey;
    // Blocked source addresses for any-source memberships. Assigned when required.
    private Set<InetAddress> blockedSourceAddresses;

    // Source-specific membership keys. Mutually exclusive with anySourceMembershipKey.
    private final Map<InetAddress, MembershipKeyImpl> sourceSpecificMembershipKeys;

    /** Use {@link #createSourceSpecific} or {@link #createAnySource} to construct. */
    private Membership(
        DatagramChannelImpl channel,
        InetAddress groupAddress,
        NetworkInterface networkInterface,
        MembershipKeyImpl anySourceMembershipKey,
        Map<InetAddress, MembershipKeyImpl> sourceSpecificMembershipKeys) {

      this.channel = channel;
      this.groupAddress = groupAddress;
      this.networkInterface = networkInterface;
      this.anySourceMembershipKey = anySourceMembershipKey;
      this.sourceSpecificMembershipKeys = sourceSpecificMembershipKeys;
    }

    /** Creates an any-source membership. */
    public static Membership createAnySource(DatagramChannelImpl channel,
        NetworkInterface networkInterface, InetAddress groupAddress) {

      MembershipKeyImpl withoutSourceAddressKey =
          new MembershipKeyImpl(channel, networkInterface, groupAddress, null /* sourceAddress */);
      return new Membership(
          channel, groupAddress, networkInterface, withoutSourceAddressKey,
          null /* sourceSpecificMembershipKeys */);
    }

    /**
     * Creates a source-specific membership. See {@link #addSource} to add additional source
     * addresses.
     */
    public static Membership createSourceSpecific(DatagramChannelImpl channel,
        NetworkInterface networkInterface, InetAddress groupAddress, InetAddress sourceAddress) {

      Map<InetAddress, MembershipKeyImpl> withSourceKeys =
          new HashMap<InetAddress, MembershipKeyImpl>();
      Membership membership = new Membership(
          channel, groupAddress, networkInterface, null /* anySourceMembershipKey */,
          withSourceKeys);
      membership.addSource(sourceAddress);
      return membership;
    }

    /**
     * Adds a new source address filter to an existing membership, returning the associated
     * {@link MembershipKeyImpl}. Throws an {@code IllegalStateException} if this is an
     * any-source membership.
     */
    public MembershipKeyImpl addSource(InetAddress sourceAddress) {
      if (sourceSpecificMembershipKeys == null) {
        throw new IllegalStateException(
            "Can only add sources to source-specific memberships: " + debugId());
      }

      MembershipKeyImpl membershipKey =
          new MembershipKeyImpl(channel, networkInterface, groupAddress, sourceAddress);
      sourceSpecificMembershipKeys.put(sourceAddress, membershipKey);
      return membershipKey;
    }

    /**
     * Removes the specified {@code sourceAddress} from the set of filters. Returns {@code true} if
     * the set of filters is now empty. Throws an {@code IllegalStateException} if this is an
     * any-source membership.
     */
    public boolean removeSource(InetAddress sourceAddress) {
      if (sourceSpecificMembershipKeys == null) {
        throw new IllegalStateException(
            "Can only remove sources from source-specific memberships: " + debugId());
      }
      sourceSpecificMembershipKeys.remove(sourceAddress);
      return sourceSpecificMembershipKeys.isEmpty();
    }

    /**
     * Returns {@code true} if the membership source-specific, false if it is any-source.
     */
    public boolean isSourceSpecific() {
      return sourceSpecificMembershipKeys != null;
    }

    /**
     * Returns the {@link MembershipKeyImpl} for this membership. Throws an
     * {@code IllegalStateException} if this is not an any-source membership.
     */
    public MembershipKeyImpl getAnySourceMembershipKey() {
      if (sourceSpecificMembershipKeys != null) {
        throw new IllegalStateException(
            "There an existing source-specific membership for " + debugId());
      }
      return anySourceMembershipKey;
    }

    /**
     * Returns the {@link MembershipKeyImpl} for the specified {@code sourceAddress}. Throws an
     * {@code IllegalStateException} if this is not a source-specific membership.
     */
    public MembershipKeyImpl getSourceSpecificMembershipKey(InetAddress sourceAddress) {
      if (anySourceMembershipKey != null) {
        throw new IllegalStateException("There an existing any-source membership for " + debugId());
      }
      return sourceSpecificMembershipKeys.get(sourceAddress);
    }

    /**
     * Returns {@code true} if there is an existing block for the specified address. Throws an
     * {@code IllegalStateException} if this is not an any-source membership.
     */
    public boolean isBlocked(InetAddress sourceAddress) {
      if (anySourceMembershipKey == null) {
        throw new IllegalStateException(
            "block()/unblock() are only applicable for any-source memberships: " + debugId());
      }
      return blockedSourceAddresses != null && blockedSourceAddresses.contains(sourceAddress);
    }

    /**
     * Adds a blocked address to this membership. Throws an {@code IllegalStateException} if
     * the address is already blocked. Throws an {@code IllegalStateException} if this is not an
     * any-source membership.
     */
    public void block(InetAddress sourceAddress) {
      if (anySourceMembershipKey == null) {
        throw new IllegalStateException(
            "block() is not supported for source-specific group memberships: " + debugId());
      }
      if (blockedSourceAddresses == null) {
        blockedSourceAddresses = new HashSet<InetAddress>();
      }
      if (!blockedSourceAddresses.add(sourceAddress)) {
        throw new IllegalStateException(
            "Could not block " + sourceAddress + ": it was already blocked for " + debugId());
      }
    }

    /**
     * Removes a blocked address from this membership. Throws an {@code IllegalStateException} if
     * the address is not blocked. Throws an {@code IllegalStateException} if this is not an
     * any-source membership.
     */
    public void unblock(InetAddress sourceAddress) {
      if (anySourceMembershipKey == null) {
        throw new IllegalStateException(
            "unblock() is not supported for source-specific group memberships: " + debugId());
      }
      if (blockedSourceAddresses == null || !blockedSourceAddresses.remove(sourceAddress)) {
        throw new IllegalStateException(
            "Could not unblock " + sourceAddress + ": it was not blocked for " + debugId());
      }
    }

    public String debugId() {
      return "<" + networkInterface + ":" + groupAddress + ">";
    }

  }

  /** An identifier for a multicast group membership, independent of membership type. */
  private static final class Id {

    private final InetAddress groupAddress;
    private final NetworkInterface networkInterface;

    public Id(NetworkInterface networkInterface, InetAddress groupAddress) {
      this.groupAddress = groupAddress;
      this.networkInterface = networkInterface;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Id)) {
        return false;
      }

      Id id = (Id) o;

      if (!groupAddress.equals(id.groupAddress)) {
        return false;
      }
      if (!networkInterface.equals(id.networkInterface)) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = groupAddress.hashCode();
      result = 31 * result + networkInterface.hashCode();
      return result;
    }
  }
}
