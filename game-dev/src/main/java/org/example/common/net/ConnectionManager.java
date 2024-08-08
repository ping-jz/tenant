package org.example.common.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.example.net.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 链接管理者
 * <p>
 * //TODO 这个需要改造
 *
 * @author ZJP
 * @since 2021年08月15日 22:34:55
 **/
@Sharable
@Deprecated
public class ConnectionManager extends ChannelInboundHandlerAdapter implements AutoCloseable {

  public static final Integer IDLE_TIME = 90000;

  private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);


  /** ip地址 -> 链接 */
  private ConcurrentHashMap<Integer, Connection> connections;



  public ConnectionManager() {
    connections = new ConcurrentHashMap<>();
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
      Connection connection = createConnection(channel);
      channel.attr(Connection.CONNECTION).set(connection);
      connections.put(connection.id(), connection);
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

    ctx.fireChannelInactive();
  }


  public Connection createConnection(Channel channel) {
    Objects.requireNonNull(channel, "channel can't not be null");
    Connection oldConn = channel.attr(Connection.CONNECTION).get();
    if (oldConn != null) {
      throw new IllegalStateException("duplicated create connection");
    }

    Connection connection = new Connection(channel, -1);
    channel.attr(Connection.CONNECTION).set(connection);
    return connection;
  }

  public ConcurrentHashMap<Integer, Connection> connections() {
    return connections;
  }

  public Connection connection(Integer address) {
    return connections.get(address);
  }

  @Override
  public void close() {
    for (Connection connection : connections.values()) {
      connection.close();
    }
    connections.clear();
  }
}
