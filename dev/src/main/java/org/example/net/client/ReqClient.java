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
public class ReqClient implements AutoCloseable {

  private Logger logger = LoggerFactory.getLogger(getClass());

  /** 链接管理 */
  private ConnectionManager manager;

  /** client bootstrap */
  private Bootstrap bootstrap;
  /** connection handler */
  private ChannelHandler handler;
  /** codec */
  private Serializer<?> codec;

  /** 请求实现 */
  private BaseRemoting remoting;

  public ReqClient() {
    manager = new ConnectionManager();
    remoting = new BaseRemoting();
  }

  public void init(EventLoopGroup eventExecutors) {
    Objects.requireNonNull(handler, "connection handler can't be null");

    ReqClient client = this;
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


  /**
   * send a oneway message(no response, just push the message to the remote)
   *
   * @author ZJP
   * @since 2021年08月14日 20:53:14
   **/
  public void invoke(String addr, Message push) {
    remoting.invoke(getConnection(addr), push);
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
    message.msgId(MessageIdGenerator.nextId());
    return remoting.invokeWithFuture(getConnection(addr), message, timeout);
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
    message.msgId(MessageIdGenerator.nextId());
    remoting.invokeWithCallBack(getConnection(addr), message, callback, timeout);
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

  public ReqClient handler(ChannelHandler handler) {
    this.handler = handler;
    return this;
  }

  public Serializer codec() {
    return codec;
  }

  public ReqClient codec(Serializer codec) {
    this.codec = codec;
    return this;
  }

  @Override
  public void close() {
    manager.close();
  }


}
