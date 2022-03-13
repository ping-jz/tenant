package org.example.net.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import java.net.InetSocketAddress;
import java.util.Objects;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.example.net.codec.MessageCodec;
import org.example.serde.Serializer;
import org.example.util.NettyEventLoopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认客户端，专注于链接的管理。尽量保持代码的专一
 *
 * @author ZJP
 * @since 2021年08月13日 14:56:18
 **/
public class DefaultClient implements AutoCloseable {

  private Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * 链接管理
   */
  private ConnectionManager manager;

  /**
   * client bootstrap
   */
  private Bootstrap bootstrap;
  /**
   * connection handler
   */
  private ChannelHandler handler;
  /** codec */
  private Serializer<?> codec;

  public DefaultClient() {
    manager = new ConnectionManager();
  }

  public void init(EventLoopGroup eventExecutors) {
    Objects.requireNonNull(handler, "connection handler can't be null");

    DefaultClient client = this;
    bootstrap = new Bootstrap();
    bootstrap.group(eventExecutors)
        .channel(NettyEventLoopUtil.getClientSocketChannelClass())
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            Objects.requireNonNull(codec, "codec Handler can't not be null");

            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("codec", new MessageCodec(client.codec()));
            pipeline.addLast("handler", client.handler());
          }
        });
  }

  public ConnectionManager manager() {
    return manager;
  }

  /**
   * A InetSocketAddress like this 127.0.0.1:8080 or /127.0.0.1:8080
   *
   * @param addr ip address
   * @since 2021年08月13日 18:47:18
   */
  public Connection getConnection(String addr) {
    return manager.connections().computeIfAbsent(addr, this::createConnection);
  }


  private Connection createConnection(String original) {
    try {
      String addr = original;
      if (addr.startsWith("/")) {
        addr = addr.substring(1);
      }

      int lastColon = addr.lastIndexOf(':');
      String ip = addr.substring(0, lastColon);
      int port = Integer.parseInt(addr.substring(lastColon + 1));

      ChannelFuture future = bootstrap.connect(new InetSocketAddress(ip, port));
      boolean suc = future.awaitUninterruptibly().isSuccess();
      if (suc) {
        future.channel().pipeline().addLast("manager", manager);
        return new Connection(future.channel(), original);
      } else {
        logger.error("connect to {} failed", original);
      }
    } catch (Exception e) {
      logger.error("create connection for addr:{}, error", original);
    }

    return null;
  }

  public ChannelHandler handler() {
    return handler;
  }

  public DefaultClient handler(ChannelHandler handler) {
    this.handler = handler;
    return this;
  }

  public Serializer codec() {
    return codec;
  }

  public DefaultClient codec(Serializer codec) {
    this.codec = codec;
    return this;
  }

  @Override
  public void close() {
    manager.close();
  }


}
