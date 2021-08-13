package org.example.net;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * An abstract Over netty channel
 *
 * @author ZJP
 * @since 2021年08月13日 17:44:32
 **/
public class Connection {

  /** a netty channel */
  private Channel channel;

  /** Attribute key for connection */
  public static final AttributeKey<Connection> CONNECTION = AttributeKey
      .valueOf("connection");

  public Connection(Channel channel) {
    this.channel = channel;
    this.channel.attr(CONNECTION).set(this);
  }

  public Channel channel() {
    return channel;
  }

  public boolean isActive() {
    return channel != null && channel.isActive();
  }
}
