package org.example.net.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.example.net.Connection;
import org.example.util.NettyEventLoopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server for rpc
 *
 * @author ZJP
 * @since 2021年08月13日 14:56:18
 **/
public class RpcClient implements AutoCloseable {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  /** connection manager */
  private Map<String, Connection> connections;

  /** client bootstrap */
  private Bootstrap bootstrap;
  /** connection handler */
  private ChannelHandler handler;

  public RpcClient() {
    connections = new ConcurrentHashMap<>();
  }

  public void init(EventLoopGroup eventExecutors) {
    Objects.requireNonNull(handler, "connection handler can't be null");
    bootstrap = new Bootstrap();
    bootstrap.group(eventExecutors)
        .channel(NettyEventLoopUtil.getClientSocketChannelClass())
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(handler);
  }

  /**
   * A InetSocketAddress like this 127.0.0.1:8080 or /127.0.0.1:8080
   *
   * @param addr ip address
   * @since 2021年08月13日 18:47:18
   */
  public Connection getConnection(String addr) {
    return connections.computeIfAbsent(addr, this::createConnection);
  }


  private Connection createConnection(String addr) {
    try {
      if (addr.startsWith("/")) {
        addr = addr.substring(1);
      }

      int lastColon = addr.lastIndexOf(':');
      String ip = addr.substring(0, lastColon);
      int port = Integer.parseInt(addr.substring(lastColon + 1));

      ChannelFuture future = bootstrap.connect(new InetSocketAddress(ip, port));
      boolean suc = future.awaitUninterruptibly().isSuccess();
      if (suc) {
        return new Connection(future.channel());
      } else {
        logger.error("connect to {} failed", addr);
      }
    } catch (Exception e) {
      logger.error("create connection for addr:{}, error", addr);
    }

    return null;
  }

  public ChannelHandler handler() {
    return handler;
  }

  public RpcClient handler(ChannelHandler handler) {
    this.handler = handler;
    return this;
  }

  @Override
  public void close() {
    for (Connection connection : connections.values()) {
      connection.channel().close();
    }
  }
}
