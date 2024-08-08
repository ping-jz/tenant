package org.example.common.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import org.example.util.NettyEventLoopUtil;

/**
 * 代理客户端(数据)
 *
 * @author ZJP
 * @since 2021年08月13日 14:56:18
 **/
public class ProxyClient implements AutoCloseable {

  /** 地址 */
  private String address;
  /** 端口 */
  private int port;
  /** 网络链接处理 */
  private ChannelHandler handler;
  /** 网络链接 */
  private Channel channel;

  public ProxyClient() {
  }

  //这个搬到Service那里去
  public ChannelFuture connect(EventLoopGroup eventExecutors) {
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(eventExecutors).channel(NettyEventLoopUtil.getClientSocketChannelClass())
        .option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.SO_KEEPALIVE, true).handler(handler);

    ChannelFuture channelFuture = bootstrap.connect(address, port).syncUninterruptibly();
    channel = channelFuture.channel();
    return channelFuture;
  }

  public ChannelHandler handler() {
    return handler;
  }

  public ProxyClient handler(ChannelHandler handler) {
    this.handler = handler;
    return this;
  }

  public Channel getChannel() {
    return channel;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public boolean isActive() {
    return channel != null && channel.isActive();
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public ChannelHandler getHandler() {
    return handler;
  }

  public void setHandler(ChannelHandler handler) {
    this.handler = handler;
  }

  @Override
  public void close() {
    if (channel != null) {
      channel.close();
    }
  }


}
