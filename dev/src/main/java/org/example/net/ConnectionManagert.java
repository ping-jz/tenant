package org.example.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 链接管理者
 *
 * @author ZJP
 * @since 2021年08月15日 22:34:55
 **/
@Sharable
public class ConnectionManagert extends ChannelInboundHandlerAdapter {

  private ConcurrentHashMap<String, Connection> connections;

  public ConnectionManagert() {
    connections = new ConcurrentHashMap<>();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();
    InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
    String addressStr = socketAddress.toString();

    Connection connection = new Connection(channel, addressStr);
    channel.attr(Connection.CONNECTION).set(connection);
    connections.put(connection.address(), connection);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();
    Connection connection = channel.attr(Connection.CONNECTION).getAndSet(null);
    if (connection == null) {
      return;
    }

    connections.remove(connection.address());
    connection.close();
  }
}
