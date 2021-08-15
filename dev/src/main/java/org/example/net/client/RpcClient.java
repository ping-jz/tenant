package org.example.net.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.example.net.BaseRemoting;
import org.example.net.Connection;
import org.example.net.DefaultRemoting;
import org.example.net.InvokeFuture;
import org.example.net.Message;
import org.example.net.MessageIdGenerator;
import org.example.net.ServerIdleHandler;
import org.example.net.codec.MessageCodec;
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

  private Logger logger = LoggerFactory.getLogger(getClass());

  /** connection manager */
  private Map<String, Connection> connections;

  /** client bootstrap */
  private Bootstrap bootstrap;
  /** connection handler */
  private ChannelHandler handler;

  /** 请求实现 */
  private BaseRemoting remoting;

  public RpcClient() {
    connections = new ConcurrentHashMap<>();
    remoting = new DefaultRemoting();
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
        return new Connection(future.channel(), addr);
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

  /**
   * Channel处理器
   *
   * @author ZJP
   * @since 2021年08月13日 21:41:18
   **/
  public static class ClientHandlerInitializer extends ChannelInitializer<SocketChannel> {

    private MessageCodec codec;
    private ChannelHandler hearBeatHander;

    private ChannelHandler handler;

    public ClientHandlerInitializer(ChannelHandler handler) {
      this.handler = handler;
    }

    public ChannelHandler handler() {
      return handler;
    }

    public ClientHandlerInitializer handler(ChannelHandler handler) {
      this.handler = handler;
      return this;
    }

    public ChannelHandler hearBeatHander() {
      return hearBeatHander;
    }

    public ClientHandlerInitializer hearBeatHander(ChannelHandler hearBeatHander) {
      this.hearBeatHander = hearBeatHander;
      return this;
    }

    public MessageCodec codec() {
      return codec;
    }

    public ClientHandlerInitializer codec(MessageCodec codec) {
      this.codec = codec;
      return this;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
      Objects.requireNonNull(codec, "codec Handler can't not be null");

      ChannelPipeline pipeline = ch.pipeline();
      if (hearBeatHander != null) {
        pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, ServerIdleHandler.IDLE_TIME,
            TimeUnit.MILLISECONDS));
        pipeline.addLast("hearBeat", hearBeatHander);
      }
      pipeline.addLast("codec", codec);
      pipeline.addLast("handler", handler);
    }
  }
}
