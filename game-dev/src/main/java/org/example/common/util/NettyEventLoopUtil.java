package org.example.common.util;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public final class NettyEventLoopUtil {

  private NettyEventLoopUtil() {
  }

  /**
   * @return a SocketChannel class suitable for the given EventLoopGroup implementation
   */
  public static Class<? extends SocketChannel> getClientSocketChannelClass() {
    return Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class;
  }

  /**
   * @return a ServerSocketChannel class suitable for the given EventLoopGroup implementation
   */
  public static Class<? extends ServerSocketChannel> getServerSocketChannelClass() {
    return Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
  }
}
