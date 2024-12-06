package org.example.common.handler;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.example.net.ConnectionManager;

@Sharable
public class ConnectionManagerHandler extends ChannelInboundHandlerAdapter {

  private ConnectionManager connectionManager;

  public ConnectionManagerHandler(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    connectionManager.anoymousChannel(ctx.channel());
    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    connectionManager.channelInactive(ctx.channel());
    ctx.fireChannelInactive();
  }

  public ConnectionManager getConnectionManager() {
    return connectionManager;
  }
}
