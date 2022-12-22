package org.example.net.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.example.net.Connection;

/**
 * Connection构建
 *
 * @author zhongjianping
 * @since 2022/12/22 11:12
 */
@Sharable
public class ConnectionCreate extends ChannelInboundHandlerAdapter {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();
    if (channel.attr(Connection.CONNECTION).get() == null) {
      new Connection(ctx.channel(), Connection.IdGenerator.incrementAndGet());
    }
    ctx.fireChannelActive();
  }
}
