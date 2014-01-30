/*
 * Copyright (C) 2014 The Android Open Source Project
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

package java.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.channels.MembershipKey;
import java.nio.channels.MulticastChannel;

/**
 * An implementation of {@link MembershipKey}.
 *
 * To keep this class simple and keep all mutation operations in one place and easily synchronized,
 * most socket logic is held in {@link java.nio.DatagramChannelImpl}.
 */
final class MembershipKeyImpl extends MembershipKey {

  private final DatagramChannelImpl channel;
  private final InetAddress groupAddress;
  private final NetworkInterface networkInterface;
  private final InetAddress sourceAddress;
  private volatile boolean isValid;

  public MembershipKeyImpl(DatagramChannelImpl channel, NetworkInterface networkInterface,
      InetAddress groupAddress, InetAddress sourceAddress) {

    this.channel = channel;
    this.networkInterface = networkInterface;
    this.groupAddress = groupAddress;
    this.sourceAddress = sourceAddress;
    this.isValid = true;
  }

  @Override
  public boolean isValid() {
    // invalidate() is called if the key is dropped, but for simplicity it is not
    // invalidated when the channel is closed. Therefore, the channel must also be checked to see
    // if it is still open.
    return isValid && channel.isOpen();
  }

  void invalidate() {
    this.isValid = false;
  }

  @Override
  public void drop() {
    channel.multicastDrop(this);
  }

  @Override
  public MembershipKey block(InetAddress source) throws IOException {
    channel.multicastBlock(this, source);
    return this;
  }

  @Override
  synchronized public MembershipKey unblock(InetAddress source) {
    channel.multicastUnblock(this, source);
    return this;
  }

  @Override
  public MulticastChannel channel() {
    return channel;
  }

  @Override
  public InetAddress group() {
    return groupAddress;
  }

  @Override
  public NetworkInterface networkInterface() {
    return networkInterface;
  }

  @Override
  public InetAddress sourceAddress() {
    return sourceAddress;
  }

  @Override
  public String toString() {
    return "MembershipKeyImpl{" +
      "groupAddress=" + groupAddress +
      ", networkInterface=" + networkInterface +
      ", sourceAddress=" + sourceAddress +
      '}';
    }
}
