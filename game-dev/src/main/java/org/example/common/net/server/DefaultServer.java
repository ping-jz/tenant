package org.example.common.net.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.example.common.ThreadCommonResource;
import org.example.net.ConnectionManager;
import org.example.net.codec.MessageCodec;
import org.example.serde.Serializer;
import org.example.util.NettyEventLoopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认服务器，专注于链接的管理。尽量保持代码的专一
 * <p>
 * 这两个抽象什么用
 *
 * @author ZJP
 * @since 2021年08月13日 14:56:18
 **/
@Deprecated
public class DefaultServer implements AutoCloseable {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private String ip;
  private int port;

  /**
   * channelFuture of netty
   */
  private ChannelFuture channelFuture;
  /**
   * connection handler
   */
  private ChannelHandler handler;
  /** codec */
  private Serializer<?> serializer;
  /** 链接管理 */
  private ConnectionManager connectionManager;

  public DefaultServer() {
    this(0);
  }

  public DefaultServer(int port) {
    this(new InetSocketAddress(port).getAddress().getHostAddress(), port);
  }

  public DefaultServer(String ip, int port) {
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException(
          String.format("Illegal port value: %d, which should between 0 and 65535.", port));
    }
    this.ip = ip;
    this.port = port;
    connectionManager = new ConnectionManager();
  }

  /**
   * 开始服务器
   *
   * @param threadCommonResource 线程资源(线程由外部管理)
   * @since 2021年08月17日 17:39:02
   */
  public boolean start(ThreadCommonResource threadCommonResource) throws InterruptedException {
    Objects.requireNonNull(handler, "connection can't be null");

    final DefaultServer server = this;
    ServerBootstrap b = new ServerBootstrap();
    b.option(ChannelOption.SO_BACKLOG, 1024);
    b.group(threadCommonResource.getBoss(), threadCommonResource.getWorker())
        .channel(NettyEventLoopUtil.getServerSocketChannelClass())
        .option(ChannelOption.SO_REUSEADDR, true).handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer<>() {
          @Override
          protected void initChannel(Channel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            if (server.connectionManager() != null) {
              pipeline.addLast("idleStateHandler",
                  new IdleStateHandler(0, 0, ConnectionManager.IDLE_TIME, TimeUnit.MILLISECONDS));
              pipeline.addLast("manager", server.connectionManager());
            }
            pipeline.addLast("codec", new MessageCodec(server.serializer()));
            pipeline.addLast("handler", server.handler());
          }
        });

    channelFuture = b.bind(new InetSocketAddress(ip, port)).sync();
    if (port == 0 && channelFuture.isSuccess()) {
      InetSocketAddress address = (InetSocketAddress) channelFuture.channel().localAddress();
      port = address.getPort();
      logger.info("rpc server start with random port: {}!", port);
    }
    return channelFuture.isSuccess();
  }

  public String ip() {
    return ip;
  }

  public int port() {
    return port;
  }

  public Channel channel() {
    return channelFuture.channel();
  }

  public ChannelHandler handler() {
    return handler;
  }

  public DefaultServer handler(ChannelHandler handler) {
    this.handler = handler;
    return this;
  }

  public Serializer<?> serializer() {
    return serializer;
  }

  public DefaultServer serializer(Serializer<?> codec) {
    this.serializer = codec;
    return this;
  }

  public ConnectionManager connectionManager() {
    return connectionManager;
  }

  @Override
  public void close() {
    connectionManager.close();

    if (channelFuture != null) {
      logger.info("rpcServer:{}:{}, closing", ip, port);
      channelFuture.channel().close().awaitUninterruptibly();
      logger.info("rpcServer:{}:{}, closed", ip, port);
    }

  }
}
