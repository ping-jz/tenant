package org.example.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 链接管理者
 * <p>
 *
 * @author ZJP
 * @since 2021年08月15日 22:34:55
 **/
@Sharable
public class ConnectionManager extends ChannelInboundHandlerAdapter implements AutoCloseable {

  public static final Integer IDLE_TIME = 90000;

  private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

  private final ConcurrentHashMap<Integer, Connection> connections;
  private final ConcurrentHashMap<ChannelId, Channel> anonymous;
  private final AtomicInteger callBackMsgId;
  /** callBack future */
  private final ConcurrentHashMap<Integer, CompletableFuture<?>> invokeFutures = new ConcurrentHashMap<>();

  public ConnectionManager() {
    connections = new ConcurrentHashMap<>();
    anonymous = new ConcurrentHashMap<>();
    callBackMsgId = new AtomicInteger();
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      try {
        logger.warn("Connection idle, close it from server side: {}",
            ctx.channel().remoteAddress());
        ctx.close();
      } catch (Exception e) {
        logger.warn("Exception caught when closing connection in ServerIdleHandler.", e);
      }
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    Channel channel = ctx.channel();
    if (channel.attr(Connection.CONNECTION).get() == null) {
      anonymous.put(channel.id(), channel);
    }
    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    Channel channel = ctx.channel();
    Connection connection = channel.attr(Connection.CONNECTION).getAndSet(null);
    if (connection != null) {
      connections.remove(connection.id());
      connection.close();
    }
    anonymous.remove(channel.id());

    ctx.fireChannelInactive();
  }

  public Collection<Connection> connections() {
    return connections.values();
  }

  public Connection connection(Integer address) {
    return connections.get(address);
  }

  public void registerConnection(Connection connection) {
    anonymous.remove(connection.channel().id());
    connections.put(connection.id(), connection);
  }

  @SuppressWarnings("unchecked")
  public <T> CompletableFuture<T> addInvokeFuture(Connection connection, Integer id,
      CompletableFuture<T> future) {
    if (connection.isActive()) {
      return (CompletableFuture<T>) invokeFutures.putIfAbsent(id, future);
    } else {
      closeFuture(future, stateException());
      return null;
    }
  }

  public int nextCallBackMsgId() {
    return callBackMsgId.incrementAndGet();
  }

  private void closeFuture(CompletableFuture<?> future, Exception exception) {
    if (future.isDone()) {
      return;
    }

    try {
      future.completeExceptionally(exception);
    } catch (Exception e) {
      logger.error("Exception occurred in user defined InvokeCallback#onResponse() logic.", e);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> CompletableFuture<T> removeInvokeFuture(int msgId) {
    return (CompletableFuture<T>) invokeFutures.remove(msgId);
  }

  public <T> void removeInvokeFuture(int msgId, CompletableFuture<T> future) {
    invokeFutures.remove(msgId, future);
  }

  private static IllegalStateException stateException() {
    return new IllegalStateException("Connection is close");
  }

  @Override
  public void close() {
    for (Connection connection : connections.values()) {
      connection.close();
    }
    connections.clear();
  }
}
