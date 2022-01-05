package org.example.net.server;

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
import org.example.net.BaseRemoting;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.example.net.InvokeCallback;
import org.example.net.InvokeFuture;
import org.example.net.Message;
import org.example.net.MessageIdGenerator;
import org.example.net.codec.MessageCodec;
import org.example.serde.Serializer;
import org.example.util.NettyEventLoopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server for rpc
 *
 * @author ZJP
 * @since 2021年08月13日 14:56:18
 **/
public class ReqServer implements AutoCloseable {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private String ip;
  private int port;

  /** channelFuture of netty */
  private ChannelFuture channelFuture;
  /** connection handler */
  private ChannelHandler handler;
  /** codec */
  private Serializer<?> serializer;
  /** 链接管理 */
  private ConnectionManager connectionManager;
  /** 调用逻辑 */
  private BaseRemoting remoting;

  public ReqServer() {
    this(0);
  }

  public ReqServer(int port) {
    this(new InetSocketAddress(port).getAddress().getHostAddress(), port);
  }

  public ReqServer(String ip, int port) {
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException(String.format(
          "Illegal port value: %d, which should between 0 and 65535.", port));
    }
    this.ip = ip;
    this.port = port;
    connectionManager = new ConnectionManager(true);
    remoting = new BaseRemoting();
  }


  /**
   * send a oneway message(no response, just push the message to the remote)
   *
   * @author ZJP
   * @since 2021年08月14日 20:53:14
   **/
  public void invoke(String addr, Message push) {
    Connection connection = getConnection(addr);
    if (connection != null) {
      remoting.invoke(connection, push);
    } else {
      logger.error("address:{} is not connected", addr);
    }
  }

  /**
   * Rpc invocation with future returned.<br>
   *
   * @param addr 目标地址
   * @param message 请求消息
   * @param timeout 超时时间
   * @since 2021年08月15日 15:45:03
   */
  public InvokeFuture invokeWithFuture(String addr, Message message, long timeout) {
    Connection connection = getConnection(addr);
    if (connection != null) {
      message.msgId(MessageIdGenerator.nextId());
      return remoting.invoke(connection, message, timeout);
    } else {
      logger.error("address:{} is not connected", addr);
    }

    return null;
  }

  /**
   * Rpc invocation with future returned.<br>
   *
   * @param addr 目标地址
   * @param message 请求消息
   * @param timeout 超时时间
   * @param callback 回调
   * @since 2021年08月15日 15:45:03
   */
  public void invokeWithCallBack(String addr, Message message, InvokeCallback<?> callback,
      long timeout) {
    Connection connection = getConnection(addr);
    if (connection != null) {
      message.msgId(MessageIdGenerator.nextId());
      remoting.invokeWithCallBack(connection, message, callback, timeout);
    } else {
      logger.error("address:{} is not connected", addr);
    }
  }

  /**
   * 获取链接
   *
   * @param addr A address like this 127.0.0.1:8080 or /127.0.0.1:8080
   * @since 2021年08月17日 17:42:20
   */
  public Connection getConnection(String addr) {
    return connectionManager.connections().get(addr);
  }

  /**
   * 开始服务器
   *
   * @param threadCommonResource 线程资源(线程由外部管理)
   * @since 2021年08月17日 17:39:02
   */
  public boolean start(ThreadCommonResource threadCommonResource)
      throws InterruptedException {
    Objects.requireNonNull(handler, "connection can't be null");

    final ReqServer server = this;
    ServerBootstrap b = new ServerBootstrap();
    b.option(ChannelOption.SO_BACKLOG, 1024);
    b.group(threadCommonResource.getBoss(), threadCommonResource.getWorker())
        .channel(NettyEventLoopUtil.getServerSocketChannelClass())
        .option(ChannelOption.SO_REUSEADDR, true)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer<>() {
          @Override
          protected void initChannel(Channel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            if (server.connectionManager() != null) {
              pipeline.addLast("idleStateHandler",
                  new IdleStateHandler(0, 0, ConnectionManager.IDLE_TIME,
                      TimeUnit.MILLISECONDS));
              pipeline.addLast("manager", server.connectionManager());
            }
            pipeline.addLast("codec", new MessageCodec(server.codec()));
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

  public ReqServer handler(ChannelHandler handler) {
    this.handler = handler;
    return this;
  }

  public Serializer codec() {
    return serializer;
  }

  public ReqServer codec(Serializer codec) {
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
