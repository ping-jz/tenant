package org.example.net;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract Over netty channel
 *
 * @author ZJP
 * @since 2021年08月13日 17:44:32
 **/
public class Connection {

  private Logger logger = LoggerFactory
      .getLogger(getClass());

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
    if (isActive()) {
      return invokeFutures.putIfAbsent(future.id(), future);
    } else {
      closeFuture(future);
      return null;
    }
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

  public void close() {
    if (isActive()) {
      channel.close();
    }

    for (InvokeFuture future : invokeFutures.values()) {
      closeFuture(future);
    }
    invokeFutures.clear();
  }

  private void closeFuture(InvokeFuture future) {
    try {
      future.putResult(Message.of().status(MessageStatus.SERVER_EXCEPTION));
      future.executeCallBack();
    } catch (Exception e) {
      logger
          .error(
              "Exception occurred in user defined InvokeCallback#onResponse() logic.",
              e);
    }
  }
}
