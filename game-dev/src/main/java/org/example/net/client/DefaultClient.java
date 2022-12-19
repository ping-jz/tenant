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
  private Serializer<?> serializer;

  public DefaultClient() {
    manager = new ConnectionManager();
  }

  public void init(EventLoopGroup eventExecutors) {
    Objects.requireNonNull(handler, "connection handler can't be null");

    DefaultClient client = this;
    bootstrap = new Bootstrap();
    bootstrap.group(eventExecutors).channel(NettyEventLoopUtil.getClientSocketChannelClass())
        .option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.SO_KEEPALIVE, true).handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            Objects.requireNonNull(serializer, "codec Handler can't not be null");

            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("codec", new MessageCodec(client.serializer()));
            pipeline.addLast("handler", client.handler());
          }
        });
  }


  //创建链接和管理链接不该Client来管理，以后抽出去吧
  /**
   * @since 2021年08月13日 18:47:18
   */
  public Connection connection(String add, int port) {
    Connection connection = createConnection(add, port);
    manager.connections().put(connection.id(), connection);
    return connection;
  }


  private Connection createConnection(String ip, int port) {
    try {

      ChannelFuture future = bootstrap.connect(new InetSocketAddress(ip, port));
      boolean suc = future.awaitUninterruptibly().isSuccess();
      if (suc) {
        future.channel().pipeline().addLast("manager", manager);
        return new Connection(future.channel(), Connection.IdGenerator.incrementAndGet());
      } else {
        throw new RuntimeException(String.format("connect to %s:%s failed", ip, port));
      }
    } catch (Exception e) {
      throw new RuntimeException(String.format("create connection for %s:%s, error", ip, port));
    }

  }

  public ChannelHandler handler() {
    return handler;
  }

  public DefaultClient handler(ChannelHandler handler) {
    this.handler = handler;
    return this;
  }

  public Serializer<?> serializer() {
    return serializer;
  }

  public DefaultClient serializer(Serializer<?> codec) {
    this.serializer = codec;
    return this;
  }

  public ConnectionManager manager() {
    return manager;
  }

  @Override
  public void close() {
    manager.close();
  }


}
