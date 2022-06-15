package org.example.net;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract Over netty channel
 *
 * @author ZJP
 * @since 2021年08月13日 17:44:32
 **/
public class Connection {

  private Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Attribute key for connection
   */
  public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");

  /** channelId 生产 */
  public static final AtomicInteger IdGenerator = new AtomicInteger();

  /** channelId */
  private Integer id;
  /** a netty channel */
  private Channel channel;
  /** callBack future */
  private final ConcurrentHashMap<Integer, DefaultInvokeFuture<?>> invokeFutures = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public <T> DefaultInvokeFuture<T> addInvokeFuture(DefaultInvokeFuture<T> future) {
    if (isActive()) {
      return (DefaultInvokeFuture<T>) invokeFutures.putIfAbsent(future.id(), future);
    } else {
      closeFuture(future);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  public <T> DefaultInvokeFuture<T> removeInvokeFuture(int msgId) {
    return (DefaultInvokeFuture<T>) invokeFutures.remove(msgId);
  }

  public Connection(Channel channel, Integer id) {
    this.channel = channel;
    this.id = id;
    this.channel.attr(CONNECTION).set(this);
  }

  public Channel channel() {
    return channel;
  }

  public Integer id() {
    return id;
  }

  public boolean isActive() {
    return channel != null && channel.isActive();
  }

  public void close() {
    if (isActive()) {
      channel.close();
    }

    for (DefaultInvokeFuture<?> future : invokeFutures.values()) {
      closeFuture(future);
    }
    invokeFutures.clear();
  }

  private void closeFuture(DefaultInvokeFuture<?> future) {
    try {
      future.cancelTimeout();
      future.executeCallBack(Message.of().status(MessageStatus.CLOSE));
    } catch (Exception e) {
      logger.error("Exception occurred in user defined InvokeCallback#onResponse() logic.", e);
    }
  }
}
