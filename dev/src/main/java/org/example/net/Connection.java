package org.example.net;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An abstract Over netty channel
 *
 * @author ZJP
 * @since 2021年08月13日 17:44:32
 **/
public class Connection {

  /** Attribute key for connection */
  public static final AttributeKey<Connection> CONNECTION = AttributeKey
      .valueOf("connection");

  /** ip address */
  private String address;
  /** a netty channel */
  private Channel channel;
  /** callBack future */
  private final ConcurrentHashMap<Integer, InvokeFuture> invokeFutures = new ConcurrentHashMap<>();

  public InvokeFuture addInvokeFuture(InvokeFuture future) {
    return invokeFutures.putIfAbsent(future.id(), future);
  }

  public InvokeFuture removeInvokeFuture(int msgId) {
    return invokeFutures.remove(msgId);
  }


  public Connection(Channel channel, String address) {
    this.channel = channel;
    this.address = address;
    this.channel.attr(CONNECTION).set(this);
  }

  public Channel channel() {
    return channel;
  }

  public String address() {
    return address;
  }

  public boolean isActive() {
    return channel != null && channel.isActive();
  }
}
