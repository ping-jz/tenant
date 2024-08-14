package org.example.net.handler;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.example.net.Dispatcher;
import org.example.net.Message;

/**
 * Message消息分发 //TODO 你可以增加多个Handler，来使用不同的Message
 *
 * @author zhongjianping
 * @since 2022/12/22 12:34
 */
@Sharable
public class DispatcherHandler extends SimpleChannelInboundHandler<Message> {

  private Dispatcher dispatcher;

  public DispatcherHandler(Dispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
    dispatcher.dispatcher(ctx.channel(), msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
    ctx.fireChannelReadComplete();
  }
}
